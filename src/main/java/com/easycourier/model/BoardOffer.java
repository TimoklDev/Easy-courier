package com.easycourier.model;

import net.runelite.api.widgets.Widget;

public final class BoardOffer
{
	private final Widget widget;
	private final TaskDefinition task;
	private final OfferStatus status;
	private final int score;
	private final String reason;

	public BoardOffer(Widget widget, TaskDefinition task, OfferStatus status, int score, String reason)
	{
		this.widget = widget;
		this.task = task;
		this.status = status;
		this.score = score;
		this.reason = reason;
	}

	public Widget getWidget()
	{
		return widget;
	}

	public TaskDefinition getTask()
	{
		return task;
	}

	public OfferStatus getStatus()
	{
		return status;
	}

	public int getScore()
	{
		return score;
	}

	public String getReason()
	{
		return reason;
	}
}

