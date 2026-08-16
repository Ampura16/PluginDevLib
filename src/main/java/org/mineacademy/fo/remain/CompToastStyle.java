package org.mineacademy.fo.remain;

import org.mineacademy.fo.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 表示 Toast 通知中出现的不同首屏样式，
 * 可在 {@link Remain#sendToast(org.bukkit.entity.Player, String, CompMaterial, CompToastStyle)} 中使用
 */
@RequiredArgsConstructor
public enum CompToastStyle {

	TASK("task"),
	GOAL("goal"),
	CHALLENGE("challenge");

	@Getter
	private final String key;

	/**
	 * 尝试根据给定的键加载 CompToastStyle
	 *
	 * @param key
	 * @return
	 */
	public static CompToastStyle fromKey(String key) {
		for (final CompToastStyle style : values())
			if (style.key.equalsIgnoreCase(key))
				return style;

		throw new IllegalArgumentException("No such CompToastStyle '" + key + "'. Available: " + Common.join(values()));
	}

	@Override
	public String toString() {
		return this.key;
	}
}
