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
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;

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
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			applicantsList.clear();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == PARTY_COMPONENT_ID)
		{
			clientThread.invokeLater(() ->
			{
				scanApplicants();
			});
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == PARTY_COMPONENT_ID)
		{
			applicantsList.clear();
		}
	}

	private void scanApplicants()
	{
		Widget applicants = client.getWidget(PARTY_COMPONENT_ID, APPLICANTS_CHILD_ID);

		if (applicants == null || applicants.getChildren() == null)
		{
			return;
		}

		applicantsList.clear();

		for (Widget child : applicants.getChildren())
		{
			if (child.getIndex() % 20 == 1)
			{
				String name = child.getText();

				if (name != null && !name.isEmpty() && !name.equals("-"))
				{
					applicantsList.add(Text.toJagexName(name));
//					child.setTextColor(0x00FF00);
				}
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (!config.highlightApplicantsInChat())
		{
			return;
		}

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

		if (applicantsList.contains(jagexName))
		{
			MessageNode messageNode = chatMessage.getMessageNode();

			String originalName = messageNode.getName();

			if (originalName.contains("<col="))
			{
				return;
			}

			messageNode.setName(
				"<col=" + getColorTag() + ">" + originalName + "</col>"
			);
		}
	}

	private String getColorTag()
	{
		return String.format(
			"%06x",
			config.applicantHighlightColor().getRGB() & 0xFFFFFF
		);
	}

	@Provides
	TobRecruitHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TobRecruitHelperConfig.class);
	}
}
