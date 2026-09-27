package top.brmc.devlib.collection.expiringmap;

import java.util.concurrent.TimeUnit;

/**
 * 要存入 {@link ExpiringMap} 的值，可选控制其过期。
 *
 * @param <V> 所存值的类型
 */
public final class ExpiringValue<V> {
	private static final long UNSET_DURATION = -1L;
	private final V value;
	private final ExpirationPolicy expirationPolicy;
	private final long duration;
	private final TimeUnit timeUnit;

	/**
	 * 创建要存入 {@link ExpiringMap} 的 ExpiringValue。将使用映射默认的
	 * {@link ExpirationPolicy expiration policy} 和 {@link ExpiringMap#getExpiration()} expiration}。
	 *
	 * @param value 要存的值
	 * @see ExpiringMap#put(Object, Object)
	 */
	public ExpiringValue(V value) {
		this(value, UNSET_DURATION, null, null);
	}

	/**
	 * 创建要存入 {@link ExpiringMap} 的 ExpiringValue。将使用映射默认的
	 * {@link ExpiringMap#getExpiration()} expiration}。
	 *
	 * @param value            要存的值
	 * @param expirationPolicy 值的过期策略
	 * @see ExpiringMap#put(Object, Object, ExpirationPolicy)
	 */
	public ExpiringValue(V value, ExpirationPolicy expirationPolicy) {
		this(value, UNSET_DURATION, null, expirationPolicy);
	}

	/**
	 * 创建要存入 {@link ExpiringMap} 的 ExpiringValue。将使用映射默认的 {@link ExpirationPolicy
	 * expiration policy}。
	 *
	 * @param value    要存的值
	 * @param duration 条目创建后多久应被移除
	 * @param timeUnit {@code duration} 的时间单位
	 * @throws NullPointerException timeUnit 为 null 时
	 * @see ExpiringMap#put(Object, Object, long, TimeUnit)
	 */
	public ExpiringValue(V value, long duration, TimeUnit timeUnit) {
		this(value, duration, timeUnit, null);
		if (timeUnit == null)
			throw new NullPointerException();
	}

	/**
	 * 创建要存入 {@link ExpiringMap} 的 ExpiringValue。
	 *
	 * @param value            要存的值
	 * @param duration         条目创建后多久应被移除
	 * @param timeUnit         {@code duration} 的时间单位
	 * @param expirationPolicy 值的过期策略
	 * @throws NullPointerException timeUnit 为 null 时
	 * @see ExpiringMap#put(Object, Object, ExpirationPolicy, long, TimeUnit)
	 */
	public ExpiringValue(V value, ExpirationPolicy expirationPolicy, long duration, TimeUnit timeUnit) {
		this(value, duration, timeUnit, expirationPolicy);
		if (timeUnit == null)
			throw new NullPointerException();
	}

	private ExpiringValue(V value, long duration, TimeUnit timeUnit, ExpirationPolicy expirationPolicy) {
		this.value = value;
		this.expirationPolicy = expirationPolicy;
		this.duration = duration;
		this.timeUnit = timeUnit;
	}

	public V getValue() {
		return this.value;
	}

	public ExpirationPolicy getExpirationPolicy() {
		return this.expirationPolicy;
	}

	public long getDuration() {
		return this.duration;
	}

	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	@Override
	public int hashCode() {
		return this.value != null ? this.value.hashCode() : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || this.getClass() != o.getClass())
			return false;

		final ExpiringValue<?> that = (ExpiringValue<?>) o;
		return !(this.value != null ? !this.value.equals(that.value) : that.value != null) && this.expirationPolicy == that.expirationPolicy && this.duration == that.duration && this.timeUnit == that.timeUnit;

	}

	@Override
	public String toString() {
		return "ExpiringValue{" + "value=" + this.value + ", expirationPolicy=" + this.expirationPolicy + ", duration=" + this.duration + ", timeUnit=" + this.timeUnit + '}';
	}
}
