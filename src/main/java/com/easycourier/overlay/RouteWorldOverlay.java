package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePhase;
import com.easycourier.model.RoutePlan;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public final class RouteWorldOverlay extends Overlay
{
	private static final int DRAW_DISTANCE = 220;
	private final Client client;
	private final EasyCourierPlugin plugin;

	@Inject
	private RouteWorldOverlay(Client client, EasyCourierPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		RoutePlan plan = plugin.getRoutePlan();
		if (!plugin.getConfig().showWorldRoute() || plugin.getPhase() != RoutePhase.DELIVERY
			|| plan == null || plan.getSeaPath().size() < 2)
		{
			return null;
		}
		Anchor anchor = navigationAnchor();
		if (anchor == null)
		{
			return null;
		}
		Graphics2D copy = (Graphics2D) graphics.create();
		copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color configured = plugin.getConfig().routeColor();
		Color route = new Color(configured.getRed(), configured.getGreen(), configured.getBlue(), 215);
		copy.setColor(route);
		copy.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		List<WorldPoint> points = plan.getSeaPath();
		for (int index = 0; index < points.size() - 1; index++)
		{
			WorldPoint first = points.get(index);
			WorldPoint second = points.get(index + 1);
			if (Math.min(anchor.world.distanceTo2D(first), anchor.world.distanceTo2D(second)) > DRAW_DISTANCE)
			{
				continue;
			}
			Point start = project(anchor, first);
			Point finish = project(anchor, second);
			if (start != null && finish != null)
			{
				copy.drawLine(start.getX(), start.getY(), finish.getX(), finish.getY());
			}
		}
		for (int index = 0; index < plan.getPortOrder().size(); index++)
		{
			Port port = plan.getPortOrder().get(index);
			if (anchor.world.distanceTo2D(port.getMapPoint()) <= DRAW_DISTANCE)
			{
				drawStop(copy, anchor, port, index + 1, route);
			}
		}
		copy.dispose();
		return null;
	}

	private Anchor navigationAnchor()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		WorldView view = player.getWorldView();
		if (view != null && !view.isTopLevel() && view.getId() != WorldView.TOPLEVEL)
		{
			WorldEntity entity = client.getTopLevelWorldView().worldEntities().byIndex(view.getId());
			if (entity != null)
			{
				WorldPoint world = WorldPoint.fromLocalInstance(client, entity.getLocalLocation());
				if (world != null)
				{
					return new Anchor(world, entity.getLocalLocation());
				}
			}
		}
		WorldPoint world = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		if (world == null)
		{
			world = player.getWorldLocation();
		}
		return world == null ? null : new Anchor(world, player.getLocalLocation());
	}

	private Point project(Anchor anchor, WorldPoint world)
	{
		LocalPoint local = new LocalPoint(
			anchor.local.getX() + (world.getX() - anchor.world.getX()) * Perspective.LOCAL_TILE_SIZE,
			anchor.local.getY() + (world.getY() - anchor.world.getY()) * Perspective.LOCAL_TILE_SIZE,
			WorldView.TOPLEVEL);
		float x = local.getX() - (float) client.getCameraFpX();
		float y = local.getY() - (float) client.getCameraFpY();
		float z = -(float) client.getCameraFpZ();
		float yawSin = (float) Math.sin(client.getCameraFpYaw());
		float yawCos = (float) Math.cos(client.getCameraFpYaw());
		float pitchSin = (float) Math.sin(client.getCameraFpPitch());
		float pitchCos = (float) Math.cos(client.getCameraFpPitch());
		float sideways = x * yawCos + y * yawSin;
		float forward = y * yawCos - x * yawSin;
		float vertical = z * pitchCos - forward * pitchSin;
		float depth = forward * pitchCos + z * pitchSin;
		if (depth < 50)
		{
			return null;
		}
		int canvasX = Math.round(client.getViewportWidth() / 2f + sideways * client.getScale() / depth
			+ client.getViewportXOffset());
		int canvasY = Math.round(client.getViewportHeight() / 2f + vertical * client.getScale() / depth
			+ client.getViewportYOffset());
		return new Point(canvasX, canvasY);
	}

	private void drawStop(Graphics2D graphics, Anchor anchor, Port port, int number, Color route)
	{
		Point point = project(anchor, port.getMapPoint());
		if (point == null)
		{
			return;
		}
		String label = number + ". " + port.getDisplayName();
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(label) + 12;
		int x = point.getX() - width / 2;
		int y = point.getY() - 27;
		graphics.setColor(new Color(8, 18, 22, 225));
		graphics.fillRoundRect(x, y, width, 19, 8, 8);
		graphics.setColor(route);
		graphics.drawRoundRect(x, y, width, 19, 8, 8);
		graphics.setColor(Color.WHITE);
		graphics.drawString(label, x + 6, y + 14);
	}

	private static final class Anchor
	{
		private final WorldPoint world;
		private final LocalPoint local;

		private Anchor(WorldPoint world, LocalPoint local)
		{
			this.world = world;
			this.local = local;
		}
	}
}
