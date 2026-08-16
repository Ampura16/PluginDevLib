package org.mineacademy.fo.remain.nbt;

import java.util.Set;
import java.util.function.BiConsumer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 用于访问 ItemStack 上原版/自定义标签的 NBT 类。此类不会
 * 自动保存到 ItemStack，请使用 getItem 获取修改后的 ItemStack
 *
 * @author tr7zw
 */
public class NBTItem extends NBTCompound implements ReadWriteItemNBT {

	private ItemStack bukkitItem;
	private final boolean directApply;
	private final boolean finalizer;
	private ItemStack originalSrcStack = null;
	private Object cachedCompound = null;
	private boolean closed = false;

	/**
	 * NBTItem 的构造器。ItemStack 会被克隆！已弃用：请
	 * 使用 NBT 类处理物品。它最多快 400%，并且
	 * 更不容易把代码写错。
	 *
	 * @param item
	 */
	@Deprecated
	public NBTItem(ItemStack item) {
		this(item, false);
	}

	/**
	 * @param item
	 * @param directApply
	 * @param readOnly    开启后不会创建源物品的副本。
	 *                    这种情况下修改该物品堆是无效的！同时
	 *                    会覆盖 directApply
	 */
	protected NBTItem(ItemStack item, boolean directApply, boolean readOnly, boolean finalizer) {
		super(null, null, readOnly);
		if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0)
			throw new NullPointerException("ItemStack can't be null/air/amount of 0! This is not a NBTAPI bug!");
		this.finalizer = finalizer;
		if (finalizer) {
			this.bukkitItem = item;
			this.originalSrcStack = item;
			this.directApply = false;
		} else if (readOnly) {
			this.bukkitItem = item;
			this.directApply = false;
		} else {
			this.directApply = directApply;
			this.bukkitItem = item.clone();
			if (directApply)
				this.originalSrcStack = item;
		}
	}

	/**
	 * NBTItem 的构造器。ItemStack 会被克隆！若 directApply 为
	 * true，所有修改都会映射到原物品。这种情况下，对 NBTItem 的修改
	 * 会覆盖对原物品所做的修改。
	 *
	 * @param item
	 * @param directApply
	 */
	@Deprecated
	public NBTItem(ItemStack item, boolean directApply) {
		super(null, null);
		if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0)
			throw new NullPointerException("ItemStack can't be null/air/amount of 0! This is not a NBTAPI bug!");
		this.finalizer = false;
		this.directApply = directApply;
		this.bukkitItem = item.clone();
		if (directApply)
			this.originalSrcStack = item;
	}

	@Override
	public Object getCompound() {
		if (this.closed)
			throw new NbtApiException("Tried using closed NBT data!");
		if (this.isReadOnly() && (this.cachedCompound != null
				|| ClassWrapper.CRAFT_ITEMSTACK.getClazz().isAssignableFrom(this.bukkitItem.getClass()))) {
			if (this.cachedCompound == null)
				this.cachedCompound = NBTReflectionUtil
						.getItemRootNBTTagCompound(NBTReflectionUtil.getCraftItemHandle(this.bukkitItem));
			return this.cachedCompound;
		}
		if (this.finalizer) {
			if (this.cachedCompound == null)
				this.updateCachedCompound();
			return this.cachedCompound;
		}
		return NBTReflectionUtil.getItemRootNBTTagCompound(ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, this.bukkitItem));
	}

	private void updateCachedCompound() {
		if (this.finalizer)
			this.cachedCompound = NBTReflectionUtil
					.getItemRootNBTTagCompound(ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, this.bukkitItem));
	}

	protected void finalizeChanges() {
		if (!this.finalizer || this.cachedCompound == null)
			return;
		// There was data, but not anymore, delete the tag from the itemstack
		if (NBTReflectionUtil.getKeys(this).isEmpty())
			this.cachedCompound = null;
		if (ClassWrapper.CRAFT_ITEMSTACK.getClazz().isAssignableFrom(this.originalSrcStack.getClass())) {
			final Object nmsStack = NBTReflectionUtil.getCraftItemHandle(this.originalSrcStack);
			NBTReflectionUtil.setItemStackCompound(nmsStack, this.cachedCompound);
			this.bukkitItem = this.originalSrcStack;
		} else {
			final Object stack = ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, this.bukkitItem);
			NBTReflectionUtil.setItemStackCompound(stack, this.cachedCompound);
			this.bukkitItem = (ItemStack) ReflectionMethod.ITEMSTACK_BUKKITMIRROR.run(null, stack);
			this.originalSrcStack.setItemMeta(this.bukkitItem.getItemMeta());
		}
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
	protected void setCompound(Object compound) {
		if (this.isReadOnly())
			throw new NbtApiException("Tried setting data in read only mode!");
		if (this.closed)
			throw new NbtApiException("Tried using closed NBT data!");
		if (this.finalizer) {
			this.cachedCompound = compound;
			return;
		}
		if (compound != null && ((Set<String>) ReflectionMethod.COMPOUND_GET_KEYS.run(compound)).isEmpty())
			compound = null;
		if (ClassWrapper.CRAFT_ITEMSTACK.getClazz().isAssignableFrom(this.bukkitItem.getClass())) {
			final Object nmsStack = NBTReflectionUtil.getCraftItemHandle(this.bukkitItem);
			NBTReflectionUtil.setItemStackCompound(nmsStack, compound);
		} else {
			final Object stack = ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, this.bukkitItem);
			NBTReflectionUtil.setItemStackCompound(stack, compound);
			this.bukkitItem = (ItemStack) ReflectionMethod.ITEMSTACK_BUKKITMIRROR.run(null, stack);
		}
	}

	/**
	 * 将存储的 NBT 标签应用到所提供的 ItemStack。
	 * <p>
	 * 注意：这会完全覆盖当前物品的 {@link ItemMeta}。如果你
	 * 仍想保留原物品的 NBT 标签，请参见
	 * {@link #mergeNBT(ItemStack)} 和 {@link #mergeCustomNBT(ItemStack)}。
	 *
	 * @param item 应获得新 NBT 数据的 ItemStack
	 */
	@Deprecated
	public void applyNBT(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			throw new NullPointerException("ItemStack can't be null/Air! This is not a NBTAPI bug!");
		final NBTItem nbti = new NBTItem(new ItemStack(item.getType()));
		nbti.mergeCompound(this);
		item.setItemMeta(nbti.getItem().getItemMeta());
	}

	/**
	 * 将所有 NBT 标签合并到所提供的 ItemStack。
	 *
	 * @param item 应获得新 NBT 数据的 ItemStack
	 */
	@Deprecated
	public void mergeNBT(ItemStack item) {
		final NBTItem nbti = new NBTItem(item);
		nbti.mergeCompound(this);
		item.setItemMeta(nbti.getItem().getItemMeta());
	}

	/**
	 * 仅将自定义（非原版）NBT 标签合并到所提供的 ItemStack。
	 *
	 * @param item 应获得新 NBT 数据的 ItemStack
	 */
	@Deprecated
	public void mergeCustomNBT(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			throw new NullPointerException("ItemStack can't be null/Air!");
		if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
			// 1.20.5+ doesn't have any vanilla tags
			NBT.modify(item, nbt -> {
				nbt.mergeCompound(this);
			});
			return;
		}
		final ItemMeta meta = item.getItemMeta();
		NBTReflectionUtil.getUnhandledNBTTags(meta)
				.putAll(NBTReflectionUtil.getUnhandledNBTTags(this.bukkitItem.getItemMeta()));
		item.setItemMeta(meta);
	}

	/**
	 * 若物品带有此物品类型未知的任何标签则为 True。
	 *
	 * @return 存在自定义标签时为 true
	 */
	@Override
	@Deprecated
	public boolean hasCustomNbtData() {
		if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
			// 1.20.5+ doesn't have any vanilla tags
			return this.hasNBTData();
		this.finalizeChanges();
		final ItemMeta meta = this.bukkitItem.getItemMeta();
		return !NBTReflectionUtil.getUnhandledNBTTags(meta).isEmpty();
	}

	/**
	 * 从 NBTItem 中移除所有自定义（非原版）NBT 标签。
	 */
	@Override
	@Deprecated
	public void clearCustomNBT() {
		this.finalizeChanges();
		if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
			// 1.20.5+ doesn't have any vanilla tags
			this.setCompound(null);
			return;
		}
		final ItemMeta meta = this.bukkitItem.getItemMeta();
		NBTReflectionUtil.getUnhandledNBTTags(meta).clear();
		this.bukkitItem.setItemMeta(meta);
		this.updateCachedCompound();
	}

	/**
	 * @return 修改后的 ItemStack
	 */
	public ItemStack getItem() {
		return this.bukkitItem;
	}

	protected void setItem(ItemStack item) {
		this.bukkitItem = item;
	}

	/**
	 * 若物品带有 NBT 数据则返回 true。在调用 remove 等方法之前
	 * 需要先检查这一点，否则值可能不正确！
	 *
	 * @return 该 ItemStack 是否拥有 NBTCompound。
	 */
	@Override
	public boolean hasNBTData() {
		return this.getCompound() != null;
	}

	/**
	 * 提供对内部 {@link ItemStack} 的 {@link ItemMeta} 的安全访问。
	 * 在此作用域内支持的操作：- {@link ItemMeta} 的任意 get/set 方法
	 * - {@link NBTItem} 上的任意 getter
	 *
	 * 在此作用域内对 {@link NBTItem} 所做的所有修改都会在
	 * 结束时被还原。
	 *
	 * @param handler
	 */
	@Override
	public void modifyMeta(BiConsumer<ReadableNBT, ItemMeta> handler) {
		this.finalizeChanges();
		final ItemMeta meta = this.bukkitItem.getItemMeta();
		handler.accept(new NBTContainer(this.getResolvedObject()).setReadOnly(true), meta);
		this.bukkitItem.setItemMeta(meta);
		this.updateCachedCompound();
		if (this.directApply) {
			if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
				throw new NbtApiException(
						"Direct apply mode meta changes don't work anymore in 1.20.5+. Please switch to the modern NBT.modify sytnax!");
			this.applyNBT(this.originalSrcStack);
		}
	}

	/**
	 * 提供对内部 {@link ItemStack} 的 {@link ItemMeta} 的安全访问。
	 * 在此作用域内支持的操作：- {@link ItemMeta} 的任意 get/set 方法
	 * - {@link NBTItem} 上的任意 getter
	 *
	 * 在此作用域内对 {@link NBTItem} 所做的所有修改都会在
	 * 结束时被还原。
	 *
	 * @param handler
	 */
	@Override
	public <T extends ItemMeta> void modifyMeta(Class<T> type, BiConsumer<ReadableNBT, T> handler) {
		this.finalizeChanges();

		final T meta = (T) this.bukkitItem.getItemMeta();
		handler.accept(new NBTContainer(this.getResolvedObject()).setReadOnly(true), meta);
		this.bukkitItem.setItemMeta(meta);
		this.updateCachedCompound();
		if (this.directApply)
			this.applyNBT(this.originalSrcStack);
	}

	/**
	 * 辅助方法，将 {@link ItemStack} 连同其全部数据（如材质、损伤值、数量和标签）
	 * 转换为 {@link NBTContainer}。
	 *
	 * @param item
	 * @return 包含该物品数据的独立 {@link NBTContainer}
	 */
	@Deprecated
	public static NBTContainer convertItemtoNBT(ItemStack item) {
		return NBTReflectionUtil.convertNMSItemtoNBTCompound(ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, item));
	}

	/**
	 * 辅助方法，执行 "convertItemtoNBT" 的逆操作。使用 {@link NBTCompound}
	 * 创建 {@link ItemStack}
	 *
	 * @param comp
	 * @return 使用该 {@link NBTCompound} 数据的 ItemStack
	 */
	@Deprecated
	public static ItemStack convertNBTtoItem(NBTCompound comp) {
		return (ItemStack) ReflectionMethod.ITEMSTACK_BUKKITMIRROR.run(null,
				NBTReflectionUtil.convertNBTCompoundtoNMSItem(comp));
	}

	/**
	 * 辅助方法，将 {@link ItemStack}[] 连同其全部数据（如材质、损伤值、数量和标签）
	 * 转换为 {@link NBTContainer}。这是自定义实现，
	 * 不适用于原版代码（潜影盒内容等）。
	 *
	 * @param items
	 * @return 包含这些物品数据的独立 {@link NBTContainer}
	 */
	@Deprecated
	public static NBTContainer convertItemArraytoNBT(ItemStack[] items) {
		final NBTContainer container = new NBTContainer();
		container.setInteger("size", items.length);
		final NBTCompoundList list = container.getCompoundList("items");
		for (int i = 0; i < items.length; i++) {
			final ItemStack item = items[i];
			if (item == null || item.getType() == Material.AIR)
				continue;
			final NBTListCompound entry = list.addCompound();
			entry.setInteger("Slot", i);
			entry.mergeCompound(convertItemtoNBT(item));
		}
		return container;
	}

	/**
	 * 辅助方法，执行 "convertItemArraytoNBT" 的逆操作。使用 {@link NBTCompound}
	 * 创建 {@link ItemStack}[]。这是自定义实现，
	 * 不适用于原版代码（潜影盒内容等）。
	 *
	 * 数据无效时返回 null。数组中的空槽位会用
	 * AIR 物品堆填充！
	 *
	 * @param comp
	 * @return 使用该 {@link NBTCompound} 数据的 ItemStack[]
	 */
	@Deprecated
	public static ItemStack[] convertNBTtoItemArray(NBTCompound comp) {
		if (!comp.hasTag("size"))
			return null;
		final ItemStack[] rebuild = new ItemStack[comp.getInteger("size")];
		for (int i = 0; i < rebuild.length; i++)
			rebuild[i] = new ItemStack(Material.AIR);
		if (!comp.hasTag("items"))
			return rebuild;
		final NBTCompoundList list = comp.getCompoundList("items");
		for (final ReadWriteNBT lcomp : list)
			if (lcomp instanceof NBTCompound) {
				final int slot = lcomp.getInteger("Slot");
				rebuild[slot] = convertNBTtoItem((NBTCompound) lcomp);
			}
		return rebuild;
	}

	@Override
	protected void saveCompound() {
		if (this.directApply)
			this.applyNBT(this.originalSrcStack);
	}

}
