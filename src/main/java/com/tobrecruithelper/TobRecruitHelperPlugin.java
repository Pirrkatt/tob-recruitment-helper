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
import java.util.Arrays;
import net.runelite.api.ItemID;
import net.runelite.api.IndexedSprite;
import net.runelite.client.game.ItemManager;

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

	private final Map<String, ApplicantInfo> activeApplicants = new HashMap<>();

	private int scytheIconIdx = -1;
	private int sraIconIdx = -1;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private TobRecruitHelperConfig config;

	@Inject
	private ItemManager itemManager;

	@Override
	protected void startUp()
	{
		clientThread.invokeLater(this::loadWeaponIcons);
	}

	@Override
	protected void shutDown()
	{
		activeApplicants.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::loadWeaponIcons);
		}
		else
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

		if (event.getKey().equals("showRoleLabels") || event.getKey().equals("showWeaponIcons"))
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

		ApplicantInfo info = activeApplicants.get(jagexName);

		Role role = ChatParser.parseRole(chatMessage.getMessage());
		Weapon weapon = ChatParser.parseWeapon(chatMessage.getMessage());

		boolean updated = false;

		if (role != null && info.getRole() != role)
		{
			info.setRole(role);
			updated = true;
		}

		if (weapon != null && info.getWeapon() != weapon)
		{
			info.setWeapon(weapon);
			updated = true;
		}

		if (updated)
		{
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
			activeApplicants.putIfAbsent(applicant, new ApplicantInfo());
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

			String originalName = ChatParser.removeRoleLabel(Text.removeTags(child.getText()).trim());

			if (originalName.isEmpty() || originalName.equals("-"))
			{
				continue;
			}

			ApplicantInfo info = activeApplicants.get(Text.toJagexName(originalName));

			// If player isn't tracked or has no extra info yet, show plain name
			if (info == null)
			{
				child.setText(originalName);
				continue;
			}

			StringBuilder text = new StringBuilder(originalName);

			// Append Role tag if enabled and present
			if (config.showRoleLabels() && info.getRole() != null)
			{
				Role role = info.getRole();
				text.append(" <col=").append(role.getColorHex()).append(">(")
					.append(role.getLabel()).append(")</col>");
			}

			// Append Weapon icon if enabled and present
			if (config.showWeaponIcons() && info.getWeapon() != null)
			{
				int iconIdx = getWeaponIconIndex(info.getWeapon());

				if (iconIdx != -1)
				{
					text.append(" <img=").append(iconIdx).append(">");
				}
			}

			child.setText(text.toString());
		}
	}

	private int getWeaponIconIndex(Weapon weapon)
	{
		switch (weapon)
		{
			case SCYTHE:
				return scytheIconIdx;
			case SOULREAPER_AXE:
				return sraIconIdx;
			default:
				return -1;
		}
	}

	private String getHighlightColor()
	{
		return String.format(
			"%06x",
			config.applicantHighlightColor().getRGB() & 0xFFFFFF
		);
	}

	private void loadWeaponIcons()
	{
		if (scytheIconIdx != -1 && sraIconIdx != -1)
		{
			return;
		}

		// Ensure loadAllSprites runs safely on the ClientThread
		itemManager.getImage(ItemID.SCYTHE_OF_VITUR).onLoaded(() -> clientThread.invokeLater(this::loadAllSprites));
		itemManager.getImage(ItemID.SOULREAPER_AXE_28338).onLoaded(() -> clientThread.invokeLater(this::loadAllSprites));
	}

	private void loadAllSprites()
	{
		IndexedSprite[] modIcons = client.getModIcons();
		if (modIcons == null || (scytheIconIdx != -1 && sraIconIdx != -1))
		{
			return;
		}

		IndexedSprite scytheSprite = IconUtil.buildWeaponSprite(itemManager, client, ItemID.SCYTHE_OF_VITUR, 16, 14, 5);
		IndexedSprite sraSprite = IconUtil.buildWeaponSprite(itemManager, client, ItemID.SOULREAPER_AXE_28338, 16, 16, 5);

		// Ensure both images have finished loading before appending
		if (scytheSprite == null || sraSprite == null)
		{
			return;
		}

		int startIdx = modIcons.length;
		scytheIconIdx = startIdx;
		sraIconIdx = startIdx + 1;

		IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 2);
		newModIcons[scytheIconIdx] = scytheSprite;
		newModIcons[sraIconIdx] = sraSprite;

		client.setModIcons(newModIcons);
	}

	@Provides
	TobRecruitHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TobRecruitHelperConfig.class);
	}
}
