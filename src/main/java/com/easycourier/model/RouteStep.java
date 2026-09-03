package com.easycourier.model;

public final class RouteStep
{
	private final StepKind kind;
	private final Port port;
	private final String title;
	private final String detail;
	private final int experience;

	public RouteStep(StepKind kind, Port port, String title, String detail, int experience)
	{
		this.kind = kind;
		this.port = port;
		this.title = title;
		this.detail = detail;
		this.experience = experience;
	}

	public StepKind getKind()
	{
		return kind;
	}

	public Port getPort()
	{
		return port;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDetail()
	{
		return detail;
	}

	public int getExperience()
	{
		return experience;
	}
}

