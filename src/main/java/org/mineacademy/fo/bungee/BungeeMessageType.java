package org.mineacademy.fo.bungee;

/**
 * 代表通过 BungeeCord 发送的动作，包含
 * 一组数据。建议你创建实现此接口的枚举。
 */
public interface BungeeMessageType {

	/**
	 * 按发送顺序存储此动作中的所有有效值。
	 * 仅支持原生类型、UUID、SerializedMap 和 String。
	 *
	 * @return
	 */
	Class<?>[] getContent();

	/**
	 * 此动作的名称
	 *
	 * @return
	 */
	String name();

	/**
	 * 按名称检索 BungeeAction
	 *
	 * @param listener
	 * @param name
	 *
	 * @return
	 */
	static BungeeMessageType getByName(BungeeListener listener, String name) {
		final BungeeMessageType[] actions = listener.getActions();

		for (final BungeeMessageType action : actions)
			if (action.name().equals(name))
				return action;

		return null;
	}
}
