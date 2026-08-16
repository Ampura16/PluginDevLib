package org.mineacademy.fo.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.mineacademy.fo.menu.Menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 玩家关闭菜单时，在处理流程的最末尾触发。
 */
@Getter
@RequiredArgsConstructor
public final class MenuCloseEvent extends SimpleCancellableEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * 菜单。使用 {@link #getDrawer()} 编辑菜单物品的外观。
	 */
	private final Menu menu;

	/**
	 * 关闭时背包界面的样子
	 */
	private final Inventory inventory;

	/**
	 * 玩家
	 */
	private final Player player;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}