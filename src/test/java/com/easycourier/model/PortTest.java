package com.easycourier.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PortTest
{
	@Test
	public void resolvesNorthernIslandDockRows()
	{
		assertEquals(Port.JATIZSO, Port.fromDatabaseRow(8614));
		assertEquals(Port.NEITIZNOT, Port.fromDatabaseRow(8615));
	}
}
