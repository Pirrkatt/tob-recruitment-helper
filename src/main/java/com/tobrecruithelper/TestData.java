package com.tobrecruithelper;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;

public class TestData
{
	public static Map<String, ApplicantInfo> createApplicants()
	{
		Map<String, ApplicantInfo> testApplicants = new HashMap<>();

		ApplicantInfo mdps = new ApplicantInfo();
		mdps.setRole(Role.MDPS);
		mdps.setWeapon(Weapon.SCYTHE);
		mdps.addMessage("scythe");
		mdps.addMessage("mdps");
		mdps.getEquipment().put(KitType.WEAPON, ItemID.SCYTHE_OF_VITUR);

		ApplicantInfo rdps = new ApplicantInfo();
		rdps.setRole(Role.MDPS);
		rdps.setWeapon(Weapon.SOULREAPER_AXE);
		rdps.addMessage("sra");
		rdps.addMessage("mdps");
		rdps.getEquipment().put(KitType.WEAPON, ItemID.SOULREAPER);

		ApplicantInfo sfrz = new ApplicantInfo();
		sfrz.setRole(Role.NFRZ);
		sfrz.addMessage("nfrz");
		sfrz.addMessage("can freeze");

		testApplicants.put("TestMDPS", mdps);
		testApplicants.put("TestRDPS", rdps);
		testApplicants.put("TestSFRZ", sfrz);

		return testApplicants;
	}
}