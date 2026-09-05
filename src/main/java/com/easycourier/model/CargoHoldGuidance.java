package com.easycourier.model;

public enum CargoHoldGuidance
{
	NONE(null),
	WITHDRAW("Withdraw cargo"),
	DEPOSIT("Deposit cargo");

	private final String label;

	CargoHoldGuidance(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
