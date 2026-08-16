package org.mineacademy.fo.event;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.RequiredArgsConstructor;

/**
 * 允许插件修改事件监听优先级的简单方式
 *
 * @param <T> 我们监听的事件
 */
@RequiredArgsConstructor
public abstract class SimpleListener<T extends Event> implements Listener, EventExecutor {

	/**
	 * 我们监听的事件
	 */
	private final Class<T> eventClass;

	/**
	 * 事件优先级
	 */
	private final EventPriority priority;

	/**
	 * 是否忽略处理流程中已被取消的事件？
	 */
	private final boolean ignoreCancelled;

	/**
	 * 供部分辅助方法使用的可选玩家实现
	 */
	@Nullable
	private Player player;

	/**
	 * 使用普通优先级创建新的监听器，
	 * 并忽略已取消的事件
	 *
	 * @param event
	 */
	public SimpleListener(Class<T> event) {
		this(event, EventPriority.NORMAL);
	}

	/**
	 * 创建一个忽略已取消事件的新监听器
	 *
	 * @param event
	 * @param priority
	 */
	public SimpleListener(Class<T> event, EventPriority priority) {
		this(event, priority, true);
	}

	@Override
	public final void execute(Listener listener, Event event) throws EventException {

		if (!event.getClass().equals(this.eventClass))
			return;

		final boolean eventIgnored = event.getEventName().equals("SimpleChatEvent");
		final String logName = (listener != null ? listener.getClass().getSimpleName() + " listening to " : "") + event.getEventName() + " at " + this.priority + " priority";

		if (!eventIgnored)
			LagCatcher.start(logName);

		if (event instanceof PlayerEvent)
			this.player = ((PlayerEvent) event).getPlayer();

		else {
			try {
				final Method getPlayer = ReflectionUtil.getMethod(event.getClass(), "getPlayer");

				if (getPlayer != null)
					this.player = ReflectionUtil.invoke(getPlayer, event);
			} catch (final Throwable ignored) {
			}
		}

		try {
			this.execute(this.eventClass.cast(event));

		} catch (final EventHandledException ex) {
			final String[] messages = ex.getMessages();
			final boolean cancelled = ex.isCancelled();

			if (messages != null && this.player != null)
				for (String message : messages) {
					message = Variables.replace(message, this.player);

					if (Messenger.ENABLED)
						Messenger.error(this.player, message);
					else
						Common.tell(this.player, "&c" + message);
				}

			if (cancelled && event instanceof Cancellable)
				((Cancellable) event).setCancelled(true);

		} catch (final Throwable t) {
			Common.error(t, "Unhandled exception listening to " + this.eventClass.getSimpleName());

		} finally {
			if (!eventIgnored)
				LagCatcher.end(logName);

			// Do not null the event since this breaks findPlayer for any scheduled tasks
			//this.event = null;
		}
	}

	/**
	 * 事件运行时执行
	 *
	 * @param event
	 */
	protected abstract void execute(T event);

	/**
	 * 在你的事件中调用此方法来设置玩家。
	 *
	 * @param player
	 */
	protected final void setPlayer(Player player) {
		this.player = player;
	}

	/*
	 * Return a player from this event, null if none,
	 * used for messaging
	 */
	private Player findPlayer() {
		Valid.checkNotNull(this.player, "Call setPlayer() early in your event to set the player");

		return this.player;
	}

	/**
	 * 如果对象为 null，则停止后续代码执行，取消事件，并
	 * 向玩家发送 null 时的消息（参见 {@link #findPlayer(Event)}）
	 *
	 * @param toCheck
	 * @param falseMessages
	 */
	protected final void checkNotNull(Object toCheck, String... nullMessages) {
		this.checkBoolean(toCheck != null, nullMessages);
	}

	/**
	 * 如果条件为 false，则停止后续代码执行，取消事件，并
	 * 向玩家发送 false 时的消息（参见 {@link #findPlayer(Event)}）
	 *
	 * @param condition
	 * @param falseMessages
	 */
	protected final void checkBoolean(boolean condition, String... falseMessages) {
		if (!condition)
			throw new EventHandledException(true, falseMessages);
	}

	/**
	 * 当玩家缺少给定权限时，停止代码执行并向玩家发送消息
	 * （参见 {@link #findPlayer(Event)}）
	 *
	 * @param permission
	 */
	protected final void checkPerm(String permission) {
		this.checkPerm(permission, SimpleLocalization.NO_PERMISSION);
	}

	/**
	 * 返回 {@link #findPlayer(Event)} 找到的玩家是否拥有给定权限；
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(String permission) {
		return PlayerUtil.hasPerm(this.findPlayer(), permission);
	}

	/**
	 * 当玩家缺少给定权限时，停止代码执行并向玩家发送消息
	 * （参见 {@link #findPlayer(Event)}）
	 *
	 * @param permission
	 * @param falseMessage
	 */
	protected final void checkPerm(String permission, String falseMessage) {
		this.checkBoolean(this.findPlayer().hasPermission(permission), falseMessage.replace("{permission}", permission));
	}

	/**
	 * 取消事件并向玩家发送消息（参见 {@link #findPlayer(Event)}）
	 *
	 * @param messages
	 */
	protected final void cancel(String... messages) {
		throw new EventHandledException(true, messages);
	}

	/**
	 * 取消此事件
	 */
	protected final void cancel() {
		throw new EventHandledException(true);
	}

	/**
	 * 中止代码执行并发送消息
	 *
	 * @param messages
	 */
	protected final void returnTell(String... messages) {
		throw new EventHandledException(false, messages);
	}

	/**
	 * 在 Bukkit 中注册此事件的快捷方式
	 */
	public final void register() {
		Bukkit.getPluginManager().registerEvent(this.eventClass, this, this.priority, this, SimplePlugin.getInstance(), this.ignoreCancelled);
	}
}
