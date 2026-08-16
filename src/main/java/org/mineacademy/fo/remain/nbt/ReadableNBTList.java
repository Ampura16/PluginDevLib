package org.mineacademy.fo.remain.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ReadableNBTList<T> extends Iterable<T> {

	/**
	 * 获取具有给定 id 的对象。
	 *
	 * @param id 要获取的对象的 id。
	 * @return 具有给定 id 的对象。
	 */
	T get(int id);

	/**
	 * 返回此列表中的元素数量。
	 *
	 * @return 列表的大小。
	 */
	int size();

	/**
	 * 返回此标签的类型
	 *
	 * @return 标签的类型。
	 */
	NBTType getType();

	/**
	 * 若列表为空则返回 true，否则返回 false。
	 *
	 * @return 布尔值。
	 */
	boolean isEmpty();

	/**
	 * 若此列表包含指定元素则返回 true。
	 *
	 * @param o 要在列表中查找的对象。
	 * @return 布尔值。
	 */
	boolean contains(Object o);

	/**
	 * 返回指定元素在此列表中第一次出现的索引；
	 * 若此列表不包含该元素则返回 -1。
	 *
	 * @param o 要查找的对象。
	 * @return 指定元素在此列表中第一次出现的索引；
	 *         若此列表不包含该元素则返回 -1。
	 */
	int indexOf(Object o);

	/**
	 * 若此集合包含指定集合中的所有元素
	 * 则返回 true
	 *
	 * @param c 要检查是否全部包含在此列表中的集合
	 * @return 布尔值。
	 */
	boolean containsAll(Collection<?> c);

	/**
	 * 返回指定元素在此列表中最后一次出现的索引；
	 * 若此列表不包含该元素则返回 -1
	 *
	 * @param o 要查找的对象。
	 * @return 指定元素在此列表中最后一次出现的索引；
	 *         若此列表不包含该元素则返回 -1。
	 */
	int lastIndexOf(Object o);

	/**
	 * 返回按正确顺序（从第一个到最后一个元素）
	 * 包含此列表所有元素的数组。
	 *
	 * @return 对象数组。
	 */
	Object[] toArray();

	/**
	 * 返回按正确顺序（从第一个到最后一个元素）
	 * 包含此列表所有元素的数组。
	 *
	 * @param a 用于存放列表元素的数组（如果它足够大）；
	 *          否则会为此分配一个具有相同运行时类型的
	 *          新数组。
	 * @return 包含列表元素的数组。
	 */
	<E> E[] toArray(E[] a);

	/**
	 * 返回此列表中从 fromIndex（含）到
	 * toIndex（不含）之间部分的视图
	 *
	 * @param fromIndex 子列表的起始索引（含）。
	 * @param toIndex   子列表中最后一个元素的索引。
	 * @return 此列表中指定范围的视图。
	 */
	List<T> subList(int fromIndex, int toIndex);

	/**
	 * 创建一个包含此列表所有条目的新列表。
	 *
	 * @return 流中元素组成的列表。
	 */
	default List<T> toListCopy() {
		final List<T> list = new ArrayList<>();
		this.iterator().forEachRemaining(list::add);
		return list;
	}

}