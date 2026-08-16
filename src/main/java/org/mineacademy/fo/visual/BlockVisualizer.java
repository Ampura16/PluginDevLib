package org.mineacademy.fo.visual;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * 用于显示发光方块边角的工具类。
 */
@UtilityClass
public class BlockVisualizer {

	/**
	 * 存放当前正在可视化的方块的映射。
	 */
	private final StrictMap<Location, Object /*Old Minecraft compatibility.*/> visualizedBlocks = new StrictMap<>();

	/**
	 * 开始可视化给定位置的方块。
	 *
	 * @param block
	 * @param mask
	 * @param blockName
	 */
	public void visualize(@NonNull final Block block, final CompMaterial mask, final String blockName) {
		Valid.checkBoolean(!isVisualized(block), "Block at " + block.getLocation() + " already visualized");
		final Location location = block.getLocation();

		final FallingBlock falling = spawnFallingBlock(location, mask, blockName);

		// Also send the block change packet to barrier (fixes lightning glitches)
		for (final Player player : block.getWorld().getPlayers())
			Remain.sendBlockChange(2, player, location, MinecraftVersion.olderThan(V.v1_9) ? mask : CompMaterial.BARRIER);

		visualizedBlocks.put(location, falling == null ? false : falling);
	}

	/*
	 * Spawns a customized falling block at the given location.
	 */
	private FallingBlock spawnFallingBlock(final Location location, final CompMaterial mask, final String blockName) {
		if (MinecraftVersion.olderThan(V.v1_9))
			return null;

		final FallingBlock falling = Remain.spawnFallingBlock(location.clone().add(0.5, 0, 0.5), mask.getMaterial());

		falling.setDropItem(false);
		falling.setVelocity(new Vector(0, 0, 0));

		Remain.setCustomName(falling, blockName);

		CompProperty.GLOWING.apply(falling, true);
		CompProperty.GRAVITY.apply(falling, false);

		return falling;
	}

	/**
	 * 停止可视化给定位置的方块。
	 *
	 * @param block
	 */
	public void stopVisualizing(@NonNull final Block block) {
		Valid.checkBoolean(isVisualized(block), "Block at " + block.getLocation() + " not visualized");

		final Object fallingBlock = visualizedBlocks.remove(block.getLocation());

		// Mark the entity for removal on the next tick
		if (fallingBlock instanceof FallingBlock)
			((FallingBlock) fallingBlock).remove();

		// Then restore the client's block back to normal
		for (final Player player : block.getWorld().getPlayers())
			Remain.sendBlockChange(1, player, block);
	}

	/**
	 * 停止所有方块的可视化。
	 */
	public void stopAll() {
		for (final Location location : new HashSet<>(visualizedBlocks.keySet())) {
			final Block block = location.getBlock();

			if (isVisualized(block))
				stopVisualizing(block);
		}
	}

	/**
	 * 若给定方块当前正在被可视化则返回 true。
	 *
	 * @param block
	 * @return
	 */
	public boolean isVisualized(@NonNull final Block block) {
		return visualizedBlocks.containsKey(block.getLocation());
	}
}
