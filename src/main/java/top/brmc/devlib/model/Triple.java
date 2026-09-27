package top.brmc.devlib.model;

import lombok.Data;

/**
 * 用于存放三个值的简单容器
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
@Data
public final class Triple<A, B, C> {

	/**
	 * 存放的第一个值
	 */
	private final A first;

	/**
	 * 存放的第二个值
	 */
	private final B second;

	/**
	 * 存放的第三个值
	 */
	private final C third;
}