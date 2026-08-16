package org.mineacademy.fo.model;

import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;

import lombok.Getter;

/**
 * 表示两个 {@link SimpleTime} 实例，
 * 可从中随机取出一个时间。
 */
@Getter
public final class RangedSimpleTime {

	/**
	 * 最小时间
	 */
	private final SimpleTime min;

	/**
	 * 最大时间
	 */
	private final SimpleTime max;

	/**
	 * 根据给定时间创建一个新的范围时间，实际上不带任何范围
	 *
	 * @param time
	 */
	public RangedSimpleTime(final SimpleTime time) {
		this(time, time);
	}

	/**
	 * 根据最小值和最大值创建一个新的范围时间
	 *
	 * @param min
	 * @param max
	 */
	public RangedSimpleTime(final SimpleTime min, final SimpleTime max) {
		Valid.checkBoolean(min.getTimeTicks() >= 0 && max.getTimeTicks() >= 0, "Values may not be negative");
		Valid.checkBoolean(min.getTimeTicks() <= max.getTimeTicks(), "Minimum must be lower or equal maximum");

		this.min = min;
		this.max = max;
	}

	/**
	 * 返回最小值与最大值之间的随机时间（刻）
	 *
	 * @return
	 */
	public int getRandomTicks() {
		return RandomUtil.nextBetween(this.min.getTimeTicks(), this.max.getTimeTicks());
	}

	/**
	 * 返回最小值与最大值之间的随机时间（秒）
	 *
	 * @return
	 */
	public int getRandomSeconds() {
		return RandomUtil.nextBetween((int) this.min.getTimeSeconds(), (int) this.max.getTimeSeconds());
	}

	/**
	 * 返回给定的刻数是否位于最小值与最大值之间
	 *
	 * @param ticks
	 * @return
	 */
	public boolean isInRangeTicks(final int ticks) {
		return ticks >= this.min.getTimeTicks() && ticks <= this.min.getTimeTicks();
	}

	/**
	 * 返回给定的秒数是否位于最小值与最大值之间
	 *
	 * @param seconds
	 * @return
	 */
	public boolean isInRangeSeconds(final int seconds) {
		return seconds >= this.min.getTimeSeconds() && seconds <= this.min.getTimeSeconds();
	}

	/**
	 * 返回格式化后的文本，例如 '1 ticks - 2 minutes'
	 *
	 * @return
	 */
	public String toLine() {
		return (this.min.getRaw() + (this.min.equals(this.max) ? "" : " - " + this.max.getRaw())).replace("  ", " ");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RangedSimpleTime{min=" + this.min + ", max=" + this.max + "}";
	}

	/**
	 * 从给定文本解析范围时间，例如 "1 second - 2 minutes" 等。
	 *
	 * @param line
	 * @return
	 */
	public static RangedSimpleTime parse(final String line) {
		final String[] parts = line.split("\\-");
		Valid.checkBoolean(parts.length == 1 || parts.length == 2, "Malformed RangedSimpleTime " + line);

		final String min = parts[0].trim();
		final String max = (parts.length == 2 ? parts[1] : min).trim();

		return new RangedSimpleTime(SimpleTime.from(min), SimpleTime.from(max));
	}
}
