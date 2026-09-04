package com.easycourier.service;

import com.easycourier.model.Port;
import com.easycourier.model.Shipwright;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShipwrightLocatorTest
{
	private final SeaNetwork network = new SeaNetwork();
	private final ShipwrightLocator locator = new ShipwrightLocator(network);

	@Test
	public void usesShipwrightAtTheRouteStartWhenAvailable()
	{
		assertEquals(Shipwright.SCOTT, locator.nearestTo(Port.PORT_ROBERTS, 50));
	}

	@Test
	public void usesNearestAvailableShipwrightForCargoPort()
	{
		Shipwright closest = locator.nearestTo(Port.HOSIDIUS, 62);
		double distance = network.distance(closest.getPort(), Port.HOSIDIUS);
		for (Shipwright candidate : Shipwright.values())
		{
			if (candidate.getPort().getSailingLevel() <= 62)
			{
				assertTrue(distance <= network.distance(candidate.getPort(), Port.HOSIDIUS));
			}
		}
	}

	@Test
	public void respectsSailingLevel()
	{
		Shipwright shipwright = locator.nearestTo(Port.PORT_ROBERTS, 49);
		assertTrue(shipwright.getPort().getSailingLevel() <= 49);
	}
}
