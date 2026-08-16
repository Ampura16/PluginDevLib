package org.mineacademy.fo.event;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.mineacademy.fo.model.SimpleComponent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * 当使用 {@link SimpleComponent} 类中的任意 send() 或 sendAs() 方法时触发。
 *
 * 你可以用它为每个接收者单独修改消息，
 * 为每个玩家发送不同的消息。
 *
 * ** 必须使用 {@link SimpleComponent#setFiringEvent(boolean)} 设置为 true，此事件才会被调用 **
 */
@Getter
@Setter
@AllArgsConstructor
public final class SimpleComponentSendEvent extends SimpleCancellableEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * 发送者，未知时为 null
	 */
	@Nullable
	private final CommandSender sender;

	/**
	 * 该组件的接收者
	 */
	private final CommandSender receiver;

	/**
	 * 可供你修改的组件化消息
	 */
	private TextComponent component;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}