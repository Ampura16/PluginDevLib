package top.brmc.devlib.menu.tool;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * 只能对方块使用的工具的辅助类
 */
public abstract class BlockTool extends Tool {

	/**
	 * 与此工具相关的点击事件
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private PlayerInteractEvent event;

	@Override
	protected final void onBlockClick(final PlayerInteractEvent event) {
		this.event = event;

		final Player player = event.getPlayer();
		final Block block = event.getClickedBlock();

		final Action action = event.getAction();

		if (action == Action.RIGHT_CLICK_BLOCK)
			this.onBlockClick(player, ClickType.RIGHT, block);

		else if (action == Action.LEFT_CLICK_BLOCK)
			this.onBlockClick(player, ClickType.LEFT, block);

		else if (action == Action.RIGHT_CLICK_AIR)
			this.onAirClick(player, ClickType.RIGHT);

		else if (action == Action.LEFT_CLICK_AIR)
			this.onAirClick(player, ClickType.LEFT);
	}

	/**
	 * 当手持此工具的玩家点击方块时自动调用。
	 * 此处的 {@link ClickType} 只能是 RIGHT 或 LEFT。
	 *
	 * @param player
	 * @param click
	 * @param block
	 */
	protected abstract void onBlockClick(Player player, ClickType click, Block block);

	/**
	 * 当玩家点击空气时自动调用
	 *
	 * @param player
	 * @param click
	 */
	protected void onAirClick(final Player player, final ClickType click) {
	}

	/**
	 * 监听点击空气以调用 onAirClick
	 *
	 * @see top.brmc.devlib.menu.tool.Tool#ignoreCancelled()
	 */
	@Override
	protected boolean ignoreCancelled() {
		return false;
	}
}
