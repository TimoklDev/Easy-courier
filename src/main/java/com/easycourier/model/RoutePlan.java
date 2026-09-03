package com.easycourier.model;

import java.util.Collections;
import java.util.List;

public final class RoutePlan
{
	private final List<Port> portOrder;
	private final List<Port> seaPath;
	private final List<RouteStep> steps;
	private final int totalExperience;
	private final double distance;

	public RoutePlan(List<Port> portOrder, List<Port> seaPath, List<RouteStep> steps, int totalExperience, double distance)
	{
		this.portOrder = Collections.unmodifiableList(portOrder);
		this.seaPath = Collections.unmodifiableList(seaPath);
		this.steps = Collections.unmodifiableList(steps);
		this.totalExperience = totalExperience;
		this.distance = distance;
	}

	public List<Port> getPortOrder()
	{
		return portOrder;
	}

	public List<Port> getSeaPath()
	{
		return seaPath;
	}

	public List<RouteStep> getSteps()
	{
		return steps;
	}

	public int getTotalExperience()
	{
		return totalExperience;
	}

	public double getDistance()
	{
		return distance;
	}

	public Port nextPort(Port current)
	{
		for (Port port : portOrder)
		{
			if (port != current)
			{
				return port;
			}
		}
		return portOrder.isEmpty() ? null : portOrder.get(portOrder.size() - 1);
	}
}

