package com.tobrecruithelper;

import java.awt.*;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("tobrecruithelper")
public interface TobRecruitHelperConfig extends Config
{
	// ==========================================
	// SECTIONS
	// ==========================================

	@ConfigSection(
		name = "Chat Highlighting",
		description = "Settings for highlighting applicant names in public chat",
		position = 1
	)
	String chatSection = "chatSection";

	@ConfigSection(
		name = "Party Interface",
		description = "Settings for role labels and weapon icons inside the lobby interface",
		position = 2
	)
	String interfaceSection = "interfaceSection";

	@ConfigSection(
		name = "Side Panel",
		description = "Settings for the plugin side panel",
		position = 3
	)
	String sidePanelSection = "sidePanelSection";

	// ==========================================
	// CHAT HIGHLIGHTING
	// ==========================================

	@ConfigItem(
		keyName = "highlightApplicantsInChat",
		name = "Highlight applicants in chat",
		description = "Highlights Theatre of Blood applicants when they send a chat message",
		position = 1,
		section = chatSection
	)
	default boolean highlightApplicantsInChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "applicantHighlightColor",
		name = "Applicant chat color",
		description = "Color used to highlight Theatre of Blood applicants in chat",
		position = 2,
		section = chatSection
	)
	default Color applicantHighlightColor()
	{
		return new Color(255, 190, 86);
	}

	// ==========================================
	// PARTY INTERFACE
	// ==========================================

	@ConfigItem(
		keyName = "showRoleLabels",
		name = "Show role labels",
		description = "Shows applicant roles next to player names in the Theatre of Blood party interface",
		position = 1,
		section = interfaceSection
	)
	default boolean showRoleLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWeaponIcons",
		name = "Show weapon icons",
		description = "Shows weapon icons next to player names in the Theatre of Blood party interface",
		position = 4,
		section = interfaceSection
	)
	default boolean showWeaponIcons()
	{
		return true;
	}

	// ==========================================
	// SIDE PANEL
	// ==========================================

	@ConfigItem(
		keyName = "showSidePanel",
		name = "Show side panel",
		description = "Shows the side panel with information about applicants",
		position = 1,
		section = sidePanelSection
	)
	default boolean showSidePanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hidePanelOutsideTob",
		name = "Hide panel outside ToB",
		description = "Hides the side panel when you are not in the Theatre of Blood lobby",
		position = 2,
		section = sidePanelSection
	)
	default boolean hidePanelOutsideTob()
	{
		return false;
	}
}
