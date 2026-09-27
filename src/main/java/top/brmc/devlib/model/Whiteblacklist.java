package top.brmc.devlib.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import top.brmc.devlib.Common;
import top.brmc.devlib.Valid;

import lombok.Getter;
import lombok.NonNull;

/**
 * 表示一种根据列表检查白名单或黑名单的简单方式，
 * 参见 {@link #Whiteblacklist(List)}
 */
public final class Whiteblacklist {

	/**
	 * 条目列表
	 */
	@Getter
	private final Set<String> items;

	/**
	 * 预编译为模式格式的条目列表
	 */
	private final Set<Pattern> patterns;

	/**
	 * 模式是否已编译？
	 */
	private final boolean compileAsPatterns;

	/**
	 * 用于将条目与某一项进行匹配
	 *
	 * true = 除外
	 * false = 仅限
	 */
	@Getter
	private final boolean whitelist;

	/**
	 * 当列表设置为 ["*"] 时的特殊标记，此时对所有内容
	 * 始终返回 true
	 */
	@Getter
	private final boolean entireList;

	/**
	 * 根据给定列表创建一个新的白名单/黑名单
	 *
	 * 如果第一行等于 '@blacklist'，匹配方式将为
	 * 黑名单（仅限规则），否则为白名单（除外规则）
	 *
	 * @param items
	 */
	public Whiteblacklist(@NonNull List<String> items) {
		this(items, false);
	}

	/**
	 * 根据给定列表创建一个新的白名单/黑名单
	 *
	 * 如果第一行等于 '@blacklist'，匹配方式将为
	 * 黑名单（仅限规则），否则为白名单（除外规则）
	 *
	 * @param items
	 * @param compileAsPatterns 是否预编译列表以获得最佳性能？
	 */
	public Whiteblacklist(@NonNull List<String> items, boolean compileAsPatterns) {
		this.patterns = new HashSet<>();
		this.compileAsPatterns = compileAsPatterns;

		if (!items.isEmpty()) {
			final String firstLine = items.get(0);
			final String secondLine = items.size() > 1 ? items.get(1) : "";

			boolean entireList = false;
			boolean whitelist = true;

			if ("*".equals(firstLine) || "*".equals(secondLine))
				entireList = true;

			if ("@blacklist".equals(firstLine) || "@blacklist".equals(secondLine))
				whitelist = false;

			final List<String> copyList = new ArrayList<>();

			for (final String item : items)
				if (!"*".equals(item) && !"@blacklist".equals(item))
					copyList.add(item);

			this.items = new HashSet<>(copyList);
			this.whitelist = whitelist;
			this.entireList = entireList;
			this.patterns.clear();

			if (compileAsPatterns)
				for (String item : this.items)
					this.patterns.add(Common.compilePattern(item));
		}

		else {
			this.items = new HashSet<>();
			this.whitelist = true;
			this.entireList = false;
		}
	}

	/**
	 * 判断给定集合是否至少包含一个匹配项
	 *
	 * @param items
	 * @return
	 */
	public boolean isInList(Collection<String> items) {
		if (this.entireList)
			if (this.whitelist && !items.isEmpty())
				return true;

			else if (!this.whitelist && items.isEmpty())
				return true;

		for (final String item : items)
			if (this.isInList(item))
				return true;

		return false;
	}

	/**
	 * 若 {@link Valid#isInList(String, Iterable)} 返回 true 则返回 true，
	 * 并根据 {@link #isWhitelist()} 标记取反
	 *
	 * @param item
	 * @return
	 */
	public boolean isInList(String item) {
		if (this.entireList)
			return this.whitelist;

		final boolean match = Valid.isInList(item, this.items);

		return this.whitelist ? match : !match;
	}

	/**
	 * 若 {@link Valid#isInListRegex(String, Iterable)} 返回 true 则返回 true，
	 * 并根据 {@link #isWhitelist()} 标记取反
	 *
	 * @param item
	 * @return
	 */
	public boolean isInListRegex(String item) {
		if (this.entireList)
			return this.whitelist;

		final boolean match = this.compileAsPatterns ? Valid.isInListRegexFast(item, this.patterns) : Valid.isInListRegex(item, this.items);

		return this.whitelist ? match : !match;
	}

	/**
	 * 若 {@link Valid#isInListStartsWith(String, Iterable)} 返回 true 则返回 true，
	 * 并根据 {@link #isWhitelist()} 标记取反
	 *
	 * @param item
	 * @return
	 */
	public boolean isInListStartsWith(String item) {
		if (this.entireList)
			return this.whitelist;

		final boolean match = Valid.isInListStartsWith(item, this.items);

		return this.whitelist ? match : !match;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "{" + (this.entireList ? "entire list" : this.whitelist ? "whitelist" : "blacklist") + " " + this.items + "}";
	}
}
