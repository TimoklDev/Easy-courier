package com.easycourier.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CollectionStop
{
	private final Port port;
	private final String travelInstruction;
	private final int minimumLevel;
	private final boolean charterRequired;
	private final Port sailingStart;
	private final Set<TaskEdge> accepted;
	private final Set<TaskEdge> preferred;
	private final TaskEdge reservedTask;

	public CollectionStop(Port port, String travelInstruction, int minimumLevel, boolean charterRequired, Port sailingStart,
		TaskEdge reservedTask, TaskEdge[] accepted, TaskEdge[] preferred)
	{
		this.port = port;
		this.travelInstruction = travelInstruction;
		this.minimumLevel = minimumLevel;
		this.charterRequired = charterRequired;
		this.sailingStart = sailingStart;
		this.reservedTask = reservedTask;
		this.accepted = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(accepted)));
		this.preferred = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(preferred)));
	}

	public Port getPort()
	{
		return port;
	}

	public String getTravelInstruction()
	{
		return travelInstruction;
	}

	public int getMinimumLevel()
	{
		return minimumLevel;
	}

	public boolean isCharterRequired()
	{
		return charterRequired;
	}

	public boolean isSailingLeg()
	{
		return sailingStart != Port.UNKNOWN;
	}

	public Port getSailingStart()
	{
		return sailingStart;
	}

	public Set<TaskEdge> getAccepted()
	{
		return accepted;
	}

	public boolean isPreferred(TaskDefinition task)
	{
		return preferred.stream().anyMatch(edge -> edge.matches(task));
	}

	public boolean accepts(TaskDefinition task)
	{
		return accepted.stream().anyMatch(edge -> edge.matches(task));
	}

	public TaskEdge getReservedTask()
	{
		return reservedTask;
	}
}
