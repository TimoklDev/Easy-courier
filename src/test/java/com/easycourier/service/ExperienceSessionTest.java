package com.easycourier.service;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExperienceSessionTest
{
	@Test
	public void tracksGainedExperienceAndHourlyRate()
	{
		AtomicLong clock = new AtomicLong(1L);
		ExperienceSession session = new ExperienceSession(clock::get);
		session.record(1000);
		clock.addAndGet(1_800_000_000_000L);
		session.record(2000);
		assertEquals(1000, session.getGained());
		assertEquals(2000L, session.getPerHour());
	}

	@Test
	public void resetStartsANewSession()
	{
		AtomicLong clock = new AtomicLong(1L);
		ExperienceSession session = new ExperienceSession(clock::get);
		session.record(1000);
		session.record(2000);
		session.reset();
		session.record(2000);
		assertEquals(0, session.getGained());
		assertEquals(0L, session.getPerHour());
	}
}
