package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.BoardOffer;
import com.easycourier.model.OfferStatus;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public final class NoticeBoardOverlay extends Overlay
{
	private final EasyCourierPlugin plugin;

	@Inject
	private NoticeBoardOverlay(EasyCourierPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (BoardOffer offer : plugin.getBoardOffers())
		{
			renderOffer(graphics, offer);
		}
		return null;
	}

	private void renderOffer(Graphics2D graphics, BoardOffer offer)
	{
		Widget widget = offer.getWidget();
		if (widget == null || widget.isHidden())
		{
			return;
		}
		Rectangle bounds = widget.getBounds();
		if (bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}
		Graphics2D copy = (Graphics2D) graphics.create();
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (offer.getStatus() == OfferStatus.PRIORITY || offer.getStatus() == OfferStatus.USEFUL)
		{
			Color color = offer.getStatus() == OfferStatus.PRIORITY
				? plugin.getConfig().priorityColor() : plugin.getConfig().usefulColor();
			Stroke stroke = new BasicStroke(3f);
			copy.setStroke(stroke);
			copy.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 42));
			copy.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
			copy.setColor(color);
			copy.drawRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, 8, 8);
			drawBadge(copy, bounds, offer);
		}
		else if (plugin.getConfig().dimUnusableTasks())
		{
			copy.setColor(new Color(4, 8, 11, 168));
			copy.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
		}
		copy.dispose();
	}

	private void drawBadge(Graphics2D graphics, Rectangle bounds, BoardOffer offer)
	{
		String label = offer.getStatus() == OfferStatus.PRIORITY ? "BEST" : "ROUTE";
		if (bounds.width >= 125 && offer.getTask() != null && offer.getTask().getExperience() > 0)
		{
			label += "  " + String.format("%,d XP", offer.getTask().getExperience());
		}
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		int width = graphics.getFontMetrics().stringWidth(label) + 12;
		int x = Math.max(bounds.x + 4, bounds.x + bounds.width - width - 4);
		int y = bounds.y + 5;
		graphics.setColor(new Color(7, 14, 18, 225));
		graphics.fillRoundRect(x, y, width, 17, 8, 8);
		graphics.setColor(Color.WHITE);
		graphics.drawString(label, x + 6, y + 12);
	}
}
