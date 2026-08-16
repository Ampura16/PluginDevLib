package org.mineacademy.fo.remain;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;

/**
 * 为 Minecraft 中的所有附魔提供包装。
 *
 * 在旧版 Minecraft 上，部分附魔可能为 null。
 */
public final class CompEnchantment {

	/*
	 * A helper to convert enchant to string.
	 */
	private static final Function<Enchantment, String> TO_STRING = type -> {
		try {
			return type.getKey().getKey().replace("minecraft:", "");

		} catch (final NoSuchMethodError err) {
			return type.getName();
		}
	};

	/**
	 * 按名称存储所有条目。
	 */
	private static final Map<String, Enchantment> byName = new TreeMap<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 存储所有条目名称。
	 */
	private static final Set<String> names = new TreeSet<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 按类型存储所有条目。
	 */
	private static final Set<Enchantment> byType = new TreeSet<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 保存每种附魔的格式化名称，例如 DAMAGE_ALL 对应 "Sharpness" 等。
	 */
	private static final Map<Enchantment, String> loreName = new TreeMap<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 提供对环境伤害的保护
	 */
	public static final Enchantment PROTECTION_ENVIRONMENTAL = register(0, "PROTECTION_ENVIRONMENTAL", "protection");

	/**
	 * 提供对火焰伤害的保护
	 */
	public static final Enchantment PROTECTION_FIRE = register(1, "PROTECTION_FIRE", "fire_protection");

	/**
	 * 提供对摔落伤害的保护
	 */
	public static final Enchantment PROTECTION_FALL = register(2, "PROTECTION_FALL", "feather_falling");

	/**
	 * 提供对爆炸伤害的保护
	 */
	public static final Enchantment PROTECTION_EXPLOSIONS = register(3, "PROTECTION_EXPLOSIONS", "blast_protection");

	/**
	 * 提供对弹射物伤害的保护
	 */
	public static final Enchantment PROTECTION_PROJECTILE = register(4, "PROTECTION_PROJECTILE", "projectile_protection");

	/**
	 * 降低在水下时氧气的消耗速度
	 */
	public static final Enchantment OXYGEN = register(5, "OXYGEN", "respiration");

	/**
	 * 提高玩家在水下的挖掘速度
	 */
	public static final Enchantment WATER_WORKER = register(6, "WATER_WORKER", "aqua_affinity");

	/**
	 * 对攻击者造成伤害
	 */

	public static final Enchantment THORNS = register(7, "THORNS", "thorns");

	/**
	 * 提高在水中的行走速度
	 */

	public static final Enchantment DEPTH_STRIDER = register(8, "DEPTH_STRIDER", "depth_strider");

	/**
	 * 将玩家脚下周围的静止水冻结成霜冰
	 */

	public static final Enchantment FROST_WALKER = register(9, "FROST_WALKER", "frost_walker");

	/**
	 * 物品无法被移除
	 */

	public static final Enchantment BINDING_CURSE = register(10, "BINDING_CURSE", "binding_curse");

	/**
	 * 提高对所有目标的伤害
	 */
	public static final Enchantment DAMAGE_ALL = register(16, "DAMAGE_ALL", "sharpness");

	/**
	 * 提高对亡灵目标的伤害
	 */
	public static final Enchantment DAMAGE_UNDEAD = register(17, "DAMAGE_UNDEAD", "smite");

	/**
	 * 提高对节肢动物目标的伤害
	 */
	public static final Enchantment DAMAGE_ARTHROPODS = register(18, "DAMAGE_ARTHROPODS", "bane_of_arthropods");

	/**
	 * 击中其他目标时会将其击退
	 */
	public static final Enchantment KNOCKBACK = register(19, "KNOCKBACK", "knockback");

	/**
	 * 攻击目标时有几率使其着火
	 */
	public static final Enchantment FIRE_ASPECT = register(20, "FIRE_ASPECT", "fire_aspect");

	/**
	 * 击杀怪物时有几率获得额外战利品
	 */
	public static final Enchantment LOOT_BONUS_MOBS = register(21, "LOOT_BONUS_MOBS", "looting");

	/**
	 * 提高横扫攻击对目标的伤害
	 */

	public static final Enchantment SWEEPING_EDGE = register(22, "SWEEPING", "sweeping_edge");

	/**
	 * 提高挖掘速度
	 */
	public static final Enchantment DIG_SPEED = register(32, "DIG_SPEED", "efficiency");

	/**
	 * 使方块掉落其本身而非碎块（例如
	 * 掉落石头而不是圆石）
	 */
	public static final Enchantment SILK_TOUCH = register(33, "SILK_TOUCH", "silk_touch");

	/**
	 * 降低工具耐久度的损耗速度
	 */
	public static final Enchantment DURABILITY = register(34, "DURABILITY", "unbreaking");

	/**
	 * 破坏方块时有几率获得额外战利品
	 */
	public static final Enchantment LOOT_BONUS_BLOCKS = register(35, "LOOT_BONUS_BLOCKS", "fortune");

	/**
	 * 用弓射箭时造成额外伤害
	 */
	public static final Enchantment ARROW_DAMAGE = register(48, "ARROW_DAMAGE", "power");

	/**
	 * 实体被弓射出的箭击中时会被击退
	 */
	public static final Enchantment ARROW_KNOCKBACK = register(49, "ARROW_KNOCKBACK", "punch");

	/**
	 * 被弓射出的箭击中的实体会着火
	 */
	public static final Enchantment ARROW_FIRE = register(50, "ARROW_FIRE", "flame");

	/**
	 * 用弓射击时箭矢无限
	 */
	public static final Enchantment ARROW_INFINITE = register(51, "ARROW_INFINITE", "infinity");

	/**
	 * 降低钓到垃圾的几率
	 */
	public static final Enchantment LUCK = register(61, "LUCK", "luck_of_the_sea");

	/**
	 * 提高鱼咬钩的速度
	 */
	public static final Enchantment LURE = register(62, "LURE", "lure");

	/**
	 * 使掷出的三叉戟返回投掷它的玩家
	 */
	public static final Enchantment LOYALTY = register(-1, "LOYALTY", "loyalty");

	/**
	 * 对生活在海洋中的生物造成更多伤害
	 */
	public static final Enchantment IMPALING = register(-1, "IMPALING", "impaling");

	/**
	 * 下雨时，使玩家朝三叉戟投掷的方向飞出
	 */
	public static final Enchantment RIPTIDE = register(-1, "RIPTIDE", "riptide");

	/**
	 * 在雷暴天气下，三叉戟击中生物时召唤
	 * 闪电
	 */
	public static final Enchantment CHANNELING = register(-1, "CHANNELING", "channeling");

	/**
	 * 弩可一次射出多支箭
	 */
	public static final Enchantment MULTISHOT = register(-1, "MULTISHOT", "multishot");

	/**
	 * 加快弩的装填速度
	 */
	public static final Enchantment QUICK_CHARGE = register(-1, "QUICK_CHARGE", "quick_charge");

	/**
	 * 弩的弹射物可穿透实体
	 */
	public static final Enchantment PIERCING = register(-1, "PIERCING", "piercing");

	/**
	 * 提高重锤的下落伤害
	 */
	public static final Enchantment DENSITY = register(-1, "DENSITIY", "density");

	/**
	 * 降低盔甲对重锤的防护效果
	 */
	public static final Enchantment BREACH = register(-1, "BREACH", "breach");

	/**
	 * 击中敌人时释放风爆
	 */
	public static final Enchantment WIND_BURST = register(-1, "WIND_BURST", "wind_burst");

	/**
	 * 允许使用经验球修补物品
	 */
	public static final Enchantment MENDING = register(70, "MENDING", "mending");

	/**
	 * 物品会消失而不是掉落
	 */
	public static final Enchantment VANISHING_CURSE = register(71, "VANISHING_CURSE", "vanishing_curse");

	/**
	 * 在灵魂类方块上行走更快
	 */
	public static final Enchantment SOUL_SPEED = register(-1, "SOUL_SPEED", "soul_speed");

	/**
	 * 潜行时行走更快
	 */
	public static final Enchantment SWIFT_SNEAK = register(-1, "SWIFT_SNEAK", "swift_sneak");

	/**
	 * 按名称获取附魔
	 *
	 * @param name
	 * @return
	 */
	public static Enchantment getByName(final String name) {
		return byName.get(name.replace("minecraft:", "").toUpperCase());
	}

	/**
	 * 返回所有可用的附魔类型
	 *
	 * @return
	 */
	public static Collection<Enchantment> getEnchantments() {
		return byType;
	}

	/**
	 * 返回物品描述（lore）中显示的名称，找不到则为 null
	 *
	 * @param type
	 * @return
	 */
	public static String getLoreName(final Enchantment type) {
		return loreName.get(type);
	}

	/**
	 * 返回所有可用的附魔类型
	 *
	 * @return
	 */
	public static Collection<String> getEnchantmentNames() {
		return names;
	}

	/*
	 * Find the enchantment by ID or name, returns null if unsupported by server
	 */
	private static Enchantment register(final int id, final String legacyName, final String modernName) {
		Enchantment enchantment = null;

		try {
			enchantment = Enchantment.getByKey(NamespacedKey.minecraft(modernName));

		} catch (final NoClassDefFoundError | NoSuchMethodError ex) {
			enchantment = Enchantment.getByName(legacyName);

			if (enchantment == null && MinecraftVersion.olderThan(V.v1_13)) {
				final Method getById = ReflectionUtil.getMethod(Enchantment.class, "getById", int.class);

				enchantment = ReflectionUtil.invokeStatic(getById, id);
			}
		}

		if (enchantment != null) {
			try {
				byName.put(enchantment.getKey().getKey().toUpperCase(), enchantment);
			} catch (final NoSuchMethodError err) {
			}

			byName.put(modernName.toUpperCase(), enchantment);

			if (legacyName != null)
				byName.put(legacyName, enchantment);

			names.add(modernName.toUpperCase());

			byType.add(enchantment);
			loreName.put(enchantment, ChatUtil.capitalizeFully(modernName));
		}

		return enchantment;
	}

	static {
		for (final Enchantment enchantment : Enchantment.values()) {
			String name;

			try {
				name = enchantment.getKey().getKey().toUpperCase();

			} catch (final NoSuchMethodError err) {
				name = enchantment.getName().toUpperCase();
			}

			byName.put(name, enchantment);
			names.add(name);
			loreName.put(enchantment, ChatUtil.capitalizeFully(name));

			byType.add(enchantment);
		}
	}
}
