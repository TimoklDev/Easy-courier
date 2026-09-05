package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.CargoHoldGuidance;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public final class CargoHoldWorldOverlay extends Overlay
{
	private final EasyCourierPlugin plugin;

	@Inject
	private CargoHoldWorldOverlay(EasyCourierPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		CargoHoldGuidance guidance = plugin.getCargoHoldGuidance();
		if (guidance == CargoHoldGuidance.NONE)
		{
			return null;
		}
		Color color = plugin.getConfig().routeColor();
		for (TileObject cargoHold : plugin.getCargoHolds())
		{
			if (!plugin.isOnPlayersBoat(cargoHold))
			{
				continue;
			}
			Shape hull = cargoHold.getClickbox();
			if (hull != null)
			{
				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
				graphics.fill(hull);
				graphics.setStroke(new BasicStroke(3f));
				graphics.setColor(color);
				graphics.draw(hull);
			}
			String text = guidance.getLabel();
			Point point = cargoHold.getCanvasTextLocation(graphics, text, 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
			}
		}
		return null;
	}
}
