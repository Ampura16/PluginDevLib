package org.mineacademy.fo.exception;

import lombok.Getter;

/**
 * 表示处理事件时抛出的静默异常，
 * 它只会给事件相关的玩家发送一条消息
 */
public final class EventHandledException extends CommandException {

	private static final long serialVersionUID = 1L;

	/**
	 * 是否应取消该事件？
	 */
	@Getter
	private final boolean cancelled;

	public EventHandledException() {
		this(true);
	}

	/**
	 * 创建一个新的异常，附带发送给命令发送者的消息
	 *
	 * @param cancelled
	 * @param messages
	 */
	public EventHandledException(boolean cancelled, String... messages) {
		super(messages);

		this.cancelled = cancelled;
	}
}
