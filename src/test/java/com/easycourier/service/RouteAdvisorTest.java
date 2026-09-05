package com.easycourier.service;

import com.easycourier.model.ActiveTask;
import com.easycourier.model.BoardOffer;
import com.easycourier.model.CollectionStop;
import com.easycourier.model.OfferStatus;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePhase;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.TaskDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RouteAdvisorTest
{
	private final RouteAdvisor advisor = new RouteAdvisor();

	@Test
	public void marksOnlyTheExplicitPrifTravelStopAsACharter()
	{
		assertFalse(RoutePreset.PRIFDDINAS.getCollectionStops().get(0).isCharterRequired());
		assertTrue(RoutePreset.PRIFDDINAS.getCollectionStops().get(1).isCharterRequired());
		assertFalse(RoutePreset.PRIFDDINAS.getCollectionStops().get(2).isCharterRequired());
	}

	@Test
	public void rellekkaEtceteriaStopUsesASailingLeg()
	{
		CollectionStop stop = RoutePreset.RELLEKKA.getCollectionStops().get(1);
		assertTrue(stop.isSailingLeg());
		assertEquals(Port.RELLEKKA, stop.getSailingStart());
		assertEquals(Port.ETCETERIA, stop.getPort());
	}

	@Test
	public void rellekkaAndPrifddinasRequireBoatRecoveryAtAldarin()
	{
		assertEquals(Port.ALDARIN, RoutePreset.RELLEKKA.getBoatRecoveryPort());
		assertEquals(Port.ALDARIN, RoutePreset.PRIFDDINAS.getBoatRecoveryPort());
		assertEquals(Port.UNKNOWN, RoutePreset.SUMMER_SHORE.getBoatRecoveryPort());
		assertEquals(Port.UNKNOWN, RoutePreset.LUNAR_ISLE.getBoatRecoveryPort());
	}

	@Test
	public void persistentReservationIsLimitedToPrifddinas()
	{
		assertTrue(RoutePreset.PRIFDDINAS.getPersistentReservedTask()
			.matches(task(50, 70, Port.ALDARIN, Port.PRIFDDINAS, 7082)));
		assertNull(RoutePreset.SUMMER_SHORE.getPersistentReservedTask());
		assertNull(RoutePreset.RELLEKKA.getPersistentReservedTask());
		assertNull(RoutePreset.LUNAR_ISLE.getPersistentReservedTask());
	}

	@Test
	public void rejectsBackwardTaskDuringDelivery()
	{
		TaskDefinition backward = task(1, 70, Port.DEEPFIN_POINT, Port.ALDARIN, 5000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.DELIVERY, Port.DEEPFIN_POINT, 0, 99, 0,
			Collections.emptyList(), Collections.singletonList(widget(backward)));
		assertEquals(OfferStatus.OFF_ROUTE, offers.get(0).getStatus());
	}

	@Test
	public void marksUnavailableTaskAsIneligible()
	{
		TaskDefinition highLevel = task(2, 76, Port.ALDARIN, Port.LUNAR_ISLE, 12670);
		List<BoardOffer> offers = advisor.advise(RoutePreset.LUNAR_ISLE, RoutePhase.COLLECTION, Port.LUNAR_ISLE, 0, 75, 0,
			Collections.emptyList(), Collections.singletonList(widget(highLevel)));
		assertEquals(OfferStatus.INELIGIBLE, offers.get(0).getStatus());
	}

	@Test
	public void reservesLastSlotWhenPriorityTaskIsMissing()
	{
		List<ActiveTask> active = new ArrayList<>();
		active.add(active(10, Port.DEEPFIN_POINT, Port.PRIFDDINAS));
		active.add(active(11, Port.PORT_TYRAS, Port.PRIFDDINAS));
		active.add(active(12, Port.DEEPFIN_POINT, Port.PORT_TYRAS));
		TaskDefinition useful = task(3, 70, Port.DEEPFIN_POINT, Port.PRIFDDINAS, 4721);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION, Port.PRIFDDINAS, 0, 70, 3,
			active, Collections.singletonList(widget(useful)));
		assertEquals(OfferStatus.OFF_ROUTE, offers.get(0).getStatus());
		assertEquals("Keep a slot for a better task", offers.get(0).getReason());
	}

	@Test
	public void highlightsReservedTaskAsPriority()
	{
		TaskDefinition reserved = task(4, 70, Port.ALDARIN, Port.PRIFDDINAS, 7082);
		TaskDefinition useful = task(5, 70, Port.DEEPFIN_POINT, Port.PRIFDDINAS, 4721);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION, Port.PRIFDDINAS, 0, 70, 0,
			Collections.emptyList(), Arrays.asList(widget(useful), widget(reserved)));
		assertEquals(OfferStatus.PRIORITY, offers.get(0).getStatus());
	}

	@Test
	public void rellekkaDoesNotReserveTheLastSlot()
	{
		List<ActiveTask> active = new ArrayList<>();
		active.add(active(20, Port.ALDARIN, Port.PORT_ROBERTS));
		active.add(active(21, Port.PORT_ROBERTS, Port.HOSIDIUS));
		active.add(active(22, Port.HOSIDIUS, Port.PORT_PISCARILIUS));
		TaskDefinition useful = task(6, 62, Port.PORT_ROBERTS, Port.RELLEKKA, 3000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.RELLEKKA, RoutePhase.COLLECTION, Port.RELLEKKA, 0, 62, 3,
			active, Collections.singletonList(widget(useful)));
		assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
	}

	@Test
	public void rejectsTaskThatStartsAtAnotherPortDuringDelivery()
	{
		TaskDefinition inbound = task(7, 70, Port.ALDARIN, Port.PRIFDDINAS, 7082);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.DELIVERY,
			Port.DEEPFIN_POINT, 0, 99, 0, Collections.emptyList(), Collections.singletonList(widget(inbound)));
		assertEquals(OfferStatus.OFF_ROUTE, offers.get(0).getStatus());
		assertEquals("Task starts at another port", offers.get(0).getReason());
	}

	@Test
	public void acceptsForwardTaskThatStartsAtCurrentDeliveryPort()
	{
		TaskDefinition forward = task(8, 70, Port.DEEPFIN_POINT, Port.PORT_TYRAS, 3000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.DELIVERY,
			Port.DEEPFIN_POINT, 0, 99, 0, Collections.emptyList(), Collections.singletonList(widget(forward)));
		assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
	}

	@Test
	public void leavesAcceptedTaskToTheGameStamp()
	{
		TaskDefinition task = task(9, 70, Port.ALDARIN, Port.PRIFDDINAS, 7082);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.PRIFDDINAS, 0, 99, 1, Collections.singletonList(active(9, Port.ALDARIN, Port.PRIFDDINAS)),
			Collections.singletonList(widget(task)));
		assertEquals(OfferStatus.ACCEPTED, offers.get(0).getStatus());
	}

	@Test
	public void defersShortHopFromIntermediateCollectionStop()
	{
		TaskDefinition task = task(10, 70, Port.PORT_TYRAS, Port.PRIFDDINAS, 1221);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.PORT_TYRAS, 1, 99, 0, Collections.emptyList(), Collections.singletonList(widget(task)));
		assertEquals(OfferStatus.DEFERRED, offers.get(0).getStatus());
	}

	@Test
	public void defersDeepfinShortHopUntilDelivery()
	{
		TaskDefinition task = task(13, 70, Port.DEEPFIN_POINT, Port.PORT_TYRAS, 2000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.DEEPFIN_POINT, 2, 99, 0, Collections.emptyList(), Collections.singletonList(widget(task)));
		assertEquals(OfferStatus.DEFERRED, offers.get(0).getStatus());
	}

	@Test
	public void appliesDeferredRuleToOtherRoutes()
	{
		TaskDefinition task = task(14, 65, Port.ETCETERIA, Port.RELLEKKA, 2000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.RELLEKKA, RoutePhase.COLLECTION,
			Port.ETCETERIA, 1, 99, 0, Collections.emptyList(), Collections.singletonList(widget(task)));
		assertEquals(OfferStatus.DEFERRED, offers.get(0).getStatus());
	}

	@Test
	public void keepsInboundTaskUsefulAtIntermediateCollectionStop()
	{
		TaskDefinition task = task(11, 70, Port.ALDARIN, Port.PORT_TYRAS, 3793);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.PORT_TYRAS, 1, 99, 0, Collections.emptyList(), Collections.singletonList(widget(task)));
		assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
	}

	@Test
	public void keepsShortHopUsefulAtFinalCollectionStop()
	{
		TaskDefinition task = task(12, 70, Port.ALDARIN, Port.DEEPFIN_POINT, 3793);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.ALDARIN, 3, 99, 0, Collections.emptyList(), Collections.singletonList(widget(task)));
		assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
	}

	@Test
	public void rellekkaAldarinStopAcceptsCoastalTasks()
	{
		List<Port> destinations = Arrays.asList(
			Port.PORT_ROBERTS,
			Port.CIVITAS_ILLA_FORTIS,
			Port.PORT_PISCARILIUS);
		int taskId = 40;
		for (Port destination : destinations)
		{
			TaskDefinition task = task(taskId++, 62, Port.ALDARIN, destination, 3000);
			List<BoardOffer> offers = advisor.advise(RoutePreset.RELLEKKA, RoutePhase.COLLECTION,
				Port.ALDARIN, 2, 99, 0, Collections.emptyList(), Collections.singletonList(widget(task)));
			assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
		}
	}

	@Test
	public void rellekkaCoastalBoardAcceptsAForwardTask()
	{
		TaskDefinition task = task(43, 62, Port.CIVITAS_ILLA_FORTIS, Port.PORT_PISCARILIUS, 3000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.RELLEKKA, RoutePhase.DELIVERY,
			Port.CIVITAS_ILLA_FORTIS, 0, 99, 1, Collections.emptyList(),
			Collections.singletonList(widget(task)));
		assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
	}

	@Test
	public void reservesTheGuaranteedPrifTaskAtIntermediateBoards()
	{
		List<ActiveTask> active = Arrays.asList(
			active(30, Port.DEEPFIN_POINT, Port.PRIFDDINAS),
			active(31, Port.PORT_TYRAS, Port.PRIFDDINAS),
			active(32, Port.DEEPFIN_POINT, Port.PORT_TYRAS));
		TaskDefinition candidate = task(15, 70, Port.ALDARIN, Port.PORT_TYRAS, 3793);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.PORT_TYRAS, 1, 70, 3, active, Collections.singletonList(widget(candidate)));
		assertEquals(OfferStatus.OFF_ROUTE, offers.get(0).getStatus());
		assertEquals("Keep a slot for a better task", offers.get(0).getReason());
	}

	@Test
	public void releasesThePrifReservationOnceTheTaskIsOwned()
	{
		List<ActiveTask> active = Arrays.asList(
			active(33, Port.ALDARIN, Port.PRIFDDINAS),
			active(34, Port.PORT_TYRAS, Port.PRIFDDINAS),
			active(35, Port.DEEPFIN_POINT, Port.PORT_TYRAS));
		TaskDefinition candidate = task(16, 70, Port.ALDARIN, Port.PORT_TYRAS, 3793);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.PORT_TYRAS, 1, 70, 3, active, Collections.singletonList(widget(candidate)));
		assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
	}

	@Test
	public void guaranteedPrifTaskWinsTheLastSlotAtAldarin()
	{
		List<ActiveTask> active = Arrays.asList(
			active(36, Port.DEEPFIN_POINT, Port.PRIFDDINAS),
			active(37, Port.PORT_TYRAS, Port.PRIFDDINAS),
			active(38, Port.DEEPFIN_POINT, Port.PORT_TYRAS));
		TaskDefinition other = task(17, 70, Port.ALDARIN, Port.PORT_TYRAS, 3793);
		TaskDefinition guaranteed = task(18, 70, Port.ALDARIN, Port.PRIFDDINAS, 7082);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION,
			Port.ALDARIN, 3, 70, 3, active, Arrays.asList(widget(other), widget(guaranteed)));
		assertEquals(OfferStatus.PRIORITY, offers.get(0).getStatus());
		assertEquals(OfferStatus.OFF_ROUTE, offers.get(1).getStatus());
	}

	private RouteAdvisor.WidgetTask widget(TaskDefinition task)
	{
		return new RouteAdvisor.WidgetTask(null, task);
	}

	private TaskDefinition task(int id, int level, Port pickup, Port delivery, int experience)
	{
		return new TaskDefinition(id, id + 100, level, pickup, pickup, delivery, "Courier delivery", id + 200,
			5, experience);
	}

	private ActiveTask active(int id, Port pickup, Port delivery)
	{
		return new ActiveTask(task(id, 1, pickup, delivery, 1000), id, 0, 0);
	}
}
