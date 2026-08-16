package org.mineacademy.fo.remain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.bukkit.potion.PotionEffectType;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.ReflectionUtil;

/**
 * 针对 1.20.5 中 PotionEffectType 命名变更的包装类
 */
public final class CompPotionEffectType {

	/*
	 * A helper to convert potion to string.
	 */
	private static final Function<PotionEffectType, String> TO_STRING = type -> {
		try {
			return type.getKey().getKey().replace("minecraft:", "");

		} catch (final NoSuchMethodError err) {
			return type.getName();
		}
	};

	/**
	 * 按名称存储所有条目。
	 */
	private static final Map<String, PotionEffectType> byName = new TreeMap<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 存储所有条目名称。
	 */
	private static final Set<String> names = new TreeSet<>(Comparator.comparing(name -> name, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 按类型存储所有条目。
	 */
	private static final Set<PotionEffectType> byType = new TreeSet<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 保存每种药水的格式化名称，例如 SLOW_DIGGING 对应 "Mining Fatigue" 等。
	 */
	private static final Map<PotionEffectType, String> loreName = new TreeMap<>(Comparator.comparing(TO_STRING, String.CASE_INSENSITIVE_ORDER));

	/**
	 * 提高移动速度。
	 */
	public static final PotionEffectType SPEED = find("SPEED", "SPEED");

	/**
	 * 降低移动速度。
	 */
	public static final PotionEffectType SLOW = find("SLOW", "SLOWNESS");

	/**
	 * 提高挖掘速度。
	 */
	public static final PotionEffectType FAST_DIGGING = find("FAST_DIGGING", "HASTE");

	/**
	 * 降低挖掘速度。
	 */
	public static final PotionEffectType SLOW_DIGGING = find("SLOW_DIGGING", "MINING_FATIGUE");

	/**
	 * 提高造成的伤害。
	 */
	public static final PotionEffectType INCREASE_DAMAGE = find("INCREASE_DAMAGE", "STRENGTH");

	/**
	 * 治疗实体。
	 */
	public static final PotionEffectType HEAL = find("HEAL", "INSTANT_HEALTH");

	/**
	 * 伤害实体。
	 */
	public static final PotionEffectType HARM = find("HARM", "INSTANT_DAMAGE");

	/**
	 * 提高跳跃高度。
	 */
	public static final PotionEffectType JUMP = find("JUMP", "JUMP_BOOST");

	/**
	 * 使客户端视野扭曲。
	 */
	public static final PotionEffectType CONFUSION = find("CONFUSION", "NAUSEA");

	/**
	 * 恢复生命值。
	 */
	public static final PotionEffectType REGENERATION = find("REGENERATION", "REGENERATION");

	/**
	 * 减少实体受到的伤害。
	 */
	public static final PotionEffectType DAMAGE_RESISTANCE = find("DAMAGE_RESISTANCE", "RESISTANCE");

	/**
	 * 免疫火焰伤害。
	 */
	public static final PotionEffectType FIRE_RESISTANCE = find("FIRE_RESISTANCE", "FIRE_RESISTANCE");

	/**
	 * 允许在水下呼吸。
	 */
	public static final PotionEffectType WATER_BREATHING = find("WATER_BREATHING", "WATER_BREATHING");

	/**
	 * 赋予隐身效果。
	 */
	public static final PotionEffectType INVISIBILITY = find("INVISIBILITY", "INVISIBILITY");

	/**
	 * 使实体失明。
	 */
	public static final PotionEffectType BLINDNESS = find("BLINDNESS", "BLINDNESS");

	/**
	 * 使实体能在黑暗中看清。
	 */
	public static final PotionEffectType NIGHT_VISION = find("NIGHT_VISION", "NIGHT_VISION");

	/**
	 * 加快饥饿。
	 */
	public static final PotionEffectType HUNGER = find("HUNGER", "HUNGER");

	/**
	 * 降低实体造成的伤害。
	 */
	public static final PotionEffectType WEAKNESS = find("WEAKNESS", "WEAKNESS");

	/**
	 * 持续对实体造成伤害。
	 */
	public static final PotionEffectType POISON = find("POISON", "POISON");

	/**
	 * 持续对实体造成伤害，并将生命值转给
	 * 射击者。
	 */
	public static final PotionEffectType WITHER = find("WITHER", "WITHER");

	/**
	 * 提高实体的最大生命值。
	 */
	public static final PotionEffectType HEALTH_BOOST = find("HEALTH_BOOST", "HEALTH_BOOST");

	/**
	 * 以无法自然恢复、但每 30 秒重新填满的生命值
	 * 提高实体的最大生命值。
	 */
	public static final PotionEffectType ABSORPTION = find("ABSORPTION", "ABSORPTION");

	/**
	 * 每 tick 提高实体的饱食度。
	 */
	public static final PotionEffectType SATURATION = find("SATURATION", "SATURATION");

	/**
	 * 为实体添加轮廓，使其在远处也能被看到。
	 */
	public static final PotionEffectType GLOWING = find("GLOWING", "GLOWING");

	/**
	 * 使实体向空中飘浮。
	 */
	public static final PotionEffectType LEVITATION = find("LEVITATION", "LEVITATION");

	/**
	 * 战利品表幸运。
	 */
	public static final PotionEffectType LUCK = find("LUCK", "LUCK");

	/**
	 * 战利品表霉运。
	 */
	public static final PotionEffectType UNLUCK = find("UNLUCK", "UNLUCK");

	/**
	 * 减缓实体下落速度。
	 */
	public static final PotionEffectType SLOW_FALLING = find("SLOW_FALLING", "SLOW_FALLING");

	/**
	 * 附近潮涌核心赋予的效果，包括增强的水下能力。
	 */
	public static final PotionEffectType CONDUIT_POWER = find("CONDUIT_POWER", "CONDUIT_POWER");

	/**
	 * 提高水下移动速度。<br>
	 * Squee'ek uh'k kk'kkkk squeek eee'eek.
	 */
	public static final PotionEffectType DOLPHINS_GRACE = find("DOLPHINS_GRACE", "DOLPHINS_GRACE");

	/**
	 * 玩家进入村庄时触发袭击。<br>
	 * oof.
	 */
	public static final PotionEffectType BAD_OMEN = find("BAD_OMEN", "BAD_OMEN");

	/**
	 * 降低村民交易的价格。<br>
	 * \o/.
	 */
	public static final PotionEffectType HERO_OF_THE_VILLAGE = find("HERO_OF_THE_VILLAGE", "HERO_OF_THE_VILLAGE");

	/**
	 * 使玩家视野不时变暗。
	 */
	public static final PotionEffectType DARKNESS = find("DARKNESS", "DARKNESS");

	/**
	 * 使试炼刷怪笼变为不祥状态。
	 */
	public static final PotionEffectType TRIAL_OMEN = find("TRIAL_OMEN");

	/**
	 * 玩家进入村庄时触发袭击。
	 */
	public static final PotionEffectType RAID_OMEN = find("RAID_OMEN");

	/**
	 * 死亡时释放一阵风爆。
	 */
	public static final PotionEffectType WIND_CHARGED = find("WIND_CHARGED");

	/**
	 * 死亡时生成蜘蛛网。
	 */
	public static final PotionEffectType WEAVING = find("WEAVING");

	/**
	 * 死亡时生成史莱姆。
	 */
	public static final PotionEffectType OOZING = find("OOZING");

	/**
	 * 受伤时有几率生成蠹虫。
	 */
	public static final PotionEffectType INFESTED = find("INFESTED");

	/**
	 * 按名称获取药水
	 *
	 * @param name
	 * @return
	 */
	public static PotionEffectType getByName(final String name) {
		return byName.get(name.replace("minecraft:", "").toUpperCase());
	}

	/**
	 * 返回所有可用的药水效果类型
	 *
	 * @return
	 */
	public static Collection<PotionEffectType> getPotions() {
		return byType;
	}

	/**
	 * 返回物品描述（lore）中显示的名称
	 *
	 * @param type
	 * @return
	 */
	public static String getLoreName(final PotionEffectType type) {
		return loreName.get(type);
	}

	/**
	 * 返回所有可用的药水效果类型
	 *
	 * @return
	 */
	public static Collection<String> getPotionNames() {
		return names;
	}

	/*
	 * Get the potion effect type by its name
	 */
	private static PotionEffectType find(final String modernName) {
		return find(null, modernName);
	}

	/*
	 * Get the potion effect type by its name
	 */
	private static PotionEffectType find(final String legacyName, final String modernName) {
		PotionEffectType type = null;

		try {
			type = ReflectionUtil.getStaticFieldContent(PotionEffectType.class, modernName);

		} catch (final Throwable t) {

			if (legacyName != null)
				try {
					type = ReflectionUtil.getStaticFieldContent(PotionEffectType.class, legacyName);

				} catch (final Throwable tt) {
				}
		}

		if (type != null) {
			try {
				byName.put(type.getKey().getKey().toUpperCase(), type);
			} catch (final NoSuchMethodError err) {
			}

			byName.put(modernName, type);

			if (legacyName != null)
				byName.put(legacyName, type);

			names.add(modernName);

			byType.add(type);
			loreName.put(type, ChatUtil.capitalizeFully(modernName));
		}

		return type;
	}

	static {
		for (final PotionEffectType type : ReflectionUtil.getEnumValues(PotionEffectType.class)) {
			if (type == null)
				continue; // wtf 1.8.8

			String name;

			try {
				name = type.getKey().getKey().toUpperCase();

			} catch (final NoSuchMethodError err) {
				name = type.getName().toUpperCase();
			}

			byName.put(name, type);
			names.add(name);
			loreName.put(type, ChatUtil.capitalizeFully(name));

			byType.add(type);
		}
	}
}