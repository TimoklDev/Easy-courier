package com.easycourier.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExperienceTableTest
{
	@Test
	public void containsThePublishedLunarRouteRewards()
	{
		assertEquals(12671, ExperienceTable.forDatabaseRow(9100));
		assertEquals(7381, ExperienceTable.forDatabaseRow(9096));
		assertEquals(9092, ExperienceTable.forDatabaseRow(9092));
		assertEquals(5851, ExperienceTable.forDatabaseRow(9094));
		assertEquals(5491, ExperienceTable.forDatabaseRow(9087));
		assertEquals(3151, ExperienceTable.forDatabaseRow(9085));
		assertEquals(7082, ExperienceTable.forDatabaseRow(9037));
		assertEquals(4721, ExperienceTable.forDatabaseRow(9035));
	}
}

