package org.mineacademy.fo.enchant;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SimpleEnchantmentTarget {

	/**
	 * 允许该附魔用于盔甲
	 */
	ARMOR("enchantable/armor") {
		@Override
		public boolean includes(Material item) {
			return ARMOR_FEET.includes(item)
					|| ARMOR_LEGS.includes(item)
					|| ARMOR_HEAD.includes(item)
					|| ARMOR_CHEST.includes(item);
		}
	},

	/**
	 * 允许该附魔用于脚部盔甲
	 */
	ARMOR_FEET("enchantable/foot_armor") {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_BOOTS)
					|| item.equals(Material.CHAINMAIL_BOOTS)
					|| item.equals(Material.IRON_BOOTS)
					|| item.equals(Material.DIAMOND_BOOTS)
					|| item.equals(CompMaterial.GOLDEN_BOOTS.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_BOOTS.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于腿部盔甲
	 */
	ARMOR_LEGS("enchantable/leg_armor") {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_LEGGINGS)
					|| item.equals(Material.CHAINMAIL_LEGGINGS)
					|| item.equals(Material.IRON_LEGGINGS)
					|| item.equals(Material.DIAMOND_LEGGINGS)
					|| item.equals(CompMaterial.GOLDEN_LEGGINGS.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_LEGGINGS.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于胸部盔甲
	 */
	ARMOR_CHEST("enchantable/chest_armor") {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_CHESTPLATE)
					|| item.equals(Material.CHAINMAIL_CHESTPLATE)
					|| item.equals(Material.IRON_CHESTPLATE)
					|| item.equals(Material.DIAMOND_CHESTPLATE)
					|| item.equals(CompMaterial.GOLDEN_CHESTPLATE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_CHESTPLATE.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于头部盔甲
	 */
	ARMOR_HEAD("enchantable/head_armor") {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.LEATHER_HELMET)
					|| item.equals(Material.CHAINMAIL_HELMET)
					|| item.equals(Material.DIAMOND_HELMET)
					|| item.equals(Material.IRON_HELMET)
					|| item.equals(CompMaterial.GOLDEN_HELMET.getMaterial())
					|| item.equals(CompMaterial.TURTLE_HELMET.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_HELMET.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于武器（剑）
	 */
	WEAPON("enchantable/sharp_weapon") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_SWORD.getMaterial())
					|| item.equals(Material.STONE_SWORD)
					|| item.equals(Material.IRON_SWORD)
					|| item.equals(Material.DIAMOND_SWORD)
					|| item.equals(CompMaterial.GOLDEN_SWORD.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_SWORD.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于工具（锹、镐、斧）
	 */
	DIGGER("enchantable/mining") {
		@Override
		public boolean includes(Material item) {
			return SHOVEL.includes(item)
					|| PICKAXE.includes(item)
					|| AXE.includes(item)
					|| HOE.includes(item);
		}
	},

	/**
	 * 允许该附魔用于锹类工具。
	 */
	SHOVEL("shovels") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.STONE_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.IRON_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.DIAMOND_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.GOLDEN_SHOVEL.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_SHOVEL.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于镐类工具。
	 */
	PICKAXE("pickaxes") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_PICKAXE.getMaterial())
					|| item.equals(Material.STONE_PICKAXE)
					|| item.equals(Material.IRON_PICKAXE)
					|| item.equals(Material.DIAMOND_PICKAXE)
					|| item.equals(CompMaterial.GOLDEN_PICKAXE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_PICKAXE.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于锄类工具。
	 */
	AXE("axes") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_AXE.getMaterial())
					|| item.equals(Material.STONE_AXE)
					|| item.equals(Material.IRON_AXE)
					|| item.equals(Material.DIAMOND_AXE)
					|| item.equals(CompMaterial.GOLDEN_AXE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_AXE.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于锄类工具。
	 */
	HOE("hoes") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.WOODEN_HOE.getMaterial())
					|| item.equals(Material.STONE_HOE)
					|| item.equals(Material.IRON_HOE)
					|| item.equals(Material.DIAMOND_HOE)
					|| item.equals(CompMaterial.GOLDEN_HOE.getMaterial())
					|| item.equals(CompMaterial.NETHERITE_HOE.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于弓。
	 */
	BOW("enchantable/bow") {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.BOW);
		}
	},

	/**
	 * 允许该附魔用于钓鱼竿。
	 */
	FISHING_ROD("enchantable/fishing") {
		@Override
		public boolean includes(Material item) {
			return item.equals(Material.FISHING_ROD);
		}
	},

	/**
	 * 允许该附魔用于有耐久度的物品。
	 */
	BREAKABLE("enchantable/durability") {
		@Override
		public boolean includes(Material item) {
			return item.getMaxDurability() > 0 && item.getMaxStackSize() == 1;
		}
	},

	/**
	 * 允许该附魔用于可穿戴物品。
	 */
	WEARABLE("enchantable/equippable") {
		@Override
		public boolean includes(Material item) {
			return ARMOR.includes(item)
					|| ELYTRA.includes(item)
					|| item.equals(CompMaterial.CARVED_PUMPKIN.getMaterial())
					|| item.equals(CompMaterial.JACK_O_LANTERN.getMaterial())
					|| item.equals(CompMaterial.SKELETON_SKULL.getMaterial())
					|| item.equals(CompMaterial.WITHER_SKELETON_SKULL.getMaterial())
					|| item.equals(CompMaterial.ZOMBIE_HEAD.getMaterial())
					|| item.equals(CompMaterial.PLAYER_HEAD.getMaterial())
					|| item.equals(CompMaterial.CREEPER_HEAD.getMaterial())
					|| item.equals(CompMaterial.DRAGON_HEAD.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于鞘翅。
	 */
	ELYTRA("enchantable/mace") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.ELYTRA.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于三叉戟。
	 */
	TRIDENT("enchantable/trident") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.TRIDENT.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于弩。
	 */
	CROSSBOW("enchantable/crossbow") {
		@Override
		public boolean includes(Material item) {
			return item.equals(CompMaterial.CROSSBOW.getMaterial());
		}
	},

	/**
	 * 允许该附魔用于可消失的物品。
	 */
	VANISHABLE("enchantable/vanishing") {
		@Override
		public boolean includes(Material item) {
			return BREAKABLE.includes(item) || WEARABLE.includes(item) && !item.equals(CompMaterial.ELYTRA.getMaterial()) || item.equals(Material.COMPASS);
		}
	},

	/**
	 * @deprecated 请使用 {@link #DIGGER}
	 */
	@Deprecated
	TOOL("enchantable/mining") {
		@Override
		public boolean includes(Material item) {
			throw new RuntimeException("Please use SimpleEnchantmentTarget.DIGGER");
		}
	};

	@Getter
	private final String nmsName;

	public abstract boolean includes(Material item);

	public boolean includes(ItemStack item) {
		return this.includes(item.getType());
	}
}
