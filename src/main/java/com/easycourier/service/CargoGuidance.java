package com.easycourier.service;

import com.easycourier.model.GangplankGuidance;

public final class CargoGuidance
{
	private CargoGuidance()
	{
	}

	public static GangplankGuidance gangplank(boolean aboard, boolean handoffBoardingNeeded,
		boolean pickupNeeded, boolean pickupCargoHeld, boolean deliveryAvailable, boolean deliveryCargoHeld)
	{
		if (aboard)
		{
			if (deliveryCargoHeld)
			{
				return GangplankGuidance.DISEMBARK_TO_DELIVER;
			}
			if (pickupNeeded && !pickupCargoHeld)
			{
				return GangplankGuidance.DISEMBARK_TO_COLLECT;
			}
			return GangplankGuidance.NONE;
		}
		if (deliveryCargoHeld)
		{
			return GangplankGuidance.NONE;
		}
		if (pickupNeeded)
		{
			return pickupCargoHeld ? GangplankGuidance.BOARD_WITH_CARGO : GangplankGuidance.NONE;
		}
		if (pickupCargoHeld)
		{
			return GangplankGuidance.BOARD_WITH_CARGO;
		}
		if (deliveryAvailable)
		{
			return GangplankGuidance.BOARD_FOR_CARGO;
		}
		return handoffBoardingNeeded ? GangplankGuidance.BOARD_BOAT : GangplankGuidance.NONE;
	}

	public static boolean pickupLedger(boolean aboard, boolean pickupNeeded, boolean pickupCargoHeld)
	{
		return !aboard && pickupNeeded && !pickupCargoHeld;
	}

	public static boolean deliveryLedger(boolean aboard, boolean deliveryAvailable, boolean deliveryCargoHeld)
	{
		return !aboard && deliveryAvailable && deliveryCargoHeld;
	}
}
