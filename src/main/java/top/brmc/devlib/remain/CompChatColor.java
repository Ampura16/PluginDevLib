package top.brmc.devlib.remain;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import top.brmc.devlib.ItemUtil;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.Valid;

import lombok.Getter;
import lombok.NonNull;

/**
 * 所有受支持的聊天颜色值的简单枚举。
 *
 * @author md_5, backported for comp. reasons by kangarko
 */
public final class CompChatColor {

	/**
	 * 所有聊天颜色代码的前缀特殊字符。如果你需要
	 * 从自定义格式动态转换颜色代码，请使用它。
	 */
	public static final char COLOR_CHAR = '\u00A7';
	public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

	/**
	 * 以其颜色字符为键的颜色实例。
	 */
	private static final Map<Character, CompChatColor> BY_CHAR = new HashMap<>();

	/**
	 * 以其名称为键的颜色实例。
	 */
	private static final Map<String, CompChatColor> BY_NAME = new HashMap<>();

	/**
	 * 表示 MC 1.16 之前可用的颜色
	 */
	private static final Color[] LEGACY_COLORS = {
			new Color(0, 0, 0),
			new Color(0, 0, 170),
			new Color(0, 170, 0),
			new Color(0, 170, 170),
			new Color(170, 0, 0),
			new Color(170, 0, 170),
			new Color(255, 170, 0),
			new Color(170, 170, 170),
			new Color(85, 85, 85),
			new Color(85, 85, 255),
			new Color(85, 255, 85),
			new Color(85, 255, 255),
			new Color(255, 85, 85),
			new Color(255, 85, 255),
			new Color(255, 255, 85),
			new Color(255, 255, 255),
	};

	/**
	 * 表示黑色。
	 */
	public static final CompChatColor BLACK = new CompChatColor('0', "black", new Color(0x000000));

	/**
	 * 表示深蓝色。
	 */
	public static final CompChatColor DARK_BLUE = new CompChatColor('1', "dark_blue", new Color(0x0000AA));

	/**
	 * 表示深绿色。
	 */
	public static final CompChatColor DARK_GREEN = new CompChatColor('2', "dark_green", new Color(0x00AA00));

	/**
	 * 表示深青色（aqua）。
	 */
	public static final CompChatColor DARK_AQUA = new CompChatColor('3', "dark_aqua", new Color(0x00AAAA));

	/**
	 * 表示深红色。
	 */
	public static final CompChatColor DARK_RED = new CompChatColor('4', "dark_red", new Color(0xAA0000));

	/**
	 * 表示深紫色。
	 */
	public static final CompChatColor DARK_PURPLE = new CompChatColor('5', "dark_purple", new Color(0xAA00AA));

	/**
	 * 表示金色。
	 */
	public static final CompChatColor GOLD = new CompChatColor('6', "gold", new Color(0xFFAA00));

	/**
	 * 表示灰色。
	 */
	public static final CompChatColor GRAY = new CompChatColor('7', "gray", new Color(0xAAAAAA));

	/**
	 * 表示深灰色。
	 */
	public static final CompChatColor DARK_GRAY = new CompChatColor('8', "dark_gray", new Color(0x555555));

	/**
	 * 表示蓝色。
	 */
	public static final CompChatColor BLUE = new CompChatColor('9', "blue", new Color(0x05555FF));

	/**
	 * 表示绿色。
	 */
	public static final CompChatColor GREEN = new CompChatColor('a', "green", new Color(0x55FF55));

	/**
	 * 表示青色。
	 */
	public static final CompChatColor AQUA = new CompChatColor('b', "aqua", new Color(0x55FFFF));

	/**
	 * 表示红色。
	 */
	public static final CompChatColor RED = new CompChatColor('c', "red", new Color(0xFF5555));

	/**
	 * 表示淡紫色。
	 */
	public static final CompChatColor LIGHT_PURPLE = new CompChatColor('d', "light_purple", new Color(0xFF55FF));

	/**
	 * 表示黄色。
	 */
	public static final CompChatColor YELLOW = new CompChatColor('e', "yellow", new Color(0xFFFF55));

	/**
	 * 表示白色。
	 */
	public static final CompChatColor WHITE = new CompChatColor('f', "white", new Color(0xFFFFFF));

	/**
	 * 表示随机变化的魔法字符。
	 */
	public static final CompChatColor MAGIC = new CompChatColor('k', "obfuscated");

	/**
	 * 使文本加粗。
	 */
	public static final CompChatColor BOLD = new CompChatColor('l', "bold");

	/**
	 * 在文本中间显示删除线。
	 */
	public static final CompChatColor STRIKETHROUGH = new CompChatColor('m', "strikethrough");

	/**
	 * 使文本带下划线。
	 */
	public static final CompChatColor UNDERLINE = new CompChatColor('n', "underline");

	/**
	 * 使文本变为斜体。
	 */
	public static final CompChatColor ITALIC = new CompChatColor('o', "italic");

	/**
	 * 重置之前所有的聊天颜色或格式。
	 */
	public static final CompChatColor RESET = new CompChatColor('r', "reset");

	/**
	 * 表示此颜色的代码，例如 a、r 等。
	 */
	private final char code;

	/**
	 * 此颜色的名称
	 */
	@Getter
	private final String name;

	/**
	 * 此 ChatColor 的 RGB 颜色。非颜色（格式）为 null
	 */
	@Getter
	private final Color color;

	/**
	 * 此颜色的颜色字符，前面加上 {@link #COLOR_CHAR}。
	 */
	private final String toString;

	private CompChatColor(char code, String name) {
		this(code, name, null);
	}

	private CompChatColor(char code, String name, Color color) {
		this.code = code;
		this.name = name;
		this.color = color;
		this.toString = new String(new char[] { COLOR_CHAR, code });

		BY_CHAR.put(code, this);
		BY_NAME.put(name.toUpperCase(Locale.ROOT), this);
	}

	private CompChatColor(String name, String toString, int rgb) {
		this.code = '#';
		this.name = name;
		this.color = new Color(rgb);
		this.toString = toString;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + Objects.hashCode(this.toString);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || this.getClass() != obj.getClass())
			return false;

		return Objects.equals(this.toString, ((CompChatColor) obj).toString);
	}

	/**
	 * 获取颜色代码
	 *
	 * @return 颜色代码
	 */
	public char getCode() {
		Valid.checkBoolean(this.code != '#', "Cannot retrieve color code for HEX colors");

		return this.code;
	}

	/**
	 * 若该颜色为 HEX 则返回 true
	 *
	 * @return
	 */
	public boolean isHex() {
		return this.code == '#';
	}

	/**
	 * 返回颜色的字面值，并用该颜色本身着色 :)
	 *
	 * 示例：返回 "&6Gold"、"&cRed"，或在前面带有 MC 聊天会解析的实际十六进制代码的 #cc44ff。
	 *
	 * @return
	 */
	public String toColorizedChatString() {
		return this.toString /* prints color */ + this.toChatString();
	}

	/**
	 * 输出可在 Minecraft 聊天中使用的颜色表面值，
	 * 例如输出 "Gold" 而不是实际的金色魔法字母，或输出 #cc44ff 而不是真正为聊天着色。
	 *
	 * @return
	 */
	public String toChatString() {
		return this.isHex() ? "\\\\" + this.getName() : ItemUtil.bountifyCapitalized(this.getName());
	}

	/**
	 * 返回可保存到 YAML 配置的字符串
	 *
	 * @return
	 */
	public String toSaveableString() {
		return this.getName();
	}

	/**
	 * 这会把颜色转换为实际颜色；要获取可保存的颜色请使用 getName！
	 */
	@Override
	public String toString() {
		return this.toString;
	}

	/**
	 * 获取指定代码表示的颜色。
	 *
	 * @param code 要查找的代码
	 * @return 映射到的颜色，不存在则为 null
	 */
	public static CompChatColor getByChar(char code) {
		return BY_CHAR.get(code);
	}

	/**
	 * 将给定颜色解析为聊天颜色
	 *
	 * @param color
	 * @return
	 */
	public static CompChatColor of(Color color) {
		return of("#" + Integer.toHexString(color.getRGB()).substring(2));
	}

	/**
	 * 根据 #123456 HEX 代码、& 颜色代码或名称获取颜色
	 *
	 * @param string
	 * @return
	 */
	public static CompChatColor of(@NonNull String string) {

		if (string.startsWith("#") && string.length() == 7) {

			// Default to white on ancient MC versions
			if (MinecraftVersion.olderThan(V.v1_7))
				return CompChatColor.WHITE;

			if (!MinecraftVersion.atLeast(V.v1_16)) {
				final Color color = getColorFromHex(string);

				return getClosestLegacyColor(color);
			}

			int rgb;

			try {
				rgb = Integer.parseInt(string.substring(1), 16);

			} catch (final NumberFormatException ex) {
				throw new IllegalArgumentException("Illegal hex string " + string);
			}

			final StringBuilder magic = new StringBuilder(COLOR_CHAR + "x");

			for (final char c : string.substring(1).toCharArray())
				magic.append(COLOR_CHAR).append(c);

			return new CompChatColor(string, magic.toString(), rgb);
		}

		if (string.length() == 2) {
			if (string.charAt(0) != '&')
				throw new IllegalArgumentException("Invalid syntax, please use & + color code. Got: " + string);

			final CompChatColor byChar = BY_CHAR.get(string.charAt(1));

			if (byChar != null)
				return byChar;

		} else {
			final CompChatColor byName = BY_NAME.get(string.toUpperCase(Locale.ROOT));

			if (byName != null)
				return byName;

			if (string.equalsIgnoreCase("magic"))
				return MAGIC;
		}

		throw new IllegalArgumentException("Could not parse CompChatColor " + string);
	}

	/*
	 * Parse the given HEX into a Java Color object
	 */
	private static Color getColorFromHex(String hex) {
		return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16));
	}

	/**
	 * 返回与给定颜色最接近的旧版聊天颜色。
	 *
	 * 使用 MC 1.16 加入 HEX 之前的所有可用颜色。
	 *
	 * @param color
	 * @return
	 */
	public static CompChatColor getClosestLegacyColor(Color color) {
		if (MinecraftVersion.olderThan(V.v1_16)) {
			if (color.getAlpha() < 128)
				return null;

			int index = 0;
			double best = -1;

			for (int i = 0; i < LEGACY_COLORS.length; i++)
				if (areSimilar(LEGACY_COLORS[i], color))
					return CompChatColor.getColors().get(i);

			for (int i = 0; i < LEGACY_COLORS.length; i++) {
				final double distance = getDistance(color, LEGACY_COLORS[i]);

				if (distance < best || best == -1) {
					best = distance;
					index = i;
				}
			}

			return CompChatColor.getColors().get(index);
		}

		return CompChatColor.of(color);
	}

	/*
	 * Return if colors are nearly identical
	 */
	private static boolean areSimilar(Color first, Color second) {
		return Math.abs(first.getRed() - second.getRed()) <= 5 &&
				Math.abs(first.getGreen() - second.getGreen()) <= 5 &&
				Math.abs(first.getBlue() - second.getBlue()) <= 5;

	}

	/*
	 * Returns how different two colors are
	 */
	private static double getDistance(Color first, Color second) {
		final double rmean = (first.getRed() + second.getRed()) / 2.0;
		final double r = first.getRed() - second.getRed();
		final double g = first.getGreen() - second.getGreen();
		final int b = first.getBlue() - second.getBlue();

		final double weightR = 2 + rmean / 256.0;
		final double weightG = 4.0;
		final double weightB = 2 + (255 - rmean) / 256.0;

		return weightR * r * r + weightG * g * g + weightB * b * b;
	}

	/**
	 * 将 & 颜色代码替换为段落符号字符
	 *
	 * @param message
	 * @return
	 */
	public static String translateColorCodes(String message) {
		final char[] letters = message.toCharArray();

		for (int index = 0; index < letters.length - 1; index++)
			if (letters[index] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(letters[index + 1]) > -1) {
				letters[index] = CompChatColor.COLOR_CHAR;

				letters[index + 1] = Character.toLowerCase(letters[index + 1]);
			}

		return new String(letters);
	}

	/**
	 * 获取所有已定义颜色和格式的数组。
	 *
	 * @return 所有颜色和格式的数组副本
	 */
	public static CompChatColor[] values() {
		return BY_CHAR.values().toArray(new CompChatColor[BY_CHAR.size()]);
	}

	/**
	 * 返回所有颜色的列表
	 *
	 * @return
	 */
	public static List<CompChatColor> getColors() {
		return Arrays.asList(BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE);
	}

	/**
	 * 返回所有装饰格式的列表
	 *
	 * @return
	 */
	public static List<CompChatColor> getDecorations() {
		return Arrays.asList(MAGIC, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC);
	}
}
