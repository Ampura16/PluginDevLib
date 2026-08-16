package org.mineacademy.fo.enchant;

import org.bukkit.enchantments.Enchantment;

/**
 * 表示在注册自定义附魔时 Foundation 与服务器之间的桥梁。
 */
public interface NmsEnchant {

	/**
	 * 将该附魔注册到服务器中。
	 *
	 * 注意：部分版本（如 1.20+）要求你先在插件加载时解冻注册表！
	 * （完成后请务必在 onEnable 末尾将其重新冻结）。
	 *
	 * 注意：对于所有继承 {@link SimpleEnchantment} 的类，Foundation 会自动调用此方法
	 */
	void register();

	/**
	 * 返回此自定义附魔对应的 Bukkit Enchantment 类。
	 *
	 * @return
	 */
	Enchantment toBukkit();
}
