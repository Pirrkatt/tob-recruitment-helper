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
import java.awt.image.BufferedImage;
import java.util.Arrays;
import net.runelite.api.ItemID;
import net.runelite.api.IndexedSprite;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ImageUtil;

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

		Role role = parseRole(chatMessage.getMessage());
		Weapon weapon = parseWeapon(chatMessage.getMessage());

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

			String originalName = removeRoleLabel(Text.removeTags(child.getText()).trim());

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

	Role parseRole(String message)
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

	Weapon parseWeapon(String message)
	{
		String lower = message.toLowerCase();

		if (lower.contains("scy"))
		{
			return Weapon.SCYTHE;
		}

		if (lower.matches(".*\\b(sra|soulreaper|soul-reaper)\\b.*"))
		{
			return Weapon.SOULREAPER_AXE;
		}

		return null;
	}

	private String removeRoleLabel(String text)
	{
		return text.replaceAll("(?i) \\((Nfrz|Sfrz|Frz|Mdps|Rdps)\\)$", "");
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

		IndexedSprite scytheSprite = buildWeaponSprite(ItemID.SCYTHE_OF_VITUR, 16, 14, 5);
		IndexedSprite sraSprite = buildWeaponSprite(ItemID.SOULREAPER_AXE_28338, 16, 16, 5);

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

	private IndexedSprite buildWeaponSprite(int itemId, int width, int height, int offsetY)
	{
		BufferedImage rawImage = itemManager.getImage(itemId);
		if (rawImage == null)
		{
			return null;
		}

		BufferedImage cropped = cropTransparentPixels(rawImage);
		if (cropped == null)
		{
			return null;
		}

		BufferedImage resizedImage = ImageUtil.resizeImage(cropped, width, height);
		BufferedImage outlinedImage = addOutline(resizedImage);

		IndexedSprite sprite = ImageUtil.getImageIndexedSprite(outlinedImage, client);
		sprite.setOffsetY(offsetY);
		return sprite;
	}

	private BufferedImage cropTransparentPixels(BufferedImage image)
	{
		int minX = image.getWidth();
		int minY = image.getHeight();
		int maxX = 0;
		int maxY = 0;

		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if ((image.getRGB(x, y) >> 24) != 0)
				{
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			}
		}

		if (maxX < minX || maxY < minY)
		{
			return null;
		}

		return image.getSubimage(
			minX,
			minY,
			maxX - minX + 1,
			maxY - minY + 1
		);
	}

	private BufferedImage addOutline(BufferedImage image)
	{
		int width = image.getWidth() + 2;
		int height = image.getHeight() + 2;

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		// 1. Generate a dark silhouette matching the original alpha/transparency
		BufferedImage silhouette = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				int argb = image.getRGB(x, y);
				int alpha = (argb >> 24) & 0xFF;
				if (alpha > 0)
				{
					// Set to black while keeping transparency
					silhouette.setRGB(x, y, (alpha << 24));
				}
			}
		}

		java.awt.Graphics2D g = result.createGraphics();

		// 2. Draw silhouette in 8 directions offset around center (1, 1)
		int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
		int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};
		for (int i = 0; i < 8; i++)
		{
			g.drawImage(silhouette, 1 + dx[i], 1 + dy[i], null);
		}

		// 3. Draw the sharp colored image on top in the center
		g.drawImage(image, 1, 1, null);
		g.dispose();

		return result;
	}

	@Provides
	TobRecruitHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TobRecruitHelperConfig.class);
	}
}
