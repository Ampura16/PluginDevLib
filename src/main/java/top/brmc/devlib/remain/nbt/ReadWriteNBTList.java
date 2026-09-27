package top.brmc.devlib.remain.nbt;

import java.util.Collection;
import java.util.ListIterator;
import java.util.function.Predicate;

public interface ReadWriteNBTList<T> extends ReadableNBTList<T> {

	/**
	 * 如果指定元素尚不存在，则将其添加到此集合中。
	 *
	 * @param element 要添加到列表的元素。
	 * @return 布尔值。
	 */
	boolean add(T element);

	/**
	 * 在此列表的指定位置添加指定元素。
	 *
	 * @param index   要插入指定元素的索引
	 * @param element 要添加到列表的元素。
	 */
	void add(int index, T element);

	/**
	 * 用指定元素替换此列表中指定位置的
	 * 元素。
	 *
	 * @param index   要替换的元素的索引
	 * @param element 要存放到指定位置的元素
	 * @return 之前位于指定位置的元素。
	 */
	T set(int index, T element);

	/**
	 * 移除索引 i 处的元素并返回它。
	 *
	 * @param i 要移除的元素的索引
	 * @return 被移除的元素。
	 */
	T remove(int i);

	/**
	 * 清空列表内容
	 */
	void clear();

	/**
	 * 将指定集合中的所有元素添加到此集合中。
	 *
	 * @param c 要添加到列表的集合。
	 * @return 布尔值。
	 */
	boolean addAll(Collection<? extends T> c);

	/**
	 * 从指定位置开始，将指定集合中的所有元素
	 * 插入到此列表中。
	 *
	 * @param index 插入指定集合元素的
	 *              索引。
	 * @param c     要添加到列表的集合。
	 * @return 布尔值。
	 */
	boolean addAll(int index, Collection<? extends T> c);

	/**
	 * 从此集合中移除所有包含在
	 * 指定集合中的元素
	 *
	 * @param c 要从此列表中移除的集合。
	 * @return 布尔值。
	 */
	boolean removeAll(Collection<?> c);

	/**
	 * 移除此集合中所有不包含在
	 * 指定集合中的元素。
	 *
	 * @param c 要在此列表中保留的集合
	 * @return 布尔值。
	 */
	boolean retainAll(Collection<?> c);

	/**
	 * “移除列表中所有满足给定谓词的元素。”
	 *
	 * Predicate 接口是一个函数式接口，接收单个
	 * 参数并返回布尔值
	 *
	 * @param pred 应用于每个元素、用于判断是否应将其
	 *             移除的谓词。
	 * @return 布尔值。
	 */
	boolean removeIf(Predicate<? super T> pred);

	/**
	 * 如果存在，则从此列表中移除指定元素的
	 * 第一次出现。
	 *
	 * @param o 要从列表中移除的对象。
	 * @return 布尔值。
	 */
	boolean remove(Object o);

	/**
	 * 返回此列表元素（按正确顺序）的列表迭代器，
	 * 从列表中的指定位置开始
	 *
	 * @param startIndex 列表迭代器返回的第一个元素的索引
	 *                   （通过调用 next 方法）。
	 * @return 从指定索引开始的列表迭代器。
	 */
	ListIterator<T> listIterator(int startIndex);

}