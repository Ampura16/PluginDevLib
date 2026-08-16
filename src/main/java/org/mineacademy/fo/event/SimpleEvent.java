package org.mineacademy.fo.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * Bukkit 事件类的简单扩展，用于支持额外的功能，
 * 目前包括：
 * <ul>
 *   <li>让事件在同步和异步场景下都能正常工作而不报错</li>
 * </ul>
 */
public abstract class SimpleEvent extends Event {

	protected SimpleEvent() {
		// Since 1.14 Spigot has implemented a safety checks for events
		// and will throw errors when an event is fired async and not declared so
		//
		// This will automatically declare the event sync/async based off what thread it is fired from
		// see https://github.com/PaperMC/Paper/issues/2099
		super(!Bukkit.isPrimaryThread());
	}

	/**
	 * 创建一个新事件，并指明它是否在
	 * Minecraft 服务器主线程上运行
	 *
	 * @param async
	 */
	protected SimpleEvent(boolean async) {
		super(async);
	}
}
