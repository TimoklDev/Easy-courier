package com.easycourier.service;

import com.easycourier.model.Port;
import com.easycourier.model.RoutePreset;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EtceteriaShortcutRouteTest
{
	@Test
	public void requiresEveryShortcutRequirement()
	{
		assertTrue(EtceteriaShortcutRoute.isAvailable(RoutePreset.RELLEKKA, Port.ETCETERIA, 65, 55, true));
		assertFalse(EtceteriaShortcutRoute.isAvailable(RoutePreset.RELLEKKA, Port.ETCETERIA, 64, 55, true));
		assertFalse(EtceteriaShortcutRoute.isAvailable(RoutePreset.RELLEKKA, Port.ETCETERIA, 65, 54, true));
		assertFalse(EtceteriaShortcutRoute.isAvailable(RoutePreset.RELLEKKA, Port.ETCETERIA, 65, 55, false));
		assertFalse(EtceteriaShortcutRoute.isAvailable(RoutePreset.PRIFDDINAS, Port.ETCETERIA, 70, 70, true));
	}

	@Test
	public void providesTheLandRoute()
	{
		assertTrue(EtceteriaShortcutRoute.getPath().size() > 2);
	}
}
