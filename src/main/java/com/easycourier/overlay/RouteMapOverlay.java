package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePlan;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
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
import net.runelite.client.ui.FontManager;
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
		RoutePlan plan = plugin.getNavigationRoutePlan();
		if (!plugin.getConfig().showMapRoute() || !plugin.isTravelStepActive() || plan == null)
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
		Color routeColor = plugin.getConfig().routeColor();
		Color activeColor = plugin.getConfig().activeLegColor();
		Port activeDestination = plugin.getCurrentTravelDestination();
		drawSegments(copy, bounds, plan, activeDestination, false, routeColor, 4f);
		drawSegments(copy, bounds, plan, activeDestination, true, activeColor, 5f);
		for (int index = 0; index < plan.getPortOrder().size(); index++)
		{
			Port port = plan.getPortOrder().get(index);
			drawStop(copy, bounds, port, index + 1, port == activeDestination ? activeColor : routeColor);
		}
		copy.setClip(oldClip);
		copy.dispose();
		return null;
	}

	private void drawSegments(Graphics2D graphics, Rectangle bounds, RoutePlan plan, Port activeDestination,
		boolean active, Color color, float width)
	{
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		List<WorldPoint> path = plan.getSeaPath();
		for (int index = 0; index < path.size() - 1; index++)
		{
			if (plan.isSegmentOnLeg(index, activeDestination) != active)
			{
				continue;
			}
			Point first = toMapPoint(path.get(index), bounds);
			Point second = toMapPoint(path.get(index + 1), bounds);
			if (first != null && second != null)
			{
				graphics.drawLine(first.getX(), first.getY(), second.getX(), second.getY());
			}
		}
	}

	private void drawStop(Graphics2D graphics, Rectangle bounds, Port port, int number, Color color)
	{
		Point point = toMapPoint(port.getMapPoint(), bounds);
		if (point == null)
		{
			return;
		}
		graphics.setFont(FontManager.getRunescapeSmallFont());
		String marker = String.valueOf(number);
		FontMetrics metrics = graphics.getFontMetrics();
		graphics.setColor(new Color(8, 18, 22, 235));
		graphics.fillOval(point.getX() - 9, point.getY() - 9, 18, 18);
		graphics.setColor(color);
		graphics.drawOval(point.getX() - 9, point.getY() - 9, 18, 18);
		graphics.setColor(Color.WHITE);
		graphics.drawString(marker, point.getX() - metrics.stringWidth(marker) / 2, point.getY() + 4);
		String label = port.getDisplayName();
		int width = metrics.stringWidth(label) + 10;
		int x = point.getX() + 12;
		int y = point.getY() - 9;
		graphics.setColor(new Color(8, 18, 22, 220));
		graphics.fillRoundRect(x, y, width, 18, 7, 7);
		graphics.setColor(color);
		graphics.drawRoundRect(x, y, width, 18, 7, 7);
		graphics.setColor(Color.WHITE);
		graphics.drawString(label, x + 5, y + 13);
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
