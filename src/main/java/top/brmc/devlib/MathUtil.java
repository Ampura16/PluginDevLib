package top.brmc.devlib;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.bukkit.util.Vector;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 数学运算工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MathUtil {

	/**
	 * 将整数格式化为带 1 位小数的完整小数的格式化器
	 */
	private final static DecimalFormat oneDigitFormat = new DecimalFormat("#.#");

	/**
	 * 将整数格式化为带 2 位小数的完整小数的格式化器
	 */
	private final static DecimalFormat twoDigitsFormat = new DecimalFormat("#.##");

	/**
	 * 将整数格式化为带 3 位小数的完整小数的格式化器
	 */
	private final static DecimalFormat threeDigitsFormat = new DecimalFormat("#.###");

	/**
	 * 将整数格式化为带 5 位小数的完整小数的格式化器
	 */
	private final static DecimalFormat fiveDigitsFormat = new DecimalFormat("#.#####");

	/**
	 * 保存所有有效罗马数字
	 */
	private final static NavigableMap<Integer, String> romanNumbers = new TreeMap<>();

	// Load the roman numbers
	static {
		romanNumbers.put(1000, "M");
		romanNumbers.put(900, "CM");
		romanNumbers.put(500, "D");
		romanNumbers.put(400, "CD");
		romanNumbers.put(100, "C");
		romanNumbers.put(90, "XC");
		romanNumbers.put(50, "L");
		romanNumbers.put(40, "XL");
		romanNumbers.put(10, "X");
		romanNumbers.put(9, "IX");
		romanNumbers.put(5, "V");
		romanNumbers.put(4, "IV");
		romanNumbers.put(1, "I");
	}

	// ----------------------------------------------------------------------------------------------------
	// Number manipulation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 返回给定数字的罗马数字表示
	 *
	 * @param number
	 * @return
	 */
	public static String toRoman(final int number) {
		if (number == 0)
			return "0"; // Actually, Romans did not know zero lol

		final int literal = romanNumbers.floorKey(number);

		if (number == literal)
			return romanNumbers.get(number);

		return romanNumbers.get(literal) + toRoman(number - literal);
	}

	/**
	 * 返回给定数字数组中的最大整数
	 *
	 * @param numbers
	 * @return
	 */
	public static int max(int... numbers) {
		return Arrays.stream(numbers).max().getAsInt();
	}

	/**
	 * 见 {@link Math#floor(double)}
	 *
	 * @param d1
	 * @return
	 */
	public static long floor(final double d1) {
		final long i = (long) d1;

		return d1 >= i ? i : i - 1;
	}

	/**
	 * 见 {@link Math#ceil(double)}
	 *
	 * @param f1
	 * @return
	 */
	public static long ceiling(final double f1) {
		final long i = (long) f1;

		return f1 >= i ? i : i - 1;
	}

	/**
	 * 见 {@link #range(int, int, int)}
	 *
	 * @param value 实际值
	 * @param min   最小限制
	 * @param max   最大限制
	 * @return 范围内的值
	 */
	public static double range(final double value, final double min, final double max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * 获取范围内值。若值小于 min 则返回 min，若大于 max 则返回 max。
	 *
	 * @param value 实际值
	 * @param min   最小限制
	 * @param max   最大限制
	 * @return 范围内的值
	 */
	public static int range(final int value, final int min, final int max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * 若给定值高于 min 则返回它，否则返回 min
	 *
	 * @param value
	 * @param min
	 * @return
	 */
	public static double atLeast(final double value, final double min) {
		return value > min ? value : min;
	}

	/**
	 * 若给定值高于 min 则返回它，否则返回 min
	 *
	 * @param value
	 * @param min
	 * @return
	 */
	public static int atLeast(final int value, final int min) {
		return value > min ? value : min;
	}

	/**
	 * 将给定数字增加给定百分比（0 到 100）
	 *
	 * @param number
	 * @param percent
	 * @return
	 */
	public static int increase(final int number, final double percent) {
		final double myNumber = number;
		final double percentage = myNumber / 100 * percent;

		return (int) Math.round(myNumber + percentage);
	}

	/**
	 * 将给定数字增加给定百分比（0 到 100）
	 *
	 * @param number
	 * @param percent
	 * @return
	 */
	public static double increase(final double number, final double percent) {
		final double percentage = number / 100 * percent;

		return number + percentage;
	}

	/**
	 * 计算给定数字占最大值的百分比（完成度），
	 * 范围 0 到 100
	 *
	 * @param number
	 * @param maximum
	 * @return 给定数字占最大值的 0 到 100 部分
	 */
	public static int percent(final double number, final double maximum) {
		return (int) (number / maximum * 100);
	}

	/**
	 * 返回给定值的平均小数
	 *
	 * @param values
	 * @return
	 */
	public static double average(final Collection<Double> values) {
		return average(values.toArray(new Double[values.size()]));
	}

	/**
	 * 返回给定值的平均小数
	 *
	 * @param values
	 * @return
	 */
	public static double average(final Double... values) {
		Valid.checkBoolean(values.length > 0, "No values given!");

		double sum = 0;

		for (final double val : values)
			sum += val;

		return formatTwoDigitsD(sum / values.length);
	}

	// ----------------------------------------------------------------------------------------------------
	// Vectors
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 将给定向量旋转给定角度（单位：度）
	 * 建议实现缓存或预计算以获得最佳性能。
	 *
	 * @param vector
	 * @param angle
	 * @return
	 */
	public static Vector rotateAroundAxisX(Vector vector, double angle) {
		angle = Math.toRadians(angle);

		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double y = vector.getY() * cos - vector.getZ() * sin;
		final double z = vector.getY() * sin + vector.getZ() * cos;

		return vector.setY(y).setZ(z);
	}

	/**
	 * 将给定向量旋转给定角度（单位：度）
	 * 建议实现缓存或预计算以获得最佳性能。
	 *
	 * @param v
	 * @param angle
	 * @return
	 */
	public static Vector rotateAroundAxisY(Vector v, double angle) {
		angle = -angle;
		angle = Math.toRadians(angle);

		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double x = v.getX() * cos + v.getZ() * sin;
		final double z = v.getX() * -sin + v.getZ() * cos;

		return v.setX(x).setZ(z);
	}

	/**
	 * 将给定向量旋转给定角度（单位：度）
	 * 建议实现缓存或预计算以获得最佳性能。
	 *
	 * @param v
	 * @param angle
	 * @return
	 */
	public static Vector rotateAroundAxisZ(Vector v, double angle) {
		angle = Math.toRadians(angle);

		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double x = v.getX() * cos - v.getY() * sin;
		final double y = v.getX() * sin + v.getY() * cos;

		return v.setX(x).setY(y);
	}

	// ----------------------------------------------------------------------------------------------------
	// Formatting
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 将给定数字格式化为一位数
	 *
	 * @param value
	 * @return
	 */
	public static String formatOneDigit(final double value) {
		return oneDigitFormat.format(value).replace(",", ".");
	}

	/**
	 * 将给定数字格式化为一位数
	 *
	 * @param value
	 * @return
	 */
	public static double formatOneDigitD(final double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(oneDigitFormat.format(value).replace(",", "."));
	}

	/**
	 * 将给定数字格式化为两位数
	 *
	 * @param value
	 * @return
	 */
	public static String formatTwoDigits(final double value) {
		return twoDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * 将给定数字格式化为两位数
	 *
	 * @param value
	 * @return
	 */
	public static double formatTwoDigitsD(final double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(twoDigitsFormat.format(value).replace(",", "."));
	}

	/**
	 * 将给定数字格式化为三位数
	 *
	 * @param value
	 * @return
	 */
	public static String formatThreeDigits(final double value) {
		return threeDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * 将给定数字格式化为三位数
	 *
	 * @param value
	 * @return
	 */
	public static double formatThreeDigitsD(final double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(threeDigitsFormat.format(value).replace(",", "."));
	}

	/**
	 * 将给定数字格式化为五位数
	 *
	 * @param value
	 * @return
	 */
	public static String formatFiveDigits(final double value) {
		return fiveDigitsFormat.format(value).replace(",", ".");
	}

	/**
	 * 将给定数字格式化为五位数
	 *
	 * @param value
	 * @return
	 */
	public static double formatFiveDigitsD(final double value) {
		Valid.checkBoolean(!Double.isNaN(value), "Value must not be NaN");

		return Double.parseDouble(fiveDigitsFormat.format(value).replace(",", "."));
	}

	// ----------------------------------------------------------------------------------------------------
	// Calculating
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 计算给定表达式，例如 5*(4-2) 返回……让我算算！
	 *
	 * @param expression
	 * @return
	 */
	public static double calculate(final String expression) {
		class Parser {
			int pos = -1, c;

			void eatChar() {
				this.c = ++this.pos < expression.length() ? expression.charAt(this.pos) : -1;
			}

			void eatSpace() {
				while (Character.isWhitespace(this.c))
					this.eatChar();
			}

			double parse() {
				this.eatChar();

				final double v = this.parseExpression();

				if (this.c != -1)
					throw new CalculatorException("Unexpected: " + (char) this.c);

				return v;
			}

			// Grammar:
			// expression = term | expression `+` term | expression `-` term
			// term = factor | term `*` factor | term `/` factor | term brackets
			// factor = brackets | number | factor `^` factor
			// brackets = `(` expression `)`

			double parseExpression() {
				double v = this.parseTerm();

				for (;;) {
					this.eatSpace();

					if (this.c == '+') { // addition
						this.eatChar();
						v += this.parseTerm();
					} else if (this.c == '-') { // subtraction
						this.eatChar();
						v -= this.parseTerm();
					} else
						return v;

				}
			}

			double parseTerm() {
				double v = this.parseFactor();

				for (;;) {
					this.eatSpace();

					if (this.c == '/') { // division
						this.eatChar();
						v /= this.parseFactor();
					} else if (this.c == '*' || this.c == '(') { // multiplication
						if (this.c == '*')
							this.eatChar();
						v *= this.parseFactor();
					} else
						return v;
				}
			}

			double parseFactor() {
				double v;
				boolean negate = false;

				this.eatSpace();

				if (this.c == '+' || this.c == '-') { // unary plus & minus
					negate = this.c == '-';
					this.eatChar();
					this.eatSpace();
				}

				if (this.c == '(') { // brackets
					this.eatChar();
					v = this.parseExpression();
					if (this.c == ')')
						this.eatChar();
				} else { // numbers
					final StringBuilder sb = new StringBuilder();

					while (this.c >= '0' && this.c <= '9' || this.c == '.') {
						sb.append((char) this.c);
						this.eatChar();
					}

					if (sb.length() == 0)
						throw new CalculatorException("Unexpected: " + (char) this.c);

					v = Double.parseDouble(sb.toString());
				}
				this.eatSpace();
				if (this.c == '^') { // exponentiation
					this.eatChar();
					v = Math.pow(v, this.parseFactor());
				}
				if (negate)
					v = -v; // unary minus is applied after exponentiation; e.g. -3^2=-9
				return v;
			}
		}
		return new Parser().parse();
	}

	/**
	 * 计算错误数字时抛出的异常（例如除以 0）
	 * <p>
	 * 见 {@link MathUtil#calculate(String)}
	 */
	public static final class CalculatorException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public CalculatorException(final String message) {
			super(message);
		}
	}
}