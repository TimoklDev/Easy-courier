package com.easycourier.overlay;

import com.easycourier.EasyCourierPlugin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public final class InfoPanelOverlay extends OverlayPanel
{
	private static final Color LABEL = new Color(190, 196, 202);
	private static final Color XP = new Color(226, 175, 75);
	private final EasyCourierPlugin plugin;

	@Inject
	private InfoPanelOverlay(EasyCourierPlugin plugin)
	{
		super(plugin);
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panelComponent.setPreferredSize(new Dimension(225, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.getConfig().showInfoPanel())
		{
			return null;
		}
		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Easy Courier")
			.color(plugin.getConfig().routeColor())
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Current step")
			.leftColor(LABEL)
			.build());
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(plugin.getInfoStep())
			.color(Color.WHITE)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Route XP")
			.right(String.format("%,d", plugin.getRouteExperience()))
			.leftColor(LABEL)
			.rightColor(XP)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("XP gained")
			.right(String.format("%,d", plugin.getSessionExperienceGained()))
			.leftColor(LABEL)
			.rightColor(Color.WHITE)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("XP per hour")
			.right(String.format("%,d", plugin.getSessionExperiencePerHour()))
			.leftColor(LABEL)
			.rightColor(Color.WHITE)
			.build());
		return super.render(graphics);
	}
}
