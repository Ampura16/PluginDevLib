package top.brmc.devlib;

import java.awt.Color;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.model.Whiteblacklist;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.CompChatColor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 管理游戏内聊天的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatUtil {

	/**
	 * 默认的居中内边距
	 */
	public final static int CENTER_PX = 152;

	/**
	 * 玩家在聊天记录中一次可见的垂直行数
	 */
	public final static int VISIBLE_CHAT_LINES = 20;

	/**
	 * 按内边距 {@link #CENTER_PX} 自动将消息居中
	 *
	 * @param message
	 * @return
	 */
	public static String center(final String message) {
		return center(message, ' ');
	}

	/**
	 * 使用给定的 centerPx 按内边距将消息居中
	 *
	 * @param message
	 * @param centerPx
	 * @return
	 */
	public static String center(final String message, final int centerPx) {
		return center(message, ' ', centerPx);
	}

	/**
	 * 使用给定的空格字符，按内边距 {@link #CENTER_PX} 将消息居中
	 * 空格字符按给定的聊天颜色着色，例如：
	 * <p>
	 * ================= My Centered Message =================（若空格字符为 '='）
	 *
	 * @param message
	 * @param space
	 * @return
	 */
	public static String center(final String message, final char space) {
		return center(message, space, CENTER_PX);
	}

	/**
	 * 按给定的空格字符、颜色和内边距将消息居中
	 *
	 * @param message
	 * @param space
	 * @param centerPx
	 * @return
	 */
	public static String center(final String message, final char space, final int centerPx) {
		if (message == null || message.equals(""))
			return "";

		int messagePxSize = 0;

		boolean previousCode = false;
		boolean isBold = false;

		for (final char c : message.toCharArray())

			if (c == '&' || c == ChatColor.COLOR_CHAR) {
				previousCode = true;

				continue;

			} else if (previousCode) {
				previousCode = false;

				if (c == 'l' || c == 'L') {
					isBold = true;

					continue;

				}

				isBold = false;

			} else {
				final DefaultFontInfo defaultFont = DefaultFontInfo.getDefaultFontInfo(c);

				messagePxSize += isBold ? defaultFont.getBoldLength() : defaultFont.getLength();
				messagePxSize++;
			}

		final StringBuilder builder = new StringBuilder();

		final int halvedMessageSize = messagePxSize / 2;
		final int toCompensate = centerPx - halvedMessageSize;
		final DefaultFontInfo font = DefaultFontInfo.getDefaultFontInfo(space);
		final double spaceLength = isBold ? font.getBoldLength() : font.getLength();

		double compensated = 0;

		while (compensated < toCompensate) {
			builder.append(space);

			compensated += spaceLength;
		}

		return builder.toString() + " " + message + " " + builder.toString();
	}

	/**
	 * 将给定消息移至聊天屏幕中央
	 *
	 * @param messages
	 * @return
	 */
	public static String[] verticalCenter(final String... messages) {
		return verticalCenter(Arrays.asList(messages));
	}

	/**
	 * 将给定消息移至聊天屏幕中央
	 *
	 * @param messages
	 * @return
	 */
	public static String[] verticalCenter(final Collection<String> messages) {
		final List<String> lines = new ArrayList<>();
		final long padding = MathUtil.ceiling((VISIBLE_CHAT_LINES - messages.size()) / 2);

		for (int i = 0; i < padding; i++)
			lines.add(RandomUtil.nextColorOrDecoration());

		lines.addAll(messages);

		for (int i = 0; i < padding; i++)
			lines.add(RandomUtil.nextColorOrDecoration());

		return lines.toArray(new String[lines.size()]);
	}

	/**
	 * 在消息中插入点号 '.'。域名和数字会被忽略。
	 *
	 * @param message 要处理的消息
	 * @return 插入点号后的消息
	 */
	public static String insertDot(String message) {
		if (message.isEmpty())
			return "";

		final String lastChar = message.substring(message.length() - 1);
		final String[] words = message.split("\\s");
		final String lastWord = words[words.length - 1];

		if (!isDomain(lastWord) && lastChar.matches("(?i)[a-z\u0400-\u04FF]"))
			message = message + ".";

		return message;
	}

	/**
	 * 将句子首字母大写。域名会被忽略，可检测多个
	 * 句子。
	 *
	 * @param message 要检查的消息
	 * @return 首字母大写后的消息
	 */
	public static String capitalizeFirst(final String message) {
		if (message.isEmpty())
			return "";

		final String[] sentences = message.split("(?<=[!?\\.])\\s");
		String tempMessage = "";

		for (String sentence : sentences)
			try {
				final String word = message.split("\\s")[0];

				if (!isDomain(word))
					sentence = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);

				tempMessage = tempMessage + sentence + " ";
			} catch (final ArrayIndexOutOfBoundsException ex) {
				// Probably an exotic language, silence
			}

		return tempMessage.trim();
	}

	/**
	 * <p>将字符串中所有以空白分隔的单词转为首字母大写形式，
	 * 即每个单词由一个标题大小写字符开头，后跟一串
	 * 小写字符。</p>
	 *
	 * <p>空白由 {@link Character#isWhitespace(char)} 定义。
	 * 输入字符串为 <code>null</code> 时返回 <code>null</code>。
	 * 大写使用 Unicode 标题大小写，通常等同于
	 * 大写。</p>
	 *
	 * <pre>
	 * capitalizeFully(null)        = null
	 * capitalizeFully("")          = ""
	 * capitalizeFully("i am FINE") = "I Am Fine"
	 * </pre>
	 *
	 * @param message  要做首字母大写的字符串，可为 null
	 * @return 大写后的字符串，若输入字符串为 null 则返回 <code>null</code>
	 */
	public static String capitalizeFully(String message) {
		return capitalizeFully(message, (char[]) null);
	}

	private static String capitalizeFully(String str, char[] delimiters) {
		final int delimLen = delimiters == null ? -1 : delimiters.length;

		if (str != null && str.length() != 0 && delimLen != 0) {
			str = str.toLowerCase();

			return capitalize(str, delimiters);
		}

		return str;
	}

	/**
	 * <p>将字符串中所有以空白分隔的单词首字母大写。
	 * 仅改变每个单词的首字母。
	 *
	 * <p>空白由 {@link Character#isWhitespace(char)} 定义。
	 * 输入字符串为 <code>null</code> 时返回 <code>null</code>。
	 * 大写使用 Unicode 标题大小写，通常等同于
	 * 大写。</p>
	 *
	 * <pre>
	 * capitalize(null)        = null
	 * capitalize("")          = ""
	 * capitalize("i am FINE") = "I Am FINE"
	 * capitalize("&7i am FINE") = "I Am FINE" // 支持颜色！
	 * </pre>
	 *
	 * @author Apache Commons - WordUtils
	 * @param message  要做首字母大写的字符串，可为 null
	 * @return 大写后的字符串，若输入字符串为 null 则返回 <code>null</code>
	 */
	public static String capitalize(String message) {
		return capitalize(message, (char[]) null);
	}

	private static String capitalize(String str, char[] delimiters) {
		final int delimLen = delimiters == null ? -1 : delimiters.length;
		if (str != null && str.length() != 0 && delimLen != 0) {
			final int strLen = str.length();
			final StringBuffer buffer = new StringBuffer(strLen);
			boolean capitalizeNext = true;

			for (int i = 0; i < strLen; ++i) {
				final char ch = str.charAt(i);

				if (isDelimiter(ch, delimiters)) {
					buffer.append(ch);

					capitalizeNext = true;

				} else if (capitalizeNext) {
					buffer.append(Character.toTitleCase(ch));

					capitalizeNext = false;
				} else
					buffer.append(ch);

			}

			return buffer.toString();
		}

		return str;
	}

	private static boolean isDelimiter(char ch, char[] delimiters) {
		if (delimiters == null)
			return Character.isWhitespace(ch);

		int i = 0;

		for (final int isize = delimiters.length; i < isize; ++i)
			if (ch == delimiters[i])
				return true;

		return false;
	}

	/**
	 * 将消息中句子的第二个字符转小写。
	 *
	 * @param message
	 * @return
	 */
	public static String lowercaseSecondChar(final String message) {
		if (message.isEmpty())
			return "";

		final String[] sentences = message.split("(?<=[!?\\.])\\s");
		String tempMessage = "";

		for (String sentence : sentences)
			try {
				if (sentence.length() > 2)
					if (!isDomain(message.split("\\s")[0]) && sentence.length() > 2 && Character.isUpperCase(sentence.charAt(0)) && Character.isLowerCase(sentence.charAt(2)))
						sentence = sentence.substring(0, 1) + sentence.substring(1, 2).toLowerCase() + sentence.substring(2);

				tempMessage = tempMessage + sentence + " ";
			} catch (final NullPointerException ex) {
			}
		return tempMessage.trim();
	}

	/**
	 * {@link Matcher#quoteReplacement(String)} 的改进版
	 * 我们还会转义 ()+ 等额外字符
	 *
	 * @param message
	 * @return
	 */
	public static String quoteReplacement(String message) {

		final StringBuilder builder = new StringBuilder();

		for (int index = 0; index < message.length(); index++) {
			final char c = message.charAt(index);

			if (c == ' ' || c == '\\' || c == '$' || c == '(' || c == ')' || c == '+' || c == '.' || c == '-' || c == '_' || c == '^')
				builder.append('\\');

			builder.append(c);
		}

		return builder.toString();
	}

	/**
	 * 尝试移除给定输入中的所有 emoji
	 *
	 * @author https://stackoverflow.com/a/32101331
	 * @param message
	 * @return
	 */
	public static String removeEmoji(String message) {
		if (message == null)
			return "";

		final String regex = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]";
		final Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		final Matcher matcher = pattern.matcher(message);

		return matcher.replaceAll("");
	}

	/**
	 * 消息中有多少大写字母（百分比）。
	 *
	 * @param message 要检查的消息
	 *
	 * @return 消息中大写字母所占百分比（0 到 100）
	 */
	public static double getCapsPercentage(final String message) {
		if (message.isEmpty())
			return 0;

		final String[] sentences = Common.stripColors(message).split(" ");
		String messageToCheck = "";
		double upperCount = 0;

		for (final String sentence : sentences)
			if (!isDomain(sentence))
				messageToCheck += sentence + " ";

		for (final char ch : messageToCheck.toCharArray())
			if (Character.isUpperCase(ch))
				upperCount++;

		return upperCount / messageToCheck.length();
	}

	/**
	 * 消息中有多少个大写字母。
	 *
	 * @param message 要检查的消息
	 * @param ignored 要忽略的字符串列表（白名单）
	 *
	 * @return 消息中大写字母的数量
	 */
	public static int getCapsInRow(final String message, final List<String> ignored) {
		if (message.isEmpty())
			return 0;

		final int[] caps = splitCaps(Common.stripColors(message), ignored);

		int sum = 0;
		int sumTemp = 0;

		for (final int i : caps)
			if (i == 1) {
				sumTemp++;
				sum = Math.max(sum, sumTemp);
			} else
				sumTemp = 0;

		return sum;
	}

	/**
	 * 消息中有多少个大写字母。
	 *
	 * @param message 要检查的消息
	 * @param list 要忽略的字符串列表（白名单）
	 *
	 * @return 消息中大写字母的数量
	 */
	public static int getCapsInRow(final String message, final Whiteblacklist list) {
		if (message.isEmpty())
			return 0;

		final int[] caps = splitCaps(Common.stripColors(message), list);

		int sum = 0;
		int sumTemp = 0;

		for (final int i : caps)
			if (i == 1) {
				sumTemp++;
				sum = Math.max(sum, sumTemp);
			} else
				sumTemp = 0;

		return sum;
	}

	/**
	 * 计算两个字符串之间的相似度（0.00 到 1.00 之间的小数）。
	 *
	 * @param first
	 * @param second
	 *
	 * @return
	 */
	public static double getSimilarityPercentage(String first, String second) {
		if (first.isEmpty() && second.isEmpty())
			return 1D;

		first = removeSimilarity(first);
		second = removeSimilarity(second);

		String longer = first, shorter = second;

		if (first.length() < second.length()) { // longer should always have greater length
			longer = second;
			shorter = first;
		}

		final int longerLength = longer.length();

		if (longerLength == 0)
			return 0; /* both strings are zero length */

		return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
	}

	/*
	 * Remove any similarity traits of a message such as removing colors,
	 * lowercasing it, removing diacritic
	 */
	private static String removeSimilarity(String message) {

		if (SimplePlugin.getInstance().similarityStripAccents())
			message = replaceDiacritic(message);

		message = Common.stripColors(message);
		message = message.toLowerCase();

		return message;
	}

	/**
	 * 若给定字符串是 http(s) 和/或 www 域名则返回 true
	 *
	 * @param message
	 * @return
	 */
	public static boolean isDomain(final String message) {
		return Common.regExMatch("(https?:\\/\\/(?:www\\.|(?!www))[^\\s\\.]+\\.[^\\s]{2,}|www\\.[^\\s]+\\.[^\\s]{2,})", message);
	}

	/**
	 * 将特殊重音字母替换为其非重音形式
	 * 例如 á 会被替换为 a
	 *
	 * @param message
	 * @return
	 */
	public static String replaceDiacritic(final String message) {
		return Normalizer.normalize(message, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	/**
	 * 若给定消息包含 [JSON] 或任何交互部分（如 {@literal <toast> or <actionbar>}）则返回 true
	 * @param msg
	 * @return
	 */
	public static boolean isInteractive(String msg) {
		return msg.startsWith("[JSON]") || msg.startsWith("<toast>") || msg.startsWith("<title>") || msg.startsWith("<actionbar>") || msg.startsWith("<bossbar>");
	}

	/**
	 * 使用两种颜色作为起止色，自动为给定字符串添加渐变
	 *
	 * @param message
	 * @param fromColor
	 * @param toColor 颜色，例如 #FF1122、&c 或 red
	 * @return
	 */
	public static String generateGradient(String message, String fromColor, String toColor) {
		return generateGradient(message, CompChatColor.of(fromColor), CompChatColor.of(toColor));
	}

	/**
	 * 使用两种颜色作为起止色，自动为给定字符串添加渐变
	 *
	 * @param message
	 * @param fromColor
	 * @param toColor
	 * @return
	 */
	public static String generateGradient(String message, CompChatColor fromColor, CompChatColor toColor) {
		return generateGradient(message, fromColor.getColor(), toColor.getColor());
	}

	/**
	 * 使用两种颜色作为起止色，自动为给定字符串添加渐变
	 *
	 * @param message
	 * @param fromColor
	 * @param toColor
	 * @return
	 */
	public static String generateGradient(String message, Color fromColor, Color toColor) {
		if (!MinecraftVersion.atLeast(V.v1_16))
			return message;

		final char[] letters = message.toCharArray();
		String gradient = "";

		final List<String> decorations = new ArrayList<>();

		for (int i = 0; i < letters.length; i++) {
			final char letter = letters[i];

			// Support color decoration and insert it manually after each character
			if (letter == ChatColor.COLOR_CHAR && i + 1 < letters.length) {
				final char decoration = letters[i + 1];

				if (decoration == 'k')
					decorations.add(ChatColor.MAGIC.toString());

				else if (decoration == 'l')
					decorations.add(ChatColor.BOLD.toString());

				else if (decoration == 'm')
					decorations.add(ChatColor.STRIKETHROUGH.toString());

				else if (decoration == 'n')
					decorations.add(ChatColor.UNDERLINE.toString());

				else if (decoration == 'o')
					decorations.add(ChatColor.ITALIC.toString());

				else if (decoration == 'r')
					decorations.add(ChatColor.RESET.toString());

				i++;
				continue;
			}

			final float ratio = (float) i / (float) letters.length;

			final int red = (int) (toColor.getRed() * ratio + fromColor.getRed() * (1 - ratio));
			final int green = (int) (toColor.getGreen() * ratio + fromColor.getGreen() * (1 - ratio));
			final int blue = (int) (toColor.getBlue() * ratio + fromColor.getBlue() * (1 - ratio));

			final Color stepColor = new Color(red, green, blue);

			gradient += CompChatColor.of(stepColor) + String.join("", decorations) + letters[i];
		}

		return gradient;
	}

	// --------------------------------------------------------------------------------
	// Helpers
	// --------------------------------------------------------------------------------

	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
	private static int editDistance(String first, String second) {
		first = first.toLowerCase();
		second = second.toLowerCase();

		final int[] costs = new int[second.length() + 1];
		for (int i = 0; i <= first.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= second.length(); j++)
				if (i == 0)
					costs[j] = j;
				else if (j > 0) {
					int newValue = costs[j - 1];
					if (first.charAt(i - 1) != second.charAt(j - 1))
						newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
					costs[j - 1] = lastValue;
					lastValue = newValue;
				}
			if (i > 0)
				costs[second.length()] = lastValue;
		}
		return costs[second.length()];
	}

	private static int[] splitCaps(final String message, final List<String> ignored) {
		final int[] editedMsg = new int[message.length()];
		final String[] parts = message.split(" ");

		for (int i = 0; i < parts.length; i++)
			for (final String whitelisted : ignored)
				if (whitelisted.equalsIgnoreCase(parts[i]))
					parts[i] = parts[i].toLowerCase();

		for (int i = 0; i < parts.length; i++)
			if (isDomain(parts[i]))
				parts[i] = parts[i].toLowerCase();

		final String msg = String.join(" ", parts);

		for (int i = 0; i < msg.length(); i++)
			if (Character.isUpperCase(msg.charAt(i)) && Character.isLetter(msg.charAt(i)))
				editedMsg[i] = 1;
			else
				editedMsg[i] = 0;
		return editedMsg;
	}

	private static int[] splitCaps(final String message, final Whiteblacklist list) {
		final int[] editedMsg = new int[message.length()];
		final String[] parts = message.split(" ");

		for (int i = 0; i < parts.length; i++)
			if (list.isInList(parts[i]))
				parts[i] = parts[i].toLowerCase();

		for (int i = 0; i < parts.length; i++)
			if (isDomain(parts[i]))
				parts[i] = parts[i].toLowerCase();

		final String msg = String.join(" ", parts);

		for (int i = 0; i < msg.length(); i++)
			if (Character.isUpperCase(msg.charAt(i)) && Character.isLetter(msg.charAt(i)))
				editedMsg[i] = 1;
			else
				editedMsg[i] = 0;
		return editedMsg;
	}
}

/**
 * 包含所有允许的 Minecraft 字母的信息
 *
 * @deprecated 不能正确格式化粗体和新的 Minecraft Unicode 字母
 *
 */
@Deprecated
enum DefaultFontInfo {

	A('A', 5),
	a('a', 5),
	B('B', 5),
	b('b', 5),
	C('C', 5),
	c('c', 5),
	D('D', 5),
	d('d', 5),
	E('E', 5),
	e('e', 5),
	F('F', 5),
	f('f', 4),
	G('G', 5),
	g('g', 5),
	H('H', 5),
	h('h', 5),
	I('I', 3),
	i('i', 1),
	J('J', 5),
	j('j', 5),
	K('K', 5),
	k('k', 4),
	L('L', 5),
	l('l', 1),
	M('M', 5),
	m('m', 5),
	N('N', 5),
	n('n', 5),
	O('O', 5),
	o('o', 5),
	P('P', 5),
	p('p', 5),
	Q('Q', 5),
	q('q', 5),
	R('R', 5),
	r('r', 5),
	S('S', 5),
	s('s', 5),
	T('T', 5),
	t('t', 4),
	U('U', 5),
	u('u', 5),
	V('V', 5),
	v('v', 5),
	W('W', 5),
	w('w', 5),
	X('X', 5),
	x('x', 5),
	Y('Y', 5),
	y('y', 5),
	Z('Z', 5),
	z('z', 5),
	NUM_1('1', 5),
	NUM_2('2', 5),
	NUM_3('3', 5),
	NUM_4('4', 5),
	NUM_5('5', 5),
	NUM_6('6', 5),
	NUM_7('7', 5),
	NUM_8('8', 5),
	NUM_9('9', 5),
	NUM_0('0', 5),
	EXCLAMATION_POINT('!', 1),
	AT_SYMBOL('@', 6),
	NUM_SIGN('#', 5),
	DOLLAR_SIGN('$', 5),
	PERCENT('%', 5),
	UP_ARROW('^', 5),
	AMPERSAND('&', 5),
	ASTERISK('*', 5),
	LEFT_PARENTHESIS('(', 4),
	RIGHT_PERENTHESIS(')', 4),
	MINUS('-', 5),
	UNDERSCORE('_', 5),
	PLUS_SIGN('+', 5),
	EQUALS_SIGN('=', 5),
	LEFT_CURL_BRACE('{', 4),
	RIGHT_CURL_BRACE('}', 4),
	LEFT_BRACKET('[', 3),
	RIGHT_BRACKET(']', 3),
	COLON(':', 1),
	SEMI_COLON(';', 1),
	DOUBLE_QUOTE('"', 3),
	SINGLE_QUOTE('\'', 1),
	LEFT_ARROW('<', 4),
	RIGHT_ARROW('>', 4),
	QUESTION_MARK('?', 5),
	SLASH('/', 5),
	BACK_SLASH('\\', 5),
	LINE('|', 1),
	TILDE('~', 5),
	TICK('`', 2),
	PERIOD('.', 1),
	COMMA(',', 1),
	SPACE(' ', 4),
	DEFAULT('a', 4);

	private final char character;
	private final int length;

	DefaultFontInfo(final char character, final int length) {
		this.character = character;
		this.length = length;
	}

	public char getCharacter() {
		return this.character;
	}

	public int getLength() {
		return this.length;
	}

	public int getBoldLength() {
		if (this == DefaultFontInfo.SPACE)
			return this.getLength();
		return this.length + 1;
	}

	public static DefaultFontInfo getDefaultFontInfo(final char c) {
		for (final DefaultFontInfo dFI : DefaultFontInfo.values())
			if (dFI.getCharacter() == c)
				return dFI;

		return DefaultFontInfo.DEFAULT;
	}
}
