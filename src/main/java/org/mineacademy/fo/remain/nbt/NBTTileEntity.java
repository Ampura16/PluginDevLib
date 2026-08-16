package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.mineacademy.fo.Valid;

/**
 * 用于访问方块实体（TileEntity）原版标签的 NBT 类。方块实体不
 * 支持自定义标签，自定义标签请使用 NBTInjector。更改会
 * 立即应用到方块实体上，使用 merge 方法可一次完成
 * 多项操作。
 *
 * @author tr7zw
 */
public class NBTTileEntity extends NBTCompound {

	private final BlockState tile;
	private final boolean readonly;
	private final Object compound;
	private boolean closed = false;

	/**
	 * @param tile     任意方块实体的 BlockState
	 * @param readonly 只读模式会在初始化时创建副本，并只从该副本读取
	 */
	protected NBTTileEntity(BlockState tile, boolean readonly) {
		super(null, null);
		if (tile == null || MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3) && !tile.isPlaced())
			throw new NullPointerException("Tile can't be null/not placed!");
		this.tile = tile;
		this.readonly = readonly;
		if (readonly)
			this.compound = this.getCompound();
		else
			this.compound = null;
	}

	/**
	 * 已弃用：请使用 NBT 类
	 *
	 * @param tile 任意方块实体的 BlockState
	 */
	@Deprecated
	public NBTTileEntity(BlockState tile) {
		super(null, null);
		if (tile == null || MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_8_R3) && !tile.isPlaced())
			throw new NullPointerException("Tile can't be null/not placed!");
		this.readonly = false;
		this.compound = null;
		this.tile = tile;
	}

	@Override
	protected void setClosed() {
		this.closed = true;
	}

	@Override
	protected boolean isClosed() {
		return this.closed;
	}

	@Override
	protected boolean isReadOnly() {
		return this.readonly;
	}

	@Override
	public Object getCompound() {
		// this runs before async check, since it's just a copy
		if (this.readonly && this.compound != null)
			return this.compound;
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("BlockEntity NBT needs to be accessed sync!");
		return NBTReflectionUtil.getTileEntityNBTTagCompound(this.tile);
	}

	@Override
	protected void setCompound(Object compound) {
		if (this.readonly)
			throw new NbtApiException("Tried setting data in read only mode!");
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("BlockEntity NBT needs to be accessed sync!");
		NBTReflectionUtil.setTileEntityNBTTagCompound(this.tile, compound);
	}

	/**
	 * 获取 Spigot 的 PersistentDataAPI 所使用的 NBTCompound。此方法仅
	 * 在 1.14+ 可用！
	 *
	 * @return 包含 PersistentDataAPI 数据的 NBTCompound
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_14_R1), "PersistentDataContainer is only available for 1.14+!");

		if (this.hasTag("PublicBukkitValues"))
			return this.getCompound("PublicBukkitValues");
		else {
			final NBTContainer container = new NBTContainer();
			container.addCompound("PublicBukkitValues").setString("__nbtapi",
					"Marker to make the PersistentDataContainer have content");
			this.mergeCompound(container);
			return this.getCompound("PublicBukkitValues");
		}
	}

}
