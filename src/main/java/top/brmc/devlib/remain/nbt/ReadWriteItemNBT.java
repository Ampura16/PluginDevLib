package top.brmc.devlib.remain.nbt;

import java.util.function.BiConsumer;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface ReadWriteItemNBT extends ReadWriteNBT, ReadableItemNBT {

	/**
	 * 如果该物品目前带有此物品类型的任何已知标签，则为 true。
	 *
	 * @return 存在自定义标签时返回 true
	 */
	boolean hasCustomNbtData();

	/**
	 * 从 NBTItem 中移除所有自定义（非原版）NBT 标签。
	 */
	void clearCustomNBT();

	/**
	 * 提供对内部 {@link ItemStack} 的 {@link ItemMeta} 的安全访问。
	 * 在此作用域内支持的操作：- {@link ItemMeta} 的任意 get/set 方法
	 * - {@link NBTItem} 的任意 getter
	 *
	 * 在此作用域内对 {@link NBTItem} 所做的所有更改都会在结束时
	 * 被还原。
	 *
	 * @param handler
	 */
	void modifyMeta(BiConsumer<ReadableNBT, ItemMeta> handler);

	/**
	 * 提供对内部 {@link ItemStack} 的 {@link ItemMeta} 的安全访问。
	 * 在此作用域内支持的操作：- {@link ItemMeta} 的任意 get/set 方法
	 * - {@link NBTItem} 的任意 getter
	 *
	 * 在此作用域内对 {@link NBTItem} 所做的所有更改都会在结束时
	 * 被还原。
	 *
	 * @param handler
	 */
	<T extends ItemMeta> void modifyMeta(Class<T> type, BiConsumer<ReadableNBT, T> handler);

}
