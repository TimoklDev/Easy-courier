package com.easycourier.model;

public final class ActiveTask
{
	private final TaskDefinition definition;
	private final int slot;
	private int cargoTaken;
	private int cargoDelivered;

	public ActiveTask(TaskDefinition definition, int slot, int cargoTaken, int cargoDelivered)
	{
		this.definition = definition;
		this.slot = slot;
		this.cargoTaken = cargoTaken;
		this.cargoDelivered = cargoDelivered;
	}

	public TaskDefinition getDefinition()
	{
		return definition;
	}

	public int getSlot()
	{
		return slot;
	}

	public int getCargoTaken()
	{
		return cargoTaken;
	}

	public void setCargoTaken(int cargoTaken)
	{
		this.cargoTaken = cargoTaken;
	}

	public int getCargoDelivered()
	{
		return cargoDelivered;
	}

	public void setCargoDelivered(int cargoDelivered)
	{
		this.cargoDelivered = cargoDelivered;
	}

	public boolean needsPickup()
	{
		return cargoTaken < definition.getCargoAmount();
	}

	public boolean needsDelivery()
	{
		return cargoDelivered < definition.getCargoAmount();
	}

	public boolean canDeliver()
	{
		return cargoTaken > cargoDelivered;
	}

	public boolean isComplete()
	{
		return !needsDelivery();
	}
}

