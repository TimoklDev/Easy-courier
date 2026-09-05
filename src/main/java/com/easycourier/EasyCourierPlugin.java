package com.easycourier;

import com.easycourier.data.TaskCatalog;
import com.easycourier.data.TaskStateReader;
import com.easycourier.model.ActiveTask;
import com.easycourier.model.BoardOffer;
import com.easycourier.model.CollectionStop;
import com.easycourier.model.GangplankGuidance;
import com.easycourier.model.OfferStatus;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePhase;
import com.easycourier.model.RoutePlan;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.RouteStep;
import com.easycourier.model.Shipwright;
import com.easycourier.model.StepKind;
import com.easycourier.model.TaskDefinition;
import com.easycourier.model.TaskEdge;
import com.easycourier.overlay.CargoItemOverlay;
import com.easycourier.overlay.CharterCrewmemberOverlay;
import com.easycourier.overlay.DockOverlay;
import com.easycourier.overlay.EtceteriaShortcutOverlay;
import com.easycourier.overlay.GangplankOverlay;
import com.easycourier.overlay.InfoPanelOverlay;
import com.easycourier.overlay.NoticeBoardOverlay;
import com.easycourier.overlay.NoticeBoardWorldOverlay;
import com.easycourier.overlay.PortalRangeOverlay;
import com.easycourier.overlay.RouteMapOverlay;
import com.easycourier.overlay.RouteWorldOverlay;
import com.easycourier.overlay.ShipwrightOverlay;
import com.easycourier.service.CargoGuidance;
import com.easycourier.service.EtceteriaShortcutRoute;
import com.easycourier.service.ExperienceSession;
import com.easycourier.service.PortDetector;
import com.easycourier.service.RouteAdvisor;
import com.easycourier.service.RoutePlanner;
import com.easycourier.service.SeaNetwork;
import com.easycourier.service.ShipwrightLocator;
import com.easycourier.ui.EasyCourierPanel;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Easy Courier",
	description = "Guides two-phase Sailing courier collection and delivery routes",
	tags = {"sailing", "courier", "port", "tasks", "cargo", "route"}
)
public class EasyCourierPlugin extends Plugin
{
	private static final Color CHAT_LABEL_COLOR = new Color(0, 55, 130);
	private static final Color CHAT_MESSAGE_COLOR = new Color(0, 82, 180);
	private static final Color CHAT_CARGO_READY_COLOR = new Color(0, 200, 83);
	private static final Set<Integer> TASK_VARBITS = new HashSet<>();

	static
	{
		Collections.addAll(TASK_VARBITS,
			VarbitID.PORT_TASK_SLOT_0_ID,
			VarbitID.PORT_TASK_SLOT_0_CARGO_TAKEN,
			VarbitID.PORT_TASK_SLOT_0_CARGO_DELIVERED,
			VarbitID.PORT_TASK_SLOT_1_ID,
			VarbitID.PORT_TASK_SLOT_1_CARGO_TAKEN,
			VarbitID.PORT_TASK_SLOT_1_CARGO_DELIVERED,
			VarbitID.PORT_TASK_SLOT_2_ID,
			VarbitID.PORT_TASK_SLOT_2_CARGO_TAKEN,
			VarbitID.PORT_TASK_SLOT_2_CARGO_DELIVERED,
			VarbitID.PORT_TASK_SLOT_3_ID,
			VarbitID.PORT_TASK_SLOT_3_CARGO_TAKEN,
			VarbitID.PORT_TASK_SLOT_3_CARGO_DELIVERED,
			VarbitID.PORT_TASK_SLOT_4_ID,
			VarbitID.PORT_TASK_SLOT_4_CARGO_TAKEN,
			VarbitID.PORT_TASK_SLOT_4_CARGO_DELIVERED);
	}

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private EasyCourierConfig config;
	@Inject
	private NoticeBoardOverlay noticeBoardOverlay;
	@Inject
	private NoticeBoardWorldOverlay noticeBoardWorldOverlay;
	@Inject
	private DockOverlay dockOverlay;
	@Inject
	private CargoItemOverlay cargoItemOverlay;
	@Inject
	private CharterCrewmemberOverlay charterCrewmemberOverlay;
	@Inject
	private EtceteriaShortcutOverlay etceteriaShortcutOverlay;
	@Inject
	private RouteMapOverlay routeMapOverlay;
	@Inject
	private RouteWorldOverlay routeWorldOverlay;
	@Inject
	private PortalRangeOverlay portalRangeOverlay;
	@Inject
	private InfoPanelOverlay infoPanelOverlay;
	@Inject
	private ShipwrightOverlay shipwrightOverlay;
	@Inject
	private GangplankOverlay gangplankOverlay;

	private final TaskCatalog catalog = new TaskCatalog();
	private final TaskStateReader stateReader = new TaskStateReader();
	private final RouteAdvisor advisor = new RouteAdvisor();
	private final PortDetector portDetector = new PortDetector();
	private final ExperienceSession experienceSession = new ExperienceSession();
	private final SeaNetwork seaNetwork = new SeaNetwork();
	private final RoutePlanner planner = new RoutePlanner(seaNetwork);
	private final ShipwrightLocator shipwrightLocator = new ShipwrightLocator(seaNetwork);
	private final List<ActiveTask> activeTasks = new ArrayList<>();
	private final List<BoardOffer> boardOffers = new ArrayList<>();
	private final Set<GameObject> ledgers = new HashSet<>();
	private final Set<GameObject> noticeBoards = new HashSet<>();
	private final Set<GameObject> dodgePortals = new HashSet<>();
	private final Set<GameObject> etceteriaSteppingStones = new HashSet<>();
	private final Set<TileObject> gangplanks = new HashSet<>();
	private final Set<Port> pickupAnnouncements = EnumSet.noneOf(Port.class);
	private final Set<Port> deliveryAnnouncements = EnumSet.noneOf(Port.class);
	private final Set<Port> deliveryBoardsChecked = EnumSet.noneOf(Port.class);

	private EasyCourierPanel panel;
	private NavigationButton navigationButton;
	private RoutePreset selectedRoute;
	private RoutePhase phase = RoutePhase.IDLE;
	private int collectionIndex;
	private int deliverySkipCount;
	private int sailingLevel = 1;
	private int agilityLevel = 1;
	private int occupiedTaskSlots;
	private boolean fremennikTrialsComplete;
	private int experienceInitializationTicks;
	private boolean boardWasOpen;
	private Port openBoardPort = Port.UNKNOWN;
	private Port currentPort = Port.UNKNOWN;
	private Port lastKnownPort = Port.UNKNOWN;
	private RoutePlan routePlan;
	private RoutePlan collectionRoutePlan;
	private Shipwright collectionShipwright;
	private Port collectionRouteStart = Port.UNKNOWN;
	private Port collectionFirstCargoPort = Port.UNKNOWN;
	private Port forcedDeliveryStart = Port.UNKNOWN;

	@Override
	protected void startUp()
	{
		resetExperienceSession();
		experienceInitializationTicks = client.getGameState() == GameState.LOGGED_IN ? 0 : 2;
		selectedRoute = config.defaultRoute();
		restoreLastKnownPort();
		panel = new EasyCourierPanel(this);
		navigationButton = NavigationButton.builder()
			.tooltip("Easy Courier")
			.icon(ImageUtil.loadImageResource(getClass(), "icon.png"))
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(noticeBoardOverlay);
		overlayManager.add(noticeBoardWorldOverlay);
		overlayManager.add(dockOverlay);
		overlayManager.add(cargoItemOverlay);
		overlayManager.add(charterCrewmemberOverlay);
		overlayManager.add(etceteriaShortcutOverlay);
		overlayManager.add(routeMapOverlay);
		overlayManager.add(routeWorldOverlay);
		overlayManager.add(portalRangeOverlay);
		overlayManager.add(infoPanelOverlay);
		overlayManager.add(shipwrightOverlay);
		overlayManager.add(gangplankOverlay);
		clientThread.invokeLater(this::loadGameData);
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(noticeBoardOverlay);
		overlayManager.remove(noticeBoardWorldOverlay);
		overlayManager.remove(dockOverlay);
		overlayManager.remove(cargoItemOverlay);
		overlayManager.remove(charterCrewmemberOverlay);
		overlayManager.remove(etceteriaShortcutOverlay);
		overlayManager.remove(routeMapOverlay);
		overlayManager.remove(routeWorldOverlay);
		overlayManager.remove(portalRangeOverlay);
		overlayManager.remove(infoPanelOverlay);
		overlayManager.remove(shipwrightOverlay);
		overlayManager.remove(gangplankOverlay);
		activeTasks.clear();
		boardOffers.clear();
		ledgers.clear();
		noticeBoards.clear();
		dodgePortals.clear();
		etceteriaSteppingStones.clear();
		gangplanks.clear();
		pickupAnnouncements.clear();
		deliveryAnnouncements.clear();
		deliveryBoardsChecked.clear();
		resetExperienceSession();
		panel = null;
		navigationButton = null;
		collectionRoutePlan = null;
		clearCollectionHandoff();
		forcedDeliveryStart = Port.UNKNOWN;
	}

	@Provides
	EasyCourierConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EasyCourierConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (isExperienceSessionBoundary(event.getGameState()))
		{
			resetExperienceSession();
			experienceInitializationTicks = 2;
			refreshPanel();
		}
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::loadGameData);
		}
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.HOPPING)
		{
			ledgers.clear();
			noticeBoards.clear();
			dodgePortals.clear();
			etceteriaSteppingStones.clear();
			gangplanks.clear();
		}
	}

	static boolean isExperienceSessionBoundary(GameState gameState)
	{
		return gameState == GameState.STARTING || gameState == GameState.LOGIN_SCREEN;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.SAILING)
		{
			if (client.getGameState() == GameState.LOGGED_IN && experienceInitializationTicks == 0)
			{
				if (experienceSession.isStarted())
				{
					experienceSession.update(event.getXp());
				}
				else
				{
					experienceSession.start(event.getXp());
				}
			}
			sailingLevel = client.getRealSkillLevel(Skill.SAILING);
			if (phase == RoutePhase.COLLECTION)
			{
				rebuildCollectionRoutePlan();
			}
			refreshBoardAdvice();
			refreshPanel();
		}
		else if (event.getSkill() == Skill.AGILITY)
		{
			agilityLevel = client.getRealSkillLevel(Skill.AGILITY);
			if (phase == RoutePhase.COLLECTION)
			{
				rebuildCollectionRoutePlan();
			}
			refreshPanel();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (TASK_VARBITS.contains(event.getVarbitId()))
		{
			clientThread.invokeLater(this::refreshTasks);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.PORT_TASK_BOARD)
		{
			boardWasOpen = true;
			clientThread.invokeLater(this::scanNoticeBoard);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		initializeExperienceSession();
		updateCurrentPort();
		Widget board = client.getWidget(InterfaceID.PortTaskBoard.CONTAINER);
		boolean boardOpen = board != null && !board.isHidden();
		if (boardWasOpen && !boardOpen)
		{
			Port closedBoardPort = openBoardPort == Port.UNKNOWN ? currentPort : openBoardPort;
			boardWasOpen = false;
			boardOffers.clear();
			advanceCollectionAfterBoard();
			advanceDeliveryAfterBoard(closedBoardPort);
			openBoardPort = Port.UNKNOWN;
			refreshPanel();
		}
		if (isCollectionHandoffActive() && isAboardBoat())
		{
			moveToDelivery();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (Port.fromLedgerObjectId(event.getGameObject().getId()) != Port.UNKNOWN)
		{
			ledgers.add(event.getGameObject());
		}
		if (Port.fromNoticeBoardObjectId(event.getGameObject().getId()) != Port.UNKNOWN)
		{
			noticeBoards.add(event.getGameObject());
		}
		if (event.getGameObject().getId() == ObjectID.MISC_DIARY_STEPPINGSTONE)
		{
			etceteriaSteppingStones.add(event.getGameObject());
		}
		if (isDodgePortal(event.getGameObject()))
		{
			dodgePortals.add(event.getGameObject());
		}
		trackGangplank(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		ledgers.remove(event.getGameObject());
		noticeBoards.remove(event.getGameObject());
		dodgePortals.remove(event.getGameObject());
		etceteriaSteppingStones.remove(event.getGameObject());
		gangplanks.remove(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		trackGangplank(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		gangplanks.remove(event.getWallObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		trackGangplank(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		gangplanks.remove(event.getGroundObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		trackGangplank(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		gangplanks.remove(event.getDecorativeObject());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (EasyCourierConfig.GROUP.equals(event.getGroup()) && "defaultRoute".equals(event.getKey())
			&& phase == RoutePhase.IDLE)
		{
			selectedRoute = config.defaultRoute();
			refreshPanel();
		}
	}

	public void selectRoute(RoutePreset route)
	{
		if (route == null)
		{
			return;
		}
		if (sailingLevel < route.getMinimumLevel())
		{
			sendMessage("This route requires Sailing level " + route.getMinimumLevel() + ".");
			refreshPanel();
			return;
		}
		selectedRoute = route;
		configManager.setConfiguration(EasyCourierConfig.GROUP, "defaultRoute", route);
		if (phase == RoutePhase.IDLE || phase == RoutePhase.COLLECTION || phase == RoutePhase.COMPLETE)
		{
			beginCollection();
			return;
		}
		refreshPanel();
	}

	public void beginCollection()
	{
		if (sailingLevel < selectedRoute.getMinimumLevel())
		{
			sendMessage("This route requires Sailing level " + selectedRoute.getMinimumLevel() + ".");
			return;
		}
		phase = RoutePhase.COLLECTION;
		collectionIndex = 0;
		deliverySkipCount = 0;
		routePlan = null;
		collectionRoutePlan = null;
		boardOffers.clear();
		deliveryBoardsChecked.clear();
		forcedDeliveryStart = Port.UNKNOWN;
		skipUnavailableCollectionStops();
		rebuildCollectionRoutePlan();
		refreshBoardAdvice();
		refreshPanel();
	}

	public void moveToDelivery()
	{
		forcedDeliveryStart = isCollectionHandoffActive() ? collectionRouteStart : Port.UNKNOWN;
		phase = RoutePhase.DELIVERY;
		deliverySkipCount = 0;
		clearCollectionHandoff();
		rebuildRoutePlan();
		refreshBoardAdvice();
		refreshPanel();
	}

	public void skipStep()
	{
		if (phase == RoutePhase.COLLECTION)
		{
			collectionIndex++;
			skipUnavailableCollectionStops();
			rebuildCollectionRoutePlan();
			if (collectionIndex >= selectedRoute.getCollectionStops().size())
			{
				refreshPanel();
				return;
			}
			refreshBoardAdvice();
		}
		else if (phase == RoutePhase.DELIVERY && routePlan != null)
		{
			int index = Math.min(deliverySkipCount, routePlan.getSteps().size() - 1);
			if (index >= 0 && routePlan.getSteps().get(index).getKind() == StepKind.TRAVEL
				&& routePlan.getSteps().get(index).getPort() == forcedDeliveryStart)
			{
				forcedDeliveryStart = Port.UNKNOWN;
			}
			if (index >= 0 && routePlan.getSteps().get(index).getKind() == StepKind.NOTICE_BOARD)
			{
				deliveryBoardsChecked.add(routePlan.getSteps().get(index).getPort());
				rebuildRoutePlan();
				deliverySkipCount = 0;
			}
			else
			{
				deliverySkipCount = Math.min(deliverySkipCount + 1, routePlan.getSteps().size());
			}
		}
		refreshPanel();
	}

	public void resetRoute()
	{
		beginCollection();
	}

	private void loadGameData()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		catalog.load(client);
		scanSceneObjects();
		int sailingExperience = client.getSkillExperience(Skill.SAILING);
		if (experienceSession.isStarted())
		{
			experienceSession.update(sailingExperience);
		}
		else if (experienceInitializationTicks == 0)
		{
			experienceSession.start(sailingExperience);
		}
		sailingLevel = client.getRealSkillLevel(Skill.SAILING);
		agilityLevel = client.getRealSkillLevel(Skill.AGILITY);
		fremennikTrialsComplete = Quest.THE_FREMENNIK_TRIALS.getState(client) == QuestState.FINISHED;
		updateCurrentPort();
		refreshTasks();
		if (phase == RoutePhase.IDLE && !activeTasks.isEmpty())
		{
			selectedRoute = detectRoute();
			if (currentPort == Port.UNKNOWN
				&& (isAboardBoat() || selectedRoute.routeRank(lastKnownPort) < 0))
			{
				Port inferred = inferProgressPort();
				if (inferred != Port.UNKNOWN)
				{
					lastKnownPort = inferred;
				}
			}
			phase = RoutePhase.DELIVERY;
			deliverySkipCount = 0;
			deliveryBoardsChecked.clear();
			rebuildRoutePlan();
			refreshBoardAdvice();
			refreshPanel();
		}
		else if (phase == RoutePhase.IDLE)
		{
			beginCollection();
		}
	}

	private void refreshTasks()
	{
		Port expectedFirstCargo = collectionFirstCargoPort;
		boolean handoffWasActive = isCollectionHandoffActive();
		List<ActiveTask> previous = new ArrayList<>(activeTasks);
		activeTasks.clear();
		activeTasks.addAll(stateReader.read(client, catalog));
		occupiedTaskSlots = stateReader.countOccupied(client);
		updateAnnouncements(previous);
		if (phase == RoutePhase.DELIVERY)
		{
			rebuildRoutePlan();
			deliverySkipCount = 0;
		}
		else if (phase == RoutePhase.COLLECTION)
		{
			boolean firstCargoTaken = handoffWasActive
				&& cargoTakenAt(previous, expectedFirstCargo);
			rebuildCollectionRoutePlan();
			if (firstCargoTaken || (isCollectionHandoffActive() && isAboardBoat()))
			{
				moveToDelivery();
				return;
			}
		}
		refreshBoardAdvice();
		refreshPanel();
	}

	private void updateAnnouncements(List<ActiveTask> previous)
	{
		for (Port port : Port.values())
		{
			if (port == Port.UNKNOWN)
			{
				continue;
			}
			List<ActiveTask> pickupTasks = new ArrayList<>();
			for (ActiveTask task : activeTasks)
			{
				if (task.getDefinition().getPickup() == port)
				{
					pickupTasks.add(task);
				}
			}
			boolean pickupComplete = !pickupTasks.isEmpty() && pickupTasks.stream().allMatch(task -> !task.needsPickup());
			if (!pickupComplete)
			{
				pickupAnnouncements.remove(port);
			}
			else if (pickupAnnouncements.add(port) && wasIncompletePickup(previous, port))
			{
				sendMessage("You now have all the cargo", CHAT_CARGO_READY_COLOR);
			}
			List<ActiveTask> deliveryTasks = new ArrayList<>();
			for (ActiveTask task : activeTasks)
			{
				if (task.getDefinition().getDelivery() == port)
				{
					deliveryTasks.add(task);
				}
			}
			boolean deliveryComplete = !deliveryTasks.isEmpty() && deliveryTasks.stream().allMatch(ActiveTask::isComplete);
			if (!deliveryComplete)
			{
				deliveryAnnouncements.remove(port);
			}
			else if (deliveryAnnouncements.add(port) && wasIncompleteDelivery(previous, port))
			{
				sendMessage("You delivered all cargo for this dock");
			}
		}
	}

	private boolean wasIncompletePickup(List<ActiveTask> previous, Port port)
	{
		return previous.stream().anyMatch(task -> task.getDefinition().getPickup() == port && task.needsPickup());
	}

	private boolean wasIncompleteDelivery(List<ActiveTask> previous, Port port)
	{
		return previous.stream().anyMatch(task -> task.getDefinition().getDelivery() == port && task.needsDelivery());
	}

	private boolean cargoTakenAt(List<ActiveTask> previous, Port port)
	{
		if (port == Port.UNKNOWN)
		{
			return false;
		}
		for (ActiveTask task : activeTasks)
		{
			if (task.getDefinition().getPickup() != port)
			{
				continue;
			}
			for (ActiveTask oldTask : previous)
			{
				if (oldTask.getSlot() == task.getSlot() && task.getCargoTaken() > oldTask.getCargoTaken())
				{
					return true;
				}
			}
		}
		return false;
	}

	private void scanNoticeBoard()
	{
		boardOffers.clear();
		if (phase != RoutePhase.COLLECTION && phase != RoutePhase.DELIVERY)
		{
			refreshPanel();
			return;
		}
		if (phase == RoutePhase.COLLECTION && collectionIndex >= selectedRoute.getCollectionStops().size()
			&& !isCollectionHandoffActive())
		{
			refreshPanel();
			return;
		}
		Widget container = client.getWidget(InterfaceID.PortTaskBoard.CONTAINER);
		if (container == null)
		{
			return;
		}
		Widget[] children = container.getDynamicChildren();
		if (children == null)
		{
			return;
		}
		List<RouteAdvisor.WidgetTask> tasks = new ArrayList<>();
		for (Widget child : children)
		{
			Integer databaseRow = databaseRow(child);
			if (databaseRow != null)
			{
				TaskDefinition task = catalog.byDatabaseRow(databaseRow);
				tasks.add(new RouteAdvisor.WidgetTask(child, task));
				if (task != null && task.getNoticeBoard() != Port.UNKNOWN)
				{
					openBoardPort = task.getNoticeBoard();
				}
			}
		}
		Port boardPort = openBoardPort == Port.UNKNOWN ? currentPort : openBoardPort;
		RoutePhase advicePhase = phase == RoutePhase.COLLECTION && isCollectionHandoffBoard(boardPort)
			? RoutePhase.DELIVERY : phase;
		boardOffers.addAll(advisor.advise(selectedRoute, advicePhase, boardPort, collectionIndex, sailingLevel, occupiedTaskSlots,
			activeTasks, tasks));
		refreshPanel();
	}

	private void refreshBoardAdvice()
	{
		Widget board = client.getWidget(InterfaceID.PortTaskBoard.CONTAINER);
		if (board != null && !board.isHidden())
		{
			clientThread.invokeLater(this::scanNoticeBoard);
		}
	}

	private Integer databaseRow(Widget widget)
	{
		Object[] listener = widget.getOnOpListener();
		if (listener == null || listener.length < 4 || !(listener[3] instanceof Integer))
		{
			return null;
		}
		return (Integer) listener[3];
	}

	private void advanceCollectionAfterBoard()
	{
		if (phase != RoutePhase.COLLECTION)
		{
			return;
		}
		if (isCollectionHandoffBoard(openBoardPort))
		{
			deliveryBoardsChecked.add(openBoardPort);
			return;
		}
		if (collectionIndex >= selectedRoute.getCollectionStops().size())
		{
			return;
		}
		CollectionStop stop = selectedRoute.getCollectionStops().get(collectionIndex);
		if (currentPort == stop.getPort() || openBoardPort == stop.getPort())
		{
			collectionIndex++;
			skipUnavailableCollectionStops();
			rebuildCollectionRoutePlan();
		}
	}

	private void resetExperienceSession()
	{
		experienceSession.reset();
	}

	private void initializeExperienceSession()
	{
		if (client.getGameState() != GameState.LOGGED_IN || experienceInitializationTicks <= 0)
		{
			return;
		}
		experienceInitializationTicks--;
		if (experienceInitializationTicks == 0)
		{
			experienceSession.start(client.getSkillExperience(Skill.SAILING));
			refreshPanel();
		}
	}

	private void scanSceneObjects()
	{
		dodgePortals.clear();
		etceteriaSteppingStones.clear();
		gangplanks.clear();
		WorldView topLevel = client.getTopLevelWorldView();
		if (topLevel == null)
		{
			return;
		}
		scanWorldViewObjects(topLevel, new HashSet<>());
	}

	private void scanWorldViewObjects(WorldView worldView, Set<Integer> scannedViews)
	{
		if (worldView == null || worldView.getScene() == null || !scannedViews.add(worldView.getId()))
		{
			return;
		}
		Tile[][][] tiles = worldView.getScene().getTiles();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject object : tile.getGameObjects())
					{
						if (object == null)
						{
							continue;
						}
						if (isDodgePortal(object))
						{
							dodgePortals.add(object);
						}
						if (object.getId() == ObjectID.MISC_DIARY_STEPPINGSTONE)
						{
							etceteriaSteppingStones.add(object);
						}
						trackGangplank(object);
					}
					trackGangplank(tile.getWallObject());
					trackGangplank(tile.getGroundObject());
					trackGangplank(tile.getDecorativeObject());
				}
			}
		}
		for (WorldView child : worldView.worldViews())
		{
			scanWorldViewObjects(child, scannedViews);
		}
	}

	private boolean isDodgePortal(GameObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition == null)
		{
			return false;
		}
		if (composition.getImpostorIds() != null)
		{
			composition = composition.getImpostor();
		}
		String name = composition == null ? null : composition.getName();
		return name != null && name.regionMatches(true, 0, "Portal of ", 0, 10);
	}

	private void trackGangplank(TileObject object)
	{
		if (object != null && isGangplank(object))
		{
			gangplanks.add(object);
		}
	}

	private boolean isGangplank(TileObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition == null)
		{
			return false;
		}
		if (composition.getImpostorIds() != null)
		{
			ObjectComposition impostor = composition.getImpostor();
			if (impostor != null)
			{
				composition = impostor;
			}
		}
		String name = composition.getName();
		return name != null && name.equalsIgnoreCase("Gangplank");
	}

	public boolean hasGangplankOption(TileObject object, String expectedOption)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition == null)
		{
			return false;
		}
		if (composition.getImpostorIds() != null)
		{
			ObjectComposition impostor = composition.getImpostor();
			if (impostor != null)
			{
				composition = impostor;
			}
		}
		String[] actions = composition.getActions();
		for (int index = 0; index < 5; index++)
		{
			String action = object.getOpOverride(index);
			if (action == null && actions != null && index < actions.length)
			{
				action = actions[index];
			}
			if (action != null && action.trim().equalsIgnoreCase(expectedOption))
			{
				return true;
			}
		}
		return false;
	}

	private void advanceDeliveryAfterBoard(Port port)
	{
		if (phase == RoutePhase.DELIVERY && port != Port.UNKNOWN)
		{
			deliveryBoardsChecked.add(port);
			rebuildRoutePlan();
			deliverySkipCount = 0;
		}
	}

	private void skipUnavailableCollectionStops()
	{
		while (collectionIndex < selectedRoute.getCollectionStops().size()
			&& sailingLevel < selectedRoute.getCollectionStops().get(collectionIndex).getMinimumLevel())
		{
			collectionIndex++;
		}
		skipToPersistentReservationStop();
	}

	private void skipToPersistentReservationStop()
	{
		TaskEdge reserved = selectedRoute.getPersistentReservedTask();
		if (reserved == null || collectionIndex == 0)
		{
			return;
		}
		List<ActiveTask> liveTasks = stateReader.read(client, catalog);
		if (liveTasks.stream().anyMatch(task -> reserved.matches(task.getDefinition())))
		{
			return;
		}
		int freeSlots = TaskStateReader.taskCapacity(sailingLevel) - stateReader.countOccupied(client);
		if (freeSlots > 1)
		{
			return;
		}
		Port target = selectedRoute.getPersistentReservationStop();
		for (int index = collectionIndex; index < selectedRoute.getCollectionStops().size(); index++)
		{
			if (selectedRoute.getCollectionStops().get(index).getPort() == target)
			{
				collectionIndex = index;
				return;
			}
		}
	}

	private void updateCurrentPort()
	{
		if (client.getLocalPlayer() == null)
		{
			currentPort = Port.UNKNOWN;
			return;
		}
		WorldPoint point = navigationWorldPoint();
		Port detected = portDetector.detect(point, getCurrentTravelStep(), isAboardBoat());
		if (detected != Port.UNKNOWN)
		{
			rememberPort(detected);
		}
		if (detected != currentPort)
		{
			currentPort = detected;
			if (phase == RoutePhase.COLLECTION)
			{
				rebuildCollectionRoutePlan();
			}
			else if (phase == RoutePhase.DELIVERY)
			{
				rebuildRoutePlan();
				deliverySkipCount = 0;
			}
			refreshPanel();
		}
	}

	private void rebuildRoutePlan()
	{
		Port routeStart = currentPort == Port.UNKNOWN ? lastKnownPort : currentPort;
		if (routeStart == forcedDeliveryStart)
		{
			forcedDeliveryStart = Port.UNKNOWN;
		}
		routePlan = forcedDeliveryStart == Port.UNKNOWN
			? planner.plan(selectedRoute, routeStart, activeTasks,
				TaskStateReader.taskCapacity(sailingLevel), deliveryBoardsChecked)
			: planner.planVia(selectedRoute, routeStart, forcedDeliveryStart, activeTasks,
				TaskStateReader.taskCapacity(sailingLevel), deliveryBoardsChecked);
		if (activeTasks.stream().allMatch(ActiveTask::isComplete)
			&& currentPort == selectedRoute.getFinish())
		{
			beginCollection();
		}
	}

	private void rebuildCollectionRoutePlan()
	{
		clearCollectionHandoff();
		if (phase != RoutePhase.COLLECTION)
		{
			return;
		}
		boolean full = isTaskListFull();
		boolean completedWithRecovery = collectionIndex >= selectedRoute.getCollectionStops().size();
		if (full || completedWithRecovery)
		{
			RoutePlan preview = planner.planFromBestTaskStart(selectedRoute, activeTasks,
				TaskStateReader.taskCapacity(sailingLevel));
			if (preview == null || preview.getPortOrder().isEmpty())
			{
				return;
			}
			Port preferredStart = preview.getPortOrder().get(0);
			collectionShipwright = shipwrightLocator.nearestTo(preferredStart, sailingLevel);
			if (collectionShipwright == null)
			{
				return;
			}
			collectionRouteStart = preferredStart;
			for (RouteStep step : preview.getSteps())
			{
				if (collectionFirstCargoPort == Port.UNKNOWN && step.getKind() == StepKind.PICKUP)
				{
					collectionFirstCargoPort = step.getPort();
				}
			}
			if (collectionShipwright.getPort() != collectionRouteStart)
			{
				collectionRoutePlan = planner.planLeg(collectionShipwright.getPort(), collectionRouteStart);
			}
			return;
		}
		if (collectionIndex >= selectedRoute.getCollectionStops().size())
		{
			return;
		}
		CollectionStop stop = selectedRoute.getCollectionStops().get(collectionIndex);
		if (!stop.isSailingLeg() || currentPort == stop.getPort())
		{
			return;
		}
		if (isEtceteriaShortcutActive())
		{
			return;
		}
		collectionRoutePlan = planner.planLeg(stop.getSailingStart(), stop.getPort());
	}

	private void clearCollectionHandoff()
	{
		collectionRoutePlan = null;
		collectionShipwright = null;
		collectionRouteStart = Port.UNKNOWN;
		collectionFirstCargoPort = Port.UNKNOWN;
	}

	private boolean isTaskListFull()
	{
		return occupiedTaskSlots >= TaskStateReader.taskCapacity(sailingLevel);
	}

	private WorldPoint navigationWorldPoint()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		WorldView view = player.getWorldView();
		WorldView topLevel = client.getTopLevelWorldView();
		if (view != null && !view.isTopLevel() && view.getId() != WorldView.TOPLEVEL && topLevel != null)
		{
			WorldEntity entity = topLevel.worldEntities().byIndex(view.getId());
			if (entity != null)
			{
				WorldPoint point = WorldPoint.fromLocalInstance(client, entity.getLocalLocation());
				if (point != null)
				{
					return point;
				}
			}
		}
		WorldPoint point = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		return point == null ? player.getWorldLocation() : point;
	}

	private boolean isAboardBoat()
	{
		Player player = client.getLocalPlayer();
		WorldView view = player == null ? null : player.getWorldView();
		return view != null && !view.isTopLevel() && view.getId() != WorldView.TOPLEVEL;
	}

	private Port inferProgressPort()
	{
		Port best = Port.UNKNOWN;
		int bestRank = -1;
		for (ActiveTask task : activeTasks)
		{
			if (task.getCargoTaken() > 0)
			{
				int rank = selectedRoute.routeRank(task.getDefinition().getPickup());
				if (rank > bestRank)
				{
					best = task.getDefinition().getPickup();
					bestRank = rank;
				}
			}
			if (task.getCargoDelivered() > 0)
			{
				int rank = selectedRoute.routeRank(task.getDefinition().getDelivery());
				if (rank > bestRank)
				{
					best = task.getDefinition().getDelivery();
					bestRank = rank;
				}
			}
		}
		if (best != Port.UNKNOWN)
		{
			rememberPort(best);
		}
		return best;
	}

	private void rememberPort(Port port)
	{
		if (port == Port.UNKNOWN || port == lastKnownPort)
		{
			return;
		}
		lastKnownPort = port;
		configManager.setConfiguration(EasyCourierConfig.GROUP, "lastDeliveryPort", port.name());
	}

	private void restoreLastKnownPort()
	{
		String value = configManager.getConfiguration(EasyCourierConfig.GROUP, "lastDeliveryPort");
		if (value == null)
		{
			return;
		}
		try
		{
			lastKnownPort = Port.valueOf(value);
		}
		catch (IllegalArgumentException ignored)
		{
			lastKnownPort = Port.UNKNOWN;
		}
	}

	private RoutePreset detectRoute()
	{
		RoutePreset best = selectedRoute;
		int bestScore = routeScore(best);
		for (RoutePreset route : RoutePreset.values())
		{
			int score = routeScore(route);
			if (score > bestScore)
			{
				best = route;
				bestScore = score;
			}
		}
		return best;
	}

	private int routeScore(RoutePreset route)
	{
		int score = 0;
		for (ActiveTask task : activeTasks)
		{
			TaskDefinition definition = task.getDefinition();
			if (route.movesForward(definition))
			{
				score += 2;
			}
			if (definition.getDelivery() == route.getFinish())
			{
				score += 4;
			}
			if (route.routeRank(definition.getPickup()) >= 0)
			{
				score++;
			}
		}
		return score;
	}

	private void sendMessage(String message)
	{
		sendMessage(message, CHAT_MESSAGE_COLOR);
	}

	private void sendMessage(String message, Color messageColor)
	{
		if (!config.chatUpdates())
		{
			return;
		}
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(CHAT_LABEL_COLOR, "Easy Courier: ")
				.append(messageColor, message)
				.build())
			.build());
	}

	private void refreshPanel()
	{
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::refresh);
		}
	}

	public List<BoardOffer> getBoardOffers()
	{
		return Collections.unmodifiableList(boardOffers);
	}

	public List<ActiveTask> getActiveTasks()
	{
		return Collections.unmodifiableList(activeTasks);
	}

	public Set<GameObject> getLedgers()
	{
		return Collections.unmodifiableSet(ledgers);
	}

	public Set<GameObject> getNoticeBoards()
	{
		return Collections.unmodifiableSet(noticeBoards);
	}

	public Set<GameObject> getDodgePortals()
	{
		return Collections.unmodifiableSet(dodgePortals);
	}

	public Set<GameObject> getEtceteriaSteppingStones()
	{
		return Collections.unmodifiableSet(etceteriaSteppingStones);
	}

	public Set<TileObject> getGangplanks()
	{
		return Collections.unmodifiableSet(gangplanks);
	}

	public RouteStep getCurrentDeliveryStep()
	{
		if (phase != RoutePhase.DELIVERY || routePlan == null || routePlan.getSteps().isEmpty())
		{
			return null;
		}
		int index = Math.min(deliverySkipCount, routePlan.getSteps().size() - 1);
		return routePlan.getSteps().get(index);
	}

	private RouteStep getCurrentTravelStep()
	{
		if (isEtceteriaShortcutActive())
		{
			return EtceteriaShortcutRoute.getTravelStep();
		}
		if (phase == RoutePhase.COLLECTION && collectionRoutePlan != null
			&& !collectionRoutePlan.getSteps().isEmpty())
		{
			return collectionRoutePlan.getSteps().get(0);
		}
		return getCurrentDeliveryStep();
	}

	public boolean isTravelStepActive()
	{
		RouteStep step = getCurrentTravelStep();
		return step != null && step.getKind() == StepKind.TRAVEL;
	}

	public RoutePlan getNavigationRoutePlan()
	{
		return phase == RoutePhase.COLLECTION ? collectionRoutePlan : routePlan;
	}

	public boolean isEtceteriaShortcutActive()
	{
		if (phase != RoutePhase.COLLECTION || isTaskListFull()
			|| collectionIndex >= selectedRoute.getCollectionStops().size())
		{
			return false;
		}
		CollectionStop stop = selectedRoute.getCollectionStops().get(collectionIndex);
		return EtceteriaShortcutRoute.isAvailable(selectedRoute, stop.getPort(), sailingLevel, agilityLevel,
			fremennikTrialsComplete);
	}

	public String getCollectionTravelInstruction(CollectionStop stop)
	{
		return EtceteriaShortcutRoute.isAvailable(selectedRoute, stop.getPort(), sailingLevel, agilityLevel,
			fremennikTrialsComplete) ? EtceteriaShortcutRoute.INSTRUCTION : stop.getTravelInstruction();
	}

	public boolean isCollectionHandoffActive()
	{
		if (phase != RoutePhase.COLLECTION || collectionShipwright == null || collectionRouteStart == Port.UNKNOWN)
		{
			return false;
		}
		return isTaskListFull() || collectionIndex >= selectedRoute.getCollectionStops().size();
	}

	public Shipwright getActiveShipwright()
	{
		return isCollectionHandoffActive() ? collectionShipwright : null;
	}

	public Port getCollectionRecoveryPort()
	{
		return isCollectionHandoffActive() ? collectionShipwright.getPort() : selectedRoute.getBoatRecoveryPort();
	}

	public Port getCollectionRouteStart()
	{
		return collectionRouteStart;
	}

	public Port getCollectionFirstCargoPort()
	{
		return collectionFirstCargoPort;
	}

	public boolean isCollectionHandoffCargoPort(Port port)
	{
		return canCollectFirstCargoAtRecovery() && port == collectionFirstCargoPort;
	}

	public String getCollectionHandoffTitle()
	{
		return canCollectFirstCargoAtRecovery()
			? "Recover your boat and/or grab your first cargo."
			: "Recover your boat and/or board it.";
	}

	public GangplankGuidance getGangplankGuidance()
	{
		if (phase != RoutePhase.DELIVERY && !isCollectionHandoffActive())
		{
			return GangplankGuidance.NONE;
		}
		Port port = currentPort;
		if (port == Port.UNKNOWN)
		{
			return GangplankGuidance.NONE;
		}
		boolean pickupNeeded = activeTasks.stream()
			.anyMatch(task -> task.getDefinition().getPickup() == port && task.needsPickup());
		boolean deliveryAvailable = activeTasks.stream()
			.anyMatch(task -> task.getDefinition().getDelivery() == port && task.canDeliver());
		boolean pickupCargoHeld = hasInventoryCargo(port, true);
		boolean deliveryCargoHeld = hasInventoryCargo(port, false);
		boolean handoffBoardingNeeded = isCollectionHandoffActive() && !canCollectFirstCargoAtRecovery();
		return CargoGuidance.gangplank(isAboardBoat(), handoffBoardingNeeded, pickupNeeded,
			pickupCargoHeld, deliveryAvailable, deliveryCargoHeld);
	}

	public boolean shouldHighlightPickupLedger(Port port)
	{
		if ((phase != RoutePhase.DELIVERY && !isCollectionHandoffActive()) || port != currentPort)
		{
			return false;
		}
		boolean pickupNeeded = activeTasks.stream()
			.anyMatch(task -> task.getDefinition().getPickup() == port && task.needsPickup());
		return CargoGuidance.pickupLedger(isAboardBoat(), pickupNeeded, hasInventoryCargo(port, true));
	}

	public boolean shouldHighlightDeliveryLedger(Port port)
	{
		if (phase != RoutePhase.DELIVERY || port != currentPort)
		{
			return false;
		}
		boolean deliveryAvailable = activeTasks.stream()
			.anyMatch(task -> task.getDefinition().getDelivery() == port && task.canDeliver());
		return CargoGuidance.deliveryLedger(isAboardBoat(), deliveryAvailable, hasInventoryCargo(port, false));
	}

	private boolean hasInventoryCargo(Port port, boolean pickup)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return false;
		}
		for (ActiveTask task : activeTasks)
		{
			boolean relevant = pickup
				? task.getDefinition().getPickup() == port
				: task.getDefinition().getDelivery() == port;
			int cargoItemId = task.getDefinition().getCargoItemId();
			if (relevant && task.needsDelivery() && cargoItemId > 0 && inventory.contains(cargoItemId))
			{
				return true;
			}
		}
		return false;
	}

	public String getCollectionHandoffDetail()
	{
		if (!isCollectionHandoffActive())
		{
			return "Move on to the delivery phase.";
		}
		Port recoveryPort = collectionShipwright.getPort();
		StringBuilder detail = new StringBuilder();
		if (currentPort != recoveryPort)
		{
			detail.append("Teleport to ").append(recoveryPort).append(". ");
		}
		if (canCheckCollectionHandoffBoard())
		{
			detail.append("Open the highlighted notice board, then ");
		}
		detail.append("speak to ").append(collectionShipwright.getNpcName()).append(" if needed");
		if (canCollectFirstCargoAtRecovery())
		{
			return detail.append(", collect the first cargo from the highlighted ledger, and board your boat.").toString();
		}
		return detail.append(", recover your boat, and board it. The delivery route begins toward ")
			.append(collectionRouteStart).append('.').toString();
	}

	private boolean canCollectFirstCargoAtRecovery()
	{
		return collectionShipwright != null
			&& collectionShipwright.getPort() == collectionRouteStart
			&& collectionShipwright.getPort() == collectionFirstCargoPort;
	}

	private boolean canCheckCollectionHandoffBoard()
	{
		return isCollectionHandoffActive()
			&& collectionShipwright.getPort().hasNoticeBoard()
			&& occupiedTaskSlots < TaskStateReader.taskCapacity(sailingLevel);
	}

	private boolean isCollectionHandoffBoard(Port port)
	{
		return isCollectionHandoffActive()
			&& port != Port.UNKNOWN
			&& port == collectionShipwright.getPort();
	}

	public Port getNoticeBoardTarget()
	{
		if (occupiedTaskSlots >= TaskStateReader.taskCapacity(sailingLevel))
		{
			return Port.UNKNOWN;
		}
		if (phase == RoutePhase.COLLECTION)
		{
			if (isCollectionHandoffActive() && collectionShipwright.getPort().hasNoticeBoard())
			{
				return collectionShipwright.getPort();
			}
			if (collectionIndex < selectedRoute.getCollectionStops().size())
			{
				return selectedRoute.getCollectionStops().get(collectionIndex).getPort();
			}
		}
		RouteStep step = getCurrentDeliveryStep();
		return step != null && step.getKind() == StepKind.NOTICE_BOARD ? step.getPort() : Port.UNKNOWN;
	}

	public Port getCharterTarget()
	{
		if (phase != RoutePhase.COLLECTION || isCollectionHandoffActive()
			|| collectionIndex >= selectedRoute.getCollectionStops().size())
		{
			return Port.UNKNOWN;
		}
		CollectionStop stop = selectedRoute.getCollectionStops().get(collectionIndex);
		return stop.isCharterRequired() && currentPort != stop.getPort() ? stop.getPort() : Port.UNKNOWN;
	}

	public List<ActiveTask> tasksAtPickup(Port port)
	{
		List<ActiveTask> result = new ArrayList<>();
		for (ActiveTask task : activeTasks)
		{
			if (task.getDefinition().getPickup() == port && task.needsPickup())
			{
				result.add(task);
			}
		}
		return result;
	}

	public List<ActiveTask> tasksAtDelivery(Port port)
	{
		List<ActiveTask> result = new ArrayList<>();
		for (ActiveTask task : activeTasks)
		{
			if (task.getDefinition().getDelivery() == port && task.needsDelivery())
			{
				result.add(task);
			}
		}
		return result;
	}

	public EasyCourierConfig getConfig()
	{
		return config;
	}

	public RoutePreset getSelectedRoute()
	{
		return selectedRoute;
	}

	public RoutePhase getPhase()
	{
		return phase;
	}

	public int getCollectionIndex()
	{
		return collectionIndex;
	}

	public int getDeliverySkipCount()
	{
		return deliverySkipCount;
	}

	public int getSailingLevel()
	{
		return sailingLevel;
	}

	public Port getCurrentPort()
	{
		return currentPort;
	}

	public RoutePlan getRoutePlan()
	{
		return routePlan;
	}

	public boolean isBoardOpen()
	{
		Widget board = client.getWidget(InterfaceID.PortTaskBoard.CONTAINER);
		return board != null && !board.isHidden();
	}

	public int getHighlightedOfferCount()
	{
		return (int) boardOffers.stream()
			.filter(offer -> offer.getStatus() == OfferStatus.PRIORITY || offer.getStatus() == OfferStatus.USEFUL)
			.count();
	}

	public int getRouteExperience()
	{
		return routePlan == null
			? activeTasks.stream().mapToInt(task -> task.getDefinition().getExperience()).sum()
			: routePlan.getTotalExperience();
	}

	public int getSessionExperienceGained()
	{
		return experienceSession.getGained();
	}

	public long getSessionExperiencePerHour()
	{
		return experienceSession.getPerHour();
	}

	public String getInfoStep()
	{
		if (phase == RoutePhase.IDLE)
		{
			return "Preparing collection";
		}
		if (phase == RoutePhase.COMPLETE)
		{
			return "Starting next collection";
		}
		if (phase == RoutePhase.COLLECTION)
		{
			if (isCollectionHandoffActive())
			{
				return getCollectionHandoffTitle();
			}
			if (collectionIndex >= selectedRoute.getCollectionStops().size())
			{
				Port recoveryPort = selectedRoute.getBoatRecoveryPort();
				return recoveryPort == Port.UNKNOWN
					? "Move to delivery phase" : "Recover boat to " + recoveryPort;
			}
			CollectionStop stop = selectedRoute.getCollectionStops().get(collectionIndex);
			if (isBoardOpenAt(stop.getPort()))
			{
				return "Choose tasks at " + stop.getPort();
			}
			if (currentPort == stop.getPort())
			{
				return "Open " + stop.getPort() + " board";
			}
			if (isEtceteriaShortcutActive())
			{
				return "Take the sailor to Etceteria";
			}
			if (stop.isCharterRequired())
			{
				return "Charter to " + stop.getPort();
			}
			return stop.isSailingLeg() ? "Sail to " + stop.getPort() : "Travel to " + stop.getPort();
		}
		RouteStep step = getCurrentDeliveryStep();
		return step == null ? "Accept a courier task" : step.getTitle();
	}

	public int getDeferredOfferCount()
	{
		return (int) boardOffers.stream()
			.filter(offer -> offer.getStatus() == OfferStatus.DEFERRED)
			.count();
	}

	public boolean isBoardOpenAt(Port port)
	{
		return isBoardOpen() && (currentPort == port || openBoardPort == port);
	}
}
