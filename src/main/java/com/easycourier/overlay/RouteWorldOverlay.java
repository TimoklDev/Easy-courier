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
	private static final float NEAR_PLANE = 50f;
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
			|| !plugin.isTravelStepActive()
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
		ProjectionFrame frame = new ProjectionFrame(client);
		List<WorldPoint> points = plan.getSeaPath();
		for (int index = 0; index < points.size() - 1; index++)
		{
			WorldPoint first = points.get(index);
			WorldPoint second = points.get(index + 1);
			if (Math.min(anchor.world.distanceTo2D(first), anchor.world.distanceTo2D(second)) > DRAW_DISTANCE)
			{
				continue;
			}
			drawSegment(copy, frame, anchor, first, second);
		}
		for (int index = 0; index < plan.getPortOrder().size(); index++)
		{
			Port port = plan.getPortOrder().get(index);
			if (anchor.world.distanceTo2D(port.getMapPoint()) <= DRAW_DISTANCE)
			{
				drawStop(copy, frame, anchor, port, index + 1, route);
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
					LocalPoint stableLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), world);
					return new Anchor(world, stableLocal == null ? entity.getLocalLocation() : stableLocal);
				}
			}
		}
		WorldPoint world = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		if (world == null)
		{
			world = player.getWorldLocation();
		}
		if (world == null)
		{
			return null;
		}
		LocalPoint stableLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), world);
		return new Anchor(world, stableLocal == null ? player.getLocalLocation() : stableLocal);
	}

	private void drawSegment(Graphics2D graphics, ProjectionFrame frame, Anchor anchor,
		WorldPoint start, WorldPoint finish)
	{
		CameraPoint first = frame.toCamera(anchor, start);
		CameraPoint second = frame.toCamera(anchor, finish);
		if (first.depth < NEAR_PLANE && second.depth < NEAR_PLANE)
		{
			return;
		}
		if (first.depth < NEAR_PLANE)
		{
			first = first.toward(second, (NEAR_PLANE - first.depth) / (second.depth - first.depth));
		}
		if (second.depth < NEAR_PLANE)
		{
			second = second.toward(first, (NEAR_PLANE - second.depth) / (first.depth - second.depth));
		}
		Point firstCanvas = frame.toCanvas(first);
		Point secondCanvas = frame.toCanvas(second);
		graphics.drawLine(firstCanvas.getX(), firstCanvas.getY(), secondCanvas.getX(), secondCanvas.getY());
	}

	private void drawStop(Graphics2D graphics, ProjectionFrame frame, Anchor anchor,
		Port port, int number, Color route)
	{
		CameraPoint cameraPoint = frame.toCamera(anchor, port.getMapPoint());
		Point point = cameraPoint.depth < NEAR_PLANE ? null : frame.toCanvas(cameraPoint);
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

	private static final class ProjectionFrame
	{
		private final float cameraX;
		private final float cameraY;
		private final float cameraZ;
		private final float yawSin;
		private final float yawCos;
		private final float pitchSin;
		private final float pitchCos;
		private final float viewportX;
		private final float viewportY;
		private final float scale;

		private ProjectionFrame(Client client)
		{
			cameraX = (float) client.getCameraFpX();
			cameraY = (float) client.getCameraFpY();
			cameraZ = (float) client.getCameraFpZ();
			yawSin = (float) Math.sin(client.getCameraFpYaw());
			yawCos = (float) Math.cos(client.getCameraFpYaw());
			pitchSin = (float) Math.sin(client.getCameraFpPitch());
			pitchCos = (float) Math.cos(client.getCameraFpPitch());
			viewportX = client.getViewportWidth() / 2f + client.getViewportXOffset();
			viewportY = client.getViewportHeight() / 2f + client.getViewportYOffset();
			scale = client.getScale();
		}

		private CameraPoint toCamera(Anchor anchor, WorldPoint world)
		{
			float x = anchor.local.getX() + (world.getX() - anchor.world.getX()) * Perspective.LOCAL_TILE_SIZE - cameraX;
			float y = anchor.local.getY() + (world.getY() - anchor.world.getY()) * Perspective.LOCAL_TILE_SIZE - cameraY;
			float z = -cameraZ;
			float sideways = x * yawCos + y * yawSin;
			float forward = y * yawCos - x * yawSin;
			return new CameraPoint(sideways, z * pitchCos - forward * pitchSin,
				forward * pitchCos + z * pitchSin);
		}

		private Point toCanvas(CameraPoint point)
		{
			return new Point(Math.round(viewportX + point.sideways * scale / point.depth),
				Math.round(viewportY + point.vertical * scale / point.depth));
		}
	}

	private static final class CameraPoint
	{
		private final float sideways;
		private final float vertical;
		private final float depth;

		private CameraPoint(float sideways, float vertical, float depth)
		{
			this.sideways = sideways;
			this.vertical = vertical;
			this.depth = depth;
		}

		private CameraPoint toward(CameraPoint target, float amount)
		{
			return new CameraPoint(sideways + (target.sideways - sideways) * amount,
				vertical + (target.vertical - vertical) * amount,
				depth + (target.depth - depth) * amount);
		}
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
