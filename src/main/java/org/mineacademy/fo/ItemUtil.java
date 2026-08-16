
package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.SerializeUtil.EnchantmentWrapper;
import org.mineacademy.fo.SerializeUtil.PotionWrapper;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompChatColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.remain.nbt.NBTItem;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 管理物品的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemUtil {

	// Is Minecraft older than 1.13? Storing here for best performance.
	private static final boolean LEGACY_MATERIALS = MinecraftVersion.olderThan(V.v1_13);

	// ----------------------------------------------------------------------------------------------------
	// Enumeration - fancy names
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 见 {@link #bountifyCapitalized(String)}
	 *
	 * @param type
	 * @return
	 */
	public static String bountifyCapitalized(@NonNull PotionEffectType type) {
		return PotionWrapper.getLocalizedName(type.getName());
	}

	/**
	 * 见 {@link #bountifyCapitalized(String)}
	 *
	 * @param color
	 * @return
	 */
	public static String bountifyCapitalized(@NonNull CompChatColor color) {
		return bountifyCapitalized(color.getName());
	}

	/**
	 * 去掉枚举中的 _，全部转小写，最后首字母大写
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountifyCapitalized(@NonNull Enum<?> enumeration) {
		return ChatUtil.capitalizeFully(bountify(enumeration.toString().toLowerCase()));
	}

	/**
	 * 去掉名称中的 _，全部转小写，最后首字母大写
	 *
	 * @param name
	 * @return
	 */
	public static String bountifyCapitalized(String name) {
		return ChatUtil.capitalizeFully(bountify(name));
	}

	/**
	 * 将给定枚举转小写，并把 _ 替换为空格
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountify(@NonNull Enum<?> enumeration) {
		return bountify(enumeration.toString());
	}

	/**
	 * 将给定名称转小写，并把 _ 替换为空格
	 *
	 * @param name
	 * @return
	 */
	public static String bountify(@NonNull String name) {
		return name.toLowerCase().replace("_", " ");
	}

	/**
	 * 返回人类可读的美化药水效果类型
	 *
	 * @param type
	 * @return
	 */
	public static String bountify(@NonNull PotionEffectType type) {
		return PotionWrapper.getLocalizedName(type.getName()).toLowerCase();
	}

	/**
	 * 返回美化的附魔名称
	 *
	 * @param enchant
	 * @return
	 */
	public static String bountify(@NonNull Enchantment enchant) {
		return EnchantmentWrapper.toMinecraft(enchant.getName());
	}

	// ----------------------------------------------------------------------------------------------------
	// Comparing items
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 比较两个物品。若相似则返回 true。
	 * <p>
	 * 两个物品在都不为 null，且类型、数据、名称和 Lore 都相同时视为相似。
	 * 耐久、数量、物品旗标、附魔及其他属性会被忽略。
	 *
	 * @param first
	 * @param second
	 * @return 若物品相似则返回 true（见上文）
	 */
	public static boolean isSimilar(ItemStack first, ItemStack second) {
		if (first == null || second == null)
			return false;

		final boolean firstAir = CompMaterial.isAir(first.getType());
		final boolean secondAir = CompMaterial.isAir(second.getType());

		if ((firstAir && !secondAir) || (!firstAir && secondAir))
			return false;

		if (firstAir && secondAir)
			return true;

		final boolean idMatch = first.getType() == second.getType();

		final boolean isSkull = CompMaterial.isSkull(first.getType()) && CompMaterial.isSkull(second.getType());
		boolean dataMatch = !LEGACY_MATERIALS || isSkull || first.getData().getData() == second.getData().getData();
		final boolean metaMatch = Remain.hasItemMeta() && first.hasItemMeta() == second.hasItemMeta();

		if (!idMatch || !metaMatch || !(dataMatch || (dataMatch = first.getType() == Material.BOW)))
			return false;

		// ItemMeta
		{
			final ItemMeta f = first.getItemMeta();
			final ItemMeta s = second.getItemMeta();

			if ((f == null && s != null) || (s == null && f != null))
				return false;

			if (f != null && s != null) {
				final String fName = Common.stripColors(f.getDisplayName());
				final String sName = Common.stripColors(s.getDisplayName());

				if ((fName != null && !fName.equals(sName)) || !listMatch(f.getLore(), s.getLore()))
					return false;
			}
		}

		if (MinecraftVersion.atLeast(V.v1_7)) {
			final NBTItem firstNbt = new NBTItem(first);
			final NBTItem secondNbt = new NBTItem(second);

			return matchNbt(SimplePlugin.getNamed(), firstNbt, secondNbt) && matchNbt(SimplePlugin.getNamed() + "_Item", firstNbt, secondNbt);
		}

		return true;
	}

	private static boolean listMatch(List<String> first, List<String> second) {

		if (first == null)
			first = new ArrayList<>();

		if (second == null)
			second = new ArrayList<>();

		if (first.isEmpty() && second.isEmpty())
			return true;

		if (first.size() != second.size())
			return false;

		for (int i = 0; i < first.size(); i++) {
			final String firstString = first.get(i);
			final String secondString = second.get(i);

			if (!Common.stripColors(firstString).equals(Common.stripColors(secondString)))
				return false;
		}

		return true;
	}

	// Compares the NBT string tag of two items
	private static boolean matchNbt(String key, NBTItem firstNbt, NBTItem secondNbt) {
		final boolean firstHas = firstNbt.hasTag(key);
		final boolean secondHas = secondNbt.hasTag(key);

		if (!firstHas && !secondHas)
			return true; // nothing has, essentially same

		else if (firstHas && !secondHas || !firstHas && secondHas)
			return false; // one has but another hasn't, cannot be same

		return firstNbt.getString(key).equals(secondNbt.getString(key));
	}
}