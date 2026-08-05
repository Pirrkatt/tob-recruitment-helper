package com.tobrecruithelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("tobrecruithelper")
public interface TobRecruitHelperConfig extends Config
{
	@ConfigItem(
		keyName = "highlightApplicantsInChat",
		name = "Highlight applicants in chat",
		description = "Highlights Theatre of Blood applicants when they send a chat message",
		position = 1
	)
	default boolean highlightApplicantsInChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "applicantHighlightColor",
		name = "Applicant highlight color",
		description = "Color used to highlight Theatre of Blood applicants in chat",
		position = 2
	)
	default Color applicantHighlightColor()
	{
		return new Color(255, 190, 86);
	}

	@ConfigItem(
		keyName = "showRoleLabels",
		name = "Show role labels",
		description = "Shows applicant roles next to player names in the Theatre of Blood party interface",
		position = 3
	)
	default boolean showRoleLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWeaponIcons",
		name = "Show weapon icons",
		description = "Shows weapon icons next to player names in the Theatre of Blood party interface",
		position = 4
	)
	default boolean showWeaponIcons()
	{
		return true;
	}
}
