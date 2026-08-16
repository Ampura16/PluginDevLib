package org.mineacademy.fo.menu.tool;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;

/**
 * 表示一个工具。工具是在插件中注册、
 * 会自动触发事件的简单 ItemStack
 */
public abstract class Tool {

	/**
	 * 已注册的工具
	 */
	private static final Collection<Tool> tools = new ConcurrentLinkedQueue<>();

	/**
	 * 添加一个要注册的新工具。
	 * <p>
	 * 自动调用。
	 *
	 * @param tool 工具
	 */
	static void register(Tool tool) {
		Valid.checkBoolean(!isRegistered(tool), "Tool with itemstack " + tool.getItem() + " already registered");

		tools.add(tool);
	}

	/**
	 * 检查该工具是否已注册
	 *
	 * @param tool 工具
	 * @return 工具已注册时返回 true
	 */
	static boolean isRegistered(Tool tool) {
		return getTool(tool.getItem()) != null;
	}

	/**
	 * 尝试根据给定物品堆查找已注册的工具
	 *
	 * @param item 物品
	 * @return 对应的工具，或 null
	 */
	public static Tool getTool(ItemStack item) {
		for (final Tool t : tools)
			if (t.isTool(item))
				return t;

		return null;
	}

	/**
	 * 获取所有工具
	 *
	 * @return 已注册工具的数组
	 */
	public static Tool[] getTools() {
		return tools.toArray(new Tool[tools.size()]);
	}

	// -------------------------------------------------------------------------------------------
	// Main class implementation
	// -------------------------------------------------------------------------------------------

	/**
	 * 创建一个新工具
	 */
	protected Tool() {

		// A hacky way of automatically registering it AFTER the parent constructor, assuming all went okay
		new Thread(() -> {

			try {
				Thread.sleep(3);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

			// Sync to main thread
			Common.runLater(() -> {
				final Tool instance = Tool.this;

				if (!isRegistered(instance))
					register(instance);
			});

		}).start();
	}

	/**
	 * 判断给定物品堆是否为此工具
	 *
	 * @param item 物品堆
	 * @return 若给定物品堆就是此工具则返回 true
	 */
	public final boolean isTool(final ItemStack item) {
		return ItemUtil.isSimilar(this.getItem(), item);
	}

	/**
	 * 若给定玩家主手持有此工具则返回 true
	 *
	 * @param player
	 * @return
	 */
	public final boolean hasToolInHand(final Player player) {
		return this.isTool(player.getItemInHand());
	}

	/**
	 * 若玩家已拥有此工具则返回 true
	 *
	 * @param player
	 * @return
	 */
	public final boolean hasTool(Player player) {
		for (final ItemStack item : player.getInventory().getContents())
			if (this.isTool(item))
				return true;

		return false;
	}

	/**
	 * 获取工具物品
	 * <p>
	 * 提示：使用 {@link ItemCreator}
	 *
	 * @return 工具物品
	 */
	public abstract ItemStack getItem();

	/**
	 * 点击工具时自动调用
	 *
	 * @param event 事件
	 */
	protected void onBlockClick(PlayerInteractEvent event) {
	}

	/**
	 * 使用此工具放置方块时自动调用
	 *
	 * @param event
	 */
	protected void onBlockPlace(BlockPlaceEvent event) {
	}

	/**
	 * 当玩家切换快捷栏物品且新槽位为
	 * 此工具时调用。
	 *
	 * @param player 玩家
	 */
	protected void onHotbarFocused(final Player player) {
	}

	/**
	 * 当工具在快捷栏中失去焦点时调用
	 *
	 * @param player 玩家
	 */
	protected void onHotbarDefocused(final Player player) {
	}

	/**
	 * 即使事件已被取消，是否仍触发 {@link #onBlockClick(PlayerInteractEvent)}？
	 * <p>
	 * 默认为 true。若你想捕获点击空气，请设为 false。
	 *
	 * @return 若点击事件已被取消时应忽略它，则返回 true
	 */
	protected boolean ignoreCancelled() {
		return true;
	}

	/**
	 * 便捷方法：是否应自动取消
	 * {@link PlayerInteractEvent}？
	 *
	 * @return 若交互事件应被自动取消则返回 true，默认为
	 * false
	 */
	protected boolean autoCancel() {
		return false;
	}

	/**
	 * 如果玩家还没有此工具，则将其给予玩家
	 *
	 * @param player
	 * @return 已给予工具时返回 true，玩家已拥有时返回 false
	 */
	public final boolean giveIfHasnt(Player player) {
		if (this.hasTool(player))
			return false;

		this.give(player);
		return true;
	}

	/**
	 * 将此工具快速放到玩家背包指定槽位的便捷方法
	 *
	 * @param player
	 * @param slot
	 */
	public final void give(final Player player, final int slot) {
		player.getInventory().setItem(slot, this.getItem());
	}

	/**
	 * 将此工具快速添加到玩家背包的便捷方法
	 *
	 * @param player
	 */
	public final void give(final Player player) {
		player.getInventory().addItem(this.getItem());
	}

	/**
	 * 若比较对象是具有相同 {@link #getItem()} 的工具则返回 true
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public final boolean equals(final Object obj) {
		return obj instanceof Tool && ((Tool) obj).getItem().equals(this.getItem());
	}
}
