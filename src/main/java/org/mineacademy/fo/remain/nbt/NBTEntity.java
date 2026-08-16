package org.mineacademy.fo.remain.nbt;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

/**
 * 用于访问实体原版标签的 NBT 类。实体不支持自定义
 * 标签，自定义标签请使用 NBTInjector。更改会立即应用
 * 到实体上，使用 merge 方法可一次完成多项操作。
 *
 * @author tr7zw
 */
public class NBTEntity extends NBTCompound {

	private final Entity ent;
	private final boolean readonly;
	private final Object compound;
	private boolean closed = false;

	/**
	 * @param entity   任意有效的 Bukkit 实体
	 * @param readonly 只读模式会在初始化时创建副本，并只从该副本读取
	 */
	protected NBTEntity(Entity entity, boolean readonly) {
		super(null, null);
		if (entity == null)
			throw new NullPointerException("Entity can't be null!");
		this.readonly = readonly;
		this.ent = entity;
		if (readonly)
			this.compound = this.getCompound();
		else
			this.compound = null;
	}

	/**
	 * 已弃用：请使用 NBT 类
	 *
	 * @param entity 任意有效的 Bukkit 实体
	 */
	@Deprecated
	public NBTEntity(Entity entity) {
		super(null, null);
		if (entity == null)
			throw new NullPointerException("Entity can't be null!");
		this.readonly = false;
		this.compound = null;
		this.ent = entity;
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
			throw new NbtApiException("Entity NBT needs to be accessed sync!");
		return NBTReflectionUtil.getEntityNBTTagCompound(NBTReflectionUtil.getNMSEntity(this.ent));
	}

	@Override
	protected void setCompound(Object compound) {
		if (this.readonly)
			throw new NbtApiException("Tried setting data in read only mode!");
		if (!Bukkit.isPrimaryThread())
			throw new NbtApiException("Entity NBT needs to be accessed sync!");
		NBTReflectionUtil.setEntityNBTTag(compound, NBTReflectionUtil.getNMSEntity(this.ent));
	}

	/**
	 * 获取 Spigot 的 PersistentDataAPI 所使用的 NBTCompound。此方法仅
	 * 在 1.14+ 可用！
	 *
	 * @return 包含 PersistentDataAPI 数据的 NBTCompound
	 */
	public NBTCompound getPersistentDataContainer() {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_14), "PersistentDataContainer is only available for 1.14+!");

		return new NBTPersistentDataContainer(this.ent.getPersistentDataContainer());
	}

}
