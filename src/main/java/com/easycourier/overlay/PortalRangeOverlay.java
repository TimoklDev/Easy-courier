package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public final class PortalRangeOverlay extends Overlay
{
	private static final int RANGE_SIZE = 17;
	private static final Color RANGE_COLOR = new Color(255, 38, 38);
	private final EasyCourierPlugin plugin;
	private final Client client;

	@Inject
	private PortalRangeOverlay(EasyCourierPlugin plugin, Client client)
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
		if (!plugin.getConfig().paintPortalRange())
		{
			return null;
		}
		Graphics2D copy = (Graphics2D) graphics.create();
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		copy.setFont(FontManager.getRunescapeBoldFont());
		copy.setStroke(new BasicStroke(3f));
		for (GameObject portal : plugin.getDodgePortals())
		{
			Polygon area = Perspective.getCanvasTileAreaPoly(client, portal.getLocalLocation(), RANGE_SIZE);
			if (area == null)
			{
				continue;
			}
			copy.setColor(new Color(RANGE_COLOR.getRed(), RANGE_COLOR.getGreen(), RANGE_COLOR.getBlue(), 35));
			copy.fill(area);
			copy.setColor(RANGE_COLOR);
			copy.draw(area);
			Point point = Perspective.getCanvasTextLocation(client, copy, portal.getLocalLocation(), "DODGE", 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(copy, point, "DODGE", RANGE_COLOR);
			}
		}
		copy.dispose();
		return null;
	}
}
