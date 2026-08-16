package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictSet;

import lombok.Getter;

/**
 * 一个用于判断某项是否在列表中的简单类。
 * <p>
 * 示例：列表包含 "apple"、"red"、"car"，
 * 调用 isInList("car") 会得到 true。其他数据类型同理
 * <p>
 * 如果使用 new IsInList("*") 或空列表创建，则所有内容都会匹配
 *
 * @param <T>
 */
public final class IsInList<T> implements Iterable<T> {

	/**
	 * 用于匹配的内部集合
	 */
	@Getter
	private final StrictSet<T> list;

	/**
	 * 是否匹配所有内容？
	 */
	private final boolean matchAll;

	/**
	 * 创建一个新的 IsInList
	 *
	 * @param list
	 * @param matchAll
	 */
	private IsInList(final Iterable<T> list, boolean matchAll) {
		this.list = new StrictSet<>(list);
		this.matchAll = matchAll;
	}

	/**
	 * 若给定值在此列表中则返回 true
	 *
	 * @param toEvaluateAgainst
	 * @return
	 */
	public boolean contains(final T toEvaluateAgainst) {

		// Return false when list is empty and we are not always true
		if (!this.matchAll && this.list.isEmpty())
			return false;

		return this.matchAll || this.list.contains(toEvaluateAgainst);
	}

	/**
	 * 判断列表中是否有任何条目以该项开头。
	 * 调用 toString() 进行不区分大小写的比较。
	 *
	 * @param toEvaluateAgainst
	 * @return
	 */
	public boolean startsWith(final T toEvaluateAgainst) {

		// Return false when list is empty and we are not always true
		if (!this.matchAll && this.list.isEmpty())
			return false;

		if (this.matchAll)
			return true;

		final String evaluatedString = toEvaluateAgainst.toString().toLowerCase();

		for (final T item : this.list) {
			final String itemString = item.toString().toLowerCase();

			if (evaluatedString.startsWith(itemString))
				return true;
		}

		return false;
	}

	/**
	 * 判断列表中是否有任何条目匹配正则。
	 * 调用 toString() 进行不区分大小写的比较。
	 *
	 * @param toEvaluateAgainst
	 * @return
	 */
	public boolean regexMatch(final T toEvaluateAgainst) {

		// Return false when list is empty and we are not always true
		if (!this.matchAll && this.list.isEmpty())
			return false;

		if (this.matchAll)
			return true;

		final String evaluatedString = toEvaluateAgainst.toString().toLowerCase();

		for (final T item : this.list) {
			final String itemString = item.toString().toLowerCase();

			if (Pattern.compile(itemString).matcher(evaluatedString).find())
				return true;
		}

		return false;
	}

	/**
	 * 若列表等于 ["*"] 则返回 true
	 *
	 * @return
	 */
	public boolean isEntireList() {
		return this.matchAll;
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return this.list.iterator();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IsInList[entire=" + this.matchAll + ", list=" + Common.join(this.list) + "]";
	}

	/**
	 * 根据给定列表创建一个新的匹配列表
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> IsInList<T> fromList(Iterable<T> list) {
		boolean matchAll = false;

		for (final T t : list)
			if ("*".equals(t)) {
				matchAll = true;

				break;
			}

		return new IsInList<>(list, matchAll);
	}

	/**
	 * 创建一个始终返回 true 的匹配列表
	 *
	 * @param <T>
	 * @return
	 */
	public static <T> IsInList<T> fromStar() {
		return new IsInList<>(new ArrayList<T>(), true);
	}

}
