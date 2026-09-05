package com.easycourier.model;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoutePlanTest
{
	@Test
	public void identifiesSegmentsInTheCurrentPortLeg()
	{
		WorldPoint firstTurn = new WorldPoint(1800, 3220, 0);
		WorldPoint secondTurn = new WorldPoint(1600, 3050, 0);
		RoutePlan plan = new RoutePlan(
			Arrays.asList(Port.PORT_ROBERTS, Port.CIVITAS_ILLA_FORTIS, Port.ALDARIN),
			Arrays.asList(Port.PORT_ROBERTS.getMapPoint(), firstTurn, Port.CIVITAS_ILLA_FORTIS.getMapPoint(),
				secondTurn, Port.ALDARIN.getMapPoint()),
			Collections.emptyList(), 0, 0);
		assertTrue(plan.isSegmentOnLeg(0, Port.CIVITAS_ILLA_FORTIS));
		assertTrue(plan.isSegmentOnLeg(1, Port.CIVITAS_ILLA_FORTIS));
		assertFalse(plan.isSegmentOnLeg(2, Port.CIVITAS_ILLA_FORTIS));
		assertFalse(plan.isSegmentOnLeg(1, Port.ALDARIN));
		assertTrue(plan.isSegmentOnLeg(2, Port.ALDARIN));
		assertTrue(plan.isSegmentOnLeg(3, Port.ALDARIN));
	}
}
