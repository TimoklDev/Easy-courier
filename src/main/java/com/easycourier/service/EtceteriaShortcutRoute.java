package com.easycourier.service;

import com.easycourier.model.Port;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.RouteStep;
import com.easycourier.model.StepKind;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

public final class EtceteriaShortcutRoute
{
	public static final String INSTRUCTION = "Right click the sailor to go to Etceteria and take the agility shortcut "
		+ "to the notice board.";
	private static final int MINIMUM_SAILING = 65;
	private static final int MINIMUM_AGILITY = 55;
	private static final WorldPoint RELLEKKA_SAILOR = new WorldPoint(2629, 3693, 0);
	private static final WorldPoint STEPPING_STONE = new WorldPoint(2572, 3862, 0);
	private static final RouteStep TRAVEL_STEP = new RouteStep(StepKind.TRAVEL, Port.ETCETERIA,
		"Take the sailor to Etceteria", INSTRUCTION, 0);
	private static final List<WorldPoint> PATH = Collections.unmodifiableList(Arrays.asList(
		new WorldPoint(2581, 3847, 0),
		new WorldPoint(2581, 3845, 0),
		new WorldPoint(2573, 3845, 0),
		new WorldPoint(2573, 3859, 0),
		new WorldPoint(2576, 3861, 0),
		new WorldPoint(2576, 3862, 0),
		new WorldPoint(2580, 3862, 0),
		new WorldPoint(2580, 3861, 0),
		new WorldPoint(2607, 3861, 0),
		new WorldPoint(2607, 3858, 0),
		new WorldPoint(2611, 3858, 0),
		new WorldPoint(2611, 3856, 0),
		new WorldPoint(2615, 3856, 0),
		new WorldPoint(2615, 3855, 0),
		new WorldPoint(2616, 3855, 0),
		new WorldPoint(2616, 3849, 0)));

	private EtceteriaShortcutRoute()
	{
	}

	public static boolean isAvailable(RoutePreset route, Port stop, int sailingLevel, int agilityLevel,
		boolean fremennikTrialsComplete)
	{
		return route == RoutePreset.RELLEKKA
			&& stop == Port.ETCETERIA
			&& sailingLevel >= MINIMUM_SAILING
			&& agilityLevel >= MINIMUM_AGILITY
			&& fremennikTrialsComplete;
	}

	public static WorldPoint getRellekkaSailor()
	{
		return RELLEKKA_SAILOR;
	}

	public static WorldPoint getSteppingStone()
	{
		return STEPPING_STONE;
	}

	public static RouteStep getTravelStep()
	{
		return TRAVEL_STEP;
	}

	public static List<WorldPoint> getPath()
	{
		return PATH;
	}
}
