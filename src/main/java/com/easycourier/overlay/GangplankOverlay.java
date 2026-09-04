package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public final class GangplankOverlay extends Overlay
{
	private final EasyCourierPlugin plugin;

	@Inject
	private GangplankOverlay(EasyCourierPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.shouldHighlightGangplank())
		{
			return null;
		}
		Color color = plugin.getConfig().routeColor();
		for (GameObject gangplank : plugin.getGangplanks())
		{
			Shape hull = gangplank.getConvexHull();
			if (hull != null)
			{
				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
				graphics.fill(hull);
				graphics.setStroke(new BasicStroke(3f));
				graphics.setColor(color);
				graphics.draw(hull);
			}
			String text = "Board your boat";
			Point point = gangplank.getCanvasTextLocation(graphics, text, 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
			}
		}
		return null;
	}
}
