package org.mineacademy.fo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.NonNull;

/**
 * 文本 ASCII 艺术生成器。
 */
public class ASCIIUtil {

	public static final int SMALL = 12;
	public static final int MEDIUM = 18;
	public static final int LARGE = 24;

	/**
	 * 使用 {@link #MEDIUM} 尺寸和 * 符号，为指定文本打印 ASCII 艺术字
	 *
	 * @param message
	 *
	 * @return 可在控制台打印或发送给玩家的文本行列表
	 */
	public static List<String> generate(String message) {
		return generate(message, MEDIUM, Arrays.asList("*"));
	}

	/**
	 * 使用 {@link #MEDIUM} 尺寸和给定符号，为指定文本打印 ASCII 艺术字
	 *
	 * @param message
	 * @param letterSymbols - 用于绘制文本的符号，随机混合，以 | 分隔
	 *
	 * @return 可在控制台打印或发送给玩家的文本行列表
	 */
	public static List<String> generate(String message, @NonNull String letterSymbols) {
		return generate(message, MEDIUM, Arrays.asList(letterSymbols.split("\\|")));
	}

	/**
	 * 为指定文本打印 ASCII 艺术字。尺寸可以使用本类中预定义的尺寸，也可以自定义尺寸。
	 *
	 * @param message
	 * @param textHeight - 使用本类预定义的尺寸或自定义类型
	 * @param letterSymbols - 用于绘制文本的符号，随机混合
	 *
	 * @return 可在控制台打印或发送给玩家的文本行列表
	 */
	public static List<String> generate(String message, int textHeight, @NonNull List<String> letterSymbols) {
		final List<String> texts = new ArrayList<>();

		final int imageWidth = findImageWidth(textHeight, message, "SansSerif");
		final BufferedImage bufferedImage = new BufferedImage(imageWidth, textHeight, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
		final Font font = new Font("SansSerif", Font.BOLD, textHeight);

		graphics.setFont(font);
		graphics.drawString(message, 0, getBaselinePosition(graphics, font));

		for (int y = 0; y < textHeight; y++) {
			final StringBuilder sb = new StringBuilder();

			for (int x = 0; x < imageWidth; x++)
				sb.append(bufferedImage.getRGB(x, y) == Color.WHITE.getRGB() ? RandomUtil.nextItem(letterSymbols) : " ");

			if (sb.toString().trim().isEmpty())
				continue;

			texts.add(sb.toString());
		}

		return texts;
	}

	/*
	 * Using the Current font and current art text find the width of the full image
	 */
	private static int findImageWidth(int textHeight, String artText, String fontName) {
		final BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics graphics = bufferedImage.getGraphics();

		graphics.setFont(new Font(fontName, Font.BOLD, textHeight));

		return graphics.getFontMetrics().stringWidth(artText);
	}

	/*
	 * Find where the text baseline should be drawn so that the characters are within image
	 */
	private static int getBaselinePosition(Graphics g, Font font) {
		final FontMetrics metrics = g.getFontMetrics(font);
		final int y = metrics.getAscent() - metrics.getDescent();

		return y;
	}
}