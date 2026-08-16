package org.mineacademy.fo.remain;

import org.mineacademy.fo.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Bukkit BarColor 的包装类
 */
@RequiredArgsConstructor
public enum CompBarColor {

	PINK("PINK"),
	BLUE("BLUE"),
	RED("RED"),
	GREEN("GREEN"),
	YELLOW("YELLOW"),
	PURPLE("PURPLE"),
	WHITE("WHITE");

	@Getter
	private final String key;

	/**
	 * 尝试根据给定的键加载 CompBarColor
	 *
	 * @param key
	 * @return
	 */
	public static CompBarColor fromKey(String key) {
		for (final CompBarColor mode : values())
			if (mode.key.equalsIgnoreCase(key))
				return mode;

		throw new IllegalArgumentException("No such CompBarColor: " + key + ". Available: " + Common.join(values()));
	}

	@Override
	public String toString() {
		return this.key;
	}
}