package top.brmc.devlib.model;

import top.brmc.devlib.TimeUtil;

import lombok.Getter;
import lombok.NonNull;

/**
 * 以人类可读形式（如 1 second 或 5 minutes）存放时间值的简单类
 */
@Getter
public final class SimpleTime {

	private final String raw;
	private final long timeTicks;
	private final boolean enabled;

	protected SimpleTime(@NonNull final String time) {
		if ("0".equals(time) || "none".equalsIgnoreCase(time) || "never".equalsIgnoreCase(time)) {
			this.raw = "0";
			this.timeTicks = 0;
			this.enabled = false;

		} else {
			this.raw = time;
			this.timeTicks = TimeUtil.toTicks(time);
			this.enabled = true;
		}
	}

	/**
	 * 获取以秒为单位的时间（刻 / 20）
	 *
	 * @return
	 */
	public long getTimeSeconds() {
		return this.timeTicks / 20L;
	}

	/**
	 * 获取以刻为单位的时间
	 * *警告* 如果刻数超过 {@value Integer#MAX_VALUE}，将会溢出！
	 *
	 * @return
	 */
	public int getTimeTicks() {
		return (int) this.timeTicks;
	}

	/**
	 * 获取以毫秒为单位的时间（刻 * 20）
	 *
	 * @return
	 */
	public long getTimeMilliseconds() {
		return this.timeTicks * 50;
	}

	/**
	 * 返回此时间的人类可读表示，例如 69 seconds（纯属巧合）
	 *
	 * @return
	 */
	public String getRaw() {
		return this.timeTicks == 0 ? "0" : this.raw;
	}

	/**
	 * 若从当前时间戳算起已超过给定时限则返回 true
	 *
	 * @param limitMs
	 * @return
	 */
	public boolean isOverLimitMs(final long limitMs) {
		return (System.currentTimeMillis() - limitMs) > this.getTimeMilliseconds();
	}

	/**
	 * 若从当前时间戳算起尚未超过给定时限则返回 true
	 *
	 * @param limitMs
	 * @return
	 */
	public boolean isUnderLimitMs(final long limitMs) {
		return (System.currentTimeMillis() - limitMs) < this.getTimeMilliseconds();
	}

	/**
	 * 以人类可读形式格式化距离给定时限的剩余时间
	 *
	 * @param lastLastExecutionMs
	 * @return
	 */
	public String formatWaitTime(final long lastLastExecutionMs) {
		final long limit = this.getTimeMilliseconds();
		final long delay = System.currentTimeMillis() - lastLastExecutionMs;

		return TimeUtil.formatTimeGeneric(1 + (limit - delay) / 1000L);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SimpleTime && ((SimpleTime) obj).timeTicks == this.timeTicks;
	}

	@Override
	public String toString() {
		return this.getRaw();
	}

	/**
	 * 根据给定秒数生成新的时间
	 *
	 * @param seconds
	 * @return
	 */
	public static SimpleTime fromSeconds(final int seconds) {
		return from(seconds + " seconds");
	}

	/**
	 * 生成新的时间。有效示例：15 ticks、1 second、25 minutes、3 hours 等，
	 * 或输入 "none" 创建时间为 0 的实例
	 *
	 * @param time
	 * @return
	 */
	public static SimpleTime from(final String time) {
		return new SimpleTime(time);
	}
}