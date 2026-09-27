package top.brmc.devlib.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import top.brmc.devlib.menu.Menu;
import top.brmc.devlib.menu.model.InventoryDrawer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 为玩家打开菜单时触发
 */
@Getter
@RequiredArgsConstructor
public final class MenuOpenEvent extends SimpleCancellableEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * 菜单。使用 {@link #getDrawer()} 编辑菜单物品的外观。
	 */
	private final Menu menu;

	/**
	 * 包含准备为玩家渲染的物品的绘制器
	 * 用它来编辑菜单的外观
	 */
	private final InventoryDrawer drawer;

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