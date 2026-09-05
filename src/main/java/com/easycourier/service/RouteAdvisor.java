package com.easycourier.service;

import com.easycourier.data.TaskStateReader;
import com.easycourier.model.ActiveTask;
import com.easycourier.model.BoardOffer;
import com.easycourier.model.CollectionStop;
import com.easycourier.model.OfferStatus;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePhase;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.TaskDefinition;
import com.easycourier.model.TaskEdge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.widgets.Widget;

public final class RouteAdvisor
{
	public List<BoardOffer> advise(RoutePreset preset, RoutePhase phase, Port boardPort, int collectionIndex, int sailingLevel,
		int occupiedSlots, List<ActiveTask> activeTasks, List<WidgetTask> widgetTasks)
	{
		return advise(preset, phase, boardPort, collectionIndex, sailingLevel, occupiedSlots, activeTasks,
			widgetTasks, false);
	}

	public List<BoardOffer> advise(RoutePreset preset, RoutePhase phase, Port boardPort, int collectionIndex, int sailingLevel,
		int occupiedSlots, List<ActiveTask> activeTasks, List<WidgetTask> widgetTasks, boolean finalNorthernBoard)
	{
		List<BoardOffer> decisions = new ArrayList<>();
		CollectionStop stop = phase == RoutePhase.COLLECTION && collectionIndex < preset.getCollectionStops().size()
			? preset.getCollectionStops().get(collectionIndex) : null;
		for (WidgetTask widgetTask : widgetTasks)
		{
			TaskDefinition task = widgetTask.getTask();
			if (task == null)
			{
				decisions.add(new BoardOffer(widgetTask.getWidget(), null, OfferStatus.OFF_ROUTE, 0, "Not a courier task"));
				continue;
			}
			if (!task.isCourier())
			{
				decisions.add(new BoardOffer(widgetTask.getWidget(), task, OfferStatus.BOUNTY, 0, "Bounty task"));
				continue;
			}
			if (sailingLevel < task.getLevelRequired())
			{
				decisions.add(new BoardOffer(widgetTask.getWidget(), task, OfferStatus.INELIGIBLE, 0,
					"Requires level " + task.getLevelRequired()));
				continue;
			}
			boolean alreadyAccepted = activeTasks.stream()
				.anyMatch(activeTask -> activeTask.getDefinition().getTaskId() == task.getTaskId());
			if (alreadyAccepted)
			{
				decisions.add(new BoardOffer(widgetTask.getWidget(), task, OfferStatus.ACCEPTED, 0, "Already accepted"));
				continue;
			}
			boolean startsHere = task.getPickup() == boardPort;
			boolean northernIslandTask = finalNorthernBoard && preset == RoutePreset.RELLEKKA
				&& sailingLevel >= 68 && isNorthernIsland(task.getDelivery());
			boolean accepted = phase == RoutePhase.DELIVERY
				? startsHere && (northernIslandTask
					|| (!isNorthernIsland(task.getDelivery()) && preset.movesForward(task)))
				: stop != null && stop.accepts(task);
			if (!accepted)
			{
				String reason = phase == RoutePhase.DELIVERY && !startsHere
					? "Task starts at another port" : "Moves away from this route";
				decisions.add(new BoardOffer(widgetTask.getWidget(), task, OfferStatus.OFF_ROUTE, 0, reason));
				continue;
			}
			boolean preferred = stop != null && stop.isPreferred(task);
			int score = task.getExperience() + (preferred ? 1_000_000 : 0);
			if (shouldDefer(preset, phase, stop, collectionIndex, task))
			{
				decisions.add(new BoardOffer(widgetTask.getWidget(), task, OfferStatus.DEFERRED, score,
					"Useful later during delivery"));
				continue;
			}
			decisions.add(new BoardOffer(widgetTask.getWidget(), task,
				preferred ? OfferStatus.PRIORITY : OfferStatus.USEFUL, score,
				preferred ? "Priority task" : "Useful forward task"));
		}
		TaskEdge persistentReservation = phase == RoutePhase.COLLECTION ? preset.getPersistentReservedTask() : null;
		limitHighlights(decisions, stop, persistentReservation, sailingLevel, occupiedSlots, activeTasks);
		decisions.sort(Comparator.comparingInt(BoardOffer::getScore).reversed());
		return decisions;
	}

	private boolean isNorthernIsland(Port port)
	{
		return port == Port.NEITIZNOT || port == Port.JATIZSO;
	}

	private boolean shouldDefer(RoutePreset preset, RoutePhase phase, CollectionStop stop, int collectionIndex,
		TaskDefinition task)
	{
		if (phase != RoutePhase.COLLECTION || stop == null
			|| collectionIndex >= preset.getCollectionStops().size() - 1
			|| task.getPickup() != stop.getPort())
		{
			return false;
		}
		int pickupRank = preset.routeRank(task.getPickup());
		int deliveryRank = preset.routeRank(task.getDelivery());
		int finishRank = preset.routeRank(preset.getFinish());
		return pickupRank > 0 && pickupRank < finishRank && deliveryRank == pickupRank + 1;
	}

	private void limitHighlights(List<BoardOffer> decisions, CollectionStop stop, TaskEdge persistentReservation,
		int sailingLevel, int occupiedSlots, List<ActiveTask> activeTasks)
	{
		int capacity = TaskStateReader.taskCapacity(sailingLevel);
		int freeSlots = Math.max(0, capacity - occupiedSlots);
		TaskEdge localReservation = stop == null ? null : stop.getReservedTask();
		boolean reservationRequired = persistentReservation != null || localReservation != null;
		boolean alreadyHasPreferred = persistentReservation != null
			? activeTasks.stream().anyMatch(task -> persistentReservation.matches(task.getDefinition()))
			: localReservation != null && activeTasks.stream().anyMatch(task -> stop.isPreferred(task.getDefinition()));
		boolean boardHasPreferred = persistentReservation != null
			? decisions.stream().anyMatch(offer -> isActionable(offer) && offer.getTask() != null
				&& persistentReservation.matches(offer.getTask()))
			: localReservation != null && decisions.stream().anyMatch(offer -> isActionable(offer)
				&& offer.getTask() != null && stop.isPreferred(offer.getTask()));
		int usableSlots = freeSlots;
		if (reservationRequired && !alreadyHasPreferred && !boardHasPreferred)
		{
			usableSlots = Math.max(0, freeSlots - 1);
		}
		Set<BoardOffer> keep = new HashSet<>();
		boolean boardReservationRequired = reservationRequired && !alreadyHasPreferred && boardHasPreferred;
		BoardOffer bestExperience = usableSlots == 1 && !boardReservationRequired
			? decisions.stream()
				.filter(this::isActionable)
				.filter(offer -> offer.getTask() != null)
				.max(Comparator.comparingInt(offer -> offer.getTask().getExperience()))
				.orElse(null)
			: null;
		if (bestExperience != null)
		{
			keep.add(bestExperience);
		}
		else
		{
			decisions.stream()
				.filter(this::isActionable)
				.sorted(Comparator.comparingInt(BoardOffer::getScore).reversed())
				.limit(usableSlots)
				.forEach(keep::add);
		}
		for (int index = 0; index < decisions.size(); index++)
		{
			BoardOffer offer = decisions.get(index);
			if (offer == bestExperience)
			{
				decisions.set(index, new BoardOffer(offer.getWidget(), offer.getTask(), OfferStatus.PRIORITY,
					offer.getTask().getExperience(), "Best XP for the available slot"));
				continue;
			}
			if (offer.getStatus() == OfferStatus.DEFERRED && usableSlots == 0)
			{
				decisions.set(index, new BoardOffer(offer.getWidget(), offer.getTask(), OfferStatus.OFF_ROUTE, 0,
					freeSlots == 0 ? "No free task slots" : "Keep a slot for a better task"));
				continue;
			}
			if ((offer.getStatus() == OfferStatus.PRIORITY || offer.getStatus() == OfferStatus.USEFUL) && !keep.contains(offer))
			{
				decisions.set(index, new BoardOffer(offer.getWidget(), offer.getTask(), OfferStatus.OFF_ROUTE, 0,
					freeSlots == 0 ? "No free task slots" : "Keep a slot for a better task"));
			}
		}
	}

	private boolean isActionable(BoardOffer offer)
	{
		return offer.getStatus() == OfferStatus.PRIORITY || offer.getStatus() == OfferStatus.USEFUL;
	}

	public static final class WidgetTask
	{
		private final Widget widget;
		private final TaskDefinition task;

		public WidgetTask(Widget widget, TaskDefinition task)
		{
			this.widget = widget;
			this.task = task;
		}

		public Widget getWidget()
		{
			return widget;
		}

		public TaskDefinition getTask()
		{
			return task;
		}
	}
}
