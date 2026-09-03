package com.easycourier.service;

import com.easycourier.model.ActiveTask;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePlan;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.RouteStep;
import com.easycourier.model.StepKind;
import com.easycourier.model.TaskDefinition;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RoutePlannerTest
{
	private final RoutePlanner planner = new RoutePlanner(new SeaNetwork());

	@Test
	public void respectsPickupBeforeDeliveryAndFinishesAtRoutePoint()
	{
		List<ActiveTask> tasks = Arrays.asList(
			active(0, Port.ALDARIN, Port.PRIFDDINAS, 8, 7082),
			active(1, Port.DEEPFIN_POINT, Port.PORT_TYRAS, 5, 4030),
			active(2, Port.ALDARIN, Port.DEEPFIN_POINT, 4, 3541));
		RoutePlan plan = planner.plan(RoutePreset.PRIFDDINAS, Port.ALDARIN, tasks);
		assertEquals(Port.ALDARIN, plan.getPortOrder().get(0));
		assertTrue(plan.getPortOrder().indexOf(Port.DEEPFIN_POINT) < plan.getPortOrder().indexOf(Port.PORT_TYRAS));
		assertEquals(Port.PRIFDDINAS, plan.getPortOrder().get(plan.getPortOrder().size() - 1));
		assertEquals(14653, plan.getTotalExperience());
	}

	@Test
	public void keepsEachTaskVisibleAtSharedDeliveryDock()
	{
		List<ActiveTask> tasks = Arrays.asList(
			active(0, Port.ALDARIN, Port.PRIFDDINAS, 8, 7082),
			active(1, Port.PORT_TYRAS, Port.PRIFDDINAS, 6, 2279));
		RoutePlan plan = planner.plan(RoutePreset.PRIFDDINAS, Port.ALDARIN, tasks);
		long prifDeliveries = plan.getSteps().stream()
			.filter(step -> step.getKind() == StepKind.DELIVER && step.getPort() == Port.PRIFDDINAS)
			.count();
		assertEquals(2, prifDeliveries);
		assertTrue(plan.getSteps().stream().map(RouteStep::getTitle)
			.anyMatch(title -> title.equals("Deliver 8 from Aldarin")));
		assertTrue(plan.getSteps().stream().map(RouteStep::getTitle)
			.anyMatch(title -> title.equals("Deliver 6 from Port Tyras")));
	}

	@Test
	public void currentPortPickupIsTheFirstAction()
	{
		TaskDefinition definition = new TaskDefinition(9037, 2000, 70, Port.PRIFDDINAS,
			Port.ALDARIN, Port.PRIFDDINAS, "Prifddinas potion delivery", 3000, 7, 7082);
		ActiveTask task = new ActiveTask(definition, 0, 0, 0);
		RoutePlan plan = planner.plan(RoutePreset.PRIFDDINAS, Port.ALDARIN,
			Collections.singletonList(task));
		assertEquals(StepKind.PICKUP, plan.getSteps().get(0).getKind());
		assertEquals("Collect 7 for Prifddinas", plan.getSteps().get(0).getTitle());
		assertEquals(7082, plan.getTotalExperience());
	}

	@Test
	public void partialPickupShowsOnlyTheRemainingCargo()
	{
		TaskDefinition definition = new TaskDefinition(9037, 2000, 70, Port.PRIFDDINAS,
			Port.ALDARIN, Port.PRIFDDINAS, "Prifddinas potion delivery", 3000, 7, 7082);
		ActiveTask task = new ActiveTask(definition, 0, 3, 0);
		RoutePlan plan = planner.plan(RoutePreset.PRIFDDINAS, Port.ALDARIN,
			Collections.singletonList(task));
		assertEquals("Collect 4 for Prifddinas", plan.getSteps().get(0).getTitle());
	}

	@Test
	public void omitsDeliveryBoardAfterItWasChecked()
	{
		ActiveTask task = active(3, Port.PORT_TYRAS, Port.PRIFDDINAS, 7, 1221);
		RoutePlan plan = planner.plan(RoutePreset.PRIFDDINAS, Port.DEEPFIN_POINT,
			Collections.singletonList(task), 4, EnumSet.of(Port.DEEPFIN_POINT));
		assertEquals(StepKind.TRAVEL, plan.getSteps().get(0).getKind());
		assertEquals(Port.PORT_TYRAS, plan.getSteps().get(0).getPort());
	}

	@Test
	public void emptyManifestStillReturnsToFinish()
	{
		RoutePlan plan = planner.plan(RoutePreset.LUNAR_ISLE, Port.PRIFDDINAS, Collections.emptyList());
		assertEquals(Arrays.asList(Port.PRIFDDINAS, Port.LUNAR_ISLE), plan.getPortOrder());
	}

	private ActiveTask active(int slot, Port pickup, Port delivery, int amount, int experience)
	{
		TaskDefinition definition = new TaskDefinition(1000 + slot, 2000 + slot, 1, pickup,
			pickup, delivery, delivery + " delivery", 3000 + slot, amount, experience);
		return new ActiveTask(definition, slot, 0, 0);
	}
}
