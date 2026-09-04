package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.Shipwright;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public final class ShipwrightOverlay extends Overlay
{
	private final EasyCourierPlugin plugin;
	private final Client client;

	@Inject
	private ShipwrightOverlay(EasyCourierPlugin plugin, Client client)
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
		Shipwright target = plugin.getActiveShipwright();
		if (target == null)
		{
			return null;
		}
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}
		Color color = plugin.getConfig().routeColor();
		for (NPC npc : worldView.npcs())
		{
			if (!target.getNpcName().equalsIgnoreCase(npc.getName()))
			{
				continue;
			}
			Shape hull = npc.getConvexHull();
			if (hull != null)
			{
				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
				graphics.fill(hull);
				graphics.setStroke(new BasicStroke(3f));
				graphics.setColor(color);
				graphics.draw(hull);
			}
			String text = "Recover your boat";
			Point point = npc.getCanvasTextLocation(graphics, text, 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
			}
		}
		return null;
	}
}
