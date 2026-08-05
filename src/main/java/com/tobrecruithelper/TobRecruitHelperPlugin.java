package com.tobrecruithelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.events.ConfigChanged;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "ToB Recruitment Helper"
)
public class TobRecruitHelperPlugin extends Plugin
{
	public static final int PARTY_COMPONENT_ID = 50;
	public static final int APPLICANTS_CHILD_ID = 42;

	private final Set<String> applicantsList = new HashSet<>();
	private final Map<String, Role> applicantRoles = new HashMap<>();

	private enum Role
	{
		NFRZ,
		SFRZ,
		FRZ,
		MDPS,
		RDPS
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private TobRecruitHelperConfig config;

	@Override
	protected void shutDown()
	{
		applicantsList.clear();
		applicantRoles.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			applicantsList.clear();
			applicantRoles.clear();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == PARTY_COMPONENT_ID)
		{
			clientThread.invokeLater(this::scanApplicants);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("tobrecruithelper"))
		{
			return;
		}

		if (event.getKey().equals("showRoleLabels"))
		{
			clientThread.invokeLater(this::updateLobbyRoles);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() != ChatMessageType.PUBLICCHAT)
		{
			return;
		}

		String sender = Text.removeTags(chatMessage.getName());

		if (sender == null)
		{
			return;
		}

		String jagexName = Text.toJagexName(sender);

		if (!applicantsList.contains(jagexName))
		{
			return;
		}

		// Always track roles
		Role role = parseRole(chatMessage.getMessage());

		if (role != null)
		{
			applicantRoles.put(jagexName, role);

			clientThread.invokeLater(this::updateLobbyRoles);
		}

		// Only highlight chat names if enabled
		if (!config.highlightApplicantsInChat())
		{
			return;
		}

		MessageNode messageNode = chatMessage.getMessageNode();

		String originalName = messageNode.getName();

		if (originalName.contains("<col="))
		{
			return;
		}

		messageNode.setName(
			"<col=" + getHighlightColor() + ">" + originalName + "</col>"
		);
	}

	private void scanApplicants()
	{
		Widget applicants = client.getWidget(PARTY_COMPONENT_ID, APPLICANTS_CHILD_ID);

		if (applicants == null || applicants.getChildren() == null)
		{
			return;
		}

		Set<String> currentApplicants = new HashSet<>();

		for (Widget child : applicants.getChildren())
		{
			if (child.getIndex() % 20 == 1)
			{
				String name = child.getText();

				if (name != null && !name.isEmpty() && !name.equals("-"))
				{
					currentApplicants.add(Text.toJagexName(name));
				}
			}
		}

		applicantsList.clear();
		applicantsList.addAll(currentApplicants);

		// Remove roles from players who left the lobby
		applicantRoles.keySet().removeIf(name -> !currentApplicants.contains(name));

		updateLobbyRoles();
	}

	private void updateLobbyRoles()
	{
		Widget applicants = client.getWidget(PARTY_COMPONENT_ID, APPLICANTS_CHILD_ID);

		if (applicants == null || applicants.getChildren() == null)
		{
			return;
		}

		for (Widget child : applicants.getChildren())
		{
			if (child.getIndex() % 20 != 1)
			{
				continue;
			}

			String originalName = removeRoleLabel(Text.removeTags(child.getText()));

			if (originalName.isEmpty() || originalName.equals("-"))
			{
				continue;
			}

			if (!config.showRoleLabels())
			{
				child.setText(originalName);
				continue;
			}

			Role role = applicantRoles.get(Text.toJagexName(originalName));

			if (role != null)
			{
				child.setText(
					originalName + " <col=" + getRoleColor(role) + ">("
						+ roleToString(role) + ")</col>"
				);
			}
			else
			{
				child.setText(originalName);
			}
		}
	}

	private String getHighlightColor()
	{
		return String.format(
			"%06x",
			config.applicantHighlightColor().getRGB() & 0xFFFFFF
		);
	}

	private Role parseRole(String message)
	{
		String lower = message.toLowerCase();

		if (lower.contains("nfrz") || lower.contains("nf"))
		{
			return Role.NFRZ;
		}

		if (lower.contains("sfrz") || lower.contains("sf"))
		{
			return Role.SFRZ;
		}

		if (lower.contains("mdps"))
		{
			return Role.MDPS;
		}

		if (lower.contains("rdps"))
		{
			return Role.RDPS;
		}

		// frz last so nfrz/sfrz has priority
		if (lower.contains("frz"))
		{
			return Role.FRZ;
		}

		return null;
	}

	private String getRoleColor(Role role)
	{
		switch (role)
		{
			case MDPS:
				return "D22B2B"; // Red

			case RDPS:
				return "0BDA51"; // Green

			case NFRZ:
			case SFRZ:
			case FRZ:
				return "0096FF"; // Blue
		}

		return "FFFFFF";
	}

	private String roleToString(Role role)
	{
		switch (role)
		{
			case NFRZ:
				return "Nfrz";
			case SFRZ:
				return "Sfrz";
			case FRZ:
				return "Frz";
			case MDPS:
				return "Mdps";
			case RDPS:
				return "Rdps";
		}

		return "";
	}

	private String removeRoleLabel(String text)
	{
		return text.replaceAll(" \\((Nfrz|Sfrz|Frz|Mdps|Rdps)\\)$", "");
	}

	@Provides
	TobRecruitHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TobRecruitHelperConfig.class);
	}
}
