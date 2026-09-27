package top.brmc.devlib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 生成随机数的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtil {

	/**
	 * 本类的随机实例
	 */
	private static final Random random = new Random();

	/**
	 * 使用 & 字符的聊天颜色符号，包括粗体斜体等装饰
	 */
	private static final char[] COLORS_AND_DECORATION = {
			'0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e',
			'f', 'k', 'l', 'n', 'o'
	};

	/**
	 * 仅有效聊天颜色，不含装饰
	 */
	private static final char[] CHAT_COLORS = {
			'0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e',
			'f'
	};

	/**
	 * 英文字母
	 */
	private static final char[] LETTERS = {
			'a', 'b', 'c', 'd', 'e',
			'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o',
			'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'y', 'z',
	};

	/**
	 * 返回随机实例
	 *
	 * @return
	 */
	public static Random getRandom() {
		return random;
	}

	/**
	 * 若命中给定百分比则返回 true
	 *
	 * @param percent 百分比，0 到 100
	 * @return
	 */
	public static boolean chance(final long percent) {
		return chance((int) percent);
	}

	/**
	 * 若命中给定百分比则返回 true
	 *
	 * @param percent 百分比，0 到 100
	 * @return
	 */
	public static boolean chance(final int percent) {
		return random.nextDouble() * 100D < percent;
	}

	/**
	 * 若命中给定百分比则返回 true
	 *
	 * @param percent 百分比，0.00 到 1.00
	 * @return
	 */
	public static boolean chanceD(final double percent) {
		return random.nextDouble() < percent;
	}

	/**
	 * 返回随机染料颜色
	 *
	 * @return
	 */
	public static DyeColor nextDyeColor() {
		return DyeColor.values()[random.nextInt(DyeColor.values().length)];
	}

	/**
	 * 返回以下格式的随机聊天颜色：& + 颜色字符
	 * 例如：&e 代表黄色
	 * <p>
	 * 也会返回装饰
	 *
	 * @return
	 */
	public static String nextColorOrDecoration() {
		return "&" + COLORS_AND_DECORATION[nextInt(COLORS_AND_DECORATION.length)];
	}

	/**
	 * 生成随机文本，像 lorem ipsum 但完全
	 * 不同。
	 *
	 * @param length
	 * @return
	 */
	public static String nextString(int length) {
		String text = "";

		for (int i = 0; i < length; i++)
			text += LETTERS[nextInt(LETTERS.length)];

		return text;
	}

	/**
	 * 返回随机聊天颜色
	 *
	 * @return
	 */
	public static ChatColor nextChatColor() {
		final char letter = CHAT_COLORS[nextInt(CHAT_COLORS.length)];

		return ChatColor.getByChar(letter);
	}

	/**
	 * 返回随机鲜艳 Bukkit 颜色，从 7 种颜色中选
	 *
	 * @return
	 */
	public static Color nextColor() {
		return nextItem(Color.AQUA, Color.ORANGE, Color.WHITE, Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE);
	}

	/**
	 * 返回范围内随机整数
	 *
	 * @param min
	 * @param max
	 * @return
	 */
	public static int nextBetween(final int min, final int max) {
		Valid.checkBoolean(min <= max, "Min !< max");

		return min + nextInt(max - min + 1);
	}

	/**
	 * 返回随机整数，见 {@link Random#nextInt(int)}
	 *
	 * @param boundExclusive
	 * @return
	 */
	public static int nextInt(final int boundExclusive) {
		Valid.checkBoolean(boundExclusive > 0, "Getting a random number must have the bound above 0, got: " + boundExclusive);

		return random.nextInt(boundExclusive);
	}

	/**
	 * 按 50% 概率返回随机 true/false
	 *
	 * @return
	 */
	public static boolean nextBoolean() {
		return random.nextBoolean();
	}

	/**
	 * 返回数组中的随机项
	 *
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> T nextItem(final T... items) {
		return items[nextInt(items.length)];
	}

	/**
	 * 返回列表中的随机项
	 *
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> T nextItem(final Iterable<T> items) {
		return nextItem(items, null);
	}

	/**
	 * 只在匹配给定条件的项中返回列表中的随机项
	 *
	 * @param <T>
	 * @param items
	 * @param condition 选择时应用的条件
	 * @return
	 */
	public static <T> T nextItem(final Iterable<T> items, final Predicate<T> condition) {
		final List<T> list = items instanceof List ? new ArrayList<>((List<T>) items) : Common.toList(items);

		// Remove values failing the condition
		if (condition != null)
			for (final Iterator<T> it = list.iterator(); it.hasNext();) {
				final T item = it.next();

				if (!condition.test(item))
					it.remove();
			}

		return list.get(nextInt(list.size()));
	}

	/**
	 * 返回随机位置
	 *
	 * @param origin
	 * @param radius
	 * @param is3D 球体搜索为 true，圆柱搜索为 false
	 * @return
	 */
	public static Location nextLocation(final Location origin, final double radius, final boolean is3D) {
		final double rectX = random.nextDouble() * radius;
		final double rectZ = random.nextDouble() * radius;
		final double offsetX;
		final double offsetZ;
		double offsetY = 0;
		final int transform = random.nextInt(4);
		if (is3D) {
			final double rectY = random.nextDouble() * radius;
			offsetY = getYCords(transform, rectY);
		}
		if (transform == 0) {
			offsetX = rectX;
			offsetZ = rectZ;
		} else if (transform == 1) {
			offsetX = -rectZ;
			offsetZ = rectX;
		} else if (transform == 2) {
			offsetX = -rectX;
			offsetZ = -rectZ;
		} else {
			offsetX = rectZ;
			offsetZ = -rectX;
		}

		return origin.clone().add(offsetX, offsetY, offsetZ);
	}

	/**
	 * 返回随机位置，介于最小和最大半径之间：
	 * 例如：最小半径 500、最大 2000，则返回距原点 500-2000 格的位置
	 *
	 * @param origin
	 * @param minRadius
	 * @param maxRadius
	 * @param is3D 球体搜索为 true，圆柱搜索为 false
	 * @return
	 */
	public static Location nextLocation(final Location origin, final double minRadius, final double maxRadius, final boolean is3D) {
		Valid.checkBoolean(maxRadius > 0 && minRadius > 0, "Max and min radius must be over 0");
		Valid.checkBoolean(maxRadius > minRadius, "Max radius must be greater than min radius");

		final double rectX = random.nextDouble() * (maxRadius - minRadius) + minRadius;
		final double rectZ = random.nextDouble() * (maxRadius + minRadius) - minRadius;
		final double offsetX;
		final double offsetZ;
		double offsetY = 0;
		final int transform = random.nextInt(4);
		if (is3D) {
			final double rectY = random.nextDouble() * (maxRadius + minRadius) - minRadius;
			offsetY = getYCords(transform, rectY);
		}
		if (transform == 0) {
			offsetX = rectX;
			offsetZ = rectZ;
		} else if (transform == 1) {
			offsetX = -rectZ;
			offsetZ = rectX;
		} else if (transform == 2) {
			offsetX = -rectX;
			offsetZ = -rectZ;
		} else {
			offsetX = rectZ;
			offsetZ = -rectX;
		}

		return origin.clone().add(offsetX, offsetY, offsetZ);
	}

	public static double getYCords(int transform, double rectY) {
		double offsetY;
		double nextY = random.nextDouble();
		if (transform < 2) {
			offsetY = nextY >= 0.5 ? -rectY : rectY;
		} else {
			offsetY = nextY >= 0.5 ? rectY : -rectY;
		}
		return offsetY;
	}

	/**
	 * 返回该区块内随机的 x 位置
	 *
	 * @param chunk
	 * @return
	 */
	public static int nextChunkX(final Chunk chunk) {
		return RandomUtil.nextInt(16) + (chunk.getX() << 4) - 16;
	}

	/**
	 * 返回该区块内随机的 z 位置
	 *
	 * @param chunk
	 * @return
	 */
	public static int nextChunkZ(final Chunk chunk) {
		return RandomUtil.nextInt(16) + (chunk.getZ() << 4) - 16;
	}
}
