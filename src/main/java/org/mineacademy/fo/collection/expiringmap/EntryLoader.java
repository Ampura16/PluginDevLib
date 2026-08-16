package org.mineacademy.fo.collection.expiringmap;

/**
 * 按需加载条目。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface EntryLoader<K, V> {
	/**
	 * 调用以为过期映射中的 {@code key} 加载新值。
	 *
	 * @param key 要加载值的键
	 * @return 要加载的新值
	 */
	V load(K key);
}