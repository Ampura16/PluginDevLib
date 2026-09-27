package top.brmc.devlib.model;

import top.brmc.devlib.SerializeUtil;
import top.brmc.devlib.SerializeUtil.Mode;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.exception.FoException;

import lombok.Data;

/**
 * 用于键值对的简单元组
 * @param <K>
 * @param <V>
 */
@Data
public final class Tuple<K, V> implements ConfigSerializable {

	/**
	 * 键
	 */
	private final K key;

	/**
	 * 值
	 */
	private final V value;

	/**
	 * @see top.brmc.devlib.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray("Key", this.key, "Value", this.value);
	}

	/**
	 * 以 X - Y 语法返回此元组
	 *
	 * @return
	 */
	public String toLine() {
		return this.key + " - " + this.value;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.toLine();
	}

	/**
	 * 将给定的配置节转换为元组
	 *
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public static <K, V> Tuple<K, V> deserialize(SerializedMap map, Class<K> keyType, Class<V> valueType) {

		final K key = map.containsKey("Key") ? map.get("Key", keyType) : null;
		final V value = map.containsKey("Value") ? map.get("Value", valueType) : null;

		return new Tuple<>(key, value);
	}

	/**
	 * 将给定行（必须为 KEY - VALUE 语法）反序列化为给定元组，
	 * 适用于 YAML 存储而非 JSON。
	 *
	 * @param <K>
	 * @param <V>
	 * @param line
	 * @param keyType
	 * @param valueType
	 * @return 元组；line 为 null 时返回 null
	 */
	public static <K, V> Tuple<K, V> deserialize(String line, Class<K> keyType, Class<V> valueType) {
		if (line == null)
			return null;

		final String split[] = line.split(" - ");
		Valid.checkBoolean(split.length == 2, "Line must have the syntax <" + keyType.getSimpleName() + "> - <" + valueType.getSimpleName() + "> but got: " + line);

		final K key = SerializeUtil.deserialize(Mode.YAML, keyType, split[0]);
		final V value = SerializeUtil.deserialize(Mode.YAML, valueType, split[1]);

		return new Tuple<>(key, value);
	}

	/**
	 * 请勿使用
	 *
	 * @param <K>
	 * @param <V>
	 * @param map
	 *
	 * @deprecated 请勿使用
	 * @return
	 */
	@Deprecated
	public static <K, V> Tuple<K, V> deserialize(SerializedMap map) {
		throw new FoException("Tuple cannot be deserialized automatically, call Tuple#deserialize(map, keyType, valueType)");
	}
}
