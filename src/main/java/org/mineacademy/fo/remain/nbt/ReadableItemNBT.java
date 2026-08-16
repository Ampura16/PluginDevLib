package org.mineacademy.fo.remain.nbt;

public interface ReadableItemNBT extends ReadableNBT {

	/**
	 * 若该物品带有 NBT 数据则返回 true。
	 *
	 * @return 该 ItemStack 是否带有 NBTCompound。
	 */
	boolean hasNBTData();

}
