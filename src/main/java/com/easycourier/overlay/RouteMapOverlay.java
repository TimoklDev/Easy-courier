package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePlan;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public final class RouteMapOverlay extends Overlay
{
	private final Client client;
	private final EasyCourierPlugin plugin;

	@Inject
	private RouteMapOverlay(Client client, EasyCourierPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		RoutePlan plan = plugin.getRoutePlan();
		if (!plugin.getConfig().showMapRoute() || plan == null)
		{
			return null;
		}
		Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (map == null || map.isHidden())
		{
			return null;
		}
		Rectangle bounds = map.getBounds();
		Graphics2D copy = (Graphics2D) graphics.create();
		Shape oldClip = copy.getClip();
		copy.clip(bounds);
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color color = plugin.getConfig().routeColor();
		copy.setColor(color);
		copy.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		List<Port> path = plan.getSeaPath();
		for (int index = 0; index < path.size() - 1; index++)
		{
			Point first = toMapPoint(path.get(index).getMapPoint(), bounds);
			Point second = toMapPoint(path.get(index + 1).getMapPoint(), bounds);
			if (first != null && second != null)
			{
				copy.drawLine(first.getX(), first.getY(), second.getX(), second.getY());
			}
		}
		for (Port port : plan.getPortOrder())
		{
			Point point = toMapPoint(port.getMapPoint(), bounds);
			if (point != null)
			{
				copy.setColor(new Color(8, 18, 22, 235));
				copy.fillOval(point.getX() - 6, point.getY() - 6, 12, 12);
				copy.setColor(color);
				copy.drawOval(point.getX() - 6, point.getY() - 6, 12, 12);
			}
		}
		copy.setClip(oldClip);
		copy.dispose();
		return null;
	}

	private Point toMapPoint(WorldPoint point, Rectangle bounds)
	{
		if (point == null || client.getWorldMap() == null || client.getWorldMap().getWorldMapData() == null
			|| !client.getWorldMap().getWorldMapData().surfaceContainsPosition(point.getX(), point.getY()))
		{
			return null;
		}
		float pixelsPerTile = client.getWorldMap().getWorldMapZoom();
		int widthInTiles = (int) Math.ceil(bounds.getWidth() / pixelsPerTile);
		int heightInTiles = (int) Math.ceil(bounds.getHeight() / pixelsPerTile);
		Point center = client.getWorldMap().getWorldMapPosition();
		int xOffset = point.getX() + widthInTiles / 2 - center.getX();
		int northEdge = center.getY() - heightInTiles / 2;
		int yOffset = (northEdge - point.getY() - 1) * -1;
		int x = bounds.x + (int) (xOffset * pixelsPerTile + pixelsPerTile / 2f);
		int yFromBottom = (int) (yOffset * pixelsPerTile - pixelsPerTile / 2f);
		int y = bounds.y + bounds.height - yFromBottom;
		return new Point(x, y);
	}
}
