package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.ActiveTask;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public final class CargoItemOverlay extends WidgetItemOverlay
{
	private final EasyCourierPlugin plugin;
	private final ItemManager itemManager;

	@Inject
	private CargoItemOverlay(EasyCourierPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
		showOnInterfaces(InterfaceID.SAILING_BOAT_CARGOHOLD);
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!isRouteCargo(itemId))
		{
			return;
		}
		Color color = plugin.getConfig().routeColor();
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), color);
		Rectangle bounds = widgetItem.getCanvasBounds();
		graphics.drawImage(outline, bounds.x, bounds.y, null);
	}

	private boolean isRouteCargo(int itemId)
	{
		for (ActiveTask task : plugin.getActiveTasks())
		{
			if (!task.isComplete() && task.getDefinition().getCargoItemId() == itemId)
			{
				return true;
			}
		}
		return false;
	}
}

