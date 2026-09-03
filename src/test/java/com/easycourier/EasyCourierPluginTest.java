package com.easycourier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class EasyCourierPluginTest
{
	private EasyCourierPluginTest()
	{
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EasyCourierPlugin.class);
		RuneLite.main(args);
	}
}

