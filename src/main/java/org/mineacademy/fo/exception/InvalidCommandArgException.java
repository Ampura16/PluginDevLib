package org.mineacademy.fo.exception;

/**
 * 当命令参数无效时抛出
 */
public final class InvalidCommandArgException extends CommandException {

	private static final long serialVersionUID = 1L;

	public InvalidCommandArgException() {
	}

	public InvalidCommandArgException(String message) {
		super(message);
	}
}