package com.tobrecruithelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.api.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

@Singleton
public class TobRecruitHelperPanel extends PluginPanel
{
	private final JPanel applicantsListPanel;
	private final JLabel headerLabel;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final Map<Weapon, AsyncBufferedImage> weaponIcons = new HashMap<>();
	private final Set<String> expandedApplicants = new HashSet<>();

	@Inject
	public TobRecruitHelperPanel(ItemManager itemManager, SpriteManager spriteManager)
	{
		super(false);
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		weaponIcons.put(Weapon.SCYTHE, itemManager.getImage(ItemID.SCYTHE_OF_VITUR));
		weaponIcons.put(Weapon.SOULREAPER_AXE, itemManager.getImage(ItemID.SOULREAPER_AXE_28338));

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		headerPanel.setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(8, 10, 8, 10)
		));

		headerLabel = new JLabel("Party Applicants (0)");
		headerLabel.setFont(FontManager.getRunescapeBoldFont());
		headerLabel.setForeground(Color.WHITE);
		headerPanel.add(headerLabel, BorderLayout.WEST);

		applicantsListPanel = new JPanel();
		applicantsListPanel.setLayout(new BoxLayout(applicantsListPanel, BoxLayout.Y_AXIS));
		applicantsListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(applicantsListPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(headerPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void rebuild(Map<String, ApplicantInfo> applicants)
	{
		SwingUtilities.invokeLater(() ->
		{
			applicantsListPanel.removeAll();
			headerLabel.setText("Party Applicants (" + applicants.size() + ")");

			// Clean up expansion tracking for applicants who left
			expandedApplicants.retainAll(applicants.keySet());

			if (applicants.isEmpty())
			{
				JLabel emptyLabel = new JLabel("No active applicants in lobby");
				emptyLabel.setFont(FontManager.getRunescapeSmallFont());
				emptyLabel.setForeground(Color.GRAY);
				emptyLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
				applicantsListPanel.add(emptyLabel);
			}
			else
			{
				for (Map.Entry<String, ApplicantInfo> entry : applicants.entrySet())
				{
					String applicantName = entry.getKey();
					boolean isExpanded = expandedApplicants.contains(applicantName);

					applicantsListPanel.add(new ApplicantBoxPanel(
						applicantName,
						entry.getValue(),
						itemManager,
						spriteManager,
						weaponIcons,
						isExpanded,
				expanded -> {
							if (expanded)
							{
								expandedApplicants.add(applicantName);
							}
							else
							{
								expandedApplicants.remove(applicantName);
							}
						}
					));
				}
			}

			applicantsListPanel.revalidate();
			applicantsListPanel.repaint();
		});
	}
}