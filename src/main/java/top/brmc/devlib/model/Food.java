package top.brmc.devlib.model;

import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.remain.CompMaterial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 记录每种食物应恢复多少生命值。
 *
 * 玩家共有 20 点生命值，因此 10 点生命值相当于
 * 左下角生命条所示总生命值的一半。
 *
 * 食物名称必须与 {@link CompMaterial} 中的名称相同
 */
@RequiredArgsConstructor
public enum Food {
	APPLE(2),
	BAKED_POTATO(5),
	BEEF(4),
	BEETROOT(1),
	BEETROOT_SOUP(6),
	BREAD(5),
	CAKE(2),
	CARROT(3),
	CHICKEN(6),
	CHORUS_FRUIT(4),
	COD(3),
	COOKED_BEEF(8),
	COOKED_CHICKEN(6),
	COOKED_COD(5),
	COOKED_MUTTON(6),
	COOKED_PORKCHOP(8),
	COOKED_RABBIT(5),
	COOKED_SALMON(6),
	COOKIE(2),
	DRIED_KELP(1),
	ENCHANTED_GOLDEN_APPLE(4),
	GLOW_BERRIES(3),
	GOLDEN_APPLE(4),
	GOLDEN_CARROT(6),
	HONEY_BOTLE(4),
	MELON_SLICE(2),
	MUSHROOM_STEW(6),
	MUTTON(2),
	POISONOUS_POTATO(2),
	PORKCHOP(4),
	POTATO(1),
	PUFFERFISH(1),
	PUMPKIN_PIE(8),
	RABBIT(3),
	RABBIT_STEW(10),
	ROTTEN_FLESH(4),
	SALMON(2),
	SPIDER_EYE(2),
	SUSPICIOUS_STEW(6),
	SWEET_BERRIES(2),
	TROPICAL_FISH(1);

	/**
	 * 这种食物恢复多少生命值？
	 */
	@Getter
	private final int healthPoints;

	/**
	 * 尝试为给定物品堆查找对应的食物
	 *
	 * @param item
	 * @return 对应的食物，没有则为 null
	 */
	public static Food getFood(ItemStack item) {
		final CompMaterial material = CompMaterial.fromMaterial(item.getType());

		return ReflectionUtil.lookupEnumSilent(Food.class, material.toString());
	}
}