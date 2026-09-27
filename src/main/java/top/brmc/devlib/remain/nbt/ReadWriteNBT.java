package top.brmc.devlib.remain.nbt;

import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public interface ReadWriteNBT extends ReadableNBT {

	/**
	 * 将 comp 中的所有数据合并到此 compound。此操作一次完成，因此
	 * 也适用于方块实体/实体
	 *
	 * @param comp
	 */
	void mergeCompound(ReadableNBT comp);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setString(String key, String value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setInteger(String key, Integer value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setDouble(String key, Double value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setByte(String key, Byte value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setShort(String key, Short value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setLong(String key, Long value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setFloat(String key, Float value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setByteArray(String key, byte[] value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setIntArray(String key, int[] value);

	/**
	 * 设置器
	 *
	 * 需要 1.16+
	 *
	 * @param key
	 * @param value
	 */
	void setLongArray(String key, long[] value);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setBoolean(String key, Boolean value);

	/**
	 * 将 ItemStack 以 compound 形式保存到给定键下
	 *
	 * @param key
	 * @param item
	 */
	void setItemStack(String key, ItemStack item);

	/**
	 * 将 ItemStack 数组以 compound 形式保存到给定键下
	 *
	 * @param key
	 * @param items
	 */
	void setItemStackArray(String key, ItemStack[] items);

	/**
	 * 设置器
	 *
	 * @param key
	 * @param value
	 */
	void setUUID(String key, UUID value);

	/**
	 * @param key 删除给定的键
	 */
	void removeKey(String key);

	/**
	 * 与 addCompound 相同，只是名称更能体现其作用
	 *
	 * @param name
	 * @return
	 */
	ReadWriteNBT getOrCreateCompound(String name);

	/**
	 * @param name
	 * @return Compound 实例，或 null
	 */
	@Override
	ReadWriteNBT getCompound(String name);

	/**
	 * 返回解析后的 compound，并按需创建 compound。
	 * <p>
	 *
	 * @param key 路径键，以 '.' 分隔。例如："foo.bar.baz"。点号可以
	 *            用反斜杠转义。
	 * @return 解析后的 compound。
	 */
	ReadWriteNBT resolveOrCreateCompound(String key);

	/**
	 * 通过所提供的处理器将对象设置到键上。
	 *
	 * @param <T>
	 * @param key
	 * @param value
	 * @param handler
	 */
	<T> void set(String key, T value, NBTHandler<T> handler);

	/**
	 * 将键设置为给定的 Enum 值，以 String 形式存储。传入 null
	 * 作为值时会改为调用 removeKey(key)。
	 *
	 * @param <E>
	 * @param key
	 * @param value
	 */
	<E extends Enum<?>> void setEnum(String key, E value);

	@Override
	ReadWriteNBTList<String> getStringList(String name);

	@Override
	ReadWriteNBTList<Integer> getIntegerList(String name);

	@Override
	ReadWriteNBTList<int[]> getIntArrayList(String name);

	@Override
	ReadWriteNBTList<UUID> getUUIDList(String name);

	@Override
	ReadWriteNBTList<Float> getFloatList(String name);

	@Override
	ReadWriteNBTList<Double> getDoubleList(String name);

	@Override
	ReadWriteNBTList<Long> getLongList(String name);

	@Override
	ReadWriteNBTCompoundList getCompoundList(String name);

	@Override
	ReadWriteNBT resolveCompound(String key);

	/**
	 * 移除此 compound 中的所有键
	 */
	void clearNBT();

}