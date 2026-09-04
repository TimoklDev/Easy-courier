package com.easycourier.service;

import com.easycourier.model.ActiveTask;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePlan;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.RouteStep;
import com.easycourier.model.StepKind;
import com.easycourier.model.TaskDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;

public final class RoutePlanner
{
	private final SeaNetwork seaNetwork;

	public RoutePlanner(SeaNetwork seaNetwork)
	{
		this.seaNetwork = seaNetwork;
	}

	public RoutePlan planLeg(Port start, Port finish)
	{
		List<Port> order = new ArrayList<>();
		order.add(start);
		order.add(finish);
		List<RouteStep> steps = Collections.singletonList(new RouteStep(StepKind.TRAVEL, finish,
			"Sail to " + finish, "Follow the highlighted sea route.", 0));
		return new RoutePlan(order, expandSeaPath(order), steps, 0, seaNetwork.distance(start, finish));
	}

	public RoutePlan planFromBestTaskStart(RoutePreset preset, List<ActiveTask> activeTasks, int taskCapacity)
	{
		Set<Port> candidates = new LinkedHashSet<>();
		for (ActiveTask task : activeTasks)
		{
			if (task.isComplete())
			{
				continue;
			}
			candidates.add(task.needsPickup()
				? task.getDefinition().getPickup() : task.getDefinition().getDelivery());
		}
		RoutePlan best = null;
		for (Port candidate : candidates)
		{
			RoutePlan plan = plan(preset, candidate, activeTasks, taskCapacity);
			if (best == null || plan.getDistance() < best.getDistance())
			{
				best = plan;
			}
		}
		return best;
	}

	public RoutePlan plan(RoutePreset preset, Port start, List<ActiveTask> activeTasks)
	{
		return plan(preset, start, activeTasks, activeTasks.size());
	}

	public RoutePlan plan(RoutePreset preset, Port start, List<ActiveTask> activeTasks, int taskCapacity)
	{
		return plan(preset, start, activeTasks, taskCapacity, Collections.emptySet());
	}

	public RoutePlan plan(RoutePreset preset, Port start, List<ActiveTask> activeTasks, int taskCapacity,
		Set<Port> checkedBoards)
	{
		List<ActiveTask> tasks = new ArrayList<>();
		for (ActiveTask task : activeTasks)
		{
			if (!task.isComplete())
			{
				tasks.add(task);
			}
		}
		Port routeStart = start == null || start == Port.UNKNOWN
			? preset.getCollectionStops().get(preset.getCollectionStops().size() - 1).getPort() : start;
		int picked = 0;
		int delivered = 0;
		for (int index = 0; index < tasks.size(); index++)
		{
			ActiveTask task = tasks.get(index);
			if (!task.needsPickup())
			{
				picked |= 1 << index;
			}
			if (task.isComplete())
			{
				delivered |= 1 << index;
			}
		}
		Map<String, SearchResult> memo = new HashMap<>();
		SearchResult best = search(routeStart, picked, delivered, tasks, preset.getFinish(), memo);
		List<Port> order = new ArrayList<>();
		order.add(routeStart);
		order.addAll(best.order);
		List<WorldPoint> seaPath = expandSeaPath(order);
		List<RouteStep> steps = buildSteps(order, tasks, preset, activeTasks.size(), taskCapacity, checkedBoards);
		int experience = activeTasks.stream().mapToInt(task -> task.getDefinition().getExperience()).sum();
		return new RoutePlan(order, seaPath, steps, experience, best.distance);
	}

	private SearchResult search(Port current, int picked, int delivered, List<ActiveTask> tasks, Port finish,
		Map<String, SearchResult> memo)
	{
		int[] normalized = applyPort(current, picked, delivered, tasks);
		picked = normalized[0];
		delivered = normalized[1];
		int all = (1 << tasks.size()) - 1;
		if (delivered == all)
		{
			if (current == finish)
			{
				return new SearchResult(0, Collections.emptyList());
			}
			return new SearchResult(seaNetwork.distance(current, finish), Collections.singletonList(finish));
		}
		String key = current.ordinal() + ":" + picked + ":" + delivered;
		SearchResult cached = memo.get(key);
		if (cached != null)
		{
			return cached;
		}
		Set<Port> candidates = new LinkedHashSet<>();
		for (int index = 0; index < tasks.size(); index++)
		{
			int bit = 1 << index;
			TaskDefinition task = tasks.get(index).getDefinition();
			if ((picked & bit) == 0)
			{
				candidates.add(task.getPickup());
			}
			else if ((delivered & bit) == 0)
			{
				candidates.add(task.getDelivery());
			}
		}
		candidates.remove(current);
		SearchResult best = new SearchResult(Double.POSITIVE_INFINITY, Collections.emptyList());
		for (Port candidate : candidates)
		{
			SearchResult tail = search(candidate, picked, delivered, tasks, finish, memo);
			double distance = seaNetwork.distance(current, candidate) + tail.distance;
			if (distance < best.distance)
			{
				List<Port> order = new ArrayList<>();
				order.add(candidate);
				order.addAll(tail.order);
				best = new SearchResult(distance, order);
			}
		}
		memo.put(key, best);
		return best;
	}

	private int[] applyPort(Port port, int picked, int delivered, List<ActiveTask> tasks)
	{
		for (int index = 0; index < tasks.size(); index++)
		{
			if (tasks.get(index).getDefinition().getPickup() == port)
			{
				picked |= 1 << index;
			}
		}
		for (int index = 0; index < tasks.size(); index++)
		{
			int bit = 1 << index;
			if ((picked & bit) != 0 && tasks.get(index).getDefinition().getDelivery() == port)
			{
				delivered |= bit;
			}
		}
		return new int[]{picked, delivered};
	}

	private List<WorldPoint> expandSeaPath(List<Port> order)
	{
		List<WorldPoint> result = new ArrayList<>();
		for (int index = 0; index < order.size() - 1; index++)
		{
			List<WorldPoint> leg = seaNetwork.path(order.get(index), order.get(index + 1));
			if (!result.isEmpty() && !leg.isEmpty())
			{
				leg = new ArrayList<>(leg.subList(1, leg.size()));
			}
			result.addAll(leg);
		}
		if (result.isEmpty() && !order.isEmpty())
		{
			result.add(order.get(0).getMapPoint());
		}
		return result;
	}

	private List<RouteStep> buildSteps(List<Port> order, List<ActiveTask> tasks, RoutePreset preset,
		int occupiedSlots, int taskCapacity, Set<Port> checkedBoards)
	{
		List<RouteStep> steps = new ArrayList<>();
		Set<Integer> picked = new LinkedHashSet<>();
		Set<Integer> delivered = new LinkedHashSet<>();
		for (ActiveTask task : tasks)
		{
			if (!task.needsPickup())
			{
				picked.add(task.getSlot());
			}
		}
		for (int orderIndex = 0; orderIndex < order.size(); orderIndex++)
		{
			Port port = order.get(orderIndex);
			if (orderIndex > 0)
			{
				steps.add(new RouteStep(StepKind.TRAVEL, port, "Sail to " + port, "Follow the highlighted sea route.", 0));
			}
			for (ActiveTask task : tasks)
			{
				if (!picked.contains(task.getSlot()) && task.getDefinition().getPickup() == port)
				{
					int remaining = Math.max(0, task.getDefinition().getCargoAmount() - task.getCargoTaken());
					steps.add(new RouteStep(StepKind.PICKUP, port,
						"Collect " + remaining + " for " + task.getDefinition().getDelivery(),
						"At " + port + ", use the highlighted cargo ledger and collect every remaining crate.", 0));
					picked.add(task.getSlot());
				}
			}
			for (ActiveTask task : tasks)
			{
				if (!delivered.contains(task.getSlot()) && picked.contains(task.getSlot())
					&& task.getDefinition().getDelivery() == port)
				{
					int remaining = Math.max(0, task.getDefinition().getCargoAmount() - task.getCargoDelivered());
					steps.add(new RouteStep(StepKind.DELIVER, port,
						"Deliver " + remaining + " from " + task.getDefinition().getPickup(),
						"At " + port + ", use the highlighted cargo ledger to deposit the remaining crates.",
						task.getDefinition().getExperience()));
					delivered.add(task.getSlot());
				}
			}
			if (port.hasNoticeBoard() && port != preset.getDeliveryStart() && port != preset.getFinish()
				&& occupiedSlots < taskCapacity
				&& !checkedBoards.contains(port))
			{
				steps.add(new RouteStep(StepKind.NOTICE_BOARD, port, "Check the notice board",
					"If a slot is open, take only a highlighted forward task.", 0));
			}
		}
		steps.add(new RouteStep(StepKind.FINISH, preset.getFinish(), "Finish at " + preset.getFinish(),
			"Claim rewards and begin the next collection lap.", 0));
		return steps;
	}

	private static final class SearchResult
	{
		private final double distance;
		private final List<Port> order;

		private SearchResult(double distance, List<Port> order)
		{
			this.distance = distance;
			this.order = order;
		}
	}
}
