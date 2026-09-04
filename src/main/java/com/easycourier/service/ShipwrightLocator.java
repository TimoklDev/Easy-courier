package com.easycourier.service;

import com.easycourier.model.Port;
import com.easycourier.model.Shipwright;

public final class ShipwrightLocator
{
	private final SeaNetwork seaNetwork;

	public ShipwrightLocator(SeaNetwork seaNetwork)
	{
		this.seaNetwork = seaNetwork;
	}

	public Shipwright nearestTo(Port target, int sailingLevel)
	{
		if (target == null || target == Port.UNKNOWN)
		{
			return null;
		}
		Shipwright best = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		for (Shipwright shipwright : Shipwright.values())
		{
			Port port = shipwright.getPort();
			if (port.getSailingLevel() > sailingLevel)
			{
				continue;
			}
			double distance = seaNetwork.distance(port, target);
			if (distance < bestDistance)
			{
				best = shipwright;
				bestDistance = distance;
			}
		}
		return best;
	}
}
