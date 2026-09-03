package com.easycourier.service;

import com.easycourier.model.Port;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeaNetworkTest
{
	private final SeaNetwork network = new SeaNetwork();

	@Test
	public void usesDetailedSeaWaypoints()
	{
		List<WorldPoint> path = network.path(Port.ALDARIN, Port.PRIFDDINAS);
		assertEquals(Port.ALDARIN.getMapPoint(), path.get(0));
		assertEquals(Port.PRIFDDINAS.getMapPoint(), path.get(path.size() - 1));
		assertTrue(path.size() > 20);
		assertTrue(network.distance(Port.ALDARIN, Port.PRIFDDINAS)
			> Port.ALDARIN.getMapPoint().distanceTo2D(Port.PRIFDDINAS.getMapPoint()));
	}

	@Test
	public void reversesTheSameDetailedRoute()
	{
		List<WorldPoint> forward = network.path(Port.DEEPFIN_POINT, Port.PORT_TYRAS);
		List<WorldPoint> reverse = network.path(Port.PORT_TYRAS, Port.DEEPFIN_POINT);
		assertEquals(forward.get(0), reverse.get(reverse.size() - 1));
		assertEquals(forward.get(forward.size() - 1), reverse.get(0));
		assertEquals(forward.size(), reverse.size());
	}

	@Test
	public void usesDirectSunsetCoastRouteToPortRoberts()
	{
		List<WorldPoint> forward = network.path(Port.SUNSET_COAST, Port.PORT_ROBERTS);
		List<WorldPoint> reverse = network.path(Port.PORT_ROBERTS, Port.SUNSET_COAST);
		assertFalse(forward.contains(Port.ALDARIN.getMapPoint()));
		assertFalse(reverse.contains(Port.ALDARIN.getMapPoint()));
		assertFalse(forward.contains(Port.CIVITAS_ILLA_FORTIS.getMapPoint()));
		assertFalse(reverse.contains(Port.CIVITAS_ILLA_FORTIS.getMapPoint()));
		assertEquals(Port.SUNSET_COAST.getMapPoint(), forward.get(0));
		assertEquals(Port.PORT_ROBERTS.getMapPoint(), forward.get(forward.size() - 1));
	}

	@Test
	public void usesDirectPortRobertsRouteToHosidius()
	{
		List<WorldPoint> forward = network.path(Port.PORT_ROBERTS, Port.HOSIDIUS);
		List<WorldPoint> reverse = network.path(Port.HOSIDIUS, Port.PORT_ROBERTS);
		assertFalse(forward.contains(Port.PORT_PISCARILIUS.getMapPoint()));
		assertFalse(reverse.contains(Port.PORT_PISCARILIUS.getMapPoint()));
		assertEquals(Port.PORT_ROBERTS.getMapPoint(), forward.get(0));
		assertEquals(Port.HOSIDIUS.getMapPoint(), forward.get(forward.size() - 1));
	}
}
