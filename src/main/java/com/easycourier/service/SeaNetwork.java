package com.easycourier.service;

import com.easycourier.model.Port;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
	private static final int MAX_SEGMENT_LENGTH = 32;
	private final Map<Integer, GraphPoint> graphPoints = new HashMap<>();
	private final Map<Port, Integer> graphPorts = new EnumMap<>(Port.class);

	public SeaNetwork()
	{
		this(SeaNetwork.class.getResourceAsStream(NETWORK_RESOURCE));
	}

	SeaNetwork(InputStream networkStream)
	{
		loadNetwork(networkStream);
	}

	public double distance(Port start, Port finish)
	{
		return find(start, finish).distance;
	}

	public List<WorldPoint> path(Port start, Port finish)
	{
		return find(start, finish).points;
	}

	private void loadNetwork(InputStream stream)
	{
		if (stream == null)
		{
			throw new IllegalStateException("Missing sea network data");
		}
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			JsonObject document = new Gson().fromJson(reader, JsonObject.class);
			if (!"easy-courier-sea-network".equals(document.get("format").getAsString()))
			{
				throw new IllegalStateException("Unsupported sea network format");
			}
			if (document.get("schemaVersion").getAsInt() != 1)
			{
				throw new IllegalStateException("Unsupported sea network schema");
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
					Port port = Port.valueOf(portName.getAsString());
					if (graphPorts.put(port, id) != null)
					{
						throw new IllegalStateException("Duplicate sea network port " + port);
					}
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
					int targetId = targetElement.getAsInt();
					GraphPoint target = graphPoints.get(targetId);
					if (target == null)
					{
						throw new IllegalStateException("Missing sea network node " + targetId);
					}
					if (target.point.getPlane() != point.point.getPlane())
					{
						throw new IllegalStateException("Sea network connection crosses planes");
					}
					addGraphConnection(point, target);
				}
			}
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
		return findGraph(start, finish);
	}

	private PathResult findGraph(Port start, Port finish)
	{
		Integer startId = graphPorts.get(start);
		Integer finishId = graphPorts.get(finish);
		if (startId == null || finishId == null)
		{
			return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
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
			return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
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

	private double graphDistance(GraphPoint first, GraphPoint second)
	{
		return Math.hypot(second.point.getX() - first.point.getX(), second.point.getY() - first.point.getY());
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
