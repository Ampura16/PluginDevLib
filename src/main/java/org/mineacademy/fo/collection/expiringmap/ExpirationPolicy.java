package org.mineacademy.fo.collection.expiringmap;

/**
 * 决定 ExpiringMap 条目应如何过期。
 */
public enum ExpirationPolicy {
	/**
	 * 按上次访问时间过期条目
	 */
	ACCESSED,
	/**
	 * 按创建时间过期条目
	 */
	CREATED;
}