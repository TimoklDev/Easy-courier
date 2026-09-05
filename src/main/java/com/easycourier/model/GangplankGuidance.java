package com.easycourier.model;

public enum GangplankGuidance
{
	NONE(null, null),
	BOARD_WITH_CARGO("Board", "Board with cargo"),
	BOARD_FOR_CARGO("Board", "Get more cargo"),
	BOARD_BOAT("Board", "Board your boat"),
	DISEMBARK_TO_COLLECT("Disembark", "Get more cargo"),
	DISEMBARK_TO_DELIVER("Disembark", "Deliver your cargo");

	private final String menuOption;
	private final String label;

	GangplankGuidance(String menuOption, String label)
	{
		this.menuOption = menuOption;
		this.label = label;
	}

	public String getMenuOption()
	{
		return menuOption;
	}

	public String getLabel()
	{
		return label;
	}
}
