package top.brmc.devlib.remain.nbt;

import java.util.function.Predicate;

public interface ReadWriteNBTCompoundList extends ReadableNBTList<ReadWriteNBT> {

	/**
	 * 向当前复合标签中添加一个新的复合标签
	 *
	 * @return 该类的一个新实例。
	 */
	ReadWriteNBT addCompound();

	/**
	 * 将该复合标签的副本添加到列表末尾并返回它。当传入 null 时，
	 * 会创建一个新的复合标签
	 *
	 * @param comp
	 * @return
	 */
	ReadWriteNBT addCompound(ReadableNBT comp);

	/**
	 * 移除此列表中指定位置的元素
	 *
	 * @param i 要移除的元素的索引。
	 * @return 该类的一个新实例。
	 */
	ReadWriteNBT remove(int i);

	/**
	 * 清空列表内容
	 */
	void clear();

	/**
	 * 移除此列表中所有满足给定条件的元素
	 *
	 * @param pred 用于测试元素的谓词。
	 * @return 布尔值。
	 */
	boolean removeIf(Predicate<? super ReadWriteNBT> pred);

}