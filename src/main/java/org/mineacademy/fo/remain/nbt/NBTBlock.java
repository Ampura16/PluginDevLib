package org.mineacademy.fo.remain.nbt;

import org.bukkit.block.Block;

/**
 * 用于将 NBT 数据存储到方块位置的辅助类。使用 getData() 获取
 * NBT 实例。重要说明：
 * <p>
 * - 非方块实体（BlockEntity）不能拥有 NBT 数据。此类会改为将数据存储到
 * 区块中！
 * <p>
 * - 数据实际上只绑定在位置上。如果方块被
 * 破坏/改变/炸毁/移动等，数据仍然留在该位置！
 *
 * @author tr7zw
 * @deprecated 请使用 {@link NBT} 类中的方法读取/修改方块的 NBT
 */
@Deprecated
public class NBTBlock {

	private final Block block;
	private final NBTChunk nbtChunk;

	public NBTBlock(Block block) {
		this.block = block;
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R3))
			throw new NbtApiException("NBTBlock is only working for 1.16.4+!");
		this.nbtChunk = new NBTChunk(block.getChunk());
	}

	/**
	 * 获取存储在区块 PDC 中的方块 NBT 数据。
	 *
	 * @return 方块 NBT 数据
	 * @deprecated 请使用 {@link NBT} 类中的方法读取/修改方块的 NBT
	 */
	@Deprecated
	public NBTCompound getData() {
		return this.nbtChunk.getPersistentDataContainer().getOrCreateCompound("blocks")
				.getOrCreateCompound(this.block.getX() + "_" + this.block.getY() + "_" + this.block.getZ());
	}

}
