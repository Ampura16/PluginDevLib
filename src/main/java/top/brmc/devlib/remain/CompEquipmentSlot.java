package top.brmc.devlib.remain;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.ValidCore;
import top.brmc.devlib.menu.model.ItemCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 表示 EquipmentSlot
 */
@RequiredArgsConstructor
public enum CompEquipmentSlot {

	HAND("HAND", "HAND"),
	/**
	 * 需要 Minecraft 1.9+
	 */
	OFF_HAND("OFF_HAND", "OFF_HAND"),
	HEAD("HEAD", "HELMET"),
	CHEST("CHEST", "CHESTPLATE"),
	LEGS("LEGS", "LEGGINGS"),
	FEET("FEET", "BOOTS"),
	/**
	 * 狼、马和羊驼的身体护甲，需要 Minecraft 1.20.5+
	 */
	BODY("BODY", "BODY"),
	/**
	 * 可骑乘动物的鞍，需要 Minecraft 1.21.5+
	 */
	SADDLE("SADDLE", "SADDLE");

	/**
	 * 可本地化的键
	 */
	@Getter
	private final String key;

	/**
	 * 备用的 Bukkit 名称。
	 */
	private final String bukkitName;

	/**
	 * 将给定物品应用到给定实体的此装备槽
	 *
	 * @param entity
	 * @param itemCreator
	 */
	public void applyTo(final LivingEntity entity, final ItemCreator itemCreator) {
		this.applyTo(entity, itemCreator.make(), null);
	}

	/**
	 * 将给定物品应用到给定实体的此装备槽，
	 * 可选掉落几率范围为 0 到 1.0
	 *
	 * @param entity
	 * @param itemCreator
	 * @param dropChance
	 */
	public void applyTo(final LivingEntity entity, final ItemCreator itemCreator, final Double dropChance) {
		this.applyTo(entity, itemCreator.make(), dropChance);
	}

	/**
	 * 将给定物品应用到给定实体的此装备槽
	 *
	 * @param entity
	 * @param material
	 */
	public void applyTo(final LivingEntity entity, final CompMaterial material) {
		this.applyTo(entity, material.toItem(), null);
	}

	/**
	 * 将给定物品应用到给定实体的此装备槽，
	 * 可选掉落几率范围为 0 到 1.0
	 *
	 * @param entity
	 * @param material
	 * @param dropChance
	 */
	public void applyTo(final LivingEntity entity, final CompMaterial material, final Double dropChance) {
		this.applyTo(entity, material.toItem(), dropChance);
	}

	/**
	 * 将给定物品应用到给定实体的此装备槽
	 *
	 * @param entity
	 * @param material
	 */
	public void applyTo(final LivingEntity entity, final Material material) {
		this.applyTo(entity, new ItemStack(material), null);
	}

	/**
	 * 将给定物品应用到给定实体的此装备槽，
	 * 可选掉落几率范围为 0 到 1.0
	 *
	 * @param entity
	 * @param material
	 * @param dropChance
	 */
	public void applyTo(final LivingEntity entity, final Material material, final Double dropChance) {
		this.applyTo(entity, new ItemStack(material), dropChance);
	}

	/**
	 * 将给定物品应用到给定实体的此装备槽
	 *
	 * @param entity
	 * @param item
	 */
	public void applyTo(final LivingEntity entity, final ItemStack item) {
		this.applyTo(entity, item, null);
	}

	/**
	 * 清空此装备槽。
	 *
	 * @param entity
	 */
	public void clear(final LivingEntity entity) {
		this.applyTo(entity, (ItemStack) null, (Double) null);
	}

	/**
	 * 将给定物品应用到给定实体的此装备槽，
	 * 可选掉落几率范围为 0 到 1.0
	 *
	 * @param entity
	 * @param item
	 * @param dropChance
	 */
	public void applyTo(@NonNull final LivingEntity entity, ItemStack item, final Double dropChance) {
		final EntityEquipment equipment = entity instanceof LivingEntity ? entity.getEquipment() : null;
		ValidCore.checkNotNull(equipment);

		final boolean lacksDropChance = entity instanceof HumanEntity || entity.getType().toString().equals("ARMOR_STAND") || entity.getType().toString().equals("MANNEQUIN");

		if (MinecraftVersion.olderThan(V.v1_9) && item == null)
			item = new ItemStack(Material.AIR);

		switch (this) {
			case HAND:
				if (entity instanceof Enderman) {
					final Enderman enderman = (Enderman) entity;

					if (item != null && item.getType().isBlock())
						try {
							enderman.setCarriedBlock(Bukkit.createBlockData(item.getType()));

						} catch (final Throwable t) {
							enderman.setCarriedMaterial(item.getData());
						}

				} else {
					equipment.setItemInHand(item);

					if (dropChance != null && !lacksDropChance)
						equipment.setItemInHandDropChance(dropChance.floatValue());
				}

				break;

			case OFF_HAND:
				ValidCore.checkBoolean(MinecraftVersion.atLeast(V.v1_9), "Setting off hand item requires Minecraft 1.9+");

				equipment.setItemInOffHand(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setItemInOffHandDropChance(dropChance.floatValue());

				break;

			case HEAD:
				equipment.setHelmet(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setHelmetDropChance(dropChance.floatValue());

				break;

			case CHEST:
				equipment.setChestplate(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setChestplateDropChance(dropChance.floatValue());

				break;

			case LEGS:
				equipment.setLeggings(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setLeggingsDropChance(dropChance.floatValue());

				break;

			case FEET:
				equipment.setBoots(item);

				if (dropChance != null && !lacksDropChance)
					equipment.setBootsDropChance(dropChance.floatValue());

				break;

			case BODY:
			case SADDLE:
				this.applyToModernSlot(equipment, item, dropChance, lacksDropChance);

				break;
		}
	}

	/*
	 * Body armor and saddles never had a dedicated EntityEquipment setter, they are
	 * only reachable through the slot based API added in Minecraft 1.9.
	 */
	private void applyToModernSlot(final EntityEquipment equipment, final ItemStack item, final Double dropChance, final boolean lacksDropChance) {
		final EquipmentSlot bukkitSlot = ReflectionUtil.lookupEnumSilent(EquipmentSlot.class, this.bukkitName);

		ValidCore.checkNotNull(bukkitSlot, "Equipment slot " + this.name() + " requires Minecraft " + (this == BODY ? "1.20.5" : "1.21.5") + "+, running on " + MinecraftVersion.getFullVersion());

		equipment.setItem(bukkitSlot, item);

		if (dropChance != null && !lacksDropChance)
			equipment.setDropChance(bukkitSlot, dropChance.floatValue());
	}

	/**
	 * 返回此装备的 Bukkit 名称，
	 * 找不到则抛出错误
	 *
	 * @return
	 */
	public String getBukkitName() {
		ValidCore.checkNotNull(this.bukkitName, "CompEquipmentSlot." + this.name() + " does not have a Bukkit counterpart!");

		return this.bukkitName;
	}

	/**
	 * 返回此装备对应的 Bukkit 装备槽，
	 * 找不到则抛出错误
	 *
	 * @return
	 */
	public EquipmentSlot toBukkit() {
		return ReflectionUtil.lookupEnum(EquipmentSlot.class, this.getBukkitName());
	}

	/**
	 * 尝试根据给定键解析装备槽，找不到则
	 * 抛出错误
	 *
	 * @param key
	 * @return
	 */
	public static CompEquipmentSlot fromKey(String key) {
		key = key.toUpperCase().replace(" ", "_");

		for (final CompEquipmentSlot slot : values())
			if (slot.key.equals(key) || slot.bukkitName.equals(key))
				return slot;

		throw new IllegalArgumentException("No such comp equipment slot: " + key + " Available: " + values());
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定颜色、
	 * 不会掉落的皮革盔甲。
	 *
	 * @param entity
	 * @param color
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color) {
		applyArmor(entity, color, 0D, new HashSet<>());
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定颜色、
	 * 不会掉落的皮革盔甲。
	 *
	 * @param entity
	 * @param color
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color, final Set<CompEquipmentSlot> ignoredSlots) {
		applyArmor(entity, color, 0D, ignoredSlots);
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定颜色的皮革盔甲
	 *
	 * @param entity
	 * @param color
	 * @param dropChance
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color, final double dropChance) {
		applyArmor(entity, color, dropChance, new HashSet<>());
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定颜色的皮革盔甲
	 *
	 * @param entity
	 * @param color
	 * @param dropChance
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, final CompColor color, final Double dropChance, final Set<CompEquipmentSlot> ignoredSlots) {
		if (!ignoredSlots.contains(HEAD))
			HEAD.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_HELMET).color(color).make(), dropChance);

		if (!ignoredSlots.contains(CHEST))
			CHEST.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_CHESTPLATE).color(color).make(), dropChance);

		if (!ignoredSlots.contains(LEGS))
			LEGS.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_LEGGINGS).color(color).make(), dropChance);

		if (!ignoredSlots.contains(FEET))
			FEET.applyTo(entity, ItemCreator.fromMaterial(CompMaterial.LEATHER_BOOTS).color(color).make(), dropChance);
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定类型的盔甲，
	 * 掉落几率为 0
	 *
	 * @param entity
	 * @param type
	 */
	public static void applyArmor(final LivingEntity entity, final Type type) {
		applyArmor(entity, type, 0d, new HashSet<>());
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定类型的盔甲，
	 * 掉落几率为 0
	 *
	 * @param entity
	 * @param type
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, final Type type, final Set<CompEquipmentSlot> ignoredSlots) {
		applyArmor(entity, type, 0d, ignoredSlots);
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定类型的盔甲
	 *
	 * @param entity
	 * @param type
	 * @param dropChance
	 */
	public static void applyArmor(final LivingEntity entity, final Type type, final double dropChance) {
		applyArmor(entity, type, dropChance, new HashSet<>());
	}

	/**
	 * 便捷快捷方法，快速为实体穿上一整套给定类型的盔甲
	 *
	 * @param entity
	 * @param type
	 * @param dropChance
	 * @param ignoredSlots
	 */
	public static void applyArmor(final LivingEntity entity, Type type, final Double dropChance, final Set<CompEquipmentSlot> ignoredSlots) {

		// Compatibility
		if (type == Type.NETHERITE && MinecraftVersion.olderThan(V.v1_16))
			type = Type.DIAMOND;

		final String name = type == Type.GOLD ? "GOLDEN" : type.toString();

		if (!ignoredSlots.contains(HEAD))
			HEAD.applyTo(entity, CompMaterial.valueOf(name + "_HELMET").toItem(), dropChance);

		if (!ignoredSlots.contains(CHEST))
			CHEST.applyTo(entity, CompMaterial.valueOf(name + "_CHESTPLATE").toItem(), dropChance);

		if (!ignoredSlots.contains(LEGS))
			LEGS.applyTo(entity, CompMaterial.valueOf(name + "_LEGGINGS").toItem(), dropChance);

		if (!ignoredSlots.contains(FEET))
			FEET.applyTo(entity, CompMaterial.valueOf(name + "_BOOTS").toItem(), dropChance);
	}

	@Override
	public String toString() {
		return this.key.toUpperCase();
	}

	/**
	 * 表示盔甲的主要材质类型，例如皮革或钻石
	 */
	public static enum Type {
		LEATHER,
		CHAINMAIL,
		IRON,
		GOLD,
		DIAMOND,
		NETHERITE;

		/**
		 * 尝试将盔甲材质（任意头盔、胸甲、护腿或靴子）
		 * 按其类型解析为材质类型（例如 iron_helmet -> iron）
		 *
		 * @param armorMaterial
		 * @return
		 */
		public static Type fromArmor(final CompMaterial armorMaterial) {
			final String n = armorMaterial.name();

			ValidCore.checkBoolean(n.contains("LEATHER") || n.contains("CHAINMAIL") || n.contains("IRON") || n.contains("GOLD") || n.contains("DIAMOND") || n.contains("NETHERITE"),
					"Only leather to netherite armors are supported, not: " + armorMaterial);

			return Type.valueOf(n.split("_")[0]);
		}
	}
}
