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
	public void buildsRellekkaToEtceteriaCollectionLeg()
	{
		RoutePlan plan = planner.planLeg(Port.RELLEKKA, Port.ETCETERIA);
		assertEquals(Arrays.asList(Port.RELLEKKA, Port.ETCETERIA), plan.getPortOrder());
		assertEquals(Port.RELLEKKA.getMapPoint(), plan.getSeaPath().get(0));
		assertEquals(Port.ETCETERIA.getMapPoint(), plan.getSeaPath().get(plan.getSeaPath().size() - 1));
		assertEquals(StepKind.TRAVEL, plan.getSteps().get(0).getKind());
		assertEquals(Port.ETCETERIA, plan.getSteps().get(0).getPort());
	}

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
	public void findsBestDynamicTaskStart()
	{
		List<ActiveTask> tasks = Arrays.asList(
			active(0, Port.ALDARIN, Port.PRIFDDINAS, 8, 7082),
			active(1, Port.DEEPFIN_POINT, Port.PORT_TYRAS, 5, 4030));
		RoutePlan best = planner.planFromBestTaskStart(RoutePreset.PRIFDDINAS, tasks, 4);
		RoutePlan fromAldarin = planner.plan(RoutePreset.PRIFDDINAS, Port.ALDARIN, tasks, 4);
		RoutePlan fromDeepfin = planner.plan(RoutePreset.PRIFDDINAS, Port.DEEPFIN_POINT, tasks, 4);
		Port expected = fromAldarin.getDistance() <= fromDeepfin.getDistance()
			? Port.ALDARIN : Port.DEEPFIN_POINT;
		assertEquals(expected, best.getPortOrder().get(0));
	}

	@Test
	public void startsAtSharedPortRobertsPickupAfterCollection()
	{
		List<ActiveTask> tasks = Arrays.asList(
			active(7, Port.PORT_ROBERTS, Port.RELLEKKA, 4, 5277),
			active(8, Port.PORT_ROBERTS, Port.ETCETERIA, 5, 6005));
		RoutePlan best = planner.planFromBestTaskStart(RoutePreset.RELLEKKA, tasks, 4);
		assertEquals(Port.PORT_ROBERTS, best.getPortOrder().get(0));
		assertEquals(StepKind.PICKUP, best.getSteps().get(0).getKind());
		assertEquals(Port.PORT_ROBERTS, best.getSteps().get(0).getPort());
	}

	@Test
	public void preservesSelectedFirstPickupAfterBoatRecovery()
	{
		ActiveTask task = active(2, Port.SUNSET_COAST, Port.RELLEKKA, 5, 5000);
		RoutePlan plan = planner.planVia(RoutePreset.RELLEKKA, Port.ALDARIN, Port.SUNSET_COAST,
			Collections.singletonList(task), 4, Collections.emptySet());
		assertEquals(Arrays.asList(Port.ALDARIN, Port.SUNSET_COAST, Port.RELLEKKA), plan.getPortOrder());
		assertEquals(StepKind.TRAVEL, plan.getSteps().get(0).getKind());
		assertEquals(Port.SUNSET_COAST, plan.getSteps().get(0).getPort());
		assertEquals("Sail to Sunset Coast", plan.getSteps().get(0).getTitle());
		assertEquals(StepKind.PICKUP, plan.getSteps().get(1).getKind());
		assertEquals(Port.SUNSET_COAST, plan.getSteps().get(1).getPort());
		assertEquals(Port.ALDARIN.getMapPoint(), plan.getSeaPath().get(0));
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
	public void neverAddsNoticeBoardAtDeliveryStart()
	{
		ActiveTask task = active(4, Port.ALDARIN, Port.PRIFDDINAS, 7, 7082);
		RoutePlan plan = planner.plan(RoutePreset.PRIFDDINAS, Port.ALDARIN,
			Collections.singletonList(task), 4, Collections.emptySet());
		assertTrue(plan.getSteps().stream()
			.noneMatch(step -> step.getKind() == StepKind.NOTICE_BOARD && step.getPort() == Port.ALDARIN));
	}

	@Test
	public void stillAddsNoticeBoardAtLaterDeliveryPort()
	{
		List<ActiveTask> tasks = Arrays.asList(
			active(5, Port.ALDARIN, Port.DEEPFIN_POINT, 7, 3793),
			active(6, Port.DEEPFIN_POINT, Port.PRIFDDINAS, 7, 4721));
		RoutePlan plan = planner.plan(RoutePreset.PRIFDDINAS, Port.ALDARIN,
			tasks, 4, Collections.emptySet());
		assertTrue(plan.getSteps().stream()
			.anyMatch(step -> step.getKind() == StepKind.NOTICE_BOARD && step.getPort() == Port.DEEPFIN_POINT));
	}

	@Test
	public void addsCoastalBoardAfterDeliveryFreesASlot()
	{
		List<ActiveTask> tasks = Arrays.asList(
			active(7, Port.ALDARIN, Port.CIVITAS_ILLA_FORTIS, 4, 3000),
			active(8, Port.ALDARIN, Port.RELLEKKA, 4, 5000));
		RoutePlan plan = planner.plan(RoutePreset.RELLEKKA, Port.ALDARIN,
			tasks, 2, Collections.emptySet());
		assertTrue(plan.getSteps().stream()
			.anyMatch(step -> step.getKind() == StepKind.NOTICE_BOARD
				&& step.getPort() == Port.CIVITAS_ILLA_FORTIS));
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
