package top.brmc.devlib.exception;

import lombok.Getter;

/**
 * 当我们从数据文件（以 .db 结尾的文件）加载数据，但其中某个位置所在的世界
 * 已不存在时抛出
 */
public final class InvalidWorldException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 无效的世界
	 */
	@Getter
	private final String world;

	public InvalidWorldException(String message, String world) {
		super(message);

		this.world = world;
	}
}