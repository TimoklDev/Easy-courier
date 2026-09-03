package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.Port;
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

public final class NoticeBoardWorldOverlay extends Overlay
{
	private static final Color BOARD = new Color(226, 175, 75);
	private final EasyCourierPlugin plugin;

	@Inject
	private NoticeBoardWorldOverlay(EasyCourierPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Port target = plugin.getNoticeBoardTarget();
		if (target == Port.UNKNOWN)
		{
			return null;
		}
		for (GameObject board : plugin.getNoticeBoards())
		{
			if (Port.fromNoticeBoardObjectId(board.getId()) != target)
			{
				continue;
			}
			Shape hull = board.getConvexHull();
			if (hull != null)
			{
				graphics.setColor(new Color(BOARD.getRed(), BOARD.getGreen(), BOARD.getBlue(), 55));
				graphics.fill(hull);
				graphics.setStroke(new BasicStroke(3f));
				graphics.setColor(BOARD);
				graphics.draw(hull);
			}
			String text = "Check notice board";
			Point point = board.getCanvasTextLocation(graphics, text, 0);
			if (point != null)
			{
				OverlayUtil.renderTextLocation(graphics, point, text, Color.WHITE);
			}
		}
		return null;
	}
}
