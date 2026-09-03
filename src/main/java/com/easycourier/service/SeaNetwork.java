package com.easycourier.service;

import com.easycourier.model.Port;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.runelite.api.coords.WorldPoint;

public final class SeaNetwork
{
	private static final String NETWORK_RESOURCE = "/com/easycourier/data/sea-network.json";
	private static final String ROUTE_RESOURCE = "/com/easycourier/data/sea-routes.csv";
	private static final int MAX_SEGMENT_LENGTH = 32;
	private final Map<Port, List<SeaLeg>> lanes = new EnumMap<>(Port.class);
	private final Map<Integer, GraphPoint> graphPoints = new HashMap<>();
	private final Map<Port, Integer> graphPorts = new EnumMap<>(Port.class);

	public SeaNetwork()
	{
		this(SeaNetwork.class.getResourceAsStream(NETWORK_RESOURCE), SeaNetwork.class.getResourceAsStream(ROUTE_RESOURCE));
	}

	SeaNetwork(InputStream networkStream, InputStream routeStream)
	{
		if (!loadNetwork(networkStream))
		{
			loadRoutes(routeStream);
		}
	}

	public double distance(Port start, Port finish)
	{
		return find(start, finish).distance;
	}

	public List<WorldPoint> path(Port start, Port finish)
	{
		return find(start, finish).points;
	}

	private boolean loadNetwork(InputStream stream)
	{
		if (stream == null)
		{
			return false;
		}
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			JsonObject document = new Gson().fromJson(reader, JsonObject.class);
			if (!"easy-courier-sea-network".equals(document.get("format").getAsString()))
			{
				throw new IllegalStateException("Unsupported sea network format");
			}
			JsonArray nodes = document.getAsJsonArray("nodes");
			if (nodes == null || nodes.size() == 0)
			{
				throw new IllegalStateException("Sea network has no nodes");
			}
			for (JsonElement element : nodes)
			{
				JsonObject node = element.getAsJsonObject();
				int id = node.get("id").getAsInt();
				int x = node.get("x").getAsInt();
				int y = node.get("y").getAsInt();
				int plane = node.get("plane").getAsInt();
				if (graphPoints.put(id, new GraphPoint(id, new WorldPoint(x, y, plane))) != null)
				{
					throw new IllegalStateException("Duplicate sea network node " + id);
				}
				JsonElement portName = node.get("port");
				if (portName != null && !portName.isJsonNull())
				{
					graphPorts.put(Port.valueOf(portName.getAsString()), id);
				}
			}
			for (JsonElement element : nodes)
			{
				JsonObject node = element.getAsJsonObject();
				GraphPoint point = graphPoints.get(node.get("id").getAsInt());
				JsonArray connections = node.getAsJsonArray("connections");
				if (connections == null)
				{
					continue;
				}
				for (JsonElement targetElement : connections)
				{
					GraphPoint target = graphPoints.get(targetElement.getAsInt());
					if (target != null && target.point.getPlane() == point.point.getPlane())
					{
						addGraphConnection(point, target);
					}
				}
			}
			return true;
		}
		catch (IOException | RuntimeException ex)
		{
			throw new IllegalStateException("Unable to read sea network data", ex);
		}
	}

	private void addGraphConnection(GraphPoint first, GraphPoint second)
	{
		if (first.id == second.id)
		{
			return;
		}
		if (!first.connections.contains(second.id))
		{
			first.connections.add(second.id);
		}
		if (!second.connections.contains(first.id))
		{
			second.connections.add(first.id);
		}
	}

	private void loadRoutes(InputStream stream)
	{
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
		if (!graphPoints.isEmpty())
		{
			return findGraph(start, finish);
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

	private PathResult findGraph(Port start, Port finish)
	{
		Integer startId = graphPorts.get(start);
		Integer finishId = graphPorts.get(finish);
		if (startId == null || finishId == null)
		{
			return directPath(start, finish);
		}
		GraphPoint destination = graphPoints.get(finishId);
		Map<Integer, Double> distances = new HashMap<>();
		Map<Integer, Integer> previous = new HashMap<>();
		PriorityQueue<GraphVisit> queue = new PriorityQueue<>(Comparator.comparingDouble(node -> node.score));
		distances.put(startId, 0.0);
		queue.add(new GraphVisit(startId, 0, graphDistance(graphPoints.get(startId), destination)));
		while (!queue.isEmpty())
		{
			GraphVisit visit = queue.poll();
			if (visit.distance > distances.getOrDefault(visit.id, Double.POSITIVE_INFINITY))
			{
				continue;
			}
			if (visit.id == finishId)
			{
				break;
			}
			GraphPoint point = graphPoints.get(visit.id);
			for (int targetId : point.connections)
			{
				GraphPoint target = graphPoints.get(targetId);
				double nextDistance = visit.distance + graphDistance(point, target);
				if (nextDistance < distances.getOrDefault(targetId, Double.POSITIVE_INFINITY))
				{
					distances.put(targetId, nextDistance);
					previous.put(targetId, visit.id);
					queue.add(new GraphVisit(targetId, nextDistance, nextDistance + graphDistance(target, destination)));
				}
			}
		}
		if (!distances.containsKey(finishId))
		{
			return directPath(start, finish);
		}
		List<WorldPoint> points = new ArrayList<>();
		Integer cursor = finishId;
		while (cursor != null)
		{
			points.add(graphPoints.get(cursor).point);
			cursor = previous.get(cursor);
		}
		Collections.reverse(points);
		return new PathResult(Collections.unmodifiableList(interpolate(points)), distances.get(finishId));
	}

	private PathResult directPath(Port start, Port finish)
	{
		List<WorldPoint> points = new ArrayList<>();
		points.add(start.getMapPoint());
		points.add(finish.getMapPoint());
		return new PathResult(Collections.unmodifiableList(interpolate(points)), routeDistance(points));
	}

	private double graphDistance(GraphPoint first, GraphPoint second)
	{
		return Math.hypot(second.point.getX() - first.point.getX(), second.point.getY() - first.point.getY());
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
				result.add(new WorldPoint(x, y, start.getPlane()));
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

	private static final class GraphPoint
	{
		private final int id;
		private final WorldPoint point;
		private final List<Integer> connections = new ArrayList<>();

		private GraphPoint(int id, WorldPoint point)
		{
			this.id = id;
			this.point = point;
		}
	}

	private static final class GraphVisit
	{
		private final int id;
		private final double distance;
		private final double score;

		private GraphVisit(int id, double distance, double score)
		{
			this.id = id;
			this.distance = distance;
			this.score = score;
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
