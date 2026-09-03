package com.easycourier;

import com.easycourier.data.TaskCatalog;
import com.easycourier.data.TaskStateReader;
import com.easycourier.model.ActiveTask;
import com.easycourier.model.BoardOffer;
import com.easycourier.model.CollectionStop;
import com.easycourier.model.OfferStatus;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePhase;
import com.easycourier.model.RoutePlan;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.RouteStep;
import com.easycourier.model.StepKind;
import com.easycourier.model.TaskDefinition;
import com.easycourier.overlay.DockOverlay;
import com.easycourier.overlay.CargoItemOverlay;
import com.easycourier.overlay.CharterCrewmemberOverlay;
import com.easycourier.overlay.NoticeBoardOverlay;
import com.easycourier.overlay.NoticeBoardWorldOverlay;
import com.easycourier.overlay.RouteMapOverlay;
import com.easycourier.overlay.RouteWorldOverlay;
import com.easycourier.service.RouteAdvisor;
import com.easycourier.service.RoutePlanner;
import com.easycourier.service.SeaNetwork;
import com.easycourier.ui.EasyCourierPanel;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
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

@PluginDescriptor(
	name = "Easy Courier",
	description = "Guides two-phase Sailing courier collection and delivery routes",
	tags = {"sailing", "courier", "port", "tasks", "cargo", "route"}
)
public class EasyCourierPlugin extends Plugin
{
	private static final int PORT_DETECTION_DISTANCE = 110;
	private static final int ACTIVE_DESTINATION_DISTANCE = 64;
	private static final Color CHAT_MESSAGE_COLOR = new Color(31, 78, 121);
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
	private RouteMapOverlay routeMapOverlay;
	@Inject
	private RouteWorldOverlay routeWorldOverlay;

	private final TaskCatalog catalog = new TaskCatalog();
	private final TaskStateReader stateReader = new TaskStateReader();
	private final RouteAdvisor advisor = new RouteAdvisor();
	private final RoutePlanner planner = new RoutePlanner(new SeaNetwork());
	private final List<ActiveTask> activeTasks = new ArrayList<>();
	private final List<BoardOffer> boardOffers = new ArrayList<>();
	private final Set<GameObject> ledgers = new HashSet<>();
	private final Set<GameObject> noticeBoards = new HashSet<>();
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
	private int occupiedTaskSlots;
	private boolean boardWasOpen;
	private Port openBoardPort = Port.UNKNOWN;
	private Port currentPort = Port.UNKNOWN;
	private Port lastKnownPort = Port.UNKNOWN;
	private RoutePlan routePlan;

	@Override
	protected void startUp()
	{
		selectedRoute = config.defaultRoute();
		restoreLastKnownPort();
		panel = new EasyCourierPanel(this);
		navigationButton = NavigationButton.builder()
			.tooltip("Easy Courier")
			.icon(createIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(noticeBoardOverlay);
		overlayManager.add(noticeBoardWorldOverlay);
		overlayManager.add(dockOverlay);
		overlayManager.add(cargoItemOverlay);
		overlayManager.add(charterCrewmemberOverlay);
		overlayManager.add(routeMapOverlay);
		overlayManager.add(routeWorldOverlay);
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
		overlayManager.remove(routeMapOverlay);
		overlayManager.remove(routeWorldOverlay);
		activeTasks.clear();
		boardOffers.clear();
		ledgers.clear();
		noticeBoards.clear();
		pickupAnnouncements.clear();
		deliveryAnnouncements.clear();
		deliveryBoardsChecked.clear();
		panel = null;
		navigationButton = null;
	}

	@Provides
	EasyCourierConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EasyCourierConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::loadGameData);
		}
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.HOPPING)
		{
			ledgers.clear();
			noticeBoards.clear();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.SAILING)
		{
			sailingLevel = client.getRealSkillLevel(Skill.SAILING);
			refreshBoardAdvice();
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
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		ledgers.remove(event.getGameObject());
		noticeBoards.remove(event.getGameObject());
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
		selectedRoute = route;
		configManager.setConfiguration(EasyCourierConfig.GROUP, "defaultRoute", route);
		refreshPanel();
	}

	public void beginCollection()
	{
		if (sailingLevel < selectedRoute.getMinimumLevel())
		{
			sendMessage("Easy Courier: This route requires Sailing level " + selectedRoute.getMinimumLevel() + ".");
			return;
		}
		phase = RoutePhase.COLLECTION;
		collectionIndex = 0;
		deliverySkipCount = 0;
		deliveryBoardsChecked.clear();
		skipUnavailableCollectionStops();
		refreshBoardAdvice();
		refreshPanel();
	}

	public void moveToDelivery()
	{
		phase = RoutePhase.DELIVERY;
		deliverySkipCount = 0;
		deliveryBoardsChecked.clear();
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
		phase = RoutePhase.IDLE;
		collectionIndex = 0;
		deliverySkipCount = 0;
		routePlan = null;
		boardOffers.clear();
		deliveryBoardsChecked.clear();
		refreshPanel();
	}

	private void loadGameData()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		catalog.load(client);
		sailingLevel = client.getRealSkillLevel(Skill.SAILING);
		updateCurrentPort();
		refreshTasks();
		if (phase == RoutePhase.IDLE && !activeTasks.isEmpty())
		{
			selectedRoute = detectRoute();
			if (currentPort == Port.UNKNOWN && selectedRoute.routeRank(lastKnownPort) < 0)
			{
				lastKnownPort = inferProgressPort();
			}
			phase = RoutePhase.DELIVERY;
			deliverySkipCount = 0;
			deliveryBoardsChecked.clear();
			rebuildRoutePlan();
			refreshBoardAdvice();
			refreshPanel();
		}
	}

	private void refreshTasks()
	{
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
				sendMessage("You now have all the cargo");
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

	private void scanNoticeBoard()
	{
		boardOffers.clear();
		if (phase != RoutePhase.COLLECTION && phase != RoutePhase.DELIVERY)
		{
			refreshPanel();
			return;
		}
		if (phase == RoutePhase.COLLECTION && collectionIndex >= selectedRoute.getCollectionStops().size())
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
		boardOffers.addAll(advisor.advise(selectedRoute, phase, openBoardPort, collectionIndex, sailingLevel, occupiedTaskSlots,
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
		if (phase != RoutePhase.COLLECTION || collectionIndex >= selectedRoute.getCollectionStops().size())
		{
			return;
		}
		CollectionStop stop = selectedRoute.getCollectionStops().get(collectionIndex);
		if (currentPort == stop.getPort() || openBoardPort == stop.getPort())
		{
			collectionIndex++;
			skipUnavailableCollectionStops();
		}
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
	}

	private void updateCurrentPort()
	{
		if (client.getLocalPlayer() == null)
		{
			currentPort = Port.UNKNOWN;
			return;
		}
		WorldPoint point = navigationWorldPoint();
		Port detected = activeDestinationAt(point);
		if (detected == Port.UNKNOWN)
		{
			detected = Port.nearest(point, PORT_DETECTION_DISTANCE);
		}
		if (detected != Port.UNKNOWN)
		{
			rememberPort(detected);
		}
		if (detected != currentPort)
		{
			currentPort = detected;
			if (phase == RoutePhase.DELIVERY)
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
		routePlan = planner.plan(selectedRoute, routeStart, activeTasks,
			TaskStateReader.taskCapacity(sailingLevel), deliveryBoardsChecked);
		if (activeTasks.stream().allMatch(ActiveTask::isComplete)
			&& currentPort == selectedRoute.getFinish())
		{
			phase = RoutePhase.COMPLETE;
		}
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

	private Port activeDestinationAt(WorldPoint point)
	{
		RouteStep step = getCurrentDeliveryStep();
		if (point == null || step == null || step.getKind() != StepKind.TRAVEL)
		{
			return Port.UNKNOWN;
		}
		return point.distanceTo2D(step.getPort().getMapPoint()) <= ACTIVE_DESTINATION_DISTANCE
			? step.getPort() : Port.UNKNOWN;
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
		if (!config.chatUpdates())
		{
			return;
		}
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder().append(CHAT_MESSAGE_COLOR, message).build())
			.build());
	}

	private void refreshPanel()
	{
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::refresh);
		}
	}

	private BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(70, 181, 165));
		graphics.fillRoundRect(5, 18, 22, 7, 7, 7);
		graphics.setColor(new Color(229, 214, 178));
		graphics.fillPolygon(new int[]{9, 17, 24}, new int[]{18, 6, 18}, 3);
		graphics.setColor(new Color(24, 35, 43));
		graphics.setStroke(new BasicStroke(2));
		graphics.drawLine(17, 6, 17, 23);
		graphics.dispose();
		return image;
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

	public RouteStep getCurrentDeliveryStep()
	{
		if (phase != RoutePhase.DELIVERY || routePlan == null || routePlan.getSteps().isEmpty())
		{
			return null;
		}
		int index = Math.min(deliverySkipCount, routePlan.getSteps().size() - 1);
		return routePlan.getSteps().get(index);
	}

	public boolean isTravelStepActive()
	{
		RouteStep step = getCurrentDeliveryStep();
		return step != null && step.getKind() == StepKind.TRAVEL;
	}

	public Port getNoticeBoardTarget()
	{
		if (occupiedTaskSlots >= TaskStateReader.taskCapacity(sailingLevel))
		{
			return Port.UNKNOWN;
		}
		if (phase == RoutePhase.COLLECTION && collectionIndex < selectedRoute.getCollectionStops().size())
		{
			return selectedRoute.getCollectionStops().get(collectionIndex).getPort();
		}
		RouteStep step = getCurrentDeliveryStep();
		return step != null && step.getKind() == StepKind.NOTICE_BOARD ? step.getPort() : Port.UNKNOWN;
	}

	public Port getCharterTarget()
	{
		if (phase != RoutePhase.COLLECTION || collectionIndex >= selectedRoute.getCollectionStops().size())
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
