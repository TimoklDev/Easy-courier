package com.easycourier.data;

import com.easycourier.model.Port;
import com.easycourier.model.TaskDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;

public final class TaskCatalog
{
	private final Map<Integer, TaskDefinition> byDatabaseRow = new HashMap<>();
	private final Map<Integer, TaskDefinition> byTaskId = new HashMap<>();

	public void load(Client client)
	{
		byDatabaseRow.clear();
		byTaskId.clear();
		for (int databaseRow : client.getDBTableRows(DBTableID.PortTask.ID))
		{
			TaskDefinition task = read(client, databaseRow);
			if (task != null)
			{
				byDatabaseRow.put(databaseRow, task);
				byTaskId.put(task.getTaskId(), task);
			}
		}
	}

	public TaskDefinition byDatabaseRow(int databaseRow)
	{
		return byDatabaseRow.get(databaseRow);
	}

	public TaskDefinition byTaskId(int taskId)
	{
		return byTaskId.get(taskId);
	}

	public Collection<TaskDefinition> all()
	{
		return Collections.unmodifiableCollection(byDatabaseRow.values());
	}

	private TaskDefinition read(Client client, int databaseRow)
	{
		Integer taskId = integer(client, databaseRow, DBTableID.PortTask.COL_TASK_ID, 0, 0);
		Integer level = integer(client, databaseRow, DBTableID.PortTask.COL_LEVEL_REQUIRED, 0, 0);
		Integer boardRow = integer(client, databaseRow, DBTableID.PortTask.COL_STARTING_PORT, 0, 0);
		Integer pickupRow = integer(client, databaseRow, DBTableID.PortTask.COL_CARGO_PORT, 0, 0);
		Integer deliveryRow = integer(client, databaseRow, DBTableID.PortTask.COL_ENDING_PORT, 0, 0);
		Integer cargoItem = integer(client, databaseRow, DBTableID.PortTask.COL_CARGO, 0, 0);
		Integer cargoAmount = integer(client, databaseRow, DBTableID.PortTask.COL_CARGO, 0, 1);
		String name = string(client, databaseRow, DBTableID.PortTask.COL_NAME);
		if (taskId == null || level == null || boardRow == null || pickupRow == null || deliveryRow == null || name == null)
		{
			return null;
		}
		return new TaskDefinition(
			databaseRow,
			taskId,
			level,
			Port.fromDatabaseRow(boardRow),
			Port.fromDatabaseRow(pickupRow),
			Port.fromDatabaseRow(deliveryRow),
			name,
			cargoItem == null ? -1 : cargoItem,
			cargoAmount == null ? 0 : cargoAmount,
			ExperienceTable.forDatabaseRow(databaseRow));
	}

	private Integer integer(Client client, int row, int column, int tuple, int index)
	{
		Object[] values = client.getDBTableField(row, column, tuple);
		if (values == null || values.length <= index || !(values[index] instanceof Integer))
		{
			return null;
		}
		return (Integer) values[index];
	}

	private String string(Client client, int row, int column)
	{
		Object[] values = client.getDBTableField(row, column, 0);
		if (values == null || values.length == 0 || !(values[0] instanceof String))
		{
			return null;
		}
		return (String) values[0];
	}
}

