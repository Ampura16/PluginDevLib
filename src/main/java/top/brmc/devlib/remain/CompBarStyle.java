package top.brmc.devlib.remain;

import top.brmc.devlib.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Bukkit BarStyle 的包装类。
 */
@RequiredArgsConstructor
public enum CompBarStyle {

	/**
	 * 使 Boss 栏为实心（不分段）。
	 */
	SOLID("SOLID", "SOLID"),

	/**
	 * 将 Boss 栏分成 6 段。
	 */
	SEGMENTED_6("SEGMENTED_6", "SEG6"),

	/**
	 * 将 Boss 栏分成 10 段。
	 */
	SEGMENTED_10("SEGMENTED_10", "SEG10"),

	/**
	 * 将 Boss 栏分成 12 段。
	 */
	SEGMENTED_12("SEGMENTED_12", "SEG12"),

	/**
	 * 将 Boss 栏分成 20 段。
	 */
	SEGMENTED_20("SEGMENTED_20", "SEG20");

	@Getter
	private final String key;

	@Getter
	private final String shortKey;

	/**
	 * 尝试根据给定的键加载 CompBarStyle。
	 *
	 * @param key
	 * @return
	 */
	public static CompBarStyle fromKey(String key) {
		for (final CompBarStyle mode : values())
			if (mode.key.equalsIgnoreCase(key) || mode.shortKey.equalsIgnoreCase(key))
				return mode;

		throw new IllegalArgumentException("No such CompBarStyle: " + key + ". Available: " + Common.join(values()));
	}

	@Override
	public String toString() {
		return this.key;
	}
}
