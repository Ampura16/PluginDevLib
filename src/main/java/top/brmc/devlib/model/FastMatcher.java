package top.brmc.devlib.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 一个仿照正则表达式、功能极其有限的简单匹配器，为了
 * 最大化性能和效率而有意精简。
 *
 * 它以四种不同模式匹配条目：
 * 1. 模式以 * 开头时，判断消息是否以该内容开头；
 * 2. 模式以 * 结尾时，判断消息结尾；
 * 3. 以 " 开头并以 " 结尾时，判断是否相等；
 * 4. 其他情况下，判断消息是否包含该模式
 *
 * 可以使用 | 分隔多个匹配项，例如 DIAMOND_*|GOLDEN_* 等
 * 可以使用 '*' 匹配所有内容
 *
 * 如果你仍然想要/需要使用正则，可以在消息前加上 "* " 前缀，然后按
 * 正常方式匹配，例如 "* ^DIAMOND_(SWORD|HOE)"
 *
 * 示例：DIAMOND_* 会匹配所有 DIAMOND_HOE、DIAMOND_SPADE 等，但不会匹配 SUPERDIAMOND_SPADE
 *
 * 设计原因：Protect 插件会将背包中的每个槽位（27 + 盔甲）与所有规则进行比对，
 * 而使用复杂的正则类会严重拖慢性能。
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FastMatcher implements ConfigSerializable {

	/**
	 * 完整的正则类
	 */
	private final Pattern pattern;

	/**
	 * 原始模式
	 */
	private final String rawPattern;

	/**
	 * 匹配器列表，模式为 * 时为 null
	 */
	@Nullable
	private final Matcher[] matchers;

	/**
	 * 返回此匹配器是否匹配给定消息，
	 * 区分大小写
	 *
	 * @param message
	 * @return
	 */
	public boolean find(String message) {

		// Indicates we match everything
		if (this.matchers == null)
			return true;

		if (message.isEmpty())
			return false;

		// Indicate regex is used
		if (this.pattern != null)
			return this.pattern.matcher(message).find();

		// Use our matching
		for (final Matcher matcher : this.matchers) {
			Valid.checkNotEmpty(matcher.getPattern(), "Matcher pattern cannot be empty! Use * instead to match everything in " + this);

			if (matcher.find(message))
				return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return "FastMatcher{pattern=" + rawPattern + "}";
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray("Pattern", this.rawPattern);
	}

	/**
	 * 反序列化该匹配器
	 *
	 * @param map
	 * @return
	 */
	public static FastMatcher deserialize(SerializedMap map) {
		return compile(map.getString("Pattern"));
	}

	/**
	 * 根据给定模式编译匹配器
	 *
	 * @param pattern
	 * @return
	 */
	public static FastMatcher compile(String pattern) {

		if ("*".equals(pattern))
			return new FastMatcher(null, pattern, null);

		else if (pattern.startsWith("* "))
			return new FastMatcher(Pattern.compile(pattern.substring(2)), pattern, null);

		final List<Matcher> matchers = new ArrayList<>();

		for (final String part : pattern.split("\\|"))
			matchers.add(Matcher.compile(part));

		return new FastMatcher(null, pattern, matchers.toArray(new Matcher[matchers.size()]));
	}

	/**
	 * 根据给定列表编译匹配器列表
	 *
	 * @param list
	 * @return
	 */
	public static List<FastMatcher> compileFromList(List<String> list) {
		final List<FastMatcher> matchers = new ArrayList<>();

		for (final String pattern : list)
			matchers.add(compile(pattern));

		return matchers;
	}
}

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class Matcher {

	/**
	 * 按模式调整后的匹配内容，见上文
	 */
	private final String pattern;

	/**
	 * 匹配模式
	 */
	private final int mode;

	/**
	 * 若该模式匹配给定消息则返回 true
	 * @param message
	 * @return
	 */
	public boolean find(String message) {
		if (this.mode == 1)
			return message.startsWith(this.pattern);

		else if (this.mode == 2)
			return message.endsWith(this.pattern);

		else if (this.mode == 3)
			return message.equals(this.pattern);

		else
			return message.contains(this.pattern);

	}

	/**
	 * 根据给定模式编译匹配器，区分大小写。
	 *
	 * @param pattern
	 * @return
	 */
	public static Matcher compile(String pattern) {
		int mode = 4;

		if (pattern.startsWith("*")) {
			mode = 1;
			pattern = pattern.substring(1);

		} else if (pattern.endsWith("*")) {
			mode = 2;
			pattern = pattern.substring(0, pattern.length() - 1);

		} else if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
			mode = 3;
			pattern = pattern.substring(1, pattern.length() - 1);
		}

		return new Matcher(pattern, mode);
	}
}