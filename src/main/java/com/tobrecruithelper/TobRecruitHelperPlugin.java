package com.tobrecruithelper;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.Text;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.IndexedSprite;
import net.runelite.client.game.ItemManager;
import net.runelite.api.events.GameTick;

@Slf4j
@PluginDescriptor(
	name = "ToB Recruitment Helper",
	description = "Highlights ToB applicants and provides useful information.",
	tags = {"tob", "notice", "board", "apply", "highlight","team","party","gear", "theatre","of","blood","inspect","chat"}
)
public class TobRecruitHelperPlugin extends Plugin
{
	public static final int PARTY_COMPONENT_ID = 50;
	public static final int APPLICANTS_CHILD_ID = 42;

	private final Map<String, ApplicantInfo> activeApplicants = new HashMap<>();

	private int scytheIconIdx = -1;
	private int sraIconIdx = -1;

	private NavigationButton navButton;
	private boolean panelAdded = false;
	private static final int TOB_LOBBY_REGION = 14642;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	private TobRecruitHelperConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private Provider<TobRecruitHelperPanel> panelProvider;

	private TobRecruitHelperPanel panel;

	@Override
	protected void startUp()
	{
		panel = panelProvider.get();
		clientThread.invokeLater(this::loadWeaponIcons);

		// Use Scythe item image as side panel icon
		AsyncBufferedImage icon = itemManager.getImage(ItemID.SCYTHE_OF_VITUR);

		icon.onLoaded(() -> clientThread.invokeLater(() -> {
			if (navButton == null)
			{
				navButton = NavigationButton.builder()
					.tooltip("ToB Recruitment Helper")
					.icon(icon)
					.priority(99)
					.panel(panel)
					.build();

				updatePanelVisibility();
			}
		}));
	}

	@Override
	protected void shutDown()
	{
		activeApplicants.clear();

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
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
			if (!activeApplicants.isEmpty())
			{
				activeApplicants.clear();

				if (panel != null)
				{
					panel.rebuild(activeApplicants);
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		updatePanelVisibility();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == PARTY_COMPONENT_ID)
		{
			clientThread.invokeLater(this::scanApplicants);

			// Automatically open the side panel when creating or opening a party lobby
			if (config.showSidePanel() && navButton != null)
			{
				SwingUtilities.invokeLater(() -> {
					clientToolbar.openPanel(navButton);
				});
			}
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

		if (event.getKey().equals("showSidePanel") || event.getKey().equals("hideOutsideTob"))
		{
			clientThread.invokeLater(this::updatePanelVisibility);
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

		// Append recent message to history
		info.addMessage(chatMessage.getMessage());

		// Refresh side panel UI
		if (panel != null)
		{
			panel.rebuild(activeApplicants);
		}

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

		// Clear applicants if the widget is closed or hidden
		if (applicants == null || applicants.getChildren() == null || applicants.isHidden())
		{
			if (!activeApplicants.isEmpty())
			{
				activeApplicants.clear();
				if (panel != null)
				{
					panel.rebuild(activeApplicants);
				}
			}
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
					// Clean the name from any tags/icons we added previously
					String cleanName = ChatParser.removeRoleLabel(Text.removeTags(name)).trim();
					currentApplicants.add(Text.toJagexName(cleanName));
				}
			}
		}

		// retainAll returns true if it successfully removed players who left
		boolean panelNeedsUpdate = activeApplicants.keySet().retainAll(currentApplicants);

		for (String applicant : currentApplicants)
		{
			// Check if it's a new applicant joining
			if (!activeApplicants.containsKey(applicant))
			{
				activeApplicants.put(applicant, new ApplicantInfo());
				panelNeedsUpdate = true;
			}

			ApplicantInfo info = activeApplicants.get(applicant);

			// Check if applicant is loaded in world view to fetch equipment
			Player player = findNearbyPlayer(applicant);
			if (player != null && player.getPlayerComposition() != null)
			{
				for (KitType kitType : KitType.values())
				{
					int itemId = player.getPlayerComposition().getEquipmentId(kitType);
					if (itemId != -1)
					{
						int currentItemId = info.getEquipment().getOrDefault(kitType, -1);
						if (currentItemId != itemId)
						{
							info.getEquipment().put(kitType, itemId);
							panelNeedsUpdate = true;
						}
					}
				}
			}
		}

		updateLobbyRoles();

		if (panelNeedsUpdate && panel != null)
		{
			panel.rebuild(activeApplicants);
		}
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
				String hex = role.getColorHex();
				if (hex == null || hex.length() != 6)
				{
					hex = "ffffff";
				}
				text.append(" <col=").append(hex).append(">(")
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
		Color color = config.applicantHighlightColor();
		if (color == null)
		{
			return "ff9040";
		}
		return String.format("%06x", color.getRGB() & 0xFFFFFF);
	}

	private void loadWeaponIcons()
	{
		if (scytheIconIdx != -1 && sraIconIdx != -1)
		{
			return;
		}

		// Ensure loadAllSprites runs safely on the ClientThread
		itemManager.getImage(ItemID.SCYTHE_OF_VITUR).onLoaded(() -> clientThread.invokeLater(this::loadAllSprites));
		itemManager.getImage(ItemID.SOULREAPER).onLoaded(() -> clientThread.invokeLater(this::loadAllSprites));
	}

	private void loadAllSprites()
	{
		IndexedSprite[] modIcons = client.getModIcons();
		if (modIcons == null || (scytheIconIdx != -1 && sraIconIdx != -1))
		{
			return;
		}

		IndexedSprite scytheSprite = IconUtil.buildWeaponSprite(itemManager, client, ItemID.SCYTHE_OF_VITUR, 16, 14, 5);
		IndexedSprite sraSprite = IconUtil.buildWeaponSprite(itemManager, client, ItemID.SOULREAPER, 16, 16, 5);

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

	private Player findNearbyPlayer(String jagexName)
	{
		if (client.getTopLevelWorldView() == null)
		{
			return null;
		}

		for (Player p : client.getTopLevelWorldView().players())
		{
			if (p != null && p.getName() != null && Text.toJagexName(p.getName()).equals(jagexName))
			{
				return p;
			}
		}
		return null;
	}

	private boolean isAtTob()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return false;
		}

		return client.getLocalPlayer().getWorldLocation().getRegionID() == TOB_LOBBY_REGION;
	}

	private void updatePanelVisibility()
	{
		if (navButton == null)
		{
			return;
		}

		boolean shouldShow = config.showSidePanel();

		// If the main toggle is on, but "Hide outside ToB" is also on, verify location
		if (shouldShow && config.hidePanelOutsideTob())
		{
			shouldShow = isAtTob();
		}

		if (shouldShow && !panelAdded)
		{
			clientToolbar.addNavigation(navButton);
			panelAdded = true;
		}
		else if (!shouldShow && panelAdded)
		{
			clientToolbar.removeNavigation(navButton);
			panelAdded = false;
		}
	}

	@Provides
	TobRecruitHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TobRecruitHelperConfig.class);
	}
}
