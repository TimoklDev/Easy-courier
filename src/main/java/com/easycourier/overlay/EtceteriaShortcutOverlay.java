package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.service.EtceteriaShortcutRoute;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public final class EtceteriaShortcutOverlay extends Overlay
{
	private final EasyCourierPlugin plugin;
	private final Client client;

	@Inject
	private EtceteriaShortcutOverlay(EasyCourierPlugin plugin, Client client)
	{
		this.plugin = plugin;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isEtceteriaShortcutActive())
		{
			return null;
		}
		Color color = plugin.getConfig().routeColor();
		Graphics2D copy = (Graphics2D) graphics.create();
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawSailor(copy, color);
		if (plugin.getConfig().showWorldRoute())
		{
			drawRoute(copy, color);
		}
		drawSteppingStone(copy, color);
		copy.dispose();
		return null;
	}

	private void drawSailor(Graphics2D graphics, Color color)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}
		for (NPC npc : worldView.npcs())
		{
			WorldPoint location = npc.getWorldLocation();
			if (npc.getId() != NpcID.MISC_SAILOR || location == null
				|| location.distanceTo2D(EtceteriaShortcutRoute.getRellekkaSailor()) > 6)
			{
				continue;
			}
			paintShape(graphics, npc.getConvexHull(), color);
			String text = "Right click to travel";
			Point point = npc.getCanvasTextLocation(graphics, text, 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
			}
		}
	}

	private void drawRoute(Graphics2D graphics, Color color)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 220));
		graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Point previous = null;
		for (WorldPoint worldPoint : EtceteriaShortcutRoute.getPath())
		{
			LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint);
			Point canvasPoint = localPoint == null ? null
				: Perspective.localToCanvas(client, localPoint, worldPoint.getPlane());
			if (previous != null && canvasPoint != null)
			{
				graphics.drawLine(previous.getX(), previous.getY(), canvasPoint.getX(), canvasPoint.getY());
			}
			previous = canvasPoint;
		}
	}

	private void drawSteppingStone(Graphics2D graphics, Color color)
	{
		boolean found = false;
		for (GameObject stone : plugin.getEtceteriaSteppingStones())
		{
			paintShape(graphics, stone.getConvexHull(), color);
			Point point = stone.getCanvasTextLocation(graphics, "Take shortcut", 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(graphics, point, "Take shortcut", Color.WHITE);
			}
			found = true;
		}
		if (found)
		{
			return;
		}
		WorldView worldView = client.getTopLevelWorldView();
		LocalPoint localPoint = worldView == null ? null
			: LocalPoint.fromWorld(worldView, EtceteriaShortcutRoute.getSteppingStone());
		if (localPoint == null)
		{
			return;
		}
		Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
		paintShape(graphics, polygon, color);
		Point point = Perspective.localToCanvas(client, localPoint, 0);
		if (point != null)
		{
			OverlayUtil.renderTextLocation(graphics, point, "Take shortcut", Color.WHITE);
		}
	}

	private void paintShape(Graphics2D graphics, Shape shape, Color color)
	{
		if (shape == null)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
		graphics.fill(shape);
		graphics.setStroke(new BasicStroke(3f));
		graphics.setColor(color);
		graphics.draw(shape);
	}
}
