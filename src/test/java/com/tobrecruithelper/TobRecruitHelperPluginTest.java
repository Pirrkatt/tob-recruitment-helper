package com.tobrecruithelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TobRecruitHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TobRecruitHelperPlugin.class);
		RuneLite.main(args);
	}
}