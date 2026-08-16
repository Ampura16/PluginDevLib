package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;

/**
 * 严格映射，只允许移除包含的元素，或添加不包含的元素。
 * <p>
 * 否则会出错，可带可选错误消息。
 * @param <K>
 * @param <V>
 */
public final class StrictMap<K, V> extends StrictCollection {

	/**
	 * 保存值-键对的内部映射
	 */
	private final Map<K, V> map = new LinkedHashMap<>();

	/**
	 * 创建新的严格映射
	 */
	public StrictMap() {
		super("Cannot remove '%s' as it is not in the map!", "Key '%s' is already in the map --> '%s'");
	}

	/**
	 * 用自定义的已存在/不存在错误消息创建新的严格映射
	 *
	 * @param removeMessage
	 * @param addMessage
	 */
	public StrictMap(String removeMessage, String addMessage) {
		super(removeMessage, addMessage);
	}

	/**
	 * 从给定旧映射创建新的严格映射
	 *
	 * @param copyOf
	 */
	public StrictMap(Map<K, V> copyOf) {
		this();

		this.putAll(copyOf);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 按值从映射移除第一个给定元素，若不存在则失败
	 *
	 * @param value
	 */
	public void removeByValue(V value) {
		for (final Entry<K, V> e : this.map.entrySet())
			if (e.getValue().equals(value)) {
				this.map.remove(e.getKey());
				return;
			}

		throw new NullPointerException(String.format(this.getCannotRemoveMessage(), value));
	}

	/**
	 * 移除所有键，若一个或多个不包含则失败
	 *
	 * @param keys
	 * @return
	 */
	public Object[] removeAll(Collection<K> keys) {
		final List<V> removedKeys = new ArrayList<>();

		for (final K key : keys)
			removedKeys.add(this.remove(key));

		return removedKeys.toArray();
	}

	/**
	 * 按键从映射移除给定元素，若不存在则失败
	 *
	 * @param key
	 * @return
	 */
	public V remove(K key) {
		final V removed = this.removeWeak(key);
		Valid.checkNotNull(removed, String.format(this.getCannotRemoveMessage(), key));

		return removed;
	}

	/**
	 * 向映射放入新键值对，若键已存在则失败
	 *
	 * @param key
	 * @param value
	 */
	public void put(K key, V value) {
		Valid.checkBoolean(!this.map.containsKey(key), String.format(this.getCannotAddMessage(), key, this.map.get(key)));

		this.override(key, value);
	}

	/**
	 * 将给定映射放入本映射，若键已存在则失败
	 *
	 * @param m
	 */
	public void putAll(StrictMap<? extends K, ? extends V> m) {
		for (final Map.Entry<? extends K, ? extends V> e : m.entrySet())
			Valid.checkBoolean(!this.map.containsKey(e.getKey()), String.format(this.getCannotAddMessage(), e.getKey(), this.map.get(e.getKey())));

		this.override(m);
	}

	/**
	 * 将给定映射放入本映射，若键已存在则失败
	 *
	 * @param m
	 */
	public void putAll(Map<? extends K, ? extends V> m) {
		for (final Map.Entry<? extends K, ? extends V> e : m.entrySet())
			Valid.checkBoolean(!this.map.containsKey(e.getKey()), String.format(this.getCannotAddMessage(), e.getKey(), this.map.get(e.getKey())));

		this.override(m);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 移除给定值，若不包含则什么都不做
	 *
	 * @param value
	 * @return
	 */
	public V removeWeak(K value) {
		return this.map.remove(value);
	}

	/**
	 * 向映射放入新键值对，覆盖旧的
	 *
	 * @param key
	 * @param value
	 */
	public void override(K key, V value) {
		this.map.put(key, value);
	}

	/**
	 * 向映射放入新键值对，覆盖旧的
	 *
	 * @param map
	 */
	public void override(StrictMap<? extends K, ? extends V> map) {
		this.override(map.map);
	}

	/**
	 * 向映射放入新键值对，覆盖旧的
	 *
	 * @param map
	 */
	public void override(Map<? extends K, ? extends V> map) {
		this.map.putAll(map);
	}

	/**
	 * 若键存在则正常返回，否则放入并返回它。
	 *
	 * @param key
	 * @param defaultToPut
	 * @return
	 */
	public V getOrPut(K key, V defaultToPut) {
		if (this.containsKey(key))
			return this.get(key);

		this.put(key, defaultToPut);
		return defaultToPut;
	}

	/**
	 * 按值返回第一个键，若未找到则为 null
	 *
	 * @param value
	 * @return
	 */
	public K getKeyFromValue(V value) {
		for (final Entry<K, V> e : this.map.entrySet())
			if (e.getValue().equals(value))
				return e.getKey();

		return null;
	}

	/**
	 * 返回映射中的键，若未设置则为 null
	 *
	 * @param key
	 * @return
	 */
	public V get(K key) {
		return this.map.get(key);
	}

	/**
	 * 返回映射中的键，若未设置则返回默认参数
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public V getOrDefault(K key, V def) {
		return this.map.getOrDefault(key, def);
	}

	/**
	 * 返回映射中第一个键值对的第一个键值，若映射为空则为 null
	 *
	 * @return
	 */
	@Nullable
	public K firstKey() {
		return this.map.isEmpty() ? null : this.map.keySet().iterator().next();
	}

	/**
	 * 返回映射中第一个键值对的第一个值，若映射为空则为 null
	 *
	 * @return
	 */
	@Nullable
	public V firstValue() {
		return this.map.isEmpty() ? null : this.map.values().iterator().next();
	}

	/**
	 * 若键不为 null 且包含则返回 true
	 *
	 * @param key
	 * @return
	 */
	public boolean containsKey(K key) {
		return key == null ? false : this.map.containsKey(key);
	}

	/**
	 * 若值不为 null 且包含则返回 true
	 *
	 * @param value
	 * @return
	 */
	public boolean containsValue(V value) {
		return value == null ? false : this.map.containsValue(value);
	}

	/**
	 * 对每个键值对执行给定操作
	 *
	 * @param consumer
	 */
	public void forEachIterate(BiConsumer<K, V> consumer) {
		for (final Entry<K, V> entry : this.entrySet())
			consumer.accept(entry.getKey(), entry.getValue());
	}

	/**
	 * 获取映射条目
	 *
	 * @return
	 */
	public Set<Entry<K, V>> entrySet() {
		return this.map.entrySet();
	}

	/**
	 * 获取映射键
	 *
	 * @return
	 */
	public Set<K> keySet() {
		return this.map.keySet();
	}

	/**
	 * 获取映射值
	 *
	 * @return
	 */
	public Collection<V> values() {
		return this.map.values();
	}

	/**
	 * 清空映射
	 */
	public void clear() {
		this.map.clear();
	}

	/**
	 * 若映射为空则返回 true
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	/**
	 * 返回原始 Java 映射
	 *
	 * @return
	 */
	public Map<K, V> getSource() {
		return this.map;
	}

	/**
	 * 返回映射大小
	 *
	 * @return
	 */
	public int size() {
		return this.map.size();
	}

	/**
	 * 序列化列表中的每个值，以便存入设置
	 */
	@Override
	public Object serialize() {
		if (!this.map.isEmpty()) {
			final Map<Object, Object> copy = new LinkedHashMap<>();

			for (final Entry<K, V> entry : this.entrySet()) {
				final V val = entry.getValue();

				if (val != null)
					copy.put(SerializeUtil.serialize(this.getMode(), entry.getKey()), SerializeUtil.serialize(this.getMode(), val));
			}

			return copy;
		}

		return this.getSource();
	}

	@Override
	public String toString() {
		return this.map.toString();
	}
}