package com.easycourier.data;

import com.easycourier.model.Port;
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

	@Test
	public void fillsMissingCivitasTaskRewards()
	{
		assertEquals(436, ExperienceTable.forTask(0, 38, Port.CIVITAS_ILLA_FORTIS,
			Port.CIVITAS_ILLA_FORTIS, Port.PORT_PISCARILIUS, 2));
		assertEquals(453, ExperienceTable.forTask(0, 38, Port.CIVITAS_ILLA_FORTIS,
			Port.CIVITAS_ILLA_FORTIS, Port.PORT_PISCARILIUS, 3));
	}
}

