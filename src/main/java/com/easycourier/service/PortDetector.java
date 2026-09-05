package com.easycourier.service;

import com.easycourier.model.Port;
import com.easycourier.model.RouteStep;
import com.easycourier.model.StepKind;
import net.runelite.api.coords.WorldPoint;

public final class PortDetector
{
	private static final int LAND_DISTANCE = 110;
	private static final int SAILING_DISTANCE = 20;

	public Port detect(WorldPoint point, RouteStep step, boolean aboardBoat)
	{
		return detect(point, step, aboardBoat, false);
	}

	public Port detect(WorldPoint point, RouteStep step, boolean aboardBoat, boolean docked)
	{
		if (point == null)
		{
			return Port.UNKNOWN;
		}
		if (step != null && step.getKind() == StepKind.TRAVEL)
		{
			if (aboardBoat && docked)
			{
				Port nearest = Port.nearest(point, LAND_DISTANCE);
				return nearest == step.getPort() ? nearest : Port.UNKNOWN;
			}
			return point.distanceTo2D(step.getPort().getMapPoint()) <= SAILING_DISTANCE
				? step.getPort() : Port.UNKNOWN;
		}
		int maximumDistance = aboardBoat && !docked ? SAILING_DISTANCE : LAND_DISTANCE;
		return Port.nearest(point, maximumDistance);
	}
}
