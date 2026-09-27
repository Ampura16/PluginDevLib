package top.brmc.devlib.remain;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.brmc.devlib.ReflectionUtil;

/**
 * {@link ItemFlag} 的兼容性包装类
 */
public enum CompItemFlag {

	/**
	 * 显示/隐藏附魔的设置
	 */
	HIDE_ENCHANTS,

	/**
	 * 显示/隐藏属性（如伤害）的设置
	 */
	HIDE_ATTRIBUTES,

	/**
	 * 显示/隐藏不可破坏状态的设置
	 */
	HIDE_UNBREAKABLE,

	/**
	 * 显示/隐藏该 ItemStack 可以破坏/摧毁哪些方块的设置
	 */
	HIDE_DESTROYS,

	/**
	 * 显示/隐藏该 ItemStack 可以放置在哪些方块上的设置
	 */
	HIDE_PLACED_ON,

	/**
	 * 显示/隐藏该 ItemStack 上药水效果的设置
	 */
	HIDE_POTION_EFFECTS,

	/**
	 * 显示/隐藏染色皮革盔甲颜色的设置
	 */
	HIDE_DYE,

	/**
	 * 显示/隐藏盔甲纹饰的设置
	 */
	HIDE_ARMOR_TRIM,

	/**
	 * 显示/隐藏药水效果、书与烟花信息、地图提示、旗帜图案的设置
	 */
	HIDE_ADDITIONAL_TOOLTIP,

	/**
	 * 显示/隐藏物品上存储的附魔（例如附魔书上的附魔）的
	 * 设置。
	 */
	HIDE_STORED_ENCHANTS;

	/**
	 * 尝试将此物品标志应用到给定物品，失败时静默处理
	 *
	 * @param item
	 */
	public final void applyTo(final ItemStack item) {
		try {
			final ItemMeta meta = item.getItemMeta();
			final ItemFlag bukkitFlag = ReflectionUtil.lookupEnum(ItemFlag.class, this.toString());

			meta.addItemFlags(bukkitFlag);

			item.setItemMeta(meta);

		} catch (final Throwable t) {
			// Unsupported MC version
		}
	}

	/**
	 * 检查给定物品是否带有此物品标志
	 * 失败时静默处理并返回 false
	 * @param item
	 * @return 物品带有此标志时返回 true
	 */
	public final boolean has(final ItemStack item) {
		try {
			if (!item.hasItemMeta())
				return false;

			final ItemMeta meta = item.getItemMeta();
			if (meta == null)
				return false;

			final ItemFlag bukkitFlag = ReflectionUtil.lookupEnum(ItemFlag.class, this.toString());

			return meta.hasItemFlag(bukkitFlag);

		} catch (final Throwable t) {
			// Unsupported MC version
			return false;
		}
	}
}