package org.mineacademy.fo.remain.nbt;

/**
 * NBTAPI 中大多数方法都可能抛出的通用 {@link RuntimeException}。
 *
 * @author tr7zw
 */
public class NbtApiException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -993309714559452334L;
	/**
	 * 记录插件自检的状态。null = 未检查
	 * （silentquickstart/shaded），true = 自检失败，false = 一切
	 * 本应正常，但显然并非如此？
	 */
	public static Boolean confirmedBroken = null;

	/**
	 *
	 */
	public NbtApiException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NbtApiException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public NbtApiException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public NbtApiException(Throwable cause) {
		super(cause == null ? null : cause.toString(), cause);
	}
}
