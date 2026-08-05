package com.tobrecruithelper;

import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ImageUtil;

public class IconUtil
{
	public static IndexedSprite buildWeaponSprite(ItemManager itemManager, Client client, int itemId, int width, int height, int offsetY)
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

	public static BufferedImage cropTransparentPixels(BufferedImage image)
	{
		int minX = image.getWidth();
		int minY = image.getHeight();
		int maxX = -1;
		int maxY = -1;

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

		return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	public static BufferedImage addOutline(BufferedImage image)
	{
		int width = image.getWidth() + 2;
		int height = image.getHeight() + 2;

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage silhouette = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				int argb = image.getRGB(x, y);
				int alpha = (argb >> 24) & 0xFF;
				if (alpha > 0)
				{
					silhouette.setRGB(x, y, (alpha << 24));
				}
			}
		}

		java.awt.Graphics2D g = result.createGraphics();
		int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
		int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};
		for (int i = 0; i < 8; i++)
		{
			g.drawImage(silhouette, 1 + dx[i], 1 + dy[i], null);
		}

		g.drawImage(image, 1, 1, null);
		g.dispose();

		return result;
	}
}