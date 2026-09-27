package top.brmc.devlib.remain.nbt;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * 此接口只公开获取数据的方法，不会对底层对象
 * 做任何修改。
 *
 * @author tr7zw
 */
public interface ReadableNBT {

	/**
	 * 给定一个键，返回与该键关联的值。
	 *
	 * @param key 要获取其值的键。
	 * @return 该键的值。
	 */
	String getString(String key);

	/**
	 * 给定一个键，以 Integer 形式返回与该键关联的值；若找不到该键
	 * 则返回 0。
	 *
	 * @param key 要查找的键。
	 * @return 该键的值。
	 */
	Integer getInteger(String key);

	/**
	 * 以 double 形式返回与给定键关联的值；找不到则返回 0。
	 *
	 * @param key 要获取的键。
	 * @return double 值
	 */
	Double getDouble(String key);

	/**
	 * 以 byte 形式获取给定键的值；若找不到该键则返回 0。
	 *
	 * @param key 要获取其值的键。
	 * @return byte 值
	 */
	Byte getByte(String key);

	/**
	 * 以 Short 形式返回该键的值；若找不到该键则返回 0。
	 *
	 * @param key 要获取其值的键。
	 * @return short 值
	 */
	Short getShort(String key);

	/**
	 * 以 Long 形式返回与给定键关联的值；若找不到该键
	 * 则返回 0。
	 *
	 * @param key 要获取其值的键。
	 * @return Long 对象
	 */
	Long getLong(String key);

	/**
	 * 以 Float 形式返回给定键的值；若该键不存在
	 * 则返回 0。
	 *
	 * @param key 要获取的键。
	 * @return float 值
	 */
	Float getFloat(String key);

	/**
	 * 以 byte 数组形式返回与给定键关联的值；若找不到该键
	 * 则返回 null。
	 *
	 * @param key 用于获取值的键。
	 * @return byte 数组。
	 */
	byte[] getByteArray(String key);

	/**
	 * 以整数数组形式返回与给定键关联的值；若该键不存在
	 * 则返回 null。
	 *
	 * @param key 要获取其值的键。
	 * @return 整数数组。
	 */
	int[] getIntArray(String key);

	/**
	 * 以 long 数组形式返回与给定键关联的值；若该键不存在
	 * 则返回 null。
	 *
	 * 需要 1.16+
	 *
	 * @param key 要获取其值的键。
	 * @return long 数组。
	 */
	long[] getLongArray(String key);

	/**
	 * 返回与给定键关联的值；若找不到该键
	 * 则返回 false。
	 *
	 * @param key 要获取的键。
	 * @return boolean 值。
	 */
	Boolean getBoolean(String key);

	/**
	 * 返回与给定键关联的 ItemStack；若该键
	 * 不存在则返回 null。
	 *
	 * @param key 要获取的物品堆所对应的键。
	 * @return ItemStack
	 */
	ItemStack getItemStack(String key);

	/**
	 * 获取保存在给定键下的 {@link ItemStack} 数组；若未找到
	 * 存储的数据则返回 null
	 *
	 * @param key 键
	 * @return 存储的 {@link ItemStack} 数组；若未找到存储的数据
	 *         则为 null
	 */
	ItemStack[] getItemStackArray(String key);

	/**
	 * 给定一个键，返回该键对应的 UUID。
	 *
	 * @param key 要从中获取值的键
	 * @return UUID 对象。
	 */
	UUID getUUID(String key);

	/**
	 * 检查所提供的键是否存在
	 *
	 * @param key String 键
	 * @return 若该键已设置则为 true
	 */
	boolean hasTag(String key);

	/**
	 * 检查所提供的键是否存在且具有指定类型
	 *
	 * @param key  String 键
	 * @param type nbt 标签类型
	 * @return 该键是否已设置且具有指定类型
	 */
	default boolean hasTag(String key, NBTType type) {
		return this.hasTag(key) && this.getType(key) == type;
	}

	/**
	 * @return 所有已存储键的集合
	 */
	Set<String> getKeys();

	/**
	 * 检查此 NBT 是否为空（没有存储数据）。
	 *
	 * @return 此 NBT 是否为空
	 */
	default boolean isEmpty() {
		return this.getKeys().isEmpty();
	}

	/**
	 * @param name
	 * @return Compound 实例，或 null
	 */
	ReadableNBT getCompound(String name);

	/**
	 * @param name
	 * @return 获取到的 String 列表
	 */
	ReadableNBTList<String> getStringList(String name);

	/**
	 * @param name
	 * @return 获取到的 Integer 列表
	 */
	ReadableNBTList<Integer> getIntegerList(String name);

	/**
	 * @param name
	 * @return 获取到的 Integer 列表
	 */
	ReadableNBTList<int[]> getIntArrayList(String name);

	/**
	 * @param name
	 * @return 获取到的 Integer 列表
	 */
	ReadableNBTList<UUID> getUUIDList(String name);

	/**
	 * @param name
	 * @return 获取到的 Float 列表
	 */
	ReadableNBTList<Float> getFloatList(String name);

	/**
	 * @param name
	 * @return 获取到的 Double 列表
	 */
	ReadableNBTList<Double> getDoubleList(String name);

	/**
	 * @param name
	 * @return 获取到的 Long 列表
	 */
	ReadableNBTList<Long> getLongList(String name);

	/**
	 * 返回列表的类型，若不是列表则为 null
	 *
	 * @param name
	 * @return
	 */
	NBTType getListType(String name);

	/**
	 * @param name
	 * @return 获取到的 Compound 列表
	 */
	ReadableNBTList<ReadWriteNBT> getCompoundList(String name);

	/**
	 * 若存在则返回存储的值，否则返回所提供的值。
	 * <p>
	 * 支持的类型：
	 * {@code Boolean, Byte, Short, Integer, Long, Float, Double, byte[], int[], long[]}、
	 * {@link String}、{@link UUID} 和 {@link Enum}
	 *
	 * @param key          键
	 * @param defaultValue 非 null 的默认值
	 * @param <T>          值类型
	 * @return 存储的值或所提供的值
	 */
	<T> T getOrDefault(String key, T defaultValue);

	/**
	 * 若存在则返回存储的值，否则返回 null。
	 * <p>
	 * 支持的类型：
	 * {@code Boolean, Byte, Short, Integer, Long, Float, Double, byte[], int[], long[]}、
	 * {@link String}、{@link UUID} 和 {@link Enum}
	 *
	 * @param key  键
	 * @param type 数据类型
	 * @param <T>  值类型
	 * @return 存储的值或所提供的值
	 */
	<T> T getOrNull(String key, Class<?> type);

	/**
	 * 若存在则返回解析得到的值，否则返回 null。
	 * <p>
	 * 支持的类型：
	 * {@code Boolean, Byte, Short, Integer, Long, Float, Double, byte[], int[], long[]}、
	 * {@link String}、{@link UUID} 和 {@link Enum}
	 *
	 * @param key  路径键，以 '.' 分隔。例如："foo.bar.baz"。点号可以
	 *             用反斜杠转义。
	 * @param type 数据类型
	 * @param <T>  值类型
	 * @return 解析得到的值或所提供的值
	 */
	<T> T resolveOrNull(String key, Class<?> type);

	/**
	 * 若存在则返回解析得到的值，否则返回所提供的值。
	 * <p>
	 * 支持的类型：
	 * {@code Boolean, Byte, Short, Integer, Long, Float, Double, byte[], int[], long[]}、
	 * {@link String}、{@link UUID} 和 {@link Enum}
	 *
	 * @param key          路径键，以 '.' 分隔。例如："foo.bar.baz"。
	 *                     点号可以用反斜杠转义。
	 * @param defaultValue 非 null 的默认值
	 * @param <T>          值类型
	 * @return 解析得到的值或所提供的值
	 */
	<T> T resolveOrDefault(String key, T defaultValue);

	/**
	 * 若存在则返回解析得到的 Compound，否则返回 null。
	 * <p>
	 *
	 * @param key 路径键，以 '.' 分隔。例如："foo.bar.baz"。点号可以
	 *            用反斜杠转义。
	 * @return 若存在则为解析得到的值，否则为 null。
	 */
	ReadableNBT resolveCompound(String key);

	/**
	 * 通过处理器获取指定键上的对象。
	 *
	 * @param <T>
	 * @param key
	 * @param handler
	 * @return
	 */
	<T> T get(String key, NBTHandler<T> handler);

	/**
	 * 获取通过 setEnum 或 setString(key,
	 * value.name()) 设置的 Enum 值。传入 null/无效的键会返回 null。
	 *
	 * @param <E>
	 * @param key
	 * @param type
	 * @return
	 */
	<E extends Enum<E>> E getEnum(String key, Class<E> type);

	/**
	 * @param name
	 * @return 给定存储键的类型，或 null
	 */
	NBTType getType(String name);

	/**
	 * 将此 Compound 的内容写入所提供的流。
	 *
	 * @param stream
	 */
	void writeCompound(OutputStream stream);

	/**
	 * 将此可读 nbt 与另一个进行比较，
	 * 并返回差异。
	 * <p>注意：结果只包含存在于
	 * 此可读 nbt 中、而在所提供的 nbt 中缺失/不同的数据。
	 *
	 * @param other 另一个可读 nbt
	 * @return 两个可读 nbt 之间的差异
	 */
	ReadWriteNBT extractDifference(ReadableNBT other);

	/**
	 * @return 可打印的 NBT-Json 形式的 NBT。
	 */
	@Override
	String toString();

}