package org.mineacademy.fo.exception;

import lombok.Getter;

/**
 * 当我们检查正则表达式（参见 {@link org.mineacademy.fo.Common#regExMatch(java.util.regex.Matcher)}）
 * 且匹配耗时超过给定上限时抛出
 */
@Getter
public final class RegexTimeoutException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 正在检查的消息
	 */
	private final String checkedMessage;

	/**
	 * 执行时间上限（毫秒）
	 */
	private final long executionLimit;

	public RegexTimeoutException(CharSequence checkedMessage, long timeoutLimit) {
		this.checkedMessage = checkedMessage.toString();
		this.executionLimit = timeoutLimit;
	}
}