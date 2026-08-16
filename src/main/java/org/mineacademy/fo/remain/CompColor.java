package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.FoException;

import lombok.Getter;

/**
 * 一个工具类，可以轻松地在 {@link DyeColor} 与 {@link CompChatColor} 之间转换
 */
public final class CompColor {

	/**
	 * 所有原生值的列表
	 */
	private static final List<CompColor> values = new ArrayList<>();

	/**
	 * 蓝色
	 */
	public static final CompColor BLUE = new CompColor("BLUE", DyeColor.BLUE);

	/**
	 * 黑色
	 */
	public static final CompColor BLACK = new CompColor("BLACK", DyeColor.BLACK);

	/**
	 * 深青色（青色）
	 */
	public static final CompColor DARK_AQUA = new CompColor("DARK_AQUA", DyeColor.CYAN);

	/**
	 * 深蓝色，在染料颜色中称为 BLUE
	 */
	public static final CompColor DARK_BLUE = new CompColor("DARK_BLUE", DyeColor.BLUE);

	/**
	 * 青色（在染料颜色中称为淡蓝色）
	 */
	public static final CompColor AQUA = new CompColor("AQUA", DyeColor.LIGHT_BLUE);

	/**
	 * 淡灰色，以前称为 silver（兼容所有 MC 版本）
	 */
	public static final CompColor GRAY = new CompColor("GRAY", getEnum("LIGHT_GRAY", "SILVER", DyeColor.class), null, "SILVER");

	/**
	 * 深灰色，在染料颜色中称为 gray
	 */
	public static final CompColor DARK_GRAY = new CompColor("DARK_GRAY", DyeColor.GRAY);

	/**
	 * 深绿色，在染料颜色中称为 green
	 */
	public static final CompColor DARK_GREEN = new CompColor("DARK_GREEN", DyeColor.GREEN);

	/**
	 * 绿色，在染料颜色中称为 lime
	 */
	public static final CompColor GREEN = new CompColor("GREEN", DyeColor.LIME);

	/**
	 * 金色，在染料颜色中称为 orange
	 */
	public static final CompColor GOLD = new CompColor("GOLD", DyeColor.ORANGE);

	/**
	 * 棕色
	 * <p>
	 * 注意：此颜色没有对应的 {@link CompChatColor}，
	 * 因此会使用 GOLD 聊天颜色代替
	 */
	public static final CompColor BROWN = new CompColor("BROWN", DyeColor.BROWN, CompChatColor.GOLD);

	/**
	 * 深红色，在染料颜色中称为 red
	 */
	public static final CompColor DARK_RED = new CompColor("DARK_RED", DyeColor.RED);

	/**
	 * 红色
	 */
	public static final CompColor RED = new CompColor("RED", DyeColor.RED);

	/**
	 * 白色
	 */
	public static final CompColor WHITE = new CompColor("WHITE", DyeColor.WHITE);

	/**
	 * 黄色
	 */
	public static final CompColor YELLOW = new CompColor("YELLOW", DyeColor.YELLOW);

	/**
	 * 深紫色，在染料颜色中称为 purple
	 */
	public static final CompColor DARK_PURPLE = new CompColor("DARK_PURPLE", DyeColor.PURPLE);

	/**
	 * 淡紫色，在染料颜色中称为 magenta
	 */
	public static final CompColor LIGHT_PURPLE = new CompColor("LIGHT_PURPLE", DyeColor.MAGENTA);

	/**
	 * 粉红色
	 * <p>
	 * 注意：此颜色没有对应的 {@link CompChatColor}，
	 * 因此会使用 LIGHT_PURPLE 聊天颜色代替
	 */
	public static final CompColor PINK = new CompColor("PINK", DyeColor.PINK, CompChatColor.LIGHT_PURPLE);

	/**
	 * toString 表示
	 */
	@Getter
	private final String name;

	/**
	 * {@link DyeColor} 表示
	 */
	@Getter
	private final DyeColor dye;

	/**
	 * {@link CompChatColor} 表示
	 */
	@Getter
	private final CompChatColor chatColor;

	/**
	 * 旧版染料/聊天颜色名称，没有则为 null
	 */
	private final String legacyName;

	/**
	 * 关联的 HEX 颜色，未设置则为 null
	 */
	private Color color;

	private CompColor(final Color color) {
		this(null, null, null);

		this.color = color;
	}

	private CompColor(final String name, final DyeColor dye) {
		this(name, dye, null);
	}

	private CompColor(final String name, final DyeColor dye, final CompChatColor chatColor) {
		this(name, dye, chatColor, null);
	}

	private CompColor(final String name, final DyeColor dye, final CompChatColor chatColor, final String legacyName) {
		this.name = name;
		this.dye = dye;
		this.chatColor = chatColor == null ? name != null ? CompChatColor.of(name) : CompChatColor.WHITE : chatColor;
		this.legacyName = Common.getOrEmpty(legacyName);

		values.add(this);
	}

	/**
	 * 获取 bukkit 颜色
	 *
	 * @return
	 */
	public Color getColor() {
		return this.color != null ? this.color : this.dye.getColor();
	}

	/**
	 * 转换为羊毛材质
	 *
	 * @return
	 */
	public CompMaterial getWool() {
		return CompColor.toWool(this.chatColor);
	}

	// ----------------------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 尝试按主名称查找枚举，失败则按次名称查找，
	 * 仍失败则返回 null
	 *
	 * @param newName
	 * @param oldName
	 * @param clazz
	 * @return
	 */
	private static <T extends Enum<T>> T getEnum(final String newName, final String oldName, final Class<T> clazz) {
		T en = ReflectionUtil.lookupEnumSilent(clazz, newName);

		if (en == null)
			en = ReflectionUtil.lookupEnumSilent(clazz, oldName);

		return en;
	}

	/**
	 * 根据羊毛数据值创建新的兼容染料
	 *
	 * @param data
	 * @return
	 */
	public static CompColor fromWoolData(final byte data) {
		return fromDye(DyeColor.getByWoolData(data));
	}

	/**
	 * 根据名称创建新的兼容染料
	 *
	 * @param color
	 * @return
	 */
	public static CompColor fromColor(Color color) {
		return fromName("#" + Integer.toHexString(color.asRGB()).substring(2));
	}

	/**
	 * 根据名称创建新的兼容染料。名称也可以是有效的
	 * RGB：#123456
	 *
	 * @param name
	 * @return
	 */
	public static CompColor fromName(String name) {

		// Support HEX colors
		if (name.startsWith("#") && name.length() == 7)
			return new CompColor(Color.fromRGB(
					Integer.parseInt(name.substring(1, 3), 16),
					Integer.parseInt(name.substring(3, 5), 16),
					Integer.parseInt(name.substring(5, 7), 16)));

		name = name.toUpperCase();

		for (final CompColor comp : values())
			if (comp.chatColor.toString().equals(name) || comp.dye.toString().equals(name) || comp.legacyName.equals(name))
				return comp;

		throw new IllegalArgumentException("Could not get CompColor from name: " + name);
	}

	/**
	 * 根据 bukkit 染料创建新的兼容染料
	 *
	 * @param dye
	 * @return
	 */
	public static CompColor fromDye(final DyeColor dye) {
		for (final CompColor comp : values())
			if (comp.dye == dye || comp.legacyName.equals(dye.toString()))
				return comp;

		throw new IllegalArgumentException("Could not get CompColor from DyeColor." + dye.toString());
	}

	/**
	 * 根据给定聊天颜色返回 {@link CompColor}
	 *
	 * @param color
	 * @return
	 */
	public static CompColor fromChatColor(final CompChatColor color) {
		for (final CompColor comp : values())
			if (comp.chatColor == color || comp.legacyName.equalsIgnoreCase(color.toString()))
				return comp;

		throw new FoException("Could not get CompColor from ChatColor." + color.getName());
	}

	// ----------------------------------------------------------------------------------------------------
	// Converters
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 将聊天颜色转换为染料颜色
	 *
	 * @param color
	 * @return
	 */
	public static DyeColor toDye(final CompChatColor color) {
		final CompColor c = fromName(color.getName());

		return c != null ? c.getDye() : DyeColor.WHITE;
	}

	/**
	 * 将染料颜色转换为聊天颜色
	 *
	 * @param dye
	 * @return
	 */
	public static CompChatColor toColor(final DyeColor dye) {
		for (final CompColor color : CompColor.values())
			if (color.getDye() == dye)
				return color.getChatColor();

		return CompChatColor.WHITE;
	}

	/**
	 * 返回彩色混凝土（若当前 MC 版本不支持则返回羊毛）
	 *
	 * @param color
	 * @return
	 */
	public static CompMaterial toConcrete(final CompChatColor color) {
		final CompMaterial wool = toWool(color);

		return CompMaterial.fromString(wool.toString().replace("_WOOL", MinecraftVersion.olderThan(V.v1_12) ? "_STAINED_GLASS" : "_CONCRETE"));
	}

	/**
	 * 根据给定聊天颜色创建彩色羊毛
	 *
	 * @param color
	 * @return
	 */
	public static CompMaterial toWool(final CompChatColor color) {
		final CompColor comp = fromChatColor(color);

		if (comp == AQUA)
			return CompMaterial.LIGHT_BLUE_WOOL;

		if (comp == BLACK)
			return CompMaterial.BLACK_WOOL;

		if (comp == BLUE)
			return CompMaterial.BLUE_WOOL;

		if (comp == BROWN)
			return CompMaterial.BROWN_WOOL;

		if (comp == DARK_AQUA)
			return CompMaterial.CYAN_WOOL;

		if (comp == DARK_BLUE)
			return CompMaterial.BLUE_WOOL;

		if (comp == DARK_GRAY)
			return CompMaterial.GRAY_WOOL;

		if (comp == DARK_GREEN)
			return CompMaterial.GREEN_WOOL;

		if (comp == DARK_PURPLE)
			return CompMaterial.PURPLE_WOOL;

		if (comp == DARK_RED)
			return CompMaterial.RED_WOOL;

		if (comp == GOLD)
			return CompMaterial.ORANGE_WOOL;

		if (comp == GRAY)
			return CompMaterial.LIGHT_GRAY_WOOL;

		if (comp == GREEN)
			return CompMaterial.LIME_WOOL;

		if (comp == LIGHT_PURPLE)
			return CompMaterial.MAGENTA_WOOL;

		if (comp == PINK)
			return CompMaterial.PINK_WOOL;

		if (comp == RED)
			return CompMaterial.RED_WOOL;

		if (comp == WHITE)
			return CompMaterial.WHITE_WOOL;

		if (comp == YELLOW)
			return CompMaterial.YELLOW_WOOL;

		return CompMaterial.WHITE_WOOL;

	}

	// ----------------------------------------------------------------------------------------------------
	// Leftovers from when this class was an enum
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 返回所有预定义颜色
	 *
	 * @return
	 */
	public static CompColor[] values() {
		return values.toArray(new CompColor[values.size()]);
	}

	@Override
	public String toString() {
		return this.name;
	}
}