package com.tobrecruithelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import net.runelite.api.kit.KitType;

@Data
public class ApplicantInfo
{
	private Role role;
	private Weapon weapon;
	private final List<String> recentMessages = new ArrayList<>();
	private final Map<KitType, Integer> equipment = new HashMap<>();

	public void addMessage(String msg)
	{
		if (recentMessages.size() >= 5)
		{
			recentMessages.remove(0);
		}
		recentMessages.add(msg);
	}
}