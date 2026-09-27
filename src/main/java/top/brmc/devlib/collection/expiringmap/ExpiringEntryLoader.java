package top.brmc.devlib.collection.expiringmap;

/**
 * 按需加载条目，可控制每个值的过期时长（即可变过期）。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface ExpiringEntryLoader<K, V> {
	/**
	 * 调用以为过期映射中的 {@code key} 加载新值。
	 *
	 * @param key 要加载值的键
	 * @return 包含要加载的新值及其过期时长
	 */
	ExpiringValue<V> load(K key);
}