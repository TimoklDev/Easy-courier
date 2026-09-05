package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.ActiveTask;
import com.easycourier.model.Port;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public final class DockOverlay extends Overlay
{
	private final EasyCourierPlugin plugin;

	@Inject
	private DockOverlay(EasyCourierPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (GameObject ledger : plugin.getLedgers())
		{
			Port port = Port.fromLedgerObjectId(ledger.getId());
			List<ActiveTask> pickups = plugin.tasksAtPickup(port);
			List<ActiveTask> deliveries = plugin.tasksAtDelivery(port);
			if (!plugin.shouldHighlightPickupLedger(port))
			{
				pickups.clear();
			}
			if (!plugin.shouldHighlightDeliveryLedger(port))
			{
				deliveries.clear();
			}
			if (plugin.isCollectionHandoffActive())
			{
				deliveries.clear();
				if (!plugin.isCollectionHandoffCargoPort(port))
				{
					pickups.clear();
				}
			}
			deliveries.removeIf(task -> !task.canDeliver());
			if (pickups.isEmpty() && deliveries.isEmpty())
			{
				continue;
			}
			Shape hull = ledger.getConvexHull();
			if (hull != null)
			{
				Color color = plugin.getConfig().routeColor();
				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
				graphics.fill(hull);
				graphics.setStroke(new BasicStroke(3f));
				graphics.setColor(color);
				graphics.draw(hull);
			}
			String text = label(pickups, deliveries);
			Point point = ledger.getCanvasTextLocation(graphics, text, 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
			}
		}
		return null;
	}

	private String label(List<ActiveTask> pickups, List<ActiveTask> deliveries)
	{
		int collect = pickups.stream().mapToInt(task -> Math.max(0,
			task.getDefinition().getCargoAmount() - task.getCargoTaken())).sum();
		int deliver = deliveries.stream().mapToInt(task -> Math.max(0,
			task.getDefinition().getCargoAmount() - task.getCargoDelivered())).sum();
		if (collect > 0 && deliver > 0)
		{
			return "Collect " + collect + " | Deliver " + deliver;
		}
		return collect > 0 ? "Collect " + collect + " cargo" : "Deliver " + deliver + " cargo";
	}
}

