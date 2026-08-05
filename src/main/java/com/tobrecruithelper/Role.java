package com.tobrecruithelper;

public enum Role
{
	NFRZ("Nfrz", "0096FF"),
	SFRZ("Sfrz", "0096FF"),
	FRZ("Frz", "0096FF"),
	MDPS("Mdps", "D22B2B"),
	RDPS("Rdps", "0BDA51");

	private final String label;
	private final String colorHex;

	Role(String label, String colorHex)
	{
		this.label = label;
		this.colorHex = colorHex;
	}

	public String getLabel() { return label; }
	public String getColorHex() { return colorHex; }
}