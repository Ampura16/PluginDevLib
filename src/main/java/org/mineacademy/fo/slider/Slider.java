package org.mineacademy.fo.slider;

/**
 * 表示用于为文本或物品制作动画的滑块。
 *
 * 滑块接收一个物品列表（或一个字符串），
 * 然后通过 {@link #next()} 方法将它们按预设方向移动。
 * @param <T>
 */
public interface Slider<T> {

	/**
	 * 移动到列表中的下一项。
	 *
	 * @return
	 */
	T next();
}
