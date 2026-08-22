package com.tobrecruithelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import java.util.function.Consumer;

public class ApplicantBoxPanel extends JPanel
{
	private static final Dimension SLOT_SIZE = new Dimension(36, 32);

	private final JPanel contentPanel;
	private boolean expanded;

	private static final KitType[][] OSRS_EQUIPMENT_GRID = {
		{ null,           KitType.HEAD,   null },
		{ KitType.CAPE,   KitType.AMULET, null },
		{ KitType.WEAPON, KitType.TORSO,  KitType.SHIELD },
		{ KitType.HANDS,  KitType.LEGS,   null },
		{ null,           KitType.BOOTS,  null }
	};

	public ApplicantBoxPanel(
		String name,
		ApplicantInfo info,
		ItemManager itemManager,
		SpriteManager spriteManager,
		Map<Weapon, AsyncBufferedImage> weaponIcons,
		boolean initialExpanded,
		Consumer<Boolean> onToggle)
	{
		this.expanded = initialExpanded;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)
		));

		// Header Panel
		JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		headerPanel.setBorder(new EmptyBorder(6, 4, 6, 4));
		headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel nameLabel = new JLabel(name);
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setForeground(Color.WHITE);

		JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		rightHeader.setOpaque(false);

		if (info.getWeapon() != null && weaponIcons.containsKey(info.getWeapon()))
		{
			JLabel weaponLabel = new JLabel();
			AsyncBufferedImage icon = weaponIcons.get(info.getWeapon());
			if (icon != null)
			{
				icon.addTo(weaponLabel);
			}
			rightHeader.add(weaponLabel);
		}

		if (info.getRole() != null)
		{
			JLabel roleLabel = new JLabel("[" + info.getRole().getLabel() + "]");
			roleLabel.setFont(FontManager.getRunescapeSmallFont());
			try
			{
				roleLabel.setForeground(Color.decode("#" + info.getRole().getColorHex()));
			}
			catch (Exception e)
			{
				roleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			rightHeader.add(roleLabel);
		}

		JLabel toggleArrow = new JLabel(expanded ? "▼" : "▶");
		toggleArrow.setFont(FontManager.getRunescapeSmallFont());
		toggleArrow.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		rightHeader.add(toggleArrow);

		headerPanel.add(nameLabel, BorderLayout.WEST);
		headerPanel.add(rightHeader, BorderLayout.EAST);

		// Expanded Content Panel
		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setBorder(new CompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			new EmptyBorder(8, 8, 8, 8)
		));
		contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		contentPanel.setVisible(expanded);

		// Equipment Section Container
		JPanel gearBox = new JPanel();
		gearBox.setLayout(new BoxLayout(gearBox, BoxLayout.Y_AXIS));
		gearBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		gearBox.setBorder(new CompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			new EmptyBorder(6, 6, 6, 6)
		));
		gearBox.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel gearTitle = new JLabel("EQUIPMENT");
		gearTitle.setFont(FontManager.getRunescapeSmallFont());
		gearTitle.setForeground(ColorScheme.BRAND_ORANGE);
		gearTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		gearBox.add(gearTitle);
		gearBox.add(Box.createVerticalStrut(4));

		JPanel gearGridPanel = createPaperdollGrid(info.getEquipment(), itemManager, spriteManager);
		gearBox.add(gearGridPanel);

		contentPanel.add(gearBox);
		contentPanel.add(Box.createVerticalStrut(6));

		// Chat Section Container
		JPanel chatBox = new JPanel();
		chatBox.setLayout(new BoxLayout(chatBox, BoxLayout.Y_AXIS));
		chatBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		chatBox.setBorder(new CompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			new EmptyBorder(6, 6, 6, 6)
		));
		chatBox.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel chatTitle = new JLabel("RECENT CHAT");
		chatTitle.setFont(FontManager.getRunescapeSmallFont());
		chatTitle.setForeground(ColorScheme.BRAND_ORANGE);
		chatTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		chatBox.add(chatTitle);
		chatBox.add(Box.createVerticalStrut(4));

		JPanel chatList = new JPanel();
		chatList.setLayout(new BoxLayout(chatList, BoxLayout.Y_AXIS));
		chatList.setOpaque(false);

		if (info.getRecentMessages().isEmpty())
		{
			JLabel noMsgLabel = new JLabel("No recent messages");
			noMsgLabel.setFont(FontManager.getRunescapeSmallFont());
			noMsgLabel.setForeground(Color.GRAY);
			noMsgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			chatList.add(noMsgLabel);
		}
		else
		{
			int count = 0;
			for (String msg : info.getRecentMessages())
			{
				if (count >= 5) break;

				JLabel msgLabel = new JLabel("<html><div style='width: 140px;'>• " + ChatParser.escapeHtml(msg) + "</div></html>");
				msgLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				msgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				chatList.add(msgLabel);
				count++;
			}
		}

		JScrollPane chatPanel = new JScrollPane(chatList);
		chatPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		chatPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		chatPanel.setBorder(null);
		chatPanel.setOpaque(false);
		chatPanel.getViewport().setOpaque(false);

		chatPanel.setPreferredSize(new Dimension(200, 80));
		chatPanel.setMaximumSize(new Dimension(200, 80));
		chatPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		chatBox.add(chatPanel);
		contentPanel.add(chatBox);

		headerPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				expanded = !expanded;
				toggleArrow.setText(expanded ? "▼" : "▶");
				contentPanel.setVisible(expanded);
				if (onToggle != null)
				{
					onToggle.accept(expanded);
				}
				revalidate();
				repaint();
			}
		});

		Dimension headerPref = headerPanel.getPreferredSize();
		headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPref.height));

		Dimension contentPref = contentPanel.getPreferredSize();
		contentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, contentPref.height));

		add(headerPanel);
		add(contentPanel);
	}

	private JPanel createPaperdollGrid(Map<KitType, Integer> equipmentMap, ItemManager itemManager, SpriteManager spriteManager)
	{
		JPanel grid = new JPanel(new GridLayout(5, 3, 2, 2));
		grid.setOpaque(false);

		Dimension gridSize = new Dimension((3 * SLOT_SIZE.width) + (2 * 2), (5 * SLOT_SIZE.height) + (4 * 2));
		grid.setPreferredSize(gridSize);
		grid.setMinimumSize(gridSize);
		grid.setMaximumSize(gridSize);

		for (int row = 0; row < 5; row++)
		{
			for (int col = 0; col < 3; col++)
			{
				KitType slotType = OSRS_EQUIPMENT_GRID[row][col];

				if (slotType == null)
				{
					JPanel emptySpacer = new JPanel();
					emptySpacer.setOpaque(false);
					emptySpacer.setPreferredSize(SLOT_SIZE);
					grid.add(emptySpacer);
				}
				else
				{
					JPanel slotBox = new JPanel(new BorderLayout());
					slotBox.setPreferredSize(SLOT_SIZE);
					slotBox.setMinimumSize(SLOT_SIZE);
					slotBox.setMaximumSize(SLOT_SIZE);
					slotBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
					slotBox.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));

					JLabel slotLabel = new JLabel();
					slotLabel.setHorizontalAlignment(SwingConstants.CENTER);
					slotLabel.setVerticalAlignment(SwingConstants.CENTER);

					Integer itemId = equipmentMap != null ? equipmentMap.get(slotType) : null;

					if (itemId != null && itemId != -1)
					{
						slotLabel.setToolTipText(slotType.toString());
						AsyncBufferedImage itemImage = itemManager.getImage(itemId);
						itemImage.addTo(slotLabel);
					}
					else
					{
						int spriteId = getSlotSpriteId(slotType);
						if (spriteId != -1)
						{
							spriteManager.getSpriteAsync(spriteId, 0, img -> {
								if (img != null)
								{
									slotLabel.setIcon(new ImageIcon(img));
								}
							});
						}
					}

					slotBox.add(slotLabel, BorderLayout.CENTER);
					grid.add(slotBox);
				}
			}
		}

		JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		wrapper.setOpaque(false);
		wrapper.add(grid);
		wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
		return wrapper;
	}

	private int getSlotSpriteId(KitType kitType)
	{
		switch (kitType)
		{
			case HEAD:
				return SpriteID.Wornicons.HEAD;
			case CAPE:
				return SpriteID.Wornicons.CAPE;
			case AMULET:
				return SpriteID.Wornicons.NECK;
			case WEAPON:
				return SpriteID.Wornicons.WEAPON;
			case TORSO:
				return SpriteID.Wornicons.TORSO;
			case SHIELD:
				return SpriteID.Wornicons.SHIELD;
			case LEGS:
				return SpriteID.Wornicons.LEGS;
			case HANDS:
				return SpriteID.Wornicons.HANDS;
			case BOOTS:
				return SpriteID.Wornicons.FEET;
			default:
				return -1;
		}
	}
}