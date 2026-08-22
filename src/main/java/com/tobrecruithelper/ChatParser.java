package com.tobrecruithelper;

public class ChatParser
{
	public static Role parseRole(String message)
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

		// Important: parse frz after nfrz/sfrz
		if (lower.contains("frz"))
		{
			return Role.FRZ;
		}

		return null;
	}

	public static Weapon parseWeapon(String message)
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

	public static String removeRoleLabel(String text)
	{
		return text.replaceAll("(?i) \\((Nfrz|Sfrz|Frz|Mdps|Rdps)\\)$", "");
	}

	public static String escapeHtml(String text)
	{
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}