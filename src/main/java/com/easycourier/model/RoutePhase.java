package com.easycourier.model;

public enum RoutePhase
{
	IDLE("Ready"),
	COLLECTION("Collect tasks"),
	DELIVERY("Deliver cargo"),
	COMPLETE("Route complete");

	private final String label;

	RoutePhase(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}

