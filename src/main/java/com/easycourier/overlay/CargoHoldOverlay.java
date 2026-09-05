package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public final class CargoHoldOverlay extends WidgetItemOverlay
{
	private static final Color GREEN = new Color(60, 255, 110);
	private static final Color GREEN_FILL = new Color(0, 210, 75, 70);
	private final EasyCourierPlugin plugin;
	private final ItemManager itemManager;

	@Inject
	private CargoHoldOverlay(EasyCourierPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
		showOnInterfaces(InterfaceID.SAILING_BOAT_CARGOHOLD, InterfaceID.SAILING_BOAT_CARGOHOLD_SIDE);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isDeliveryCargoAtCurrentPort(itemId))
		{
			return;
		}
		Rectangle bounds = widgetItem.getCanvasBounds();
		Color previousColor = graphics.getColor();
		Stroke previousStroke = graphics.getStroke();
		graphics.setColor(GREEN_FILL);
		graphics.fillRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 6, 6);
		graphics.setColor(GREEN);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 6, 6);
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), GREEN);
		graphics.drawImage(outline, bounds.x, bounds.y, null);
		graphics.setColor(previousColor);
		graphics.setStroke(previousStroke);
	}
}
