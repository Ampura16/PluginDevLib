package org.mineacademy.fo.exception;

import org.mineacademy.fo.debug.Debugger;

import lombok.Getter;
import lombok.Setter;

/**
 * 表示我们的核心异常。所有此类异常
 * 都会自动记录到 error.log 文件中
 */
public class FoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 抛出异常时是否自动将其保存到 error.log 文件？
	 */
	@Getter
	@Setter
	private static boolean errorSavedAutomatically = true;

	/**
	 * 创建一个新异常并记录它
	 *
	 * @param t
	 */
	public FoException(Throwable t) {
		super(t);

		if (errorSavedAutomatically)
			Debugger.saveError(t);
	}

	/**
	 * 创建一个新异常并记录它
	 * @deprecated 这是为兼容 Foundation v7 而保留的过渡方法。
	 *
	 * @param t
	 * @param ignoredParam
	 */
	@Deprecated
	public FoException(Throwable t, boolean ignoredParam) {
		super(t);

		if (errorSavedAutomatically)
			Debugger.saveError(t);
	}

	/**
	 * 创建一个新异常并记录它
	 *
	 * @param message
	 */
	public FoException(String message) {
		super(message);

		if (errorSavedAutomatically)
			Debugger.saveError(this, message);
	}

	/**
	 * 创建一个新异常并记录它
	 *
	 * @deprecated 这是为兼容 Foundation v7 而保留的过渡方法。
	 * @param message
	 * @param ignoredParam
	 */
	@Deprecated
	public FoException(String message, boolean ignoredParam) {
		super(message);

		if (errorSavedAutomatically)
			Debugger.saveError(this, message);
	}

	/**
	 * 创建一个新异常并记录它
	 *
	 * @param message
	 * @param t
	 */
	public FoException(String message, Throwable t) {
		this(t, message);
	}

	/**
	 * 创建一个新异常并记录它
	 *
	 * @param message
	 * @param t
	 */
	public FoException(Throwable t, String message) {
		super(message, t);

		if (errorSavedAutomatically)
			Debugger.saveError(t, message);
	}

	/**
	 * 创建一个新异常并记录它
	 */
	public FoException() {

		if (errorSavedAutomatically)
			Debugger.saveError(this);
	}

	@Override
	public String getMessage() {
		return "Report: " + super.getMessage();
	}
}