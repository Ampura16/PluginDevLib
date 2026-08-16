package org.mineacademy.fo.slider;

import org.bukkit.ChatColor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 彩色文本滑块接收一个字符串，然后从左到右为其
 * 着色，并自动让颜色在文本中移动。
 *
 * 用法示例：动画计分板、菜单标题等。
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ColoredTextSlider implements Slider<String> {

	/**
	 * 要着色的字符串。
	 */
	private final String text;

	/**
	 * 每次为字符串中的多少个字母着色？
	 */
	private int width = 5;

	/**
	 * 文本的主颜色。默认为黑色。
	 */
	private String primaryColor = ChatColor.BLACK.toString();

	/**
	 * 着色部分的文本颜色。默认为深红色。
	 */
	private String secondaryColor = ChatColor.DARK_RED.toString();

	/*
	 * The current head in the slider.
	 */
	private int currentPointer = Integer.MIN_VALUE;

	/**
	 * 设置文本中应用
	 * {@link #getSecondaryColor()} 的字母数量。
	 *
	 * @param width
	 * @return
	 */
	public ColoredTextSlider width(int width) {
		this.width = width;

		return this;
	}

	/**
	 * 设置文本的主颜色。
	 *
	 * @param primaryColor
	 * @return
	 */
	public ColoredTextSlider primaryColor(String primaryColor) {
		this.primaryColor = primaryColor;

		return this;
	}

	/**
	 * 设置应用于文本中指定 X 个字母的
	 * 次颜色。
	 *
	 * @param secondaryColor
	 * @return
	 */
	public ColoredTextSlider secondaryColor(String secondaryColor) {
		this.secondaryColor = secondaryColor;

		return this;
	}

	/**
	 * @see org.mineacademy.fo.slider.Slider#next()
	 */
	@Override
	public String next() {

		if (this.currentPointer == Integer.MIN_VALUE || this.currentPointer == this.text.length())
			this.currentPointer = 1 - this.width;

		final int from = MathUtil.range(this.currentPointer, 0, this.text.length());
		final int to = MathUtil.range(this.currentPointer + this.width, 0, this.text.length());

		final String before = Common.colorize(this.primaryColor + this.text.substring(0, from));
		final String part = Common.colorize(this.secondaryColor + this.text.substring(from, to));
		final String after = Common.colorize(this.primaryColor + this.text.substring(to));

		this.currentPointer++;

		return before + part + after;
	}

	/**
	 * 为给定文本创建一个新的滑块。
	 *
	 * @param text
	 * @return
	 */
	public static ColoredTextSlider from(String text) {
		Valid.checkBoolean(!Common.hasColors(text), "Text in a slider may not contain colors: " + text + ", instead, use primaryColor() and secondaryColor()");

		return new ColoredTextSlider(text);
	}
}