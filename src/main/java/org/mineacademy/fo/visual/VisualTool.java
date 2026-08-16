package org.mineacademy.fo.visual;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.tool.BlockTool;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 可以在竞技场中可视化方块选区的类
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class VisualTool extends BlockTool {

	/**
	 * 处理方块点击，并自动刷新已可视化方块的渲染
	 *
	 * @param player
	 * @param click
	 * @param block
	 */
	@Override
	protected final void onBlockClick(final Player player, final ClickType click, final Block block) {
		// Remove old blocks
		this.stopVisualizing(player);

		// Call the block handling, probably new blocks will appear
		this.handleBlockClick(player, click, block);

		// Render the new blocks
		this.visualize(player);
	}

	/**
	 * 处理方块点击。此处的任何更改都会自动反映到可视化中
	 * 如果你想保存所选区域或所点击的方块，需要覆盖此方法！
	 *
	 * @param player
	 * @param click
	 * @param block
	 */
	protected void handleBlockClick(Player player, ClickType click, Block block) {
		final boolean isPrimary = click == ClickType.LEFT;
		final Location location = block.getLocation();

		final Region region = this.getVisualizedRegion(player);

		if (region != null) {

			// If you place primary location over a secondary location point, remove secondary
			if (!isPrimary && region.hasPrimary() && region.isPrimary(location))
				region.setPrimary(null);

			// ...and vice versa
			if (isPrimary && region.hasSecondary() && region.isSecondary(location))
				region.setSecondary(null);

			final boolean removed = !region.toggleLocation(location, click);
			Messenger.success(player, (isPrimary ? "&cPrimary" : "&6Secondary") + " &7location has been " + (removed ? "&cremoved" : "&2set") + "&7.");
		}
	}

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onAirClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected final void onAirClick(final Player player, final ClickType click) {
		// Remove old blocks
		this.stopVisualizing(player);

		// Call the block handling, probably new blocks will appear
		this.handleAirClick(player, click);

		// Render the new blocks
		this.visualize(player);
	}

	/**
	 * 处理点击空气，并自动更新可视化
	 *
	 * @param player
	 * @param click
	 */
	protected void handleAirClick(final Player player, final ClickType click) {
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#onHotbarFocused(org.bukkit.entity.Player)
	 */
	@Override
	protected void onHotbarFocused(final Player player) {
		this.visualize(player);
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#onHotbarDefocused(org.bukkit.entity.Player)
	 */
	@Override
	protected void onHotbarDefocused(final Player player) {
		this.stopVisualizing(player);
	}

	/**
	 * 返回此可视化中应渲染的点列表或单个点
	 *
	 * @param player
	 *
	 * @return
	 */
	protected List<Location> getVisualizedPoints(Player player) {
		final Region region = this.getVisualizedRegion(player);
		final List<Location> points = new ArrayList<>();

		if (region != null) {
			if (region.hasPrimary())
				points.add(region.getPrimary());

			if (region.hasSecondary())
				points.add(region.getSecondary());
		}

		return points;
	}

	/**
	 * 返回此工具应在其周围绘制粒子的区域
	 *
	 * @param player
	 *
	 * @return
	 */
	protected VisualizedRegion getVisualizedRegion(Player player) {
		return null;
	}

	/**
	 * 根据给定参数返回发光方块上方显示的名称
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	protected String getBlockName(Block block, Player player) {
		final Region region = this.getVisualizedRegion(player);
		String name = "&7Point";

		if (region != null) {
			final Location location = block.getLocation();

			name = region.isPrimary(location) ? "&cPrimary" : region.isSecondary(location) ? "&6Secondary" : name;
		}

		return "&8[" + name + "&8]";
	}

	/**
	 * 根据给定参数返回方块遮罩
	 *
	 * @param block
	 * @param player
	 * @return
	 */
	protected abstract CompMaterial getBlockMask(Block block, Player player);

	/**
	 * 返回可应用到物品上的示例描述
	 *
	 * @return
	 */
	protected final String[] getItemLore() {
		return new String[] {
				"",
				"&6&l<- &7(left) Primary",
				"Secondary (right) &6&l->",
				"",
				"Click a block to set."
		};
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}

	/*
	 * Visualize the region and points if exist
	 */
	private void visualize(@NonNull final Player player) {
		final VisualizedRegion region = this.getVisualizedRegion(player);

		if (region != null && region.isWhole())
			if (!region.canSeeParticles(player))
				region.showParticles(player);

		for (final Location location : this.getVisualizedPoints(player)) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (!BlockVisualizer.isVisualized(block))
				BlockVisualizer.visualize(block, this.getBlockMask(block, player), this.getBlockName(block, player));
		}
	}

	/*
	 * Stop visualizing region and points if they were so before
	 */
	private void stopVisualizing(@NonNull final Player player) {
		final VisualizedRegion region = this.getVisualizedRegion(player);

		if (region != null && region.canSeeParticles(player))
			region.hideParticles(player);

		for (final Location location : this.getVisualizedPoints(player)) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (BlockVisualizer.isVisualized(block))
				BlockVisualizer.stopVisualizing(block);
		}
	}
}
