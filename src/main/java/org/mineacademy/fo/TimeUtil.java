package org.mineacademy.fo;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.settings.SimpleLocalization.Cases;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 在 tick 与时间之间换算的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtil {

	/**
	 * 识别 1d1h1s 这类日期的正则
	 */
	private static final Pattern TOKEN_PATTERN = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?"
			// Months
			+ "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?"

			// Weeks
			+ "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?"

			// Days
			+ "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?"

			// Hours
			+ "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?"

			// Minutes
			+ "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?"

			// Seconds (the "s" may be left out)
			+ "(?:([0-9]+)\\s*(?:s[a-z]*)?)?",

			Pattern.CASE_INSENSITIVE);

	// ------------------------------------------------------------------------------------------------------------
	// Current time
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 自 1970 年 1 月 1 日起经过的秒数
	 *
	 * @return System.currentTimeMillis / 1000
	 */
	public static long currentTimeSeconds() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * 自 1970 年 1 月 1 日起经过的 tick 数
	 *
	 * @return System.currentTimeMillis / 50
	 */
	public static long currentTimeTicks() {
		return System.currentTimeMillis() / 50;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Formatting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回按 {@link SimpleSettings#DATE_FORMAT} 格式化的当前时间
	 * 默认格式为 DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @return
	 */
	public static String getFormattedDate() {
		return getFormattedDate(System.currentTimeMillis());
	}

	/**
	 * 返回按 {@link SimpleSettings#DATE_FORMAT} 格式化的给定毫秒时间戳
	 * 默认格式为 DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDate(final long time) {
		return SimpleSettings.DATE_FORMAT.format(time);
	}

	/**
	 * 返回按 {@link SimpleSettings#DATE_FORMAT_SHORT} 格式化的当前时间
	 * 默认格式为 DAY.MONTH.YEAR HOUR:MINUTES
	 *
	 * @return
	 */
	public static String getFormattedDateShort() {
		return getFormattedDateShort(System.currentTimeMillis());
	}

	/**
	 * 返回按 {@link SimpleSettings#DATE_FORMAT_SHORT} 格式化的给定毫秒日期
	 * 默认格式为 DAY.MONTH.YEAR HOUR:MINUTES
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDateShort(final long time) {
		return SimpleSettings.DATE_FORMAT_SHORT.format(time);
	}

	/**
	 * 返回按 {@link SimpleSettings#DATE_FORMAT_MONTH} 格式化的当前时间
	 * 默认格式为 DAY.MONTH HOUR:MINUTES
	 *
	 * @return
	 */
	public static String getFormattedDateMonth() {
		return getFormattedDateMonth(System.currentTimeMillis());
	}

	/**
	 * 返回按 {@link SimpleSettings#DATE_FORMAT_MONTH} 格式化的给定毫秒日期
	 * 默认格式为 DAY.MONTH HOUR:MINUTES
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDateMonth(final long time) {
		return SimpleSettings.DATE_FORMAT_MONTH.format(time);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 将人类可读格式的时间（如 "10 minutes"）转换为
	 * 秒。
	 *
	 * @param humanReadableTime 人类可读时间格式：{time} {period}
	 *                          例如：5 seconds、10 ticks、7 minutes、12 hours 等。
	 * @return 转换后的人类时间（秒）
	 */
	public static long toTicks(final String humanReadableTime) {
		Valid.checkNotNull(humanReadableTime, "Time is null");

		long seconds = 0L;

		final String[] split = humanReadableTime.split(" ");

		if (split.length < 2)
			throw new IllegalArgumentException("Expected human readable time like '1 second', got '" + humanReadableTime + "' instead");

		for (int i = 1; i < split.length; i++) {
			final String sub = split[i].toLowerCase();
			int multiplier = 0; // e.g 2 hours = 2
			long unit = 0; // e.g hours = 3600
			boolean isTicks = false;

			try {
				multiplier = Integer.parseInt(split[i - 1]);
			} catch (final NumberFormatException e) {
				continue;
			}

			// attempt to match the unit time
			if (sub.startsWith("tick"))
				isTicks = true;

			else if (sub.startsWith("second"))
				unit = 1;

			else if (sub.startsWith("minute"))
				unit = 60;

			else if (sub.startsWith("hour"))
				unit = 3600;

			else if (sub.startsWith("day"))
				unit = 86400;

			else if (sub.startsWith("week"))
				unit = 604800;

			else if (sub.startsWith("month"))
				unit = 2629743;

			else if (sub.startsWith("year"))
				unit = 31556926;

			else if (sub.startsWith("potato"))
				unit = 1337;

			else
				throw new IllegalArgumentException("Must define date type! Example: '1 second' (Got '" + sub + "')");

			seconds += multiplier * (isTicks ? 1 : unit * 20);
		}

		return seconds;
	}

	/**
	 * 将给定的秒数时间格式化为以下格式：
	 * <p>
	 * "1 hour 50 minutes 10 seconds" 或类似，更短也行
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeGeneric(final long seconds) {
		final long second = seconds % 60;
		long minute = seconds / 60;
		String hourMsg = "";

		if (minute >= 60) {
			final long hour = seconds / 60 / 60;
			minute %= 60;

			hourMsg = Cases.HOUR.formatWithCount(hour) + " ";
		}

		return hourMsg + (minute != 0 ? Cases.MINUTE.formatWithCount(minute) + " " : "") + Cases.SECOND.formatWithCount(second);
	}

	/**
	 * 将时间格式化为 "X days Y hours Z minutes Å seconds"
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeDays(final long seconds) {
		final long minutes = seconds / 60;
		final long hours = minutes / 60;
		final long days = hours / 24;

		return (days != 0 ? Cases.DAY.formatWithCount(days) + " " : "")
				+ (hours % 24 != 0 ? Cases.HOUR.formatWithCount(hours % 24) + " " : "")
				+ (minutes % 60 != 0 ? Cases.MINUTE.formatWithCount(minutes % 60) + " " : "")
				+ Cases.SECOND.formatWithCount(seconds % 60);
	}

	/**
	 * 将秒数时间格式化，例如：10d 5h 10m 20s 或仅 5m 10s
	 * 若秒数为零则输出 0s
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeShort(long seconds) {
		long minutes = seconds / 60;
		long hours = minutes / 60;
		final long days = hours / 24;

		hours = hours % 24;
		minutes = minutes % 60;
		seconds = seconds % 60;

		return (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "m " : "") + seconds + "s";
	}

	/**
	 * 将秒数时间格式化为冒号分隔，例如：1 小时 20 分钟为 01:20:00
	 * 若秒数为零则输出 00:00
	 *
	 * @param seconds
	 * @return
	 */
	public static String formatTimeColon(long seconds) {
		long minutes = seconds / 60;
		long hours = minutes / 60;
		final long days = hours / 24;

		hours = hours % 24;
		minutes = minutes % 60;
		seconds = seconds % 60;

		return (days > 0 ? (days < 10 ? "0" : "") + days + ":" : "") +
				(hours > 0 ? (hours < 10 ? "0" : "") + hours + ":" : "") +
				(minutes > 0 ? (minutes < 10 ? "0" : "") + minutes + ":" : "00:") +
				(seconds < 10 ? "0" : "") + seconds;
	}

	/**
	 * 将给定字符串标记转换为毫秒
	 *
	 * 例如：1y、1mo、1w、1d、1h、1m、1s，这些可以组合使用
	 *
	 * @param text
	 * @return
	 */
	public static long toMilliseconds(final String text) {
		final Matcher matcher = TOKEN_PATTERN.matcher(text);

		long years = 0, months = 0, weeks = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
		boolean found = false;

		while (matcher.find()) {

			if (matcher.group() == null || matcher.group().isEmpty())
				continue;

			for (int i = 0; i < matcher.groupCount(); i++)
				if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
					found = true;

					break;
				}

			if (found) {
				for (int i = 1; i < 8; i++)
					if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
						final long output = Long.parseLong(matcher.group(i));

						if (i == 1) {
							checkLimit("years", output, 10);

							years = output;
						}

						else if (i == 2) {
							checkLimit("months", output, 12 * 100);

							months = output;
						}

						else if (i == 3) {
							checkLimit("weeks", output, 4 * 100);

							weeks = output;
						}

						else if (i == 4) {
							checkLimit("days", output, 31 * 100);

							days = output;
						}

						else if (i == 5) {
							checkLimit("hours", output, 24 * 100);

							hours = output;
						}

						else if (i == 6) {
							checkLimit("minutes", output, 60 * 100);

							minutes = output;
						}

						else if (i == 7) {
							checkLimit("seconds", output, 60 * 100);

							seconds = output;
						}
					}

				break;
			}
		}

		if (!found)
			throw new NumberFormatException("Date not found from: " + text);

		return (seconds + (minutes * 60) + (hours * 3600) + (days * 86400) + (weeks * 7 * 86400) + (months * 30 * 86400) + (years * 365 * 86400)) * 1000;
	}

	/*
	 * Check value over limit
	 */
	private static void checkLimit(final String type, final long value, final int maxLimit) {
		if (value > maxLimit)
			throw new IllegalArgumentException("Value type " + type + " is out of bounds! Max limit: " + maxLimit + ", given: " + value);
	}

	/**
	 * 将当前时间转换为 MySQL 可识别的格式
	 *
	 * @return
	 */
	public static String toSQLTimestamp() {
		return toSQLTimestamp(System.currentTimeMillis());
	}

	/**
	 * 将给定 long 时间戳转换为 MySQL 可识别的格式
	 *
	 * @param timestamp
	 * @return
	 */
	public static String toSQLTimestamp(final long timestamp) {
		final java.util.Date date = new Date(timestamp);

		return new Timestamp(date.getTime()).toString();
	}

	/**
	 * 将给定 MySQL 时间戳转换为 long
	 *
	 * @param timestamp
	 * @return
	 */
	public static long fromSQLTimestamp(final String timestamp) {
		return Timestamp.valueOf(timestamp).getTime();
	}

	/**
	 * 检查当前时间是否在指定时间范围内。
	 *
	 * @param time 要检查的时间字符串。格式应为 'dd MMM yyyy, HH:mm'，可包含 {year}、{month}、{day}、{hour}、{minute} 和 {second} 等代表当前时间段的变量。
	 * @param future 若为 true 则检查当前时间是否早于指定时间，若为 false 则检查是否晚于指定时间。
	 * @return 若当前时间在指定时间范围内则返回 true，否则返回 false。
	 *
	 * 若时间字符串格式不正确或包含无效变量则抛出 ParseException。
	 *
	 * 本方法工作流程如下：
	 * 1. 用 Calendar.getInstance() 获取当前时间。
	 * 2. 将时间字符串中的月份简写（Jan、Feb 等）替换为全称（January、February 等）。
	 * 3. 将时间字符串中的变量（{year}、{month} 等）替换为当前值。
	 * 4. 将时间字符串解析为时间戳。
	 * 5. 若 'future' 参数为 true 则检查当前时间是否早于时间戳，若为 false 则检查是否晚于时间戳。
	 * 6. 若当前时间不在指定时间范围内则返回 false，否则返回 true。
	 */
	public static boolean isInTimeframe(@NonNull String time, boolean future) {
		final Calendar calendar = Calendar.getInstance();
		final String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
		final String[] fullNameMonths = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

		for (int i = 0; i < months.length; i++)
			time = time.replaceAll(months[i] + "\\b", fullNameMonths[i]);

		time = Replacer.replaceArray(time,
				"year", calendar.get(Calendar.YEAR),
				"month", fullNameMonths[calendar.get(Calendar.MONTH)],
				"day", calendar.get(Calendar.DAY_OF_MONTH),
				"hour", calendar.get(Calendar.HOUR_OF_DAY),
				"minute", calendar.get(Calendar.MINUTE),
				"second", calendar.get(Calendar.SECOND));

		try {
			final long timestamp = new SimpleDateFormat("dd MMM yyyy, HH:mm").parse(time).getTime();

			if (future) {
				if (System.currentTimeMillis() < timestamp)
					return false;

			} else {
				if (System.currentTimeMillis() > timestamp)
					return false;
			}

		} catch (final ParseException ex) {
			Common.throwError(ex,
					"Syntax error in time operator.",
					"Valid: 'dd MMM yyyy, HH:mm' with {year/month/day/hour/minute/second} variables.",
					"Got: " + time);
		}

		return true;
	}
}
