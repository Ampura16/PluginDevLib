package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * 严格列表，只允许移除包含的元素，或添加不包含的元素。
 * <p>
 * 否则会出错，可带可选错误消息。
 * @param <E>
 */
public final class StrictList<E> extends StrictCollection implements Iterable<E> {

	/**
	 * 内部列表
	 */
	private final List<E> list = new ArrayList<>();

	/**
	 * 用给定元素创建新列表
	 *
	 * @param elements
	 */
	@SafeVarargs
	public StrictList(E... elements) {
		this();

		this.addAll(Arrays.asList(elements));
	}

	/**
	 * 用给定元素创建新列表
	 *
	 * @param oldList
	 */
	public StrictList(Iterable<E> oldList) {
		this();

		this.addAll(oldList);
	}

	/**
	 * 创建新的空列表
	 */
	public StrictList() {
		super("Cannot remove '%s' as it is not in the list!", "Value '%s' is already in the list!");
	}

	/**
	 * 返回默认的 Java {@link ArrayList}
	 *
	 * @return
	 */
	public List<E> getSource() {
		return this.list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回给定下标的值并立即移除它
	 *
	 * @param index
	 * @return
	 */
	public E getAndRemove(int index) {
		final E e = this.list.get(index);
		this.remove(index);

		return e;
	}

	/**
	 * 移除给定键
	 *
	 * @param key
	 */
	public void remove(E key) {
		final boolean removed = this.removeWeak(key);

		Valid.checkBoolean(removed, String.format(this.getCannotRemoveMessage(), key));
	}

	/**
	 * 移除给定下标处的键
	 *
	 * @param index
	 * @return
	 */
	public E remove(int index) {
		final E removed = this.list.remove(index);

		Valid.checkNotNull(removed, String.format(this.getCannotRemoveMessage(), "index: " + index));
		return removed;
	}

	/**
	 * 添加给定元素
	 *
	 * @param elements
	 */
	public void addAll(Iterable<E> elements) {
		for (final E key : elements)
			this.add(key);
	}

	/**
	 * 若元素不存在则添加它
	 *
	 * @param key
	 */
	public void addIfNotExist(E key) {
		if (!this.contains(key))
			this.add(key);
	}

	/**
	 * 向列表添加元素
	 *
	 * @param key
	 */
	public void add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!this.list.contains(key), String.format(this.getCannotAddMessage(), key));

		this.addWeak(key);
	}

	/**
	 * 从起始下标创建列表副本
	 *
	 * @param startIndex
	 * @return
	 */
	public StrictList<E> range(int startIndex) {
		Valid.checkBoolean(startIndex <= this.list.size(), "Start index out of range " + startIndex + " vs. list size " + this.list.size());
		final StrictList<E> ranged = new StrictList<>();

		for (int i = startIndex; i < this.list.size(); i++)
			ranged.add(this.list.get(i));

		return ranged;
	}

	/**
	 * 返回第一个值，若列表为空则为 null
	 *
	 * @return
	 */
	@Nullable
	public E first() {
		return this.list.isEmpty() ? null : this.list.get(0);
	}

	/**
	 * 返回最后一个值，若列表为空则为 null
	 *
	 * @return
	 */
	@Nullable
	public E last() {
		return this.list.isEmpty() ? null : this.list.get(this.list.size() - 1);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 移除给定键，若不存在也不抛错
	 *
	 * @param value
	 * @return
	 */
	public boolean removeWeak(E value) {
		Valid.checkNotNull(value, "Cannot remove null values");

		return this.list.remove(value);
	}

	/**
	 * 添加所有键，即使它们已存在
	 *
	 * @param keys
	 */
	public void addWeakAll(Iterable<E> keys) {
		for (final E key : keys)
			this.addWeak(key);
	}

	/**
	 * 将给定键添加到列表末尾，不管它是否已存在
	 *
	 * @param key
	 */
	public void addWeak(E key) {
		this.list.add(key);
	}

	/**
	 * 将键设置到指定下标
	 *
	 * @param index
	 * @param key
	 */
	public void set(int index, E key) {
		this.list.set(index, key);
	}

	/**
	 * 返回值或默认值
	 *
	 * @param index
	 * @param def
	 * @return
	 */
	public E getOrDefault(int index, E def) {
		return index < this.list.size() ? this.list.get(index) : def;
	}

	/**
	 * 返回给定下标的值
	 *
	 * @param index
	 * @return
	 */
	public E get(int index) {
		return this.list.get(index);
	}

	/**
	 * 若列表包含该键则返回 true
	 *
	 * 若键是字符串，包含（忽略大小写）则返回 true
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(E key) {
		for (final E other : this.list) {
			if (other instanceof String && key instanceof String)
				if (((String) other).equalsIgnoreCase((String) key))
					return true;

			if (other.equals(key))
				return true;
		}

		return false;
	}

	/**
	 * 返回此列表指定 fromIndex（含）到
	 * toIndex（不含）之间部分的视图。（若 fromIndex 与 toIndex 相等，
	 * 返回的列表为空。）返回的列表由本列表支持，
	 * 因此返回列表中的非结构性修改会反映到本列表，
	 * 反之亦然。返回的列表支持本列表支持的所有可选
	 * 列表操作。
	 *
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	public List<E> subList(int fromIndex, int toIndex) {
		return this.list.subList(fromIndex, toIndex);
	}

	/**
	 * 移除该列表的每一个元素！
	 */
	public void clear() {
		this.list.clear();
	}

	/**
	 * 若列表为空则返回 true
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	/**
	 * 获取列表大小
	 *
	 * @return
	 */
	public int size() {
		return this.list.size();
	}

	/**
	 * 返回用给定分隔符连接的所有列表值
	 *
	 * @param separator
	 * @return
	 */
	public String join(String separator) {
		return Common.join(this.list, separator);
	}

	/**
	 * 见 {@link List#toArray()}
	 *
	 * @param e
	 * @return
	 */
	public E[] toArray(E[] e) {
		return this.list.toArray(e);
	}

	/**
	 * 返回 {@link List#toArray()}
	 *
	 * @return
	 */
	public Object[] toArray() {
		return this.list.toArray();
	}

	/**
	 * 见 {@link List#iterator()}
	 */
	@Override
	public Iterator<E> iterator() {
		return this.list.iterator();
	}

	/**
	 * 序列化列表中的每个值，以便存入 YAML 设置
	 */
	@Override
	public Object serialize() {
		return SerializeUtil.serialize(this.getMode(), this.list);
	}

	/**
	 * 返回此列表的字符串表示
	 * <p>
	 * 注意：存入文件请调用 {@link #serialize()}
	 */
	@Override
	public String toString() {
		return this.list.toString();
	}
}