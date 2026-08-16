package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.RangedValue;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.NonNull;

/**
 * 检查条件并抛出我们的安全异常的工具类，
 * 异常会记入文件。
 */
public class Valid {

	/**
	 * 匹配有效整数
	 */
	private static final Pattern PATTERN_INTEGER = Pattern.compile("-?\\d+");

	/**
	 * 匹配有效自然数
	 */
	private static final Pattern PATTERN_DECIMAL = Pattern.compile("([0-9]+\\.?[0-9]*|\\.[0-9]+)");

	// ------------------------------------------------------------------------------------------------------------
	// Checking for validity and throwing errors if false or null
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若给定对象为 null 则抛错
	 *
	 * @param toCheck
	 */
	public static void checkNotNull(final Object toCheck) {
		if (toCheck == null)
			throw new FoException();
	}

	/**
	 * 若给定对象为 null 则抛出带自定义消息的错误
	 *
	 * @param toCheck
	 * @param falseMessage
	 */
	public static void checkNotNull(final Object toCheck, final String falseMessage) {
		if (toCheck == null)
			throw new FoException(falseMessage);
	}

	/**
	 * 若给定表达式为 false 则抛错
	 *
	 * @param expression
	 */
	public static void checkBoolean(final boolean expression) {
		if (!expression)
			throw new FoException();
	}

	/**
	 * 若给定表达式为 false 则抛出带自定义消息的错误
	 *
	 * @param expression
	 * @param falseMessage
	 * @param replacements
	 */
	public static void checkBoolean(final boolean expression, final String falseMessage, final Object... replacements) {
		if (!expression) {
			String message = falseMessage;

			try {
				message = String.format(falseMessage, replacements);

			} catch (final Throwable t) {
			}

			throw new FoException(message);
		}
	}

	/**
	 * 若给定 toCheck 字符串不是数字则抛出带自定义消息的错误！
	 *
	 * @param toCheck
	 * @param falseMessage
	 * @param replacements
	 */
	public static void checkInteger(final String toCheck, final String falseMessage, final Object... replacements) {
		if (!Valid.isInteger(toCheck))
			throw new FoException(String.format(falseMessage, replacements));
	}

	/**
	 * 若给定集合为 null 或空则抛出带自定义消息的错误
	 *
	 * @param collection
	 * @param message
	 */
	public static void checkNotEmpty(final Map<?, ?> collection, final String message) {
		if (collection == null || collection.size() == 0)
			throw new IllegalArgumentException(message);
	}

	/**
	 * 若给定集合为 null 或空则抛出带自定义消息的错误
	 *
	 * @param collection
	 * @param message
	 */
	public static void checkNotEmpty(final Collection<?> collection, final String message) {
		if (collection == null || collection.size() == 0)
			throw new IllegalArgumentException(message);
	}

	/**
	 * 若给定消息为空或 null 则抛错
	 *
	 * @param message
	 * @param emptyMessage
	 */
	public static void checkNotEmpty(final String message, final String emptyMessage) {
		if (message == null || message.length() == 0)
			throw new IllegalArgumentException(emptyMessage);
	}

	/**
	 * 检查玩家是否有给定权限，若没有则向他发送 {@link SimpleLocalization#NO_PERMISSION}
	 * 消息并返回 false，否则不发送消息并返回 true
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public static boolean checkPermission(final CommandSender sender, final String permission) {
		if (!PlayerUtil.hasPerm(sender, permission)) {
			Common.tell(sender, SimpleLocalization.NO_PERMISSION.replace("{permission}", permission));

			return false;
		}

		return true;
	}

	/**
	 * 检查调用本方法的代码是否运行在主线程，
	 * 否则以错误消息失败
	 *
	 * @param asyncErrorMessage
	 */
	public static void checkSync(final String asyncErrorMessage) {
		Valid.checkBoolean(Bukkit.isPrimaryThread(), asyncErrorMessage);
	}

	/**
	 * 检查调用本方法的代码是否运行在非主线程，
	 * 否则以错误消息失败
	 *
	 * @param syncErrorMessage
	 */
	public static void checkAsync(final String syncErrorMessage) {
		Valid.checkBoolean(!Bukkit.isPrimaryThread() || Remain.isFolia(), syncErrorMessage);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Checking for true without throwing errors
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若给定对象为 null 则返回 true
	 *
	 * @param raw
	 * @param falseMessage
	 */
	public static void checkInteger(final String raw, String falseMessage) {
		Valid.checkBoolean(isInteger(raw), falseMessage);
	}

	/**
	 * 若给定字符串是有效整数则返回 true
	 *
	 * @param raw
	 * @return
	 */
	public static boolean isInteger(final String raw) {
		Valid.checkNotNull(raw, "Cannot check if null is an integer!");

		return Valid.PATTERN_INTEGER.matcher(raw).matches();
	}

	/**
	 * 检查给定字符串是否为有效整数，否则抛错
	 *
	 * @param raw
	 * @param falseMessage
	 */
	public static void checkDecimal(final String raw, final String falseMessage) {
		Valid.checkBoolean(isDecimal(raw), falseMessage);
	}

	/**
	 * 若给定字符串是有效自然数则返回 true
	 *
	 * @param raw
	 * @return
	 */
	public static boolean isDecimal(final String raw) {
		Valid.checkNotNull(raw, "Cannot check if null is a decimal!");

		return Valid.PATTERN_DECIMAL.matcher(raw).matches();
	}

	/**
	 * <p>检查字符串是否为有效 Java 数字。</p>
	 *
	 * <p>有效数字包括带 <code>0x</code> 限定符的十六进制、科学计数法
	 * 和带类型限定符的数字
	 * （例如 123L）。</p>
	 *
	 * <p><code>Null</code> 和空字符串将返回
	 * <code>false</code>。</p>
	 *
	 * @author Apache Commons NumberUtils
	 * @param raw  要检查的 <code>String</code>
	 * @return 若字符串是格式正确的数字则返回 <code>true</code>
	 */
	public static boolean isNumber(@NonNull final String raw) {
		Valid.checkNotNull(raw, "Cannot check if null is a Number!");

		if (raw.isEmpty())
			return false;

		final char[] letters = raw.toCharArray();
		int length = letters.length;
		boolean hasExp = false;
		boolean hasDecPoint = false;
		boolean allowSigns = false;
		boolean foundDigit = false;

		// deal with any possible sign up front
		final int start = (letters[0] == '-') ? 1 : 0;

		if (length > start + 1)
			if (letters[start] == '0' && letters[start + 1] == 'x') {
				int i = start + 2;
				if (i == length)
					return false; // str == "0x"
				// checking hex (it can't be anything else)
				for (; i < letters.length; i++)
					if ((letters[i] < '0' || letters[i] > '9')
							&& (letters[i] < 'a' || letters[i] > 'f')
							&& (letters[i] < 'A' || letters[i] > 'F'))
						return false;
				return true;
			}
		length--; // don't want to loop to the last char, check it afterwords
		// for type qualifiers
		int i = start;
		// loop to the next to last char or to the last char if we need another digit to
		// make a valid number (e.g. chars[0..5] = "1234E")
		while (i < length || (i < length + 1 && allowSigns && !foundDigit)) {
			if (letters[i] >= '0' && letters[i] <= '9') {
				foundDigit = true;
				allowSigns = false;

			} else if (letters[i] == '.') {
				if (hasDecPoint || hasExp)
					// two decimal points or dec in exponent
					return false;
				hasDecPoint = true;
			} else if (letters[i] == 'e' || letters[i] == 'E') {
				// we've already taken care of hex.
				if (hasExp)
					// two E's
					return false;
				if (!foundDigit)
					return false;
				hasExp = true;
				allowSigns = true;
			} else if (letters[i] == '+' || letters[i] == '-') {
				if (!allowSigns)
					return false;
				allowSigns = false;
				foundDigit = false; // we need a digit after the E
			} else
				return false;
			i++;
		}
		if (i < letters.length) {
			if (letters[i] >= '0' && letters[i] <= '9')
				// no type qualifier, OK
				return true;
			if (letters[i] == 'e' || letters[i] == 'E')
				// can't have an E at the last byte
				return false;
			if (letters[i] == '.') {
				if (hasDecPoint || hasExp)
					// two decimal points or dec in exponent
					return false;
				// single trailing decimal point after non-exponent is ok
				return foundDigit;
			}
			if (!allowSigns
					&& (letters[i] == 'd'
							|| letters[i] == 'D'
							|| letters[i] == 'f'
							|| letters[i] == 'F'))
				return foundDigit;
			if (letters[i] == 'l'
					|| letters[i] == 'L')
				// not allowing L with an exponent
				return foundDigit && !hasExp;
			// last character is illegal
			return false;
		}
		// allowSigns is true iff the val ends in 'E'
		// found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
		return !allowSigns && foundDigit;
	}

	/**
	 * 若数组只由 null 或空字符串值组成则返回 true
	 *
	 * @param array
	 * @return
	 */
	public static boolean isNullOrEmpty(final Collection<?> array) {
		return array == null || Valid.isNullOrEmpty(array.toArray());
	}

	/**
	 * 若映射为 null 或只含 null 值则返回 true
	 *
	 * @param map
	 * @return
	 */
	public static boolean isNullOrEmptyValues(final SerializedMap map) {
		return isNullOrEmptyValues(map == null ? null : map.asMap());
	}

	/**
	 * 若映射为 null 或只含 null 值则返回 true
	 *
	 * @param map
	 * @return
	 */
	public static boolean isNullOrEmptyValues(final Map<?, ?> map) {

		if (map == null)
			return true;

		for (final Object value : map.values())
			if (value != null)
				return false;

		return true;
	}

	/**
	 * 若数组只由 null 或空字符串值组成则返回 true
	 *
	 * @param array
	 * @return
	 */
	public static boolean isNullOrEmpty(final Object[] array) {
		if (array != null)
			for (final Object object : array)
				if (object instanceof String) {
					if (!((String) object).isEmpty())
						return false;

				} else if (object != null)
					return false;

		return true;
	}

	/**
	 * 若给定消息为 null 或空则返回 true
	 *
	 * @param message
	 * @return
	 */
	public static boolean isNullOrEmpty(final String message) {
		return message == null || message.isEmpty();
	}

	/**
	 * 若给定向量的 x-y-z 坐标全是有限有效数字则返回 true
	 * （见 {@link Double#isFinite(double)}）
	 *
	 * @param vector
	 * @return
	 */
	public static boolean isFinite(final Vector vector) {
		return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
	}

	/**
	 * 若给定值在边界之间则返回 true
	 *
	 * @param value
	 * @param ranged
	 * @return
	 */
	public static boolean isInRange(final long value, final RangedValue ranged) {
		return value >= ranged.getMinLong() && value <= ranged.getMaxLong();
	}

	/**
	 * 若给定值在边界之间则返回 true
	 *
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static boolean isInRange(final double value, final double min, final double max) {
		return value >= min && value <= max;
	}

	/**
	 * 若给定值在边界之间则返回 true
	 *
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static boolean isInRange(final long value, final long min, final long max) {
		return value >= min && value <= max;
	}

	/**
	 * 若给定对象是 {@link UUID} 则返回 true
	 *
	 * @param object
	 * @return
	 */
	public static boolean isUUID(final Object object) {
		if (object instanceof String) {
			final String[] components = object.toString().split("-");

			return components.length == 5;
		}

		return object instanceof UUID;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Equality checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若两个位置的世界和方块坐标相同则返回 true
	 *
	 * @param first
	 * @param sec
	 * @return
	 */
	public static boolean locationEquals(final Location first, final Location sec) {

		if (first == null && sec == null)
			return true;

		if ((first == null && sec == null) || (first != null && sec == null))
			return false;

		try {
			if (!first.getWorld().getName().equals(sec.getWorld().getName()))
				return false;
		} catch (final NullPointerException ex) {
			// Ignore
		}

		return first.getBlockX() == sec.getBlockX() && first.getBlockY() == sec.getBlockY() && first.getBlockZ() == sec.getBlockZ();
	}

	/**
	 * 比较两个列表。两列表等长且所有值相同时视为相等。
	 * 例外：字符串比较前会剥离颜色。
	 * 
	 * @param <T> 
	 * @param first 要比较的第一个列表
	 * @param second 要比较的第二个列表
	 * @return 若列表相等则返回 true
	 */
	public static <T> boolean listEquals(final List<T> first, final List<T> second) {
		if (first == null && second == null)
			return true;

		if (first == null)
			return false;

		if (second == null)
			return false;

		if (first.size() != second.size())
			return false;

		for (int i = 0; i < first.size(); i++) {
			final T f = first.get(i);
			final T s = second.get(i);

			if (f == null && s != null)
				return false;

			if (f != null && s == null)
				return false;

			if (f != null && !f.equals(s))
				if (!Common.stripColors(f.toString()).equalsIgnoreCase(Common.stripColors(s.toString())))
					return false;
		}

		return true;
	}

	/**
	 * 若两字符串相等（忽略颜色）则返回 true
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public static boolean colorlessEquals(final String first, final String second) {
		return Common.stripColors(first).equalsIgnoreCase(Common.stripColors(second));
	}

	/**
	 * 若两字符串列表相等（忽略颜色）则返回 true
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public static boolean colorlessEquals(final List<String> first, final List<String> second) {
		return colorlessEquals(Common.toArray(first), Common.toArray(second));
	}

	/**
	 * 若两字符串数组相等（忽略颜色）则返回 true
	 *
	 * @param firstArray
	 * @param secondArray
	 * @return
	 */
	public static boolean colorlessEquals(final String[] firstArray, final String[] secondArray) {
		for (int i = 0; i < firstArray.length; i++) {
			final String first = Common.stripColors(firstArray[i]);
			final String second = i < secondArray.length ? Common.stripColors(secondArray[i]) : "";

			if (!first.equalsIgnoreCase(second))
				return false;
		}

		return true;
	}

	/**
	 * 若给定列表中所有字符串都相等则返回 true
	 *
	 * @param values
	 * @return
	 */
	public static boolean valuesEqual(final Collection<String> values) {
		final List<String> copy = new ArrayList<>(values);
		String lastValue = null;

		for (final String value : copy) {
			if (lastValue == null)
				lastValue = value;

			if (!lastValue.equals(value))
				return false;

			lastValue = value;
		}

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Matching in lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 按模式返回给定元素是否在给定列表中
	 *
	 * 若启用黑名单模式，元素不在列表中时返回 true；
	 * 若为 false，元素在列表中时返回 true。
	 *
	 * @param element
	 * @param isBlacklist
	 * @param list
	 * @return
	 */
	public static boolean isInList(final String element, final boolean isBlacklist, final Iterable<String> list) {
		return isBlacklist == Valid.isInList(element, list);
	}

	/**
	 * 若给定列表中任一元素等于（忽略大小写）给定元素则返回 true
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public static boolean isInList(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).equalsIgnoreCase(removeSlash(matched)))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * 若给定列表中任一元素以（忽略大小写）给定元素开头则返回 true
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public static boolean isInListStartsWith(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).toLowerCase().startsWith(removeSlash(matched).toLowerCase()))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * 若给定列表中任一元素匹配给定元素则返回 true。
	 *
	 * 会从该列表元素编译正则表达式。
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public static boolean isInListRegex(final String element, final Iterable<String> list) {
		try {
			for (final String regex : list)
				if (Common.regExMatch(regex, element))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * 若给定列表中任一元素匹配给定元素则返回 true。
	 *
	 * 会从该列表元素编译正则表达式。
	 *
	 * @param element
	 * @param list
	 * @return
	 */
	public static boolean isInListRegexFast(final String element, final Iterable<Pattern> list) {
		try {
			for (final Pattern regex : list)
				if (Common.regExMatch(regex, element))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * 若给定枚举按 {@link Enum#name()}（不区分大小写）包含给定元素则返回 true
	 *
	 * @param element
	 * @param enumeration
	 * @return
	 */
	public static boolean isInListEnum(final String element, final Enum<?>[] enumeration) {
		for (final Enum<?> constant : enumeration)
			if (constant.name().equalsIgnoreCase(element))
				return true;

		return false;
	}

	/**
	 * 若给定列表中任一元素包含（忽略大小写）给定元素则返回 true
	 *
	 * @param element
	 * @param list
	 * @return
	 *
	 * @deprecated 可能导致意外匹配，例如列表中有 /time 时 /t 也会被命中
	 */
	@Deprecated
	public static boolean isInListContains(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).toLowerCase().contains(removeSlash(matched).toLowerCase()))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * 为 isInList 比较准备消息 - 转小写并移除开头的斜杠 /
	 *
	 * @param message
	 * @return
	 */
	private static String removeSlash(final String message) {
		return message.startsWith("/") ? message.substring(1) : message;
	}
}
