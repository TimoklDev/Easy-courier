package com.easycourier.service;

import com.easycourier.model.ActiveTask;
import com.easycourier.model.GangplankGuidance;
import com.easycourier.model.Port;
import java.util.List;

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

	public static boolean isDeliveryCargoAtPort(int itemId, Port port, List<ActiveTask> tasks)
	{
		if (itemId <= 0 || port == Port.UNKNOWN)
		{
			return false;
		}
		return tasks.stream().anyMatch(task -> task.getDefinition().getCargoItemId() == itemId
			&& task.getDefinition().getDelivery() == port && task.canDeliver());
	}
}
