package com.easycourier.model;

public final class TaskDefinition
{
	private final int databaseRow;
	private final int taskId;
	private final int levelRequired;
	private final Port noticeBoard;
	private final Port pickup;
	private final Port delivery;
	private final String name;
	private final int cargoItemId;
	private final int cargoAmount;
	private final int experience;

	public TaskDefinition(int databaseRow, int taskId, int levelRequired, Port noticeBoard, Port pickup,
		Port delivery, String name, int cargoItemId, int cargoAmount, int experience)
	{
		this.databaseRow = databaseRow;
		this.taskId = taskId;
		this.levelRequired = levelRequired;
		this.noticeBoard = noticeBoard;
		this.pickup = pickup;
		this.delivery = delivery;
		this.name = name;
		this.cargoItemId = cargoItemId;
		this.cargoAmount = cargoAmount;
		this.experience = experience;
	}

	public int getDatabaseRow()
	{
		return databaseRow;
	}

	public int getTaskId()
	{
		return taskId;
	}

	public int getLevelRequired()
	{
		return levelRequired;
	}

	public Port getNoticeBoard()
	{
		return noticeBoard;
	}

	public Port getPickup()
	{
		return pickup;
	}

	public Port getDelivery()
	{
		return delivery;
	}

	public String getName()
	{
		return name;
	}

	public int getCargoItemId()
	{
		return cargoItemId;
	}

	public int getCargoAmount()
	{
		return cargoAmount;
	}

	public int getExperience()
	{
		return experience;
	}

	public boolean isCourier()
	{
		return pickup != Port.UNKNOWN && delivery != Port.UNKNOWN && pickup != delivery && !name.toLowerCase().contains("bounty");
	}

	public String routeLabel()
	{
		return pickup + " to " + delivery;
	}
}

