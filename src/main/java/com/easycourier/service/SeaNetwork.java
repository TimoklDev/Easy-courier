package com.easycourier.service;

import com.easycourier.model.Port;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.runelite.api.coords.WorldPoint;

public final class SeaNetwork
{
	private static final String ROUTE_RESOURCE = "/com/easycourier/data/sea-routes.csv";
	private static final int MAX_SEGMENT_LENGTH = 32;
	private final Map<Port, List<SeaLeg>> lanes = new EnumMap<>(Port.class);

	public SeaNetwork()
	{
		loadRoutes();
	}

	public double distance(Port start, Port finish)
	{
		return find(start, finish).distance;
	}

	public List<WorldPoint> path(Port start, Port finish)
	{
		return find(start, finish).points;
	}

	private void loadRoutes()
	{
		InputStream stream = SeaNetwork.class.getResourceAsStream(ROUTE_RESOURCE);
		if (stream == null)
		{
			throw new IllegalStateException("Missing sea route data");
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!line.trim().isEmpty())
				{
					readRoute(line);
				}
			}
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Unable to read sea route data", ex);
		}
	}

	private void readRoute(String line)
	{
		String[] fields = line.split("\\|", 3);
		if (fields.length != 3)
		{
			return;
		}
		Port start = Port.valueOf(fields[0]);
		Port finish = Port.valueOf(fields[1]);
		List<WorldPoint> rawPoints = new ArrayList<>();
		for (String value : fields[2].split(";"))
		{
			String[] coordinates = value.split(",", 2);
			if (coordinates.length == 2)
			{
				rawPoints.add(new WorldPoint(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), 0));
			}
		}
		if (rawPoints.size() < 2)
		{
			return;
		}
		List<WorldPoint> forward = interpolate(rawPoints);
		List<WorldPoint> reverse = new ArrayList<>(forward);
		Collections.reverse(reverse);
		addLeg(start, finish, forward);
		addLeg(finish, start, reverse);
	}

	private void addLeg(Port start, Port finish, List<WorldPoint> points)
	{
		lanes.computeIfAbsent(start, key -> new ArrayList<>())
			.add(new SeaLeg(start, finish, Collections.unmodifiableList(points), routeDistance(points)));
	}

	private PathResult find(Port start, Port finish)
	{
		if (start == null || finish == null || start == Port.UNKNOWN || finish == Port.UNKNOWN)
		{
			return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
		}
		if (start == finish)
		{
			return new PathResult(Collections.singletonList(start.getMapPoint()), 0);
		}
		Map<Port, Double> distances = new EnumMap<>(Port.class);
		Map<Port, SeaLeg> previous = new EnumMap<>(Port.class);
		PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));
		distances.put(start, 0.0);
		queue.add(new Node(start, 0.0));
		while (!queue.isEmpty())
		{
			Node node = queue.poll();
			if (node.distance > distances.getOrDefault(node.port, Double.POSITIVE_INFINITY))
			{
				continue;
			}
			if (node.port == finish)
			{
				break;
			}
			for (SeaLeg leg : lanes.getOrDefault(node.port, Collections.emptyList()))
			{
				if (usesAldarinAsSunsetTransit(start, finish, leg.finish))
				{
					continue;
				}
				double nextDistance = node.distance + leg.distance;
				if (nextDistance < distances.getOrDefault(leg.finish, Double.POSITIVE_INFINITY))
				{
					distances.put(leg.finish, nextDistance);
					previous.put(leg.finish, leg);
					queue.add(new Node(leg.finish, nextDistance));
				}
			}
		}
		if (!distances.containsKey(finish))
		{
			List<WorldPoint> fallback = new ArrayList<>();
			fallback.add(start.getMapPoint());
			fallback.add(finish.getMapPoint());
			return new PathResult(interpolate(fallback), routeDistance(fallback));
		}
		List<SeaLeg> reverseLegs = new ArrayList<>();
		Port cursor = finish;
		while (cursor != start)
		{
			SeaLeg leg = previous.get(cursor);
			if (leg == null)
			{
				break;
			}
			reverseLegs.add(leg);
			cursor = leg.start;
		}
		Collections.reverse(reverseLegs);
		List<WorldPoint> points = new ArrayList<>();
		for (SeaLeg leg : reverseLegs)
		{
			int first = points.isEmpty() ? 0 : 1;
			points.addAll(leg.points.subList(first, leg.points.size()));
		}
		return new PathResult(Collections.unmodifiableList(points), distances.get(finish));
	}

	private boolean usesAldarinAsSunsetTransit(Port start, Port finish, Port next)
	{
		boolean includesSunset = start == Port.SUNSET_COAST || finish == Port.SUNSET_COAST;
		boolean endsAtAldarin = start == Port.ALDARIN || finish == Port.ALDARIN;
		return includesSunset && !endsAtAldarin && next == Port.ALDARIN;
	}

	private List<WorldPoint> interpolate(List<WorldPoint> points)
	{
		List<WorldPoint> result = new ArrayList<>();
		result.add(points.get(0));
		for (int index = 0; index < points.size() - 1; index++)
		{
			WorldPoint start = points.get(index);
			WorldPoint finish = points.get(index + 1);
			int dx = finish.getX() - start.getX();
			int dy = finish.getY() - start.getY();
			int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))
				/ (double) MAX_SEGMENT_LENGTH));
			for (int step = 1; step <= steps; step++)
			{
				int x = start.getX() + (int) Math.round(dx * step / (double) steps);
				int y = start.getY() + (int) Math.round(dy * step / (double) steps);
				result.add(new WorldPoint(x, y, 0));
			}
		}
		return result;
	}

	private double routeDistance(List<WorldPoint> points)
	{
		double distance = 0;
		for (int index = 0; index < points.size() - 1; index++)
		{
			int dx = points.get(index + 1).getX() - points.get(index).getX();
			int dy = points.get(index + 1).getY() - points.get(index).getY();
			distance += Math.hypot(dx, dy);
		}
		return distance;
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

	private static final class SeaLeg
	{
		private final Port start;
		private final Port finish;
		private final List<WorldPoint> points;
		private final double distance;

		private SeaLeg(Port start, Port finish, List<WorldPoint> points, double distance)
		{
			this.start = start;
			this.finish = finish;
			this.points = points;
			this.distance = distance;
		}
	}

	private static final class PathResult
	{
		private final List<WorldPoint> points;
		private final double distance;

		private PathResult(List<WorldPoint> points, double distance)
		{
			this.points = points;
			this.distance = distance;
		}
	}
}
