package com.easycourier.model;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

public enum Port
{
	MUSA_POINT(8590, "Musa Point", 10, ObjectID.PORT_TASK_BOARD_MUSA_POINT, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_MUSA_POINT, 2965, 3146),
	PORT_SARIM(8587, "Port Sarim", 1, ObjectID.PORT_TASK_BOARD_PORT_SARIM, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_PORT_SARIM, 3056, 3194),
	PANDEMONIUM(8588, "The Pandemonium", 1, ObjectID.PORT_TASK_BOARD_PANDEMONIUM, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_PANDEMONIUM, 3078, 2987),
	RUINS_OF_UNKAH(8606, "Ruins of Unkah", 48, ObjectID.PORT_TASK_BOARD_RUINS_OF_UNKAH, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_RUINS_OF_UNKAH, 3143, 2824),
	SUMMER_SHORE(8604, "The Summer Shore", 45, ObjectID.PORT_TASK_BOARD_THE_SUMMER_SHORE, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_THE_SUMMER_SHORE, 3174, 2367),
	ALDARIN(8605, "Aldarin", 46, ObjectID.PORT_TASK_BOARD_ALDARIN, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_ALDARIN, 1454, 2977),
	SUNSET_COAST(8603, "Sunset Coast", 44, -1, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_SUNSET_COAST, 1506, 2971),
	CIVITAS_ILLA_FORTIS(8600, "Civitas illa Fortis", 38, ObjectID.PORT_TASK_BOARD_CIVITAS_ILLA_FORTIS, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_CIVITAS_ILLA_FORTIS, 1769, 3144),
	PORT_ROBERTS(8608, "Port Roberts", 50, ObjectID.PORT_TASK_BOARD_PORT_ROBERTS, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_PORT_ROBERTS, 1858, 3307),
	HOSIDIUS(8591, "Hosidius", 5, -1, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_HOSIDIUS, 1726, 3447),
	PORT_PISCARILIUS(8594, "Port Piscarilius", 15, ObjectID.PORT_TASK_BOARD_PORT_PISCARILIUS, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_PORT_PISCARILIUS, 1845, 3681),
	DEEPFIN_POINT(8613, "Deepfin Point", 67, ObjectID.PORT_TASK_BOARD_DEEPFIN_POINT, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_DEEPFIN_POINT, 1923, 2752),
	PORT_TYRAS(8612, "Port Tyras", 66, ObjectID.PORT_TASK_BOARD_PORT_TYRAS, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_PORT_TYRAS, 2141, 3115),
	PRIFDDINAS(8616, "Prifddinas", 70, ObjectID.PORT_TASK_BOARD_PRIFDDINAS, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_PRIFDDINAS, 2158, 3319),
	PISCATORIS(8617, "Piscatoris", 75, -1, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_PISCATORIS, 2300, 3689),
	LUNAR_ISLE(8618, "Lunar Isle", 76, ObjectID.PORT_TASK_BOARD_LUNAR_ISLE, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_LUNAR_ISLE, 2157, 3881),
	RELLEKKA(8610, "Rellekka", 62, ObjectID.PORT_TASK_BOARD_RELLEKKA, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_RELLEKKA, 2630, 3709),
	ETCETERIA(8611, "Etceteria", 65, ObjectID.PORT_TASK_BOARD_ETCETERIA, ObjectID.DOCK_LOADING_BAY_LEDGER_TABLE_ETCETERIA, 2612, 3836),
	UNKNOWN(0, "Unknown port", 1, -1, -1, 0, 0);

	private static final Map<Integer, Port> BY_ROW = new HashMap<>();

	static
	{
		for (Port port : values())
		{
			BY_ROW.put(port.databaseRow, port);
		}
	}

	private final int databaseRow;
	private final String displayName;
	private final int sailingLevel;
	private final int noticeBoardObjectId;
	private final int ledgerObjectId;
	private final WorldPoint mapPoint;

	Port(int databaseRow, String displayName, int sailingLevel, int noticeBoardObjectId, int ledgerObjectId, int x, int y)
	{
		this.databaseRow = databaseRow;
		this.displayName = displayName;
		this.sailingLevel = sailingLevel;
		this.noticeBoardObjectId = noticeBoardObjectId;
		this.ledgerObjectId = ledgerObjectId;
		this.mapPoint = new WorldPoint(x, y, 0);
	}

	public int getDatabaseRow()
	{
		return databaseRow;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getSailingLevel()
	{
		return sailingLevel;
	}

	public boolean hasNoticeBoard()
	{
		return noticeBoardObjectId != -1;
	}

	public int getNoticeBoardObjectId()
	{
		return noticeBoardObjectId;
	}

	public int getLedgerObjectId()
	{
		return ledgerObjectId;
	}

	public WorldPoint getMapPoint()
	{
		return mapPoint;
	}

	public static Port fromDatabaseRow(int row)
	{
		return BY_ROW.getOrDefault(row, UNKNOWN);
	}

	public static Port fromLedgerObjectId(int objectId)
	{
		for (Port port : values())
		{
			if (port.ledgerObjectId == objectId)
			{
				return port;
			}
		}
		return UNKNOWN;
	}

	public static Port fromNoticeBoardObjectId(int objectId)
	{
		for (Port port : values())
		{
			if (port.noticeBoardObjectId == objectId)
			{
				return port;
			}
		}
		return UNKNOWN;
	}

	public static Port nearest(WorldPoint point, int maximumDistance)
	{
		if (point == null)
		{
			return UNKNOWN;
		}
		Port best = UNKNOWN;
		int bestDistance = maximumDistance + 1;
		for (Port port : values())
		{
			if (port == UNKNOWN)
			{
				continue;
			}
			int distance = point.distanceTo2D(port.mapPoint);
			if (distance < bestDistance)
			{
				best = port;
				bestDistance = distance;
			}
		}
		return best;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
