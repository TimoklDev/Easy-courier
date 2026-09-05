package com.easycourier.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum RoutePreset
{
	SUMMER_SHORE(
		"Summer Shore",
		45,
		"Troubled Tortugans",
		"About 20,000 XP per hour",
		Port.SUMMER_SHORE,
		Arrays.asList(
			stop(Port.SUMMER_SHORE, "Go to The Summer Shore and open its notice board.", 45, null,
				edges(
					edge(Port.MUSA_POINT, Port.SUMMER_SHORE),
					edge(Port.PORT_SARIM, Port.SUMMER_SHORE),
					edge(Port.PANDEMONIUM, Port.SUMMER_SHORE),
					edge(Port.RUINS_OF_UNKAH, Port.SUMMER_SHORE)),
				edges(edge(Port.RUINS_OF_UNKAH, Port.SUMMER_SHORE)))),
		order(
			Port.RUINS_OF_UNKAH, 0,
			Port.PANDEMONIUM, 1,
			Port.MUSA_POINT, 1,
			Port.PORT_SARIM, 1,
			Port.SUMMER_SHORE, 2)),
	RELLEKKA(
		"Rellekka",
		62,
		"Level 65 unlocks the Etceteria stop",
		"About 66,000 XP per hour, or up to 99,000 with Etceteria",
		Port.RELLEKKA,
		Arrays.asList(
			stop(Port.RELLEKKA, "Go to Rellekka and open its notice board.", 62,
				null,
				edges(
					edge(Port.ALDARIN, Port.RELLEKKA),
					edge(Port.SUNSET_COAST, Port.RELLEKKA),
					edge(Port.PORT_ROBERTS, Port.RELLEKKA)),
				edges(
					edge(Port.ALDARIN, Port.RELLEKKA),
					edge(Port.SUNSET_COAST, Port.RELLEKKA))),
			sailingStop(Port.RELLEKKA, Port.ETCETERIA,
				"At level 65, sail to Etceteria and open its notice board.", 65, null,
				edges(
					edge(Port.SUNSET_COAST, Port.PORT_ROBERTS),
					edge(Port.SUNSET_COAST, Port.HOSIDIUS),
					edge(Port.SUNSET_COAST, Port.PORT_PISCARILIUS),
					edge(Port.SUNSET_COAST, Port.ETCETERIA),
					edge(Port.SUNSET_COAST, Port.RELLEKKA),
					edge(Port.PORT_ROBERTS, Port.HOSIDIUS),
					edge(Port.PORT_ROBERTS, Port.PORT_PISCARILIUS),
					edge(Port.PORT_ROBERTS, Port.ETCETERIA),
					edge(Port.PORT_ROBERTS, Port.RELLEKKA),
					edge(Port.HOSIDIUS, Port.PORT_PISCARILIUS),
					edge(Port.HOSIDIUS, Port.ETCETERIA),
					edge(Port.HOSIDIUS, Port.RELLEKKA),
					edge(Port.PORT_PISCARILIUS, Port.ETCETERIA),
					edge(Port.PORT_PISCARILIUS, Port.RELLEKKA),
					edge(Port.ETCETERIA, Port.RELLEKKA)),
				edges()),
			stop(Port.ALDARIN, "Travel to Aldarin and open its notice board.", 62, null,
				edges(
					edge(Port.ALDARIN, Port.RELLEKKA),
					edge(Port.ALDARIN, Port.PORT_ROBERTS),
					edge(Port.ALDARIN, Port.CIVITAS_ILLA_FORTIS),
					edge(Port.ALDARIN, Port.PORT_PISCARILIUS)),
				edges(edge(Port.ALDARIN, Port.RELLEKKA)))),
		order(
			Port.ALDARIN, 0,
			Port.SUNSET_COAST, 1,
			Port.CIVITAS_ILLA_FORTIS, 2,
			Port.PORT_ROBERTS, 3,
			Port.HOSIDIUS, 4,
			Port.PORT_PISCARILIUS, 5,
			Port.ETCETERIA, 6,
			Port.NEITIZNOT, 6,
			Port.JATIZSO, 6,
			Port.RELLEKKA, 7)),
	PRIFDDINAS(
		"Prifddinas",
		70,
		"Song of the Elves is required",
		"About 71,000 to 77,000 XP per hour",
		Port.PRIFDDINAS,
		Arrays.asList(
			stop(Port.PRIFDDINAS, "Go to Prifddinas port and open its notice board.", 70,
				edge(Port.ALDARIN, Port.PRIFDDINAS),
				edges(
					edge(Port.ALDARIN, Port.PRIFDDINAS),
					edge(Port.PORT_TYRAS, Port.PRIFDDINAS),
					edge(Port.DEEPFIN_POINT, Port.PRIFDDINAS)),
				edges(edge(Port.ALDARIN, Port.PRIFDDINAS))),
			charterStop(Port.PORT_TYRAS, "Take a charter ship to Port Tyras and open its notice board.", 70, null,
				edges(
					edge(Port.ALDARIN, Port.PORT_TYRAS),
					edge(Port.DEEPFIN_POINT, Port.PORT_TYRAS),
					edge(Port.PORT_TYRAS, Port.PRIFDDINAS)),
				edges()),
			stop(Port.DEEPFIN_POINT, "Teleport to Deepfin Point and open its notice board.", 70, null,
				edges(
					edge(Port.ALDARIN, Port.DEEPFIN_POINT),
					edge(Port.DEEPFIN_POINT, Port.PORT_TYRAS)),
				edges()),
			stop(Port.ALDARIN, "Teleport to Aldarin and open its notice board.", 70, null,
				edges(
					edge(Port.ALDARIN, Port.PRIFDDINAS),
					edge(Port.ALDARIN, Port.PORT_TYRAS),
					edge(Port.ALDARIN, Port.DEEPFIN_POINT)),
				edges(edge(Port.ALDARIN, Port.PRIFDDINAS)))),
		order(
			Port.ALDARIN, 0,
			Port.DEEPFIN_POINT, 1,
			Port.PORT_TYRAS, 2,
			Port.PRIFDDINAS, 3)),
	LUNAR_ISLE(
		"Lunar Isle",
		76,
		"Level 84 allows five active courier tasks",
		"About 130,000 to 160,000 XP per hour",
		Port.LUNAR_ISLE,
		Arrays.asList(
			stop(Port.LUNAR_ISLE, "Go to Lunar Isle port and open its notice board.", 76, null,
				edges(
					edge(Port.ALDARIN, Port.LUNAR_ISLE),
					edge(Port.CIVITAS_ILLA_FORTIS, Port.LUNAR_ISLE),
					edge(Port.DEEPFIN_POINT, Port.LUNAR_ISLE),
					edge(Port.PORT_ROBERTS, Port.LUNAR_ISLE),
					edge(Port.PRIFDDINAS, Port.LUNAR_ISLE),
					edge(Port.PISCATORIS, Port.LUNAR_ISLE)),
				edges(
					edge(Port.ALDARIN, Port.LUNAR_ISLE),
					edge(Port.DEEPFIN_POINT, Port.LUNAR_ISLE))),
			stop(Port.PRIFDDINAS, "Travel to Prifddinas port and open its notice board.", 76, null,
				edges(
					edge(Port.ALDARIN, Port.PRIFDDINAS),
					edge(Port.DEEPFIN_POINT, Port.PRIFDDINAS)),
				edges(edge(Port.ALDARIN, Port.PRIFDDINAS))),
			stop(Port.ALDARIN, "Travel to the furthest pickup, usually Aldarin, and open its notice board.", 76, null,
				edges(
					edge(Port.ALDARIN, Port.DEEPFIN_POINT),
					edge(Port.ALDARIN, Port.PORT_ROBERTS),
					edge(Port.ALDARIN, Port.PRIFDDINAS),
					edge(Port.ALDARIN, Port.LUNAR_ISLE)),
				edges(edge(Port.ALDARIN, Port.LUNAR_ISLE)))),
		order(
			Port.ALDARIN, 0,
			Port.DEEPFIN_POINT, 1,
			Port.CIVITAS_ILLA_FORTIS, 1,
			Port.PORT_ROBERTS, 2,
			Port.PRIFDDINAS, 3,
			Port.PORT_PISCARILIUS, 4,
			Port.PISCATORIS, 4,
			Port.LUNAR_ISLE, 5));

	private final String displayName;
	private final int minimumLevel;
	private final String requirementNote;
	private final String expectedRate;
	private final Port finish;
	private final List<CollectionStop> collectionStops;
	private final Map<Port, Integer> forwardOrder;
	private static final TaskEdge PRIFDDINAS_GUARANTEED_TASK = edge(Port.ALDARIN, Port.PRIFDDINAS);

	RoutePreset(String displayName, int minimumLevel, String requirementNote, String expectedRate, Port finish,
		List<CollectionStop> collectionStops, Map<Port, Integer> forwardOrder)
	{
		this.displayName = displayName;
		this.minimumLevel = minimumLevel;
		this.requirementNote = requirementNote;
		this.expectedRate = expectedRate;
		this.finish = finish;
		this.collectionStops = Collections.unmodifiableList(collectionStops);
		this.forwardOrder = Collections.unmodifiableMap(forwardOrder);
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getMinimumLevel()
	{
		return minimumLevel;
	}

	public String getRequirementNote()
	{
		return requirementNote;
	}

	public String getExpectedRate()
	{
		return expectedRate;
	}

	public Port getFinish()
	{
		return finish;
	}

	public List<CollectionStop> getCollectionStops()
	{
		return collectionStops;
	}

	public Port getDeliveryStart()
	{
		return collectionStops.get(collectionStops.size() - 1).getPort();
	}

	public boolean movesForward(TaskDefinition task)
	{
		Integer from = forwardOrder.get(task.getPickup());
		Integer to = forwardOrder.get(task.getDelivery());
		return from != null && to != null && to > from;
	}

	public int routeRank(Port port)
	{
		return forwardOrder.getOrDefault(port, -1);
	}

	public TaskEdge getPersistentReservedTask()
	{
		return this == PRIFDDINAS ? PRIFDDINAS_GUARANTEED_TASK : null;
	}

	public Port getPersistentReservationStop()
	{
		return this == PRIFDDINAS ? Port.ALDARIN : Port.UNKNOWN;
	}

	public Port getBoatRecoveryPort()
	{
		return this == RELLEKKA || this == PRIFDDINAS ? Port.ALDARIN : Port.UNKNOWN;
	}

	@Override
	public String toString()
	{
		return displayName;
	}

	private static TaskEdge edge(Port pickup, Port delivery)
	{
		return new TaskEdge(pickup, delivery);
	}

	private static TaskEdge[] edges(TaskEdge... edges)
	{
		return edges;
	}

	private static CollectionStop stop(Port port, String instruction, int level, TaskEdge reserved,
		TaskEdge[] accepted, TaskEdge[] preferred)
	{
		return new CollectionStop(port, instruction, level, false, Port.UNKNOWN, reserved, accepted, preferred);
	}

	private static CollectionStop charterStop(Port port, String instruction, int level, TaskEdge reserved,
		TaskEdge[] accepted, TaskEdge[] preferred)
	{
		return new CollectionStop(port, instruction, level, true, Port.UNKNOWN, reserved, accepted, preferred);
	}

	private static CollectionStop sailingStop(Port start, Port port, String instruction, int level, TaskEdge reserved,
		TaskEdge[] accepted, TaskEdge[] preferred)
	{
		return new CollectionStop(port, instruction, level, false, start, reserved, accepted, preferred);
	}

	private static Map<Port, Integer> order(Object... values)
	{
		Map<Port, Integer> order = new EnumMap<>(Port.class);
		for (int index = 0; index < values.length; index += 2)
		{
			order.put((Port) values[index], (Integer) values[index + 1]);
		}
		return order;
	}
}
