package com.easycourier.service;

import com.easycourier.model.ActiveTask;
import com.easycourier.model.CargoHoldGuidance;
import com.easycourier.model.GangplankGuidance;
import com.easycourier.model.Port;
import com.easycourier.model.TaskDefinition;
import java.util.Collections;
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
		assertTrue(CargoGuidance.pickupLedger(false, true, false, false));
	}

	@Test
	public void landBoardsWithHeldPickupCargo()
	{
		assertEquals(GangplankGuidance.BOARD_WITH_CARGO,
			CargoGuidance.gangplank(false, false, true, true, false, false));
		assertFalse(CargoGuidance.pickupLedger(false, true, true, false));
	}

	@Test
	public void heldDeliveryCargoTakesPriorityOverCollectingMoreCargo()
	{
		assertEquals(GangplankGuidance.NONE,
			CargoGuidance.gangplank(false, false, true, false, true, true));
		assertFalse(CargoGuidance.pickupLedger(false, true, false, true));
		assertTrue(CargoGuidance.deliveryLedger(false, true, true));
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
	public void shipWithdrawsDeliveryCargoBeforeDisembarking()
	{
		assertEquals(CargoHoldGuidance.WITHDRAW,
			CargoGuidance.cargoHold(true, false, true, false));
		assertEquals(GangplankGuidance.NONE,
			CargoGuidance.gangplank(true, false, true, false, true, false));
		assertEquals(CargoHoldGuidance.NONE,
			CargoGuidance.cargoHold(true, false, true, true));
	}

	@Test
	public void shipDepositsNewPickupCargoBeforeReturningToTheDock()
	{
		assertEquals(CargoHoldGuidance.DEPOSIT,
			CargoGuidance.cargoHold(true, true, false, false));
		assertEquals(CargoHoldGuidance.NONE,
			CargoGuidance.cargoHold(false, true, false, false));
	}

	@Test
	public void recognizesEveryNamedCargoHoldVariant()
	{
		assertTrue(CargoGuidance.isCargoHoldName("Teak cargo hold"));
		assertTrue(CargoGuidance.isCargoHoldName("Camphor Cargo Hold"));
		assertFalse(CargoGuidance.isCargoHoldName("Cargo rack"));
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

	@Test
	public void identifiesOnlyDeliverableCargoForTheCurrentPort()
	{
		ActiveTask task = active(4100, Port.ETCETERIA, 3, 0);
		assertTrue(CargoGuidance.isDeliveryCargoAtPort(4100, Port.ETCETERIA,
			Collections.singletonList(task)));
		assertFalse(CargoGuidance.isDeliveryCargoAtPort(4101, Port.ETCETERIA,
			Collections.singletonList(task)));
		assertFalse(CargoGuidance.isDeliveryCargoAtPort(4100, Port.RELLEKKA,
			Collections.singletonList(task)));
	}

	@Test
	public void ignoresCargoThatHasNotBeenCollected()
	{
		ActiveTask task = active(4200, Port.ETCETERIA, 0, 0);
		assertFalse(CargoGuidance.isDeliveryCargoAtPort(4200, Port.ETCETERIA,
			Collections.singletonList(task)));
	}

	private ActiveTask active(int cargoItemId, Port delivery, int taken, int delivered)
	{
		TaskDefinition definition = new TaskDefinition(1, 1, 1, Port.PORT_ROBERTS,
			Port.PORT_ROBERTS, delivery, "Cargo", cargoItemId, 5, 1000);
		return new ActiveTask(definition, 0, taken, delivered);
	}
}
