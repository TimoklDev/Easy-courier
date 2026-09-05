package com.easycourier.service;

import com.easycourier.model.Port;
import com.easycourier.model.RouteStep;
import com.easycourier.model.StepKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PortDetectorTest
{
	private final PortDetector detector = new PortDetector();

	@Test
	public void ignoresUnrelatedPortsDuringTravel()
	{
		RouteStep step = travelTo(Port.DEEPFIN_POINT);
		assertEquals(Port.UNKNOWN, detector.detect(Port.SUNSET_COAST.getMapPoint(), step, true));
	}

	@Test
	public void doesNotArriveTwentyOneTilesEarly()
	{
		RouteStep step = travelTo(Port.DEEPFIN_POINT);
		assertEquals(Port.UNKNOWN, detector.detect(Port.DEEPFIN_POINT.getMapPoint().dx(21), step, true));
	}

	@Test
	public void arrivesWithinTwentyTiles()
	{
		RouteStep step = travelTo(Port.DEEPFIN_POINT);
		assertEquals(Port.DEEPFIN_POINT, detector.detect(Port.DEEPFIN_POINT.getMapPoint().dx(20), step, true));
	}

	@Test
	public void usesTightDetectionWithoutATravelStepWhileAboard()
	{
		assertEquals(Port.UNKNOWN, detector.detect(Port.SUNSET_COAST.getMapPoint().dx(21), null, true));
		assertEquals(Port.SUNSET_COAST, detector.detect(Port.SUNSET_COAST.getMapPoint().dx(20), null, true));
	}

	@Test
	public void usesDockInteractionToDetectThePortRange()
	{
		assertEquals(Port.UNKNOWN,
			detector.detect(Port.RELLEKKA.getMapPoint().dx(40), null, true, false));
		assertEquals(Port.RELLEKKA,
			detector.detect(Port.RELLEKKA.getMapPoint().dx(40), null, true, true));
	}

	@Test
	public void dockedBoatDoesNotCompleteAVisitToANearbyDifferentPort()
	{
		RouteStep step = new RouteStep(StepKind.TRAVEL, Port.NEITIZNOT, "Sail", "Sail", 0);
		assertEquals(Port.UNKNOWN, detector.detect(Port.JATIZSO.getMapPoint(), step, true, true));
	}

	@Test
	public void waitsUntilTheEndOfTheEtceteriaShortcutRoute()
	{
		RouteStep step = EtceteriaShortcutRoute.getTravelStep();
		assertEquals(Port.UNKNOWN, detector.detect(EtceteriaShortcutRoute.getPath().get(0), step, false));
		assertEquals(Port.ETCETERIA, detector.detect(
			EtceteriaShortcutRoute.getPath().get(EtceteriaShortcutRoute.getPath().size() - 1), step, false));
	}

	private RouteStep travelTo(Port port)
	{
		return new RouteStep(StepKind.TRAVEL, port, "Sail", "Follow the route", 0);
	}
}
