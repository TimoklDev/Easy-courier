package com.easycourier.model;

import java.util.Objects;

public final class TaskEdge
{
	private final Port pickup;
	private final Port delivery;

	public TaskEdge(Port pickup, Port delivery)
	{
		this.pickup = pickup;
		this.delivery = delivery;
	}

	public Port getPickup()
	{
		return pickup;
	}

	public Port getDelivery()
	{
		return delivery;
	}

	public boolean matches(TaskDefinition task)
	{
		return pickup == task.getPickup() && delivery == task.getDelivery();
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof TaskEdge))
		{
			return false;
		}
		TaskEdge edge = (TaskEdge) other;
		return pickup == edge.pickup && delivery == edge.delivery;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(pickup, delivery);
	}
}

