package com.easycourier.data;

import com.easycourier.model.ActiveTask;
import com.easycourier.model.TaskDefinition;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

public final class TaskStateReader
{
	private static final int[] TASK_IDS = {
		VarbitID.PORT_TASK_SLOT_0_ID,
		VarbitID.PORT_TASK_SLOT_1_ID,
		VarbitID.PORT_TASK_SLOT_2_ID,
		VarbitID.PORT_TASK_SLOT_3_ID,
		VarbitID.PORT_TASK_SLOT_4_ID
	};
	private static final int[] CARGO_TAKEN = {
		VarbitID.PORT_TASK_SLOT_0_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_1_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_2_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_3_CARGO_TAKEN,
		VarbitID.PORT_TASK_SLOT_4_CARGO_TAKEN
	};
	private static final int[] CARGO_DELIVERED = {
		VarbitID.PORT_TASK_SLOT_0_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_1_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_2_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_3_CARGO_DELIVERED,
		VarbitID.PORT_TASK_SLOT_4_CARGO_DELIVERED
	};

	public List<ActiveTask> read(Client client, TaskCatalog catalog)
	{
		List<ActiveTask> tasks = new ArrayList<>();
		int[] varps = client.getVarps();
		if (varps == null)
		{
			return tasks;
		}
		for (int slot = 0; slot < TASK_IDS.length; slot++)
		{
			int taskId = client.getVarbitValue(varps, TASK_IDS[slot]);
			TaskDefinition definition = catalog.byTaskId(taskId);
			if (definition == null || !definition.isCourier())
			{
				continue;
			}
			int taken = client.getVarbitValue(varps, CARGO_TAKEN[slot]);
			int delivered = client.getVarbitValue(varps, CARGO_DELIVERED[slot]);
			tasks.add(new ActiveTask(definition, slot, taken, delivered));
		}
		return tasks;
	}

	public int countOccupied(Client client)
	{
		int[] varps = client.getVarps();
		if (varps == null)
		{
			return 0;
		}
		int occupied = 0;
		for (int taskId : TASK_IDS)
		{
			if (client.getVarbitValue(varps, taskId) != 0)
			{
				occupied++;
			}
		}
		return occupied;
	}

	public static int taskCapacity(int sailingLevel)
	{
		if (sailingLevel >= 84)
		{
			return 5;
		}
		if (sailingLevel >= 56)
		{
			return 4;
		}
		if (sailingLevel >= 28)
		{
			return 3;
		}
		if (sailingLevel >= 7)
		{
			return 2;
		}
		return 1;
	}
}
