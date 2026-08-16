package org.mineacademy.fo.bungee.message;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.bungee.BungeeMessageType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 代表带给定动作和服务器名的输入/输出消息，
 * 读写数据时按动作
 * 内容做安全检查。
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class Message {

	/**
	 * 与此消息关联的监听器
	 */
	private final BungeeListener listener;

	/**
	 * 该动作
	 */
	private final BungeeMessageType action;

	/**
	 * 基于 {@link BungeeMessageType#getContent()} 的数据写入
	 * 当前位置
	 */
	private int actionHead = 0;

	/**
	 * 确保我们按给定 {@link BungeeMessageType} 在其
	 * {@link BungeeMessageType#getContent()} 获取器中指定的顺序读取。
	 * <p>
	 * 这也确保我们读取正确的数据类型（原生和包装类型
	 * 都支持）。
	 *
	 * @param typeOf
	 */
	protected final void moveHead(Class<?> typeOf) {
		Valid.checkNotNull(this.action, "Action not set!");

		final Class<?>[] content = this.action.getContent();
		Valid.checkBoolean(this.actionHead < content.length, "Head out of bounds! Max data size for " + this.action.name() + " is " + content.length);

		this.actionHead++;
	}

	/**
	 *
	 * @return
	 */
	public final BungeeListener getListener() {
		return listener;
	}

	/**
	 * @param <T>
	 * @return
	 */
	public final <T extends BungeeMessageType> T getAction() {
		return (T) action;
	}
}
