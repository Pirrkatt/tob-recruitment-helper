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

	private final Map<String, Role> activeApplicants = new HashMap<>();

	private enum Role
	{
		NFRZ("Nfrz", "0096FF"),
		SFRZ("Sfrz", "0096FF"),
		FRZ("Frz", "0096FF"),
		MDPS("Mdps", "D22B2B"),
		RDPS("Rdps", "0BDA51");

		private final String label;
		private final String colorHex;

		Role(String label, String colorHex)
		{
			this.label = label;
			this.colorHex = colorHex;
		}

		public String getLabel() { return label; }
		public String getColorHex() { return colorHex; }
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
		activeApplicants.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			activeApplicants.clear();
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

		if (!activeApplicants.containsKey(jagexName))
		{
			return;
		}

		// Always track roles
		Role role = parseRole(chatMessage.getMessage());

		if (role != null)
		{
			activeApplicants.put(jagexName, role);
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

		// Remove players who left the lobby
		activeApplicants.keySet().retainAll(currentApplicants);

		// Add new players with a null role (meaning they haven't spoken yet)
		for (String applicant : currentApplicants)
		{
			activeApplicants.putIfAbsent(applicant, null);
		}

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

			Role role = activeApplicants.get(Text.toJagexName(originalName));

			if (role != null)
			{
				child.setText(
					originalName + " <col=" + role.getColorHex() + ">("
						+ role.getLabel() + ")</col>"
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

	private String removeRoleLabel(String text)
	{
		return text.replaceAll("(?i) \\((Nfrz|Sfrz|Frz|Mdps|Rdps)\\)$", "");
	}

	@Provides
	TobRecruitHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TobRecruitHelperConfig.class);
	}
}
