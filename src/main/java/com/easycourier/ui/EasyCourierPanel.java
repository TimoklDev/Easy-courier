package com.easycourier.ui;

import com.easycourier.EasyCourierPlugin;
import com.easycourier.model.ActiveTask;
import com.easycourier.model.CollectionStop;
import com.easycourier.model.Port;
import com.easycourier.model.RoutePhase;
import com.easycourier.model.RoutePlan;
import com.easycourier.model.RoutePreset;
import com.easycourier.model.RouteStep;
import com.easycourier.model.StepKind;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public final class EasyCourierPanel extends PluginPanel
{
	private static final Color SEA = new Color(70, 181, 165);
	private static final Color CARGO = new Color(226, 175, 75);
	private static final Color PARCHMENT = new Color(229, 214, 178);
	private static final Color MUTED = new Color(152, 165, 169);
	private static final Color CARD = new Color(28, 39, 46);
	private static final Color CARD_DARK = new Color(20, 29, 35);
	private static final Color COMPLETE = new Color(82, 209, 139);
	private final EasyCourierPlugin plugin;

	public EasyCourierPanel(EasyCourierPlugin plugin)
	{
		this.plugin = plugin;
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	public void refresh()
	{
		removeAll();
		add(header());
		add(Box.createRigidArea(new Dimension(0, 10)));
		add(routeChooser());
		add(Box.createRigidArea(new Dimension(0, 8)));
		add(statusCard());
		add(Box.createRigidArea(new Dimension(0, 8)));
		add(actions());
		if (!plugin.getActiveTasks().isEmpty())
		{
			add(sectionTitle("Cargo manifest"));
			add(taskManifest());
		}
		add(sectionTitle(plugin.getPhase() == RoutePhase.DELIVERY ? "Delivery course" : "Route checklist"));
		add(routeSteps());
		add(Box.createVerticalGlue());
		revalidate();
		repaint();
	}

	private JPanel header()
	{
		JPanel panel = basePanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel title = new JLabel("Easy Courier");
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(22f));
		title.setForeground(PARCHMENT);
		JLabel subtitle = new JLabel("Sailing route assistant");
		subtitle.setFont(FontManager.getRunescapeSmallFont());
		subtitle.setForeground(SEA);
		panel.add(title, BorderLayout.NORTH);
		panel.add(subtitle, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel routeChooser()
	{
		JPanel panel = basePanel(new BorderLayout(0, 5));
		JLabel label = new JLabel("Training route");
		label.setForeground(MUTED);
		label.setFont(FontManager.getRunescapeSmallFont());
		JComboBox<RoutePreset> routes = new JComboBox<>(RoutePreset.values());
		routes.setSelectedItem(plugin.getSelectedRoute());
		routes.setFocusable(false);
		routes.setEnabled(plugin.getPhase() == RoutePhase.IDLE || plugin.getPhase() == RoutePhase.COMPLETE);
		routes.addActionListener(event -> plugin.selectRoute((RoutePreset) routes.getSelectedItem()));
		JPanel details = basePanel(new BorderLayout(0, 2));
		details.setOpaque(false);
		JLabel rate = new JLabel(plugin.getSelectedRoute().getExpectedRate());
		rate.setFont(FontManager.getRunescapeSmallFont());
		rate.setForeground(SEA);
		JLabel requirement = html("Requires Sailing " + plugin.getSelectedRoute().getMinimumLevel() + ". "
			+ plugin.getSelectedRoute().getRequirementNote() + ".", 215);
		requirement.setFont(FontManager.getRunescapeSmallFont());
		requirement.setForeground(MUTED);
		details.add(rate, BorderLayout.NORTH);
		details.add(requirement, BorderLayout.CENTER);
		panel.add(label, BorderLayout.NORTH);
		panel.add(routes, BorderLayout.CENTER);
		panel.add(details, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel statusCard()
	{
		JPanel card = cardPanel(new BorderLayout(0, 8));
		JPanel top = basePanel(new BorderLayout());
		top.setOpaque(false);
		JLabel phase = new JLabel(plugin.getPhase().getLabel());
		phase.setFont(FontManager.getRunescapeBoldFont());
		phase.setForeground(phaseColor());
		JLabel level = new JLabel("Sailing " + plugin.getSailingLevel());
		level.setForeground(MUTED);
		level.setHorizontalAlignment(SwingConstants.RIGHT);
		top.add(phase, BorderLayout.WEST);
		top.add(level, BorderLayout.EAST);
		JLabel instruction = html(currentInstruction(), 215);
		instruction.setForeground(Color.WHITE);
		card.add(top, BorderLayout.NORTH);
		card.add(instruction, BorderLayout.CENTER);
		card.add(summaryStrip(), BorderLayout.SOUTH);
		return card;
	}

	private JPanel summaryStrip()
	{
		JPanel strip = basePanel(new GridLayout(1, 3, 5, 0));
		strip.setOpaque(false);
		int totalXp = plugin.getRoutePlan() == null
			? plugin.getActiveTasks().stream().mapToInt(task -> task.getDefinition().getExperience()).sum()
			: plugin.getRoutePlan().getTotalExperience();
		strip.add(metric("Tasks", String.valueOf(plugin.getActiveTasks().size())));
		strip.add(metric("Route XP", totalXp > 0 ? String.format("%,d", totalXp) : "Unknown"));
		strip.add(metric("At", shortPort(plugin.getCurrentPort())));
		return strip;
	}

	private JPanel metric(String label, String value)
	{
		JPanel panel = basePanel(new BorderLayout());
		panel.setOpaque(false);
		JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
		valueLabel.setFont(FontManager.getRunescapeBoldFont());
		valueLabel.setForeground(PARCHMENT);
		JLabel nameLabel = new JLabel(label, SwingConstants.CENTER);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(MUTED);
		panel.add(valueLabel, BorderLayout.CENTER);
		panel.add(nameLabel, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel actions()
	{
		JPanel panel = basePanel(new GridLayout(0, 1, 0, 5));
		if (plugin.getPhase() == RoutePhase.IDLE || plugin.getPhase() == RoutePhase.COMPLETE)
		{
			panel.add(button("Start collection", SEA, plugin::beginCollection));
		}
		else
		{
			if (plugin.getPhase() == RoutePhase.COLLECTION)
			{
				JButton delivery = button("Move to delivery phase", CARGO, plugin::moveToDelivery);
				delivery.setEnabled(!plugin.getActiveTasks().isEmpty());
				panel.add(delivery);
			}
			panel.add(button("Skip current step", new Color(71, 86, 96), plugin::skipStep));
			panel.add(button("Reset route", new Color(71, 86, 96), plugin::resetRoute));
		}
		return panel;
	}

	private JPanel taskManifest()
	{
		JPanel list = verticalList();
		for (ActiveTask task : plugin.getActiveTasks())
		{
			JPanel row = cardPanel(new BorderLayout(0, 5));
			JLabel route = new JLabel(task.getDefinition().routeLabel());
			route.setFont(FontManager.getRunescapeBoldFont());
			route.setForeground(PARCHMENT);
			int amount = task.getDefinition().getCargoAmount();
			String state = "Collected " + task.getCargoTaken() + "/" + amount + "  |  Delivered "
				+ task.getCargoDelivered() + "/" + amount;
			JLabel progress = new JLabel(state);
			progress.setFont(FontManager.getRunescapeSmallFont());
			progress.setForeground(task.isComplete() ? COMPLETE : MUTED);
			String xp = task.getDefinition().getExperience() > 0
				? String.format("%,d XP", task.getDefinition().getExperience()) : "XP unavailable";
			JLabel experience = new JLabel(xp, SwingConstants.RIGHT);
			experience.setForeground(CARGO);
			experience.setFont(FontManager.getRunescapeSmallFont());
			row.add(route, BorderLayout.NORTH);
			row.add(progress, BorderLayout.CENTER);
			row.add(experience, BorderLayout.EAST);
			list.add(row);
			list.add(Box.createRigidArea(new Dimension(0, 5)));
		}
		return list;
	}

	private JPanel routeSteps()
	{
		JPanel list = verticalList();
		if (plugin.getPhase() == RoutePhase.DELIVERY && plugin.getRoutePlan() != null)
		{
			List<RouteStep> steps = plugin.getRoutePlan().getSteps();
			for (int index = 0; index < steps.size(); index++)
			{
				list.add(stepCard(index + 1, steps.get(index), index < plugin.getDeliverySkipCount(),
					index == plugin.getDeliverySkipCount()));
				list.add(Box.createRigidArea(new Dimension(0, 5)));
			}
			return list;
		}
		List<CollectionStop> stops = plugin.getSelectedRoute().getCollectionStops();
		for (int index = 0; index < stops.size(); index++)
		{
			CollectionStop stop = stops.get(index);
			boolean unavailable = plugin.getSailingLevel() < stop.getMinimumLevel();
			boolean complete = index < plugin.getCollectionIndex() || unavailable;
			boolean active = plugin.getPhase() == RoutePhase.COLLECTION && index == plugin.getCollectionIndex();
			String detail = unavailable ? "Skipped until Sailing " + stop.getMinimumLevel() : stop.getTravelInstruction();
			RouteStep step = new RouteStep(StepKind.NOTICE_BOARD, stop.getPort(), "Check " + stop.getPort(), detail, 0);
			list.add(stepCard(index + 1, step, complete, active));
			list.add(Box.createRigidArea(new Dimension(0, 5)));
		}
		return list;
	}

	private JPanel stepCard(int number, RouteStep step, boolean complete, boolean active)
	{
		JPanel card = cardPanel(new BorderLayout(8, 0));
		if (active)
		{
			card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(SEA, 2),
				BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		}
		JLabel marker = new JLabel(complete ? "✓" : String.valueOf(number), SwingConstants.CENTER);
		marker.setOpaque(true);
		marker.setPreferredSize(new Dimension(24, 24));
		marker.setBackground(complete ? new Color(41, 91, 72) : active ? new Color(40, 94, 91) : CARD_DARK);
		marker.setForeground(complete ? COMPLETE : active ? SEA : MUTED);
		marker.setFont(FontManager.getRunescapeBoldFont());
		JPanel text = basePanel(new BorderLayout(0, 2));
		text.setOpaque(false);
		JLabel title = new JLabel(step.getTitle());
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(complete ? MUTED : PARCHMENT);
		JLabel detail = html(step.getDetail(), 170);
		detail.setFont(FontManager.getRunescapeSmallFont());
		detail.setForeground(MUTED);
		text.add(title, BorderLayout.NORTH);
		text.add(detail, BorderLayout.CENTER);
		if (step.getExperience() > 0)
		{
			JLabel xp = new JLabel(String.format("%,d XP", step.getExperience()));
			xp.setFont(FontManager.getRunescapeSmallFont());
			xp.setForeground(CARGO);
			text.add(xp, BorderLayout.SOUTH);
		}
		card.add(marker, BorderLayout.WEST);
		card.add(text, BorderLayout.CENTER);
		return card;
	}

	private String currentInstruction()
	{
		RoutePhase phase = plugin.getPhase();
		if (phase == RoutePhase.IDLE)
		{
			return "Choose a route, check the requirement, then start the collection phase.";
		}
		if (phase == RoutePhase.COMPLETE)
		{
			return "The route is complete. Claim any remaining rewards, then begin another lap.";
		}
		if (phase == RoutePhase.COLLECTION)
		{
			if (plugin.getCollectionIndex() >= plugin.getSelectedRoute().getCollectionStops().size())
			{
				return "Every collection board has been checked. Move to the delivery phase when your task list is ready.";
			}
			CollectionStop stop = plugin.getSelectedRoute().getCollectionStops().get(plugin.getCollectionIndex());
			if (plugin.isBoardOpenAt(stop.getPort()))
			{
				if (plugin.getHighlightedOfferCount() == 0)
				{
					return "There are no more route tasks to take here. Close the board to continue.";
				}
				return "Pick the highlighted tasks, keep any requested reserve slot open, then close the board.";
			}
			if (plugin.getCurrentPort() == stop.getPort())
			{
				return "Open the " + stop.getPort() + " notice board.";
			}
			return stop.getTravelInstruction();
		}
		RoutePlan plan = plugin.getRoutePlan();
		if (plan == null || plan.getSteps().isEmpty())
		{
			return "Accept at least one courier task, then rebuild the delivery route.";
		}
		int index = Math.min(plugin.getDeliverySkipCount(), plan.getSteps().size() - 1);
		RouteStep step = plan.getSteps().get(index);
		return step.getTitle() + ". " + step.getDetail();
	}

	private String shortPort(Port port)
	{
		return port == null || port == Port.UNKNOWN ? "Away" : port.getDisplayName();
	}

	private Color phaseColor()
	{
		if (plugin.getPhase() == RoutePhase.DELIVERY)
		{
			return CARGO;
		}
		if (plugin.getPhase() == RoutePhase.COMPLETE)
		{
			return COMPLETE;
		}
		return SEA;
	}

	private JPanel sectionTitle(String text)
	{
		JPanel panel = basePanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(PARCHMENT);
		panel.add(label, BorderLayout.WEST);
		return panel;
	}

	private JButton button(String text, Color color, Runnable action)
	{
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setBackground(color);
		button.setForeground(Color.WHITE);
		button.setFont(FontManager.getRunescapeBoldFont());
		button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		button.addActionListener(event -> action.run());
		return button;
	}

	private JPanel cardPanel(java.awt.LayoutManager layout)
	{
		JPanel panel = basePanel(layout);
		panel.setBackground(CARD);
		Border line = BorderFactory.createLineBorder(new Color(52, 70, 79));
		Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		panel.setBorder(BorderFactory.createCompoundBorder(line, padding));
		return panel;
	}

	private JPanel verticalList()
	{
		JPanel panel = basePanel(null);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		return panel;
	}

	private JPanel basePanel(java.awt.LayoutManager layout)
	{
		JPanel panel = new JPanel(layout);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return panel;
	}

	private JLabel html(String text, int width)
	{
		return new JLabel("<html><div style='width:" + width + "px'>" + text + "</div></html>");
	}
}
