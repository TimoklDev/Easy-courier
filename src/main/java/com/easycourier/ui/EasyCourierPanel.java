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
import java.awt.Font;
import java.awt.FontMetrics;
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
	private static final int PANEL_TEXT_WIDTH = 205;
	private static final int CARD_TEXT_WIDTH = 181;
	private static final int STEP_TEXT_WIDTH = 143;
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
		add(sectionTitle(plugin.getPhase() == RoutePhase.DELIVERY ? "Delivery course" : "Route checklist"));
		add(routeSteps());
		if (!plugin.getActiveTasks().isEmpty())
		{
			add(sectionTitle("Cargo manifest"));
			add(taskManifest());
		}
		add(Box.createVerticalGlue());
		revalidate();
		repaint();
	}

	private JPanel header()
	{
		JPanel panel = basePanel(new BorderLayout());
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
		JLabel rate = wrapped(plugin.getSelectedRoute().getExpectedRate(), PANEL_TEXT_WIDTH,
			FontManager.getRunescapeSmallFont(), SEA);
		JLabel requirement = wrapped("Requires Sailing " + plugin.getSelectedRoute().getMinimumLevel() + ". "
			+ plugin.getSelectedRoute().getRequirementNote() + ".", PANEL_TEXT_WIDTH,
			FontManager.getRunescapeSmallFont(), MUTED);
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
		level.setFont(FontManager.getRunescapeSmallFont());
		level.setForeground(MUTED);
		level.setHorizontalAlignment(SwingConstants.RIGHT);
		top.add(phase, BorderLayout.WEST);
		top.add(level, BorderLayout.EAST);
		JLabel instruction = wrapped(currentInstruction(), CARD_TEXT_WIDTH,
			FontManager.getRunescapeBoldFont(), Color.WHITE);
		card.add(top, BorderLayout.NORTH);
		card.add(instruction, BorderLayout.CENTER);
		card.add(summaryStrip(), BorderLayout.SOUTH);
		return card;
	}

	private JPanel summaryStrip()
	{
		JPanel strip = basePanel(new GridLayout(1, 3, 4, 0));
		strip.setOpaque(false);
		int totalXp = plugin.getRoutePlan() == null
			? plugin.getActiveTasks().stream().mapToInt(task -> task.getDefinition().getExperience()).sum()
			: plugin.getRoutePlan().getTotalExperience();
		strip.add(metric("Tasks", String.valueOf(plugin.getActiveTasks().size()), PARCHMENT));
		strip.add(metric("Route XP", totalXp > 0 ? String.format("%,d", totalXp) : "0", CARGO));
		strip.add(metric("At", shortPort(plugin.getCurrentPort()), PARCHMENT));
		return strip;
	}

	private JPanel metric(String label, String value, Color valueColor)
	{
		JPanel panel = basePanel(new BorderLayout());
		panel.setOpaque(false);
		JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
		valueLabel.setFont(FontManager.getRunescapeBoldFont());
		valueLabel.setForeground(valueColor);
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
			JPanel row = cardPanel(new BorderLayout(0, 7));
			JLabel route = wrapped(task.getDefinition().routeLabel(), CARD_TEXT_WIDTH,
				FontManager.getRunescapeBoldFont(), PARCHMENT);
			JPanel metrics = basePanel(new GridLayout(1, 3, 4, 0));
			metrics.setOpaque(false);
			int amount = task.getDefinition().getCargoAmount();
			metrics.add(taskMetric("Collected", task.getCargoTaken() + "/" + amount,
				task.needsPickup() ? SEA : COMPLETE));
			metrics.add(taskMetric("Delivered", task.getCargoDelivered() + "/" + amount,
				task.isComplete() ? COMPLETE : MUTED));
			metrics.add(taskMetric("XP", task.getDefinition().getExperience() > 0
				? String.format("%,d", task.getDefinition().getExperience()) : "0", CARGO));
			row.add(route, BorderLayout.NORTH);
			row.add(metrics, BorderLayout.CENTER);
			list.add(row);
			list.add(Box.createRigidArea(new Dimension(0, 5)));
		}
		return list;
	}

	private JPanel taskMetric(String label, String value, Color color)
	{
		JPanel panel = basePanel(new BorderLayout());
		panel.setOpaque(false);
		JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
		valueLabel.setFont(FontManager.getRunescapeBoldFont());
		valueLabel.setForeground(color);
		JLabel labelView = new JLabel(label, SwingConstants.CENTER);
		labelView.setFont(FontManager.getRunescapeSmallFont());
		labelView.setForeground(MUTED);
		panel.add(valueLabel, BorderLayout.NORTH);
		panel.add(labelView, BorderLayout.SOUTH);
		return panel;
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
		if (plugin.getSelectedRoute() == RoutePreset.PRIFDDINAS)
		{
			boolean active = plugin.getPhase() == RoutePhase.COLLECTION
				&& plugin.getCollectionIndex() >= stops.size();
			RouteStep step = new RouteStep(StepKind.TRAVEL, Port.ALDARIN,
				"Recover your boat to Aldarin", "Then move on to the delivery phase.", 0);
			list.add(stepCard(stops.size() + 1, step, plugin.getPhase() == RoutePhase.COMPLETE, active));
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
		marker.setMinimumSize(new Dimension(24, 24));
		marker.setMaximumSize(new Dimension(24, 24));
		marker.setBackground(complete ? new Color(41, 91, 72) : active ? new Color(40, 94, 91) : CARD_DARK);
		marker.setForeground(complete ? COMPLETE : active ? SEA : MUTED);
		marker.setFont(FontManager.getRunescapeBoldFont());
		JPanel text = basePanel(new BorderLayout(0, 3));
		text.setOpaque(false);
		JLabel title = wrapped(step.getTitle(), STEP_TEXT_WIDTH,
			FontManager.getRunescapeBoldFont(), complete ? MUTED : PARCHMENT);
		JLabel detail = wrapped(step.getDetail(), STEP_TEXT_WIDTH,
			FontManager.getRunescapeSmallFont(), MUTED);
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
				if (plugin.getSelectedRoute() == RoutePreset.PRIFDDINAS)
				{
					return "Recover your boat to Aldarin, then move on to the delivery phase.";
				}
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
		if (port == null || port == Port.UNKNOWN)
		{
			return "At sea";
		}
		switch (port)
		{
			case SUMMER_SHORE:
				return "Summer";
			case CIVITAS_ILLA_FORTIS:
				return "Civitas";
			case PORT_PISCARILIUS:
				return "Piscarilius";
			case DEEPFIN_POINT:
				return "Deepfin";
			case PORT_TYRAS:
				return "Tyras";
			case PORT_ROBERTS:
				return "Roberts";
			default:
				return port.getDisplayName();
		}
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
		Border padding = BorderFactory.createEmptyBorder(9, 9, 9, 9);
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

	private JLabel wrapped(String text, int width, Font font, Color color)
	{
		JLabel label = new JLabel();
		label.setFont(font);
		label.setForeground(color);
		label.setVerticalAlignment(SwingConstants.TOP);
		label.setText("<html>" + wrapText(text, width, label.getFontMetrics(font)) + "</html>");
		Dimension preferred = label.getPreferredSize();
		label.setPreferredSize(new Dimension(width, preferred.height));
		label.setMinimumSize(new Dimension(width, preferred.height));
		return label;
	}

	private String wrapText(String text, int width, FontMetrics metrics)
	{
		String value = text == null ? "" : text.trim().replaceAll("\\s+", " ");
		if (value.isEmpty())
		{
			return "&nbsp;";
		}
		StringBuilder result = new StringBuilder();
		StringBuilder line = new StringBuilder();
		for (String word : value.split(" "))
		{
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && metrics.stringWidth(candidate) > width)
			{
				if (result.length() > 0)
				{
					result.append("<br>");
				}
				result.append(escape(line.toString()));
				line.setLength(0);
			}
			if (line.length() > 0)
			{
				line.append(' ');
			}
			line.append(word);
		}
		if (result.length() > 0)
		{
			result.append("<br>");
		}
		result.append(escape(line.toString()));
		return result.toString();
	}

	private String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
