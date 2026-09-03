package com.easycourier.service;

import com.easycourier.model.ActiveTask;
import com.easycourier.model.BoardOffer;
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

public class RouteAdvisorTest
{
	private final RouteAdvisor advisor = new RouteAdvisor();

	@Test
	public void rejectsBackwardTaskDuringDelivery()
	{
		TaskDefinition backward = task(1, 70, Port.DEEPFIN_POINT, Port.ALDARIN, 5000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.DELIVERY, 0, 99, 0,
			Collections.emptyList(), Collections.singletonList(widget(backward)));
		assertEquals(OfferStatus.OFF_ROUTE, offers.get(0).getStatus());
	}

	@Test
	public void marksUnavailableTaskAsIneligible()
	{
		TaskDefinition highLevel = task(2, 76, Port.ALDARIN, Port.LUNAR_ISLE, 12670);
		List<BoardOffer> offers = advisor.advise(RoutePreset.LUNAR_ISLE, RoutePhase.COLLECTION, 0, 75, 0,
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
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION, 0, 70, 3,
			active, Collections.singletonList(widget(useful)));
		assertEquals(OfferStatus.OFF_ROUTE, offers.get(0).getStatus());
		assertEquals("Keep a slot for a better task", offers.get(0).getReason());
	}

	@Test
	public void highlightsReservedTaskAsPriority()
	{
		TaskDefinition reserved = task(4, 70, Port.ALDARIN, Port.PRIFDDINAS, 7082);
		TaskDefinition useful = task(5, 70, Port.DEEPFIN_POINT, Port.PRIFDDINAS, 4721);
		List<BoardOffer> offers = advisor.advise(RoutePreset.PRIFDDINAS, RoutePhase.COLLECTION, 0, 70, 0,
			Collections.emptyList(), Arrays.asList(widget(useful), widget(reserved)));
		assertEquals(OfferStatus.PRIORITY, offers.get(0).getStatus());
	}

	@Test
	public void preferredRellekkaTaskFulfilsTheReserveRule()
	{
		List<ActiveTask> active = new ArrayList<>();
		active.add(active(20, Port.SUNSET_COAST, Port.RELLEKKA));
		active.add(active(21, Port.PORT_ROBERTS, Port.RELLEKKA));
		active.add(active(22, Port.PORT_PISCARILIUS, Port.RELLEKKA));
		TaskDefinition useful = task(6, 62, Port.PORT_ROBERTS, Port.RELLEKKA, 3000);
		List<BoardOffer> offers = advisor.advise(RoutePreset.RELLEKKA, RoutePhase.COLLECTION, 0, 62, 3,
			active, Collections.singletonList(widget(useful)));
		assertEquals(OfferStatus.USEFUL, offers.get(0).getStatus());
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
