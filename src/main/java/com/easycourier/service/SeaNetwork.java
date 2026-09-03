package com.easycourier.service;

import com.easycourier.model.Port;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class SeaNetwork
{
	private final Map<Port, Set<Port>> lanes = new EnumMap<>(Port.class);

	public SeaNetwork()
	{
		connect(Port.PORT_SARIM, Port.MUSA_POINT);
		connect(Port.PORT_SARIM, Port.PANDEMONIUM);
		connect(Port.MUSA_POINT, Port.PANDEMONIUM);
		connect(Port.PANDEMONIUM, Port.RUINS_OF_UNKAH);
		connect(Port.RUINS_OF_UNKAH, Port.SUMMER_SHORE);
		connect(Port.PANDEMONIUM, Port.SUMMER_SHORE);
		connect(Port.SUMMER_SHORE, Port.ALDARIN);
		connect(Port.ALDARIN, Port.SUNSET_COAST);
		connect(Port.ALDARIN, Port.DEEPFIN_POINT);
		connect(Port.ALDARIN, Port.CIVITAS_ILLA_FORTIS);
		connect(Port.SUNSET_COAST, Port.CIVITAS_ILLA_FORTIS);
		connect(Port.CIVITAS_ILLA_FORTIS, Port.PORT_ROBERTS);
		connect(Port.DEEPFIN_POINT, Port.PORT_ROBERTS);
		connect(Port.DEEPFIN_POINT, Port.PORT_TYRAS);
		connect(Port.PORT_TYRAS, Port.PRIFDDINAS);
		connect(Port.PORT_ROBERTS, Port.PRIFDDINAS);
		connect(Port.PORT_ROBERTS, Port.HOSIDIUS);
		connect(Port.HOSIDIUS, Port.PORT_PISCARILIUS);
		connect(Port.PRIFDDINAS, Port.PISCATORIS);
		connect(Port.PORT_PISCARILIUS, Port.PISCATORIS);
		connect(Port.PISCATORIS, Port.LUNAR_ISLE);
		connect(Port.PORT_PISCARILIUS, Port.RELLEKKA);
		connect(Port.LUNAR_ISLE, Port.RELLEKKA);
		connect(Port.LUNAR_ISLE, Port.ETCETERIA);
		connect(Port.RELLEKKA, Port.ETCETERIA);
	}

	public double distance(Port start, Port finish)
	{
		return find(start, finish).distance;
	}

	public List<Port> path(Port start, Port finish)
	{
		return find(start, finish).ports;
	}

	private PathResult find(Port start, Port finish)
	{
		if (start == finish)
		{
			return new PathResult(Collections.singletonList(start), 0);
		}
		Map<Port, Double> distance = new EnumMap<>(Port.class);
		Map<Port, Port> previous = new EnumMap<>(Port.class);
		PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));
		distance.put(start, 0.0);
		queue.add(new Node(start, 0.0));
		while (!queue.isEmpty())
		{
			Node node = queue.poll();
			if (node.distance > distance.getOrDefault(node.port, Double.POSITIVE_INFINITY))
			{
				continue;
			}
			if (node.port == finish)
			{
				break;
			}
			for (Port next : lanes.getOrDefault(node.port, Collections.emptySet()))
			{
				double nextDistance = node.distance + directDistance(node.port, next);
				if (nextDistance < distance.getOrDefault(next, Double.POSITIVE_INFINITY))
				{
					distance.put(next, nextDistance);
					previous.put(next, node.port);
					queue.add(new Node(next, nextDistance));
				}
			}
		}
		if (!distance.containsKey(finish))
		{
			List<Port> fallback = new ArrayList<>();
			fallback.add(start);
			fallback.add(finish);
			return new PathResult(fallback, directDistance(start, finish));
		}
		List<Port> result = new ArrayList<>();
		Port cursor = finish;
		while (cursor != null)
		{
			result.add(cursor);
			cursor = previous.get(cursor);
		}
		Collections.reverse(result);
		return new PathResult(result, distance.get(finish));
	}

	private void connect(Port first, Port second)
	{
		lanes.computeIfAbsent(first, key -> new HashSet<>()).add(second);
		lanes.computeIfAbsent(second, key -> new HashSet<>()).add(first);
	}

	private double directDistance(Port first, Port second)
	{
		int x = first.getMapPoint().getX() - second.getMapPoint().getX();
		int y = first.getMapPoint().getY() - second.getMapPoint().getY();
		return Math.hypot(x, y);
	}

	private static final class Node
	{
		private final Port port;
		private final double distance;

		private Node(Port port, double distance)
		{
			this.port = port;
			this.distance = distance;
		}
	}

	private static final class PathResult
	{
		private final List<Port> ports;
		private final double distance;

		private PathResult(List<Port> ports, double distance)
		{
			this.ports = ports;
			this.distance = distance;
		}
	}
}
