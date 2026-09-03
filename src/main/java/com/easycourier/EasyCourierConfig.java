package com.easycourier;

import com.easycourier.model.RoutePreset;
import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(EasyCourierConfig.GROUP)
public interface EasyCourierConfig extends Config
{
	String GROUP = "easycourier";

	@ConfigItem(
		keyName = "defaultRoute",
		name = "Default route",
		description = "The route selected when the plugin starts",
		position = 0
	)
	default RoutePreset defaultRoute()
	{
		return RoutePreset.PRIFDDINAS;
	}

	@ConfigItem(
		keyName = "dimUnusableTasks",
		name = "Dim unusable tasks",
		description = "Darken bounties, unavailable tasks, and tasks that move away from the route",
		position = 1
	)
	default boolean dimUnusableTasks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMapRoute",
		name = "Show route on world map",
		description = "Draw the planned sea lanes when the world map is open",
		position = 2
	)
	default boolean showMapRoute()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWorldRoute",
		name = "Show route while sailing",
		description = "Draw the planned sea lanes in the game view",
		position = 3
	)
	default boolean showWorldRoute()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatUpdates",
		name = "Cargo chat updates",
		description = "Send a message when all cargo at a dock has been collected or delivered",
		position = 4
	)
	default boolean chatUpdates()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "priorityColor",
		name = "Priority task color",
		description = "Border color for the best task choices",
		position = 5
	)
	default Color priorityColor()
	{
		return new Color(82, 209, 139, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "usefulColor",
		name = "Useful task color",
		description = "Border color for other tasks that fit the route",
		position = 6
	)
	default Color usefulColor()
	{
		return new Color(226, 175, 75, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "routeColor",
		name = "Sea route color",
		description = "Color used for the route and dock highlights",
		position = 7
	)
	default Color routeColor()
	{
		return new Color(70, 181, 165, 255);
	}
}
