package top.brmc.devlib.model;

import top.brmc.devlib.RandomUtil;
import top.brmc.devlib.Valid;

import lombok.Getter;

/**
 * 存放最小值和最大值的类
 */
@Getter
public final class RangedValue {

	/**
	 * 最小值
	 */
	private final Number min;

	/**
	 * 最大值
	 */
	private final Number max;

	/**
	 * 创建一个新的固定值。
	 *
	 * @param value 值
	 */
	public RangedValue(Number value) {
		this(value, value);
	}

	/**
	 * 创建一个新的范围值。
	 *
	 * @param min 最小值
	 * @param max 最大值
	 */
	public RangedValue(Number min, Number max) {
		Valid.checkBoolean(min.longValue() <= max.longValue(), "Minimum must be lower or equal maximum");

		this.min = min;
		this.max = max;
	}

	/**
	 * 以 double 形式获取最小值
	 * @return
	 */
	public double getMinDouble() {
		return this.min.doubleValue();
	}

	/**
	 * 以 double 形式获取最大值
	 * @return
	 */
	public double getMaxDouble() {
		return this.max.doubleValue();
	}

	/**
	 * 以 long 形式获取最小值
	 * @return
	 */
	public long getMinLong() {
		return this.min.longValue();
	}

	/**
	 * 以 long 形式获取最大值
	 * @return
	 */
	public long getMaxLong() {
		return this.max.longValue();
	}

	/**
	 * 判断该数字是否在范围内
	 *
	 * @param value 要比较的数字
	 * @return
	 */
	public boolean isInRangeLong(long value) {
		return value >= this.min.longValue() && value <= this.max.longValue();
	}

	/**
	 * 判断该数字是否在范围内
	 *
	 * @param value 要比较的数字
	 * @return
	 */
	public boolean isInRangeDouble(double value) {
		return value >= this.min.doubleValue() && value <= this.max.doubleValue();
	}

	/**
	 * 获取介于此类存储的两个值之间的一个值
	 *
	 * @return 随机值
	 */
	public int getRandomInt() {
		return RandomUtil.nextBetween((int) this.getMinLong(), (int) this.getMaxLong());
	}

	/**
	 * 返回此类存储的两个值是否相等
	 *
	 * @return
	 */
	public boolean isStatic() {
		return this.min.longValue() == this.max.longValue();
	}

	/**
	 * 返回此值的可保存表示形式（假定以刻为单位保存）
	 *
	 * @return
	 */
	public String toLine() {
		return this.min.longValue() + " - " + this.max.longValue();
	}

	/**
	 * 从一行文本创建 {@link RangedValue}
	 * 示例：1-10
	 * 5 - 60
	 * -5 - 5
	 * -5 - -2
	 * 4
	 * @param line
	 * @return
	 */
	public static RangedValue parse(String line) {

		line = line.replace(" ", "").trim();

		boolean firstNegative = false;

		if (line.startsWith("-")) {
			firstNegative = true;

			line = line.substring(1);
		}

		String[] parts;
		final String[] split = line.split("\\-");

		final boolean secondNegative = split.length == 3;

		if (split.length == 1)
			parts = new String[] { (firstNegative ? "-" : "") + line };
		else
			parts = new String[] { (firstNegative ? "-" : "") + split[0], (secondNegative ? "-" + split[2] : split[1]) };

		Valid.checkBoolean(parts.length == 1 || parts.length == 2, "Malformed value " + line);

		final String first = parts[0].trim();
		final String second = parts.length == 2 ? parts[1].trim() : first;

		// Check if valid numbers
		Valid.checkBoolean(Valid.isNumber(first),
				"Invalid ranged value 1. input: '" + first + "' from line: '" + line + "'. RangedValue no longer accepts human natural format, for this, use RangedSimpleTime instead.");

		Valid.checkBoolean(Valid.isNumber(second),
				"Invalid ranged value 2. input: '" + second + "' from line: '" + line + "'. RangedValue no longer accepts human natural format, for this, use RangedSimpleTime instead.");

		final Number firstNumber = first.contains(".") ? Double.parseDouble(first) : Long.parseLong(first);
		final Number secondNumber = second.contains(".") ? Double.parseDouble(second) : Long.parseLong(second);

		// Check if 1<2
		if (first.contains("."))
			Valid.checkBoolean(firstNumber.longValue() <= secondNumber.longValue(),
					"First number cannot be greater than second: " + firstNumber.longValue() + " vs " + secondNumber.longValue() + " in " + line);

		else
			Valid.checkBoolean(firstNumber.doubleValue() <= secondNumber.doubleValue(),
					"First number cannot be greater than second: " + firstNumber.doubleValue() + " vs " + secondNumber.doubleValue() + " in " + line);

		return new RangedValue(firstNumber, secondNumber);
	}

	@Override
	public String toString() {
		return this.isStatic() ? this.min.longValue() + "" : this.min.longValue() + " - " + this.max.longValue();
	}
}
