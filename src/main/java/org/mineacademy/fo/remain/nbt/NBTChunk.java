package org.mineacademy.fo.remain.nbt;

import org.bukkit.Chunk;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

/**
 * 用于将 NBT 数据存储到 {@link Chunk} 的 PDC（持久化数据容器）中的
 * 辅助类。
 *
 * @deprecated 请使用 {@link NBT} 类中的方法读取/修改区块的 nbt
 */
@Deprecated
public class NBTChunk {

	private final Chunk chunk;

	public NBTChunk(Chunk chunk) {
		this.chunk = chunk;
	}

	/**
	 * 获取 Spigot 的 PersistentDataAPI 所使用的 NBTCompound。此方法仅
	 * 在 1.16.4+ 可用！
	 *
	 * @return 包含 PersistentDataAPI 数据的 NBTCompound
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(org.mineacademy.fo.MinecraftVersion.atLeast(V.v1_16), "PersistentDataContainer is only available for 1.16.4+!");

		return new NBTPersistentDataContainer(this.chunk.getPersistentDataContainer());
	}

}
