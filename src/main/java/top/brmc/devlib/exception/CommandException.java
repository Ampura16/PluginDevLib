package top.brmc.devlib.exception;

import lombok.Getter;

/**
 * 表示处理命令时抛出的静默异常，
 * 它只会给命令发送者发送一条消息
 */
public class CommandException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 要发送给命令发送者的消息
	 */
	@Getter
	private final String[] messages;

	/**
	 * 创建一个新的命令异常，附带发送给命令发送者的消息
	 *
	 * @param messages
	 */
	public CommandException(String... messages) {
		super("");

		this.messages = messages;
	}
}
