package com.tobrecruithelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.ItemID;
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
	private final Set<String> collapsedApplicants = new HashSet<>();

	@Inject
	public TobRecruitHelperPanel(ItemManager itemManager, SpriteManager spriteManager, @Named("developerMode") boolean developerMode)
	{
		super(false);
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		weaponIcons.put(Weapon.SCYTHE, itemManager.getImage(ItemID.SCYTHE_OF_VITUR));
		weaponIcons.put(Weapon.SOULREAPER_AXE, itemManager.getImage(ItemID.SOULREAPER));

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		headerPanel.setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(20, 20, 20, 10)
		));

		headerLabel = new JLabel("Party Applicants (0)");
		headerLabel.setFont(FontManager.getRunescapeBoldFont());
		headerLabel.setForeground(Color.WHITE);
		headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		headerPanel.add(headerLabel, BorderLayout.CENTER);

		if (developerMode)
		{
			JButton testButton = new JButton("Load Test Data");

			testButton.addActionListener(e ->
			{
				toggleTestApplicants();

				if (testDataLoaded)
				{
					testButton.setText("Clear Test Data");
				}
				else
				{
					testButton.setText("Load Test Data");
				}
			});

			JPanel testButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
			testButtonContainer.setOpaque(false);
			testButtonContainer.add(testButton);
			headerPanel.add(testButtonContainer, BorderLayout.EAST);
		}

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

		rebuild(new HashMap<>());
	}

	public void rebuild(Map<String, ApplicantInfo> applicants)
	{
		SwingUtilities.invokeLater(() ->
		{
			applicantsListPanel.removeAll();

			// Update header with HTML for the dimmed number effect
			headerLabel.setText("<html>Party Applicants <font color='#a5a5a5'>(" + applicants.size() + ")</font></html>");

			// Clean up expansion tracking for applicants who left
			collapsedApplicants.retainAll(applicants.keySet());

			if (applicants.isEmpty())
			{
				// Use standard font, multi-line HTML, and lighter gray color for empty state
				JLabel emptyLabel = new JLabel("<html><center>No active applicants<br>in lobby</center></html>");
				emptyLabel.setFont(FontManager.getRunescapeFont());
				emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
				emptyLabel.setBorder(new EmptyBorder(2, 10, 10, 10)); // Push it down visually

				// Wrap in a BorderLayout panel to force perfect horizontal centering
				JPanel emptyWrapper = new JPanel(new BorderLayout());
				emptyWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
				emptyWrapper.add(emptyLabel, BorderLayout.CENTER);

				applicantsListPanel.add(emptyWrapper);
			}
			else
			{
				for (Map.Entry<String, ApplicantInfo> entry : applicants.entrySet())
				{
					String applicantName = entry.getKey();
					boolean isExpanded = !collapsedApplicants.contains(applicantName);

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
								collapsedApplicants.remove(applicantName);
							}
							else
							{
								collapsedApplicants.add(applicantName);
							}
						}
					));
				}
			}

			applicantsListPanel.revalidate();
			applicantsListPanel.repaint();
		});
	}

	private boolean testDataLoaded = false;

	private void toggleTestApplicants()
	{
		if (testDataLoaded)
		{
			rebuild(new HashMap<>());
			testDataLoaded = false;
		}
		else
		{
			rebuild(TestData.createApplicants());
			testDataLoaded = true;
		}
	}
}
