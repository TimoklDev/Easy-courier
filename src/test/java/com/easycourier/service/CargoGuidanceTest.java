package com.easycourier.service;

import com.easycourier.model.GangplankGuidance;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CargoGuidanceTest
{
	@Test
	public void landUsesLedgerUntilPickupCargoIsHeld()
	{
		assertEquals(GangplankGuidance.NONE,
			CargoGuidance.gangplank(false, false, true, false, false, false));
		assertTrue(CargoGuidance.pickupLedger(false, true, false));
	}

	@Test
	public void landBoardsWithHeldPickupCargo()
	{
		assertEquals(GangplankGuidance.BOARD_WITH_CARGO,
			CargoGuidance.gangplank(false, false, true, true, false, false));
		assertFalse(CargoGuidance.pickupLedger(false, true, true));
	}

	@Test
	public void shipDisembarksToCollectRemainingCargo()
	{
		assertEquals(GangplankGuidance.DISEMBARK_TO_COLLECT,
			CargoGuidance.gangplank(true, false, true, false, false, false));
	}

	@Test
	public void shipKeepsHeldPickupCargoAboard()
	{
		assertEquals(GangplankGuidance.NONE,
			CargoGuidance.gangplank(true, false, true, true, false, false));
	}

	@Test
	public void shipDisembarksToDeliverOnlyWhenCargoIsHeld()
	{
		assertEquals(GangplankGuidance.NONE,
			CargoGuidance.gangplank(true, false, false, false, true, false));
		assertEquals(GangplankGuidance.DISEMBARK_TO_DELIVER,
			CargoGuidance.gangplank(true, false, false, false, true, true));
	}

	@Test
	public void landUsesLedgerOnlyForHeldDeliveryCargo()
	{
		assertFalse(CargoGuidance.deliveryLedger(false, true, false));
		assertTrue(CargoGuidance.deliveryLedger(false, true, true));
	}

	@Test
	public void landReturnsToShipForMoreDeliveryCargo()
	{
		assertEquals(GangplankGuidance.BOARD_FOR_CARGO,
			CargoGuidance.gangplank(false, false, false, false, true, false));
	}

	@Test
	public void handoffBoardsWhenCargoIsAtAnotherPort()
	{
		assertEquals(GangplankGuidance.BOARD_BOAT,
			CargoGuidance.gangplank(false, true, false, false, false, false));
	}
}
