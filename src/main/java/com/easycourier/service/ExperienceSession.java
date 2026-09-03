package com.easycourier.service;

import java.util.function.LongSupplier;

public final class ExperienceSession
{
	private final LongSupplier clock;
	private int startingExperience = -1;
	private int currentExperience = -1;
	private long startedAt;

	public ExperienceSession()
	{
		this(System::nanoTime);
	}

	ExperienceSession(LongSupplier clock)
	{
		this.clock = clock;
	}

	public void reset()
	{
		startingExperience = -1;
		currentExperience = -1;
		startedAt = 0L;
	}

	public void record(int experience)
	{
		if (startingExperience < 0)
		{
			startingExperience = experience;
			startedAt = clock.getAsLong();
		}
		currentExperience = experience;
	}

	public int getGained()
	{
		if (startingExperience < 0 || currentExperience < 0)
		{
			return 0;
		}
		return Math.max(0, currentExperience - startingExperience);
	}

	public long getPerHour()
	{
		int gained = getGained();
		long elapsed = clock.getAsLong() - startedAt;
		if (gained == 0 || elapsed <= 0L)
		{
			return 0L;
		}
		return Math.round(gained * 3_600_000_000_000D / elapsed);
	}
}
