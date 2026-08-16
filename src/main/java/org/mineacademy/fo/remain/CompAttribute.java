package org.mineacademy.fo.remain;

import java.lang.reflect.Method;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.exception.FoException;

import lombok.NonNull;

/**
 * {@link Attribute} 的包装类
 * <p>
 * 更多信息请参见 https://minecraft.wiki/w/Attribute
 */
public enum CompAttribute {

	/**
	 * 实体的护甲加成。
	 */
	ARMOR("ARMOR", "GENERIC_ARMOR"),

	/**
	 * 实体的盔甲韧性加成。
	 */
	ARMOR_TOUGHNESS("ARMOR_TOUGHNESS", "GENERIC_ARMOR_TOUGHNESS"),

	/**
	 * 实体的攻击伤害。
	 * <p>
	 * 被动生物和傀儡没有此属性。
	 */
	ATTACK_DAMAGE("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE") {
		@Override
		public String getNmsName() {
			return "ATTACK_DAMAGE";
		}
	},

	/**
	 * 实体的攻击击退。
	 */
	ATTACK_KNOCKBACK("ATTACK_KNOCKBACK", "GENERIC_ATTACK_KNOCKBACK"),

	/**
	 * 实体的攻击速度。
	 */
	ATTACK_SPEED("ATTACK_SPEED", "GENERIC_ATTACK_SPEED"),

	/**
	 * 玩家的方块破坏速度。
	 */
	BLOCK_BREAK_SPEED("BLOCK_BREAK_SPEED", "PLAYER_BLOCK_BREAK_SPEED"),

	/**
	 * 玩家的方块交互距离。
	 */
	BLOCK_INTERACTION_RANGE("BLOCK_INTERACTION_RANGE", "PLAYER_BLOCK_INTERACTION_RANGE"),

	/**
	 * 实体被点燃后持续燃烧的时间。
	 */
	BURNING_TIME("BURNING_TIME", "GENERIC_BURNING_TIME"),

	/**
	 * 玩家的实体交互距离。
	 */
	ENTITY_INTERACTION_RANGE("ENTITY_INTERACTION_RANGE", "PLAYER_ENTITY_INTERACTION_RANGE"),

	/**
	 * 对爆炸击退的抗性。
	 */
	EXPLOSION_KNOCKBACK_RESISTANCE("EXPLOSION_KNOCKBACK_RESISTANCE", "GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE"),

	/**
	 * 实体的摔落伤害倍率。
	 */
	FALL_DAMAGE_MULTIPLIER("FALL_DAMAGE_MULTIPLIER", "GENERIC_FALL_DAMAGE_MULTIPLIER"),

	/**
	 * 实体的飞行速度。
	 */
	FLYING_SPEED("FLYING_SPEED", "GENERIC_FLYING_SPEED"),

	/**
	 * 实体跟随其他实体的范围。
	 */
	FOLLOW_RANGE("FOLLOW_RANGE", "GENERIC_FOLLOW_RANGE") {
		@Override
		public String getNmsName() {
			return "FOLLOW_RANGE";
		}
	},

	/**
	 * 作用于实体的重力。
	 */
	GRAVITY("GRAVITY", "GENERIC_GRAVITY"),

	/**
	 * 实体的跳跃力度。
	 */
	JUMP_STRENGTH("JUMP_STRENGTH", "GENERIC_JUMP_STRENGTH", "HORSE_JUMP_STRENGTH"),

	/**
	 * 实体的击退抗性。
	 */
	KNOCKBACK_RESISTANCE("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE") {
		@Override
		public String getNmsName() {
			return "c";
		}
	},

	/**
	 * 实体的幸运加成。
	 */
	LUCK("LUCK", "GENERIC_LUCK"),

	/**
	 * 实体的最大伤害吸收值。
	 */
	MAX_ABSORPTION("MAX_ABSORPTION", "GENERIC_MAX_ABSORPTION"),

	/**
	 * 实体的最大生命值。
	 */
	MAX_HEALTH("MAX_HEALTH", "GENERIC_MAX_HEALTH") {
		@Override
		public String getNmsName() {
			return "maxHealth";
		}
	},

	/**
	 * 使用正确工具时的挖掘速度。
	 */
	MINING_EFFICIENCY("MINING_EFFICIENCY", "PLAYER_MINING_EFFICIENCY"),

	/**
	 * 穿越难行地形时的移动速度。
	 */
	MOVEMENT_EFFICIENCY("MOVEMENT_EFFICIENCY", "GENERIC_MOVEMENT_EFFICIENCY"),

	/**
	 * 实体的移动速度。
	 * <p>
	 * 默认值请参见 https://minecraft.wiki/w/Attribute
	 */
	MOVEMENT_SPEED("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED") {
		@Override
		public String getNmsName() {
			return "MOVEMENT_SPEED";
		}
	},

	/**
	 * 水下的氧气消耗。
	 */
	OXYGEN_BONUS("OXYGEN_BONUS", "GENERIC_OXYGEN_BONUS"),

	/**
	 * 实体可以无伤摔落的距离。
	 */
	SAFE_FALL_DISTANCE("SAFE_FALL_DISTANCE", "GENERIC_SAFE_FALL_DISTANCE"),

	/**
	 * 实体的相对尺寸。
	 */
	SCALE("SCALE", "GENERIC_SCALE"),

	/**
	 * 潜行速度。
	 */
	SNEAKING_SPEED("SNEAKING_SPEED", "PLAYER_SNEAKING_SPEED"),

	/**
	 * 僵尸召唤增援的几率。
	 */
	SPAWN_REINFORCEMENTS("SPAWN_REINFORCEMENTS", "ZOMBIE_SPAWN_REINFORCEMENTS"),

	/**
	 * 实体可以直接走上的高度。
	 */
	STEP_HEIGHT("STEP_HEIGHT", "GENERIC_STEP_HEIGHT"),

	/**
	 * 水下挖掘速度。
	 */
	SUBMERGED_MINING_SPEED("SUBMERGED_MINING_SPEED", "PLAYER_SUBMERGED_MINING_SPEED"),

	/**
	 * 横扫伤害。
	 */
	SWEEPING_DAMAGE_RATIO("SWEEPING_DAMAGE_RATIO", "PLAYER_SWEEPING_DAMAGE_RATIO"),

	/**
	 * 生物被物品吸引的范围。
	 */
	TEMPT_RANGE("TEMPT_RANGE", "GENERIC_TEMPT_RANGE"),

	/**
	 * 在水中的移动速度。
	 */
	WATER_MOVEMENT_EFFICIENCY("WATER_MOVEMENT_EFFICIENCY", "GENERIC_WATER_MOVEMENT_EFFICIENCY"),

	/**
	 * 玩家相机与自身实体之间的距离。
	 */
	CAMERA_DISTANCE("CAMERA_DISTANCE"),

	/**
	 * 控制实体将自身作为路径点发送的范围的属性。
	 */
	WAYPOINT_TRANSMIT_RANGE("WAYPOINT_TRANSMIT_RANGE"),

	/**
	 * 控制实体接收其他路径点的范围的属性。
	 */
	WAYPOINT_RECEIVE_RANGE("WAYPOINT_RECEIVE_RANGE");

	/**
	 * 若服务器支持该属性则返回 true。
	 */
	private static final boolean hasAttributeClass = MinecraftVersion.atLeast(V.v1_9);

	/**
	 * 缓存的 Bukkit 属性（如果有）
	 */
	private Object bukkitAttribute;

	/**
	 * 创建新属性
	 *
	 * @param names
	 */
	CompAttribute(final String... names) {
		if (MinecraftVersion.atLeast(V.v1_9))
			for (final String name : names)
				try {
					this.bukkitAttribute = ReflectionUtil.lookupEnum(Attribute.class, name);

					break;

				} catch (final IllegalArgumentException ex) {
					// Ignore
				}
	}

	/**
	 * 获取 1.8.9 的 NMS 名称，若该版本不存在则为 null
	 *
	 * @return
	 */
	public String getNmsName() {
		return null;
	}

	/**
	 * 查找实体的属性
	 *
	 * @param entity
	 * @return 该属性；若服务器不支持或不适用于该实体则为 null
	 */
	public final Double get(@NonNull final LivingEntity entity) {

		// Minecraft 1.8.8+
		if (hasAttributeClass) {

			// Too modern attribute
			if (this.bukkitAttribute != null) {
				final AttributeInstance instance = entity.getAttribute((Attribute) this.bukkitAttribute);

				return instance != null ? instance.getValue() : null;
			}

		} else if (this.getNmsName() != null)
			try {
				return (double) ReflectionUtil.invoke("getValue", this.getLegacyAttributeInstance(entity));

			} catch (final NullPointerException exx) {
				return null;

			} catch (final Throwable t) {
				throw new FoException("Error retrieving attribute " + this + " for " + entity);
			}

		return null;

	}

	/**
	 * 若服务器支持，为实体设置新的属性值
	 *
	 * @param entity
	 * @param value
	 */
	public final void set(@NonNull final LivingEntity entity, final double value) {

		// Minecraft 1.8.8+
		if (hasAttributeClass) {
			if (this.bukkitAttribute != null) {
				AttributeInstance instance = entity.getAttribute((Attribute) this.bukkitAttribute);

				if (instance == null)
					try {
						entity.registerAttribute((Attribute) this.bukkitAttribute);
						instance = entity.getAttribute((Attribute) this.bukkitAttribute);

						if (instance == null)
							throw new IllegalStateException("Attribute " + this + " cannot be set nor registered for " + entity);

					} catch (final NoSuchMethodError ex) {
						// Only Paper supports registering attributes
						throw new IllegalStateException("Attribute " + this + " cannot be set for " + entity);
					}

				instance.setBaseValue(value);
			}

		} else if (this == MAX_HEALTH)
			entity.setMaxHealth(value);

		else if (this.getNmsName() != null) {
			final Object instance = this.getLegacyAttributeInstance(entity);

			if (instance == null)
				throw new FoException("Attribute " + this + " cannot be set for " + entity, false);

			ReflectionUtil.invoke(ReflectionUtil.getMethod(instance.getClass(), "setValue", double.class), instance, value);
		}
	}

	/**
	 * 若此属性可应用于给定实体则返回 true
	 *
	 * @param entity
	 * @return
	 */
	public final boolean canApply(@NonNull final LivingEntity entity) {
		if (hasAttributeClass) {
			if (this.bukkitAttribute != null) {
				final AttributeInstance instance = entity.getAttribute((Attribute) this.bukkitAttribute);

				return instance != null;
			}

		} else if (this == MAX_HEALTH)
			return true;

		else if (this.getNmsName() != null) {
			final Object instance = this.getLegacyAttributeInstance(entity);

			return instance != null;
		}

		return false;
	}

	private Object getLegacyAttributeInstance(final Entity entity) {
		final Object nmsEntity = ReflectionUtil.invoke("getHandle", entity);
		final Class<?> genericAttribute = Remain.getNMSClass("GenericAttributes", "net.minecraft.world.entity.ai.attributes.GenericAttributes");

		final Object iAttribute = ReflectionUtil.getStaticFieldContent(genericAttribute, this.getNmsName());

		final Class<?> nmsLiving = Remain.getNMSClass("EntityLiving", "N/A");
		final Method method = ReflectionUtil.getMethod(nmsLiving, "getAttributeInstance", Remain.getNMSClass("IAttribute", "N/A"));

		return ReflectionUtil.invoke(method, nmsEntity, iAttribute);
	}
}