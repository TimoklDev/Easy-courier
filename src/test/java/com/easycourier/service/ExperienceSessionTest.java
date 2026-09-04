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
		session.start(1000);
		clock.addAndGet(1_800_000_000_000L);
		session.update(2000);
		clock.addAndGet(1_800_000_000_000L);
		assertEquals(1000, session.getGained());
		assertEquals(2000L, session.getPerHour());
	}

	@Test
	public void excludesIdleTimeBeforeFirstGain()
	{
		AtomicLong clock = new AtomicLong(1L);
		ExperienceSession session = new ExperienceSession(clock::get);
		session.start(1000);
		clock.addAndGet(18_000_000_000_000L);
		assertEquals(0L, session.getPerHour());
		session.update(1100);
		clock.addAndGet(1_800_000_000_000L);
		assertEquals(200L, session.getPerHour());
	}

	@Test
	public void resetStartsANewSession()
	{
		AtomicLong clock = new AtomicLong(1L);
		ExperienceSession session = new ExperienceSession(clock::get);
		session.start(1000);
		session.update(2000);
		session.reset();
		session.start(2000);
		assertEquals(0, session.getGained());
		assertEquals(0L, session.getPerHour());
	}

	@Test
	public void ignoresUpdatesUntilSessionStarts()
	{
		AtomicLong clock = new AtomicLong(1L);
		ExperienceSession session = new ExperienceSession(clock::get);
		session.update(1082080);
		assertEquals(0, session.getGained());
		assertEquals(0L, session.getPerHour());
		session.start(1082080);
		assertEquals(0, session.getGained());
		assertEquals(0L, session.getPerHour());
	}
}
