package org.mineacademy.fo.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.event.SimpleListener;

/**
 * 一种注册事件及其他内容的简单方式，
 * 这些内容会在插件重载时自动取消。
 */
final class Reloadables {

	/**
	 * 当前已启用的事件监听器列表
	 */
	private final StrictList<Listener> listeners = new StrictList<>();

	/**
	 * 已注册的命令组列表
	 */
	private final StrictList<SimpleCommandGroup> commandGroups = new StrictList<>();

	// -------------------------------------------------------------------------------------------
	// Main
	// -------------------------------------------------------------------------------------------

	/**
	 * 移除所有监听器并取消所有正在运行的任务
	 */
	void reload() {
		for (final Listener listener : this.listeners)
			HandlerList.unregisterAll(listener);

		this.listeners.clear();

		for (final SimpleCommandGroup commandGroup : this.commandGroups)
			commandGroup.unregister();

		this.commandGroups.clear();
	}

	// -------------------------------------------------------------------------------------------
	// Events / Listeners
	// -------------------------------------------------------------------------------------------

	/**
	 * 向 Bukkit 注册事件
	 *
	 * @param listener
	 */
	void registerEvents(Listener listener) {
		Common.registerEvents(listener);

		this.listeners.add(listener);
	}

	/**
	 * 使用我们的监听器向 Bukkit 注册事件
	 *
	 * @param <T>
	 * @param listener
	 */
	<T extends Event> void registerEvents(SimpleListener<T> listener) {
		listener.register();

		this.listeners.add(listener);
	}

	// -------------------------------------------------------------------------------------------
	// Command groups
	// -------------------------------------------------------------------------------------------

	/**
	 * 注册给定的命令组
	 *
	 * @param label
	 * @param aliases
	 * @param group
	 */
	void registerCommands(final SimpleCommandGroup group) {
		group.register();

		this.commandGroups.add(group);
	}
}
