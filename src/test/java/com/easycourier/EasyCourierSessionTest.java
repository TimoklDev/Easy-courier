package com.easycourier;

import net.runelite.api.GameState;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EasyCourierSessionTest
{
	@Test
	public void distinguishesClientSessionsFromWorldChanges()
	{
		assertTrue(EasyCourierPlugin.isExperienceSessionBoundary(GameState.STARTING));
		assertTrue(EasyCourierPlugin.isExperienceSessionBoundary(GameState.LOGIN_SCREEN));
		assertFalse(EasyCourierPlugin.isExperienceSessionBoundary(GameState.HOPPING));
		assertFalse(EasyCourierPlugin.isExperienceSessionBoundary(GameState.LOADING));
		assertFalse(EasyCourierPlugin.isExperienceSessionBoundary(GameState.CONNECTION_LOST));
	}
}
