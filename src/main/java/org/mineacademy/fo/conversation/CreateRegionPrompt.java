package org.mineacademy.fo.conversation;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.RegionMenu;
import org.mineacademy.fo.menu.tool.RegionTool;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.NonNull;

/**
 * 用于为区域命名以便保存。
 */
public class CreateRegionPrompt extends CreatePrompt<DiskRegion> {

	/**
	 * 正在为其命名的区域
	 */
	private final VisualizedRegion region;

	/*
	 * Create prompt
	 */
	private CreateRegionPrompt(@NonNull VisualizedRegion region) {
		super("region");

		this.region = region;
	}

	@Override
	protected String findByName(String name) {
		final DiskRegion region = DiskRegion.findRegion(name);

		return region != null ? region.getName() : null;
	}

	@Override
	protected DiskRegion create(String name) {
		return DiskRegion.createRegion(name, this.region.clone());
	}

	@Override
	protected void onCreateFinish(Player player, DiskRegion createdItem) {
		RegionMenu.showTo(player, createdItem);
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	/**
	 * 显示提示；如果玩家背包中没有工具，则给予该工具。
	 *
	 * @param player
	 */
	public static void showToOrHint(Player player) {
		final VisualizedRegion region = DiskRegion.getCreatedRegion(player);

		if (!region.isWhole()) {
			final boolean gaveTool = RegionTool.getInstance().giveIfHasnt(player);
			Messenger.warn(player, "Before adding a region, select primary and secondary points first." + (gaveTool ? " You were given a tool to do this." : ""));

			player.closeInventory();

			return;
		}

		if (player.isConversing()) {
			Messenger.error(player, "You have a pending chat conversation, type 'exit' to stop it before creating a new region.");

			return;
		}

		new CreateRegionPrompt(region).show(player);
	}
}
