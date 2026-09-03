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
		if (point == null)
		{
			return Port.UNKNOWN;
		}
		if (step != null && step.getKind() == StepKind.TRAVEL)
		{
			return point.distanceTo2D(step.getPort().getMapPoint()) <= SAILING_DISTANCE
				? step.getPort() : Port.UNKNOWN;
		}
		return Port.nearest(point, aboardBoat ? SAILING_DISTANCE : LAND_DISTANCE);
	}
}
