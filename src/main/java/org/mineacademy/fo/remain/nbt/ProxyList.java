package org.mineacademy.fo.remain.nbt;

public interface ProxyList<T extends NBTProxy> extends Iterable<T> {

	/**
	 * 向当前复合标签中添加一个新的复合标签
	 *
	 * @return 该类的一个新实例。
	 */
	T addCompound();

	/**
	 * 返回此集合中的元素数量。如果此集合
	 * 包含的元素多于 Integer.MAX_VALUE 个，则返回 Integer.MAX_VALUE。
	 *
	 * @return 此集合中的元素数量
	 */
	int size();

	/**
	 * 若此集合不包含任何元素则返回 true。
	 *
	 * @return 若此集合不包含任何元素则返回 true
	 */
	boolean isEmpty();

	/**
	 * 获取具有给定 id 的对象。
	 *
	 * @param id 要获取的对象的 id。
	 * @return 具有给定 id 的对象。
	 */
	T get(int id);

	/**
	 * 移除此列表中指定位置的元素
	 *
	 * @param i 要移除的元素的索引。
	 */
	void remove(int i);

}
