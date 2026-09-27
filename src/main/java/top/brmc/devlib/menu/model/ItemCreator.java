package top.brmc.devlib.menu.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.brmc.devlib.Common;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.Valid;
import top.brmc.devlib.enchant.SimpleEnchantment;
import top.brmc.devlib.remain.CompColor;
import top.brmc.devlib.remain.CompEnchantment;
import top.brmc.devlib.remain.CompItemFlag;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.remain.CompMetadata;
import top.brmc.devlib.remain.CompMonsterEgg;
import top.brmc.devlib.remain.CompProperty;
import top.brmc.devlib.remain.Remain;
import top.brmc.devlib.remain.nbt.NBTItem;

import com.google.common.collect.MultimapBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * ItemCreator 让你可以轻松创建高度自定义的 {@link ItemStack}：
 * 只需调用静态的 "of" 方法，自定义你的物品，然后
 * 调用 {@link #make()} 将其转换为 Bukkit ItemStack。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemCreator {

	/**
	 * 自动插入在每行描述前的描述前缀。
	 * 默认为 "&7"，使描述显示为灰色，而不是原版的粉色斜体。
	 */
	@Setter
	@Getter
	@Nullable
	private static String lorePrefix = "&7";

	/**
	 * 用于开始构建的 {@link ItemStack}（如果有）。此项与 {@link #material} 必须设置其一。
	 */
	@Nullable
	private ItemStack item;

	/**
	 * 用于开始构建的物品元数据（如果有）。上面的参数
	 * 会覆盖此项。
	 */
	@Nullable
	private ItemMeta meta;

	/**
	 * 用于开始构建的 {@link CompMaterial}（如果有）。此项与 {@link #item} 必须设置其一。
	 */
	@Nullable
	private CompMaterial material;

	/**
	 * 物品数量。
	 */
	private int amount = -1;

	/**
	 * 物品损伤值。
	 */
	private int damage = -1;

	/**
	 * 物品名称（& 颜色代码会自动替换）。
	 */
	@Getter
	private String name;

	/**
	 * 此物品的描述（& 颜色代码会自动替换）。
	 */
	private final List<String> lores = new ArrayList<>();

	/**
	 * 应用到物品上的附魔。
	 */
	private final Map<Enchantment, Integer> enchants = new HashMap<>();

	/**
	 * {@link CompItemFlag}。
	 */
	private final List<CompItemFlag> flags = new ArrayList<>();

	/**
	 * 物品是否不可破坏？
	 */
	private boolean unbreakable = false;

	/**
	 * 颜色，适用于你的物品为 {@link LeatherArmorMeta}，
	 * 或属于染色玻璃、羊毛等选定的兼容物品列表的情况。
	 */
	@Nullable
	private CompColor color;

	/**
	 * 是否隐藏物品的所有标签（附魔、属性等）？
	 */
	private boolean hideTags = false;

	/**
	 * 物品的自定义模型数据
	 */
	@Nullable
	private Integer modelData;

	/**
	 * 是否为物品添加发光效果？（添加一个假附魔并使用 {@link ItemFlag}
	 * 隐藏它）。在旧版 MC 上该附魔是可见的。
	 */
	private boolean glow = false;

	/**
	 * 头颅主人，适用于物品为头颅的情况。
	 */
	@Nullable
	private String skullOwner;

	/**
	 * 头颅 URL，适用于物品为头颅的情况。
	 */
	@Nullable
	private String skullUrl;

	/**
	 * 注入到物品中的自定义隐藏数据列表。
	 */
	private final Map<String, String> tags = new HashMap<>();

	/**
	 * 如果这是一本书，可以在此设置其新页面。
	 */
	@Nullable
	private List<String> bookPages = null;

	/**
	 * 如果这是一本书，可以在此设置其作者。
	 */
	@Nullable
	private String bookAuthor;

	/**
	 * 如果这是一本书，可以在此设置其标题。
	 */
	@Nullable
	private String bookTitle;

	// ----------------------------------------------------------------------------------------
	// Builder methods
	// ----------------------------------------------------------------------------------------

	/**
	 * 设置此物品的 ItemStack。我们会在此 ItemStack 上重新应用其他所有属性，
	 * 请确保它们兼容（例如 skullOwner 需要头颅 ItemStack 等）
	 *
	 * @param item
	 * @return
	 */
	public ItemCreator item(ItemStack item) {
		this.item = item;

		return this;
	}

	/**
	 * 设置用于开始构建的 ItemMeta。此类中的其他所有属性
	 * 都会基于此元数据构建，并具有更高优先级。
	 *
	 * @param meta
	 * @return
	 */
	public ItemCreator meta(ItemMeta meta) {
		this.meta = meta;

		return this;
	}

	/**
	 * 设置物品的材质。如果已经设置了物品堆，
	 * 此材质具有更高优先级。
	 *
	 * @param material
	 * @return
	 */
	public ItemCreator material(CompMaterial material) {
		this.material = material;

		return this;
	}

	/**
	 * 设置要创建的 ItemStack 数量。
	 *
	 * @param amount
	 * @return
	 */
	public ItemCreator amount(int amount) {
		this.amount = amount;

		return this;
	}

	/**
	 * 设置 ItemStack 的损伤值。注意这只对
	 * 某些物品（例如工具）有效。
	 *
	 * 参见 Damageable#setDamage(int)
	 *
	 * @param damage
	 * @return
	 */
	public ItemCreator damage(int damage) {
		this.damage = damage;

		return this;
	}

	/**
	 * 为物品设置自定义名称（& 颜色代码会自动替换）。
	 *
	 * @param name
	 * @return
	 */
	public ItemCreator name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * 移除物品之前的所有描述。如果你用 ItemStack 初始化了此类
	 * 或已经设置了物品堆，可用它清除旧描述。
	 *
	 * @return
	 */
	public ItemCreator clearLore() {
		this.lores.clear();

		return this;
	}

	/**
	 * 将给定描述追加到现有物品描述的末尾。
	 *
	 * @param lore
	 * @return
	 */
	public ItemCreator lore(String... lore) {
		return this.lore(Arrays.asList(lore));
	}

	/**
	 * 将给定描述追加到现有物品描述的末尾。
	 *
	 * @param lore
	 * @return
	 */
	public ItemCreator lore(List<String> lore) {
		this.lores.addAll(lore);

		return this;
	}

	/**
	 * 为物品添加给定附魔。
	 *
	 * @param enchantment
	 * @return
	 */
	public ItemCreator enchant(SimpleEnchantment enchantment) {
		return this.enchant(enchantment.toBukkit(), 1);
	}

	/**
	 * 为物品添加给定附魔。
	 *
	 * @param enchantment
	 * @return
	 */
	public ItemCreator enchant(Enchantment enchantment) {
		return this.enchant(enchantment, 1);
	}

	/**
	 * 为物品添加给定附魔。
	 *
	 * @param enchantment
	 * @param level
	 * @return
	 */
	public ItemCreator enchant(SimpleEnchantment enchantment, int level) {
		this.enchants.put(enchantment.toBukkit(), level);

		return this;
	}

	/**
	 * 为物品添加给定附魔。使用 {@link CompEnchantment} 可获得
	 * 熟悉的名称。
	 *
	 * @param enchantment
	 * @param level
	 * @return
	 */
	public ItemCreator enchant(Enchantment enchantment, int level) {
		this.enchants.put(enchantment, level);

		return this;
	}

	/**
	 * @see #flags(CompItemFlag...)
	 * @deprecated 请改为调用 {@link #flags(CompItemFlag...)}
	 * @param flags
	 * @return
	 */
	@Deprecated
	public ItemCreator flag(CompItemFlag... flags) {
		return this.flags(flags);
	}

	/**
	 * 为物品添加给定标志。
	 *
	 * @param flags
	 * @return
	 */
	public ItemCreator flags(CompItemFlag... flags) {
		this.flags.addAll(Arrays.asList(flags));

		return this;
	}

	/**
	 * 设置物品为不可破坏。
	 *
	 * @param unbreakable
	 * @return
	 */
	public ItemCreator unbreakable(boolean unbreakable) {
		this.unbreakable = unbreakable;

		return this;
	}

	/**
	 * 设置染色或染料颜色，适用于你的物品为 {@link LeatherArmorMeta}，
	 * 或属于染色玻璃、羊毛等选定的兼容物品列表的情况。
	 *
	 * @param color
	 * @return
	 */
	public ItemCreator color(CompColor color) {
		this.color = color;

		return this;
	}

	/**
	 * 移除附加在物品描述末尾的所有附魔、属性及其他标签，
	 * 这些标签通常显示为蓝色。
	 *
	 * @param hideTags
	 * @return
	 */
	public ItemCreator hideTags(boolean hideTags) {
		this.hideTags = hideTags;

		return this;
	}

	/**
	 * 设置此物品的自定义模型数据，兼容 MC 1.14+
	 *
	 * @param modelData
	 * @return
	 */
	public ItemCreator modelData(int modelData) {
		this.modelData = modelData;

		return this;
	}

	/**
	 * 使此物品发光。若已有附魔则忽略。如需隐藏附魔描述，
	 * 请改为调用 {@link #hideTags(boolean)}。
	 *
	 * @param glow
	 * @return
	 */
	public ItemCreator glow(boolean glow) {
		this.glow = glow;

		return this;
	}

	/**
	 * 设置此物品的头颅主人，仅当物品为头颅时有效。
	 *
	 * 参见 {@link SkullCreator}
	 *
	 * @param skullOwner
	 * @return
	 */
	public ItemCreator skullOwner(String skullOwner) {
		this.skullOwner = skullOwner;

		return this;
	}

	/**
	 * 设置此物品的头颅皮肤 URL，仅当物品为头颅时有效。
	 *
	 * 参见 {@link SkullCreator}
	 *
	 * @param skullUrl
	 * @return
	 */
	public ItemCreator skullUrl(String skullUrl) {
		this.skullUrl = skullUrl;

		return this;
	}

	/**
	 * 为物品添加不可见的自定义标签，在大多数服务器上
	 * 它会在保存/重启后保留（为安全起见请自行检查）。
	 *
	 * 要获取该标签，请使用 CompMetadata#getMetadata
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public ItemCreator tag(String key, String value) {
		this.tags.put(key, value);

		return this;
	}

	/**
	 * 如果这是一本书，设置其页面。
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(String... pages) {
		return this.bookPages(Arrays.asList(pages));
	}

	/**
	 * 如果这是一本书，设置其页面。
	 *
	 * @param pages
	 * @return
	 */
	public ItemCreator bookPages(List<String> pages) {

		if (this.bookPages == null)
			this.bookPages = new ArrayList<>();

		this.bookPages.addAll(pages);

		return this;
	}

	/**
	 * 如果这是一本书，设置其作者。
	 *
	 * @param bookAuthor
	 * @return
	 */
	public ItemCreator bookAuthor(String bookAuthor) {
		this.bookAuthor = bookAuthor;

		return this;
	}

	/**
	 * 如果这是一本书，设置其标题。
	 *
	 * @param bookTitle
	 * @return
	 */
	public ItemCreator bookTitle(String bookTitle) {
		this.bookTitle = bookTitle;

		return this;
	}

	// ----------------------------------------------------------------------------------------
	// Convenience give methods
	// ----------------------------------------------------------------------------------------

	/**
	 * 便捷方法，快速将此物品添加到玩家背包
	 *
	 * @param player
	 */
	public void give(final Player player) {
		player.getInventory().addItem(this.make());
	}

	/**
	 * 便捷方法，在给定位置掉落此物品
	 *
	 * @param location
	 */
	public void drop(final Location location) {
		location.getWorld().dropItem(location, this.make());
	}

	// ----------------------------------------------------------------------------------------
	// Constructing items
	// ----------------------------------------------------------------------------------------

	/**
	 * @deprecated 已不再需要，只会返回自身。不要再调用 "built().make()"，现在直接调用 "make()" 即可
	 * @return
	 */
	@Deprecated
	public ItemCreator build() {
		return this;
	}

	/**
	 *
	 * @deprecated 现在只是返回 {@link #make()}，请直接调用它
	 * @return
	 */
	@Deprecated
	public ItemStack makeSurvival() {
		return this.make();
	}

	/**
	 * 创建一个隐藏所有属性的不可破坏物品，适合在菜单中使用。
	 *
	 * @return 隐藏所有属性的新菜单工具
	 */
	public ItemStack makeMenuTool() {
		this.hideTags = true;

		return this.make();
	}

	/**
	 * 根据此类的所有参数构造有效的 {@link ItemStack}。
	 *
	 * @return 构建完成的物品
	 */
	public ItemStack make() {

		// First, make sure the ItemStack is not null (it can be null if you create this class only using material)
		Valid.checkBoolean(this.material != null || this.item != null, "Material or item must be set!");

		ItemStack compiledItem = this.item != null ? this.item.clone() : this.material.toItem();

		Object compiledMeta = Remain.hasItemMeta() ? this.meta != null ? this.meta.clone() : compiledItem.getItemMeta() : null;

		// Override with given material
		if (this.material != null) {
			compiledItem.setType(this.material.getMaterial());

			if (MinecraftVersion.olderThan(V.v1_13))
				compiledItem.setData(new MaterialData(this.material.getMaterial(), this.material.getData()));
		}

		// Skip if air
		if (CompMaterial.isAir(compiledItem.getType()))
			return compiledItem;

		// Apply specific material color if possible
		color:
		if (this.color != null)
			if (compiledItem.getType().toString().contains("LEATHER")) {
				if (MinecraftVersion.atLeast(V.v1_4)) {
					Valid.checkBoolean(compiledMeta instanceof LeatherArmorMeta, "Expected a leather item, cannot apply color to " + compiledItem);

					((LeatherArmorMeta) compiledMeta).setColor(this.color.getColor());
				}
			} else // Hack: If you put WHITE_WOOL and a color, we automatically will change the material to the colorized version
			if (MinecraftVersion.atLeast(V.v1_13)) {
				final String dye = this.color.getDye().toString();
				final List<String> colorableMaterials = Arrays.asList("BANNER", "BED", "CARPET", "CONCRETE", "GLAZED_TERRACOTTA", "SHULKER_BOX", "STAINED_GLASS",
						"STAINED_GLASS_PANE", "TERRACOTTA", "WALL_BANNER", "WOOL");

				for (final String material : colorableMaterials) {
					final String suffix = "_" + material;

					if (compiledItem.getType().toString().endsWith(suffix)) {
						compiledItem.setType(Material.valueOf(dye + suffix));

						break color;
					}
				}
			} else
				try {
					final byte dataValue = this.color.getDye().getWoolData();

					compiledItem.setData(new MaterialData(compiledItem.getType(), dataValue));
					compiledItem.setDurability(dataValue);

				} catch (final NoSuchMethodError err) {
					// Ancient MC, ignore
				}

		// Fix monster eggs
		if (compiledItem.getType().toString().endsWith("SPAWN_EGG") || compiledItem.getType().toString().equals("MONSTER_EGG")) {

			EntityType entity = null;

			if (MinecraftVersion.olderThan(V.v1_13)) { // Try to find it if already exists
				final EntityType pre = CompMonsterEgg.getEntity(compiledItem);

				if (pre != null && pre != EntityType.UNKNOWN)
					entity = pre;
			}

			if (entity == null) {
				final String itemName = compiledItem.getType().toString();

				String entityRaw = itemName.replace("_SPAWN_EGG", "");

				if (entityRaw.equals("MONSTER_EGG") && this.material != null && this.material.toString().endsWith("SPAWN_EGG"))
					entityRaw = this.material.toString().replace("_SPAWN_EGG", "");

				if ("MOOSHROOM".equals(entityRaw))
					entityRaw = "MUSHROOM_COW";

				else if ("ZOMBIE_PIGMAN".equals(entityRaw))
					entityRaw = "PIG_ZOMBIE";

				try {
					entity = EntityType.valueOf(entityRaw);

				} catch (final Throwable t) {

					// Probably version incompatible
					Common.log("The following item could not be transformed into " + entityRaw + " egg, item: " + compiledItem);
				}
			}

			if (entity != null)
				compiledMeta = CompMonsterEgg.setEntity(compiledItem, entity).getItemMeta();
		}

		if (this.damage != -1) {

			try {
				compiledItem.setDurability((short) this.damage);
			} catch (final Throwable t) {
			}

			try {
				if (compiledMeta instanceof org.bukkit.inventory.meta.Damageable)
					((org.bukkit.inventory.meta.Damageable) compiledMeta).setDamage(this.damage);
			} catch (final Throwable t) {
			}
		}

		if (compiledMeta instanceof SkullMeta) {
			if (this.skullOwner != null)
				((SkullMeta) compiledMeta).setOwner(this.skullOwner);

			if (this.skullUrl != null)
				compiledMeta = SkullCreator.metaWithUrl((SkullMeta) compiledMeta, this.skullUrl);
		}

		if (compiledMeta instanceof BookMeta) {
			final BookMeta bookMeta = (BookMeta) compiledMeta;

			if (this.bookPages != null)
				bookMeta.setPages(Common.colorize(this.bookPages));

			if (this.bookAuthor != null)
				bookMeta.setAuthor(Common.getOrEmpty(this.bookAuthor));

			if (this.bookTitle != null)
				bookMeta.setTitle(Common.getOrEmpty(this.bookTitle));

			// Fix "Corrupted NBT tag" error when any of these fields are not set
			if (bookMeta.getPages() == null)
				bookMeta.setPages(Arrays.asList(""));

			if (bookMeta.getAuthor() == null)
				bookMeta.setAuthor("Anonymous");

			if (bookMeta.getTitle() == null)
				bookMeta.setTitle("Book");
		}

		if (compiledMeta instanceof ItemMeta) {
			if (this.glow && this.enchants.isEmpty()) {
				((ItemMeta) compiledMeta).addEnchant(CompEnchantment.DURABILITY, 1, true);

				this.flags.add(CompItemFlag.HIDE_ENCHANTS);
			}

			for (final Map.Entry<Enchantment, Integer> entry : this.enchants.entrySet()) {
				final Enchantment enchant = entry.getKey();
				final int level = entry.getValue();

				if (compiledMeta instanceof EnchantmentStorageMeta)
					((EnchantmentStorageMeta) compiledMeta).addStoredEnchant(enchant, level, true);

				else
					((ItemMeta) compiledMeta).addEnchant(enchant, level, true);
			}

			if (this.name != null && !"".equals(this.name))
				((ItemMeta) compiledMeta).setDisplayName(Common.colorize("&r&f" + this.name));

			if (!this.lores.isEmpty()) {
				final List<String> coloredLores = new ArrayList<>();

				for (final String lore : this.lores)
					if (lore != null)
						for (final String subLore : lore.split("\n"))
							coloredLores.add(Common.colorize((lorePrefix != null ? lorePrefix : "") + subLore));

				((ItemMeta) compiledMeta).setLore(coloredLores);
			}
		}

		if (this.unbreakable) {
			this.flags.add(CompItemFlag.HIDE_ATTRIBUTES);
			this.flags.add(CompItemFlag.HIDE_UNBREAKABLE);

			CompProperty.UNBREAKABLE.apply(compiledMeta, true);
		}

		if (this.hideTags)
			for (final CompItemFlag f : CompItemFlag.values())
				if (!this.flags.contains(f))
					this.flags.add(f);

		if (this.hideTags || this.flags.contains(CompItemFlag.HIDE_ATTRIBUTES))
			try {
				((ItemMeta) compiledMeta).setAttributeModifiers(MultimapBuilder.hashKeys().hashSetValues().build());

			} catch (final Throwable t) {
				// ignore
			}

		for (final CompItemFlag flag : this.flags)
			try {
				((ItemMeta) compiledMeta).addItemFlags(ItemFlag.valueOf(flag.toString()));
			} catch (final Throwable t) {
			}

		// Set custom model data
		if (this.modelData != null && MinecraftVersion.atLeast(V.v1_14))
			try {
				((ItemMeta) compiledMeta).setCustomModelData(this.modelData);
			} catch (final Throwable t) {
			}

		// Override with custom amount if set
		if (this.amount != -1)
			compiledItem.setAmount(this.amount);

		// Apply Bukkit metadata
		if (compiledMeta instanceof ItemMeta)
			compiledItem.setItemMeta((ItemMeta) compiledMeta);

		//
		// From now on we have to re-set the item
		//

		// 1.7.10 hack to add glow, requires no enchants
		if (this.glow && MinecraftVersion.equals(V.v1_7) && (this.enchants == null || this.enchants.isEmpty())) {
			final NBTItem nbtItem = new NBTItem(compiledItem);

			nbtItem.removeKey("ench");
			nbtItem.addCompound("ench");

			compiledItem = nbtItem.getItem();
		}

		// Apply NBT tags
		if (MinecraftVersion.atLeast(V.v1_7))
			for (final Entry<String, String> entry : this.tags.entrySet())
				compiledItem = CompMetadata.setMetadata(compiledItem, entry.getKey(), entry.getValue());

		else if (!this.tags.isEmpty() && this.item != null)
			Common.log("Item had unsupported tags " + this.tags + " that are not supported on MC " + MinecraftVersion.getFullVersion() + " Item: " + compiledItem);

		return compiledItem;
	}

	// ----------------------------------------------------------------------------------------
	// Static access
	// ----------------------------------------------------------------------------------------

	/**
	 * 便捷方法，获取已设置材质、名称和描述的新物品创建器
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator of(final CompMaterial material, final String name, @NonNull final Collection<String> lore) {
		return of(material, name, Common.toArray(lore));
	}

	/**
	 * 便捷方法，获取已设置材质、名称和描述的新物品创建器
	 *
	 * @param material
	 * @param name
	 * @param lore
	 * @return 新的物品创建器
	 */
	public static ItemCreator of(final CompMaterial material, final String name, @NonNull final String... lore) {
		return new ItemCreator().material(material).name(name).lore(lore).hideTags(true);
	}

	/**
	 * 便捷方法，获取羊毛
	 *
	 * @param color 羊毛颜色
	 * @return 新的物品创建器
	 */
	public static ItemCreator ofWool(final CompColor color) {
		return of(CompMaterial.makeWool(color, 1)).color(color);
	}

	/**
	 * 便捷方法，获取刷怪蛋
	 *
	 * @param entityType
	 * @return
	 */
	public static ItemCreator ofEgg(final EntityType entityType) {
		return of(CompMonsterEgg.makeEgg(entityType));
	}

	/**
	 * 便捷方法，获取刷怪蛋
	 *
	 * @param entityType
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofEgg(final EntityType entityType, String name, String... lore) {
		return of(CompMonsterEgg.makeEgg(entityType)).name(name).lore(lore);
	}

	/**
	 * 便捷方法，创建药水
	 *
	 * @param potionEffect
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect) {
		return ofPotion(potionEffect, 1);
	}

	/**
	 * 便捷方法，创建药水
	 *
	 * @param potionEffect
	 * @param durationTicks
	 * @param level
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int durationTicks, int level) {
		return ofPotion(potionEffect, durationTicks, level, null);
	}

	/**
	 * 便捷方法，创建药水
	 *
	 * @param potionEffect
	 * @param level
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int level) {
		return ofPotion(potionEffect, Integer.MAX_VALUE, level, null);
	}

	/**
	 * 便捷方法，创建药水
	 *
	 * @param potionEffect
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, String name, String... lore) {
		return ofPotion(potionEffect, Integer.MAX_VALUE, 1, name, lore);
	}

	/**
	 * 便捷方法，创建药水
	 *
	 * @param effect
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffect effect, String name, String... lore) {
		return ofPotion(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier() + 1, name, lore);
	}

	/**
	 * 便捷方法，创建药水
	 *
	 * @param potionEffect
	 * @param durationTicks
	 * @param level
	 * @param name
	 * @param lore
	 * @return
	 */
	public static ItemCreator ofPotion(final PotionEffectType potionEffect, int durationTicks, int level, String name, String... lore) {
		final boolean noLevel = level == 0;
		final ItemStack item = new ItemStack(level == 0 ? CompMaterial.GLASS_BOTTLE.getMaterial() : CompMaterial.POTION.getMaterial());

		if (!noLevel)
			Remain.setPotion(item, potionEffect, durationTicks, level);

		final ItemCreator builder = of(item);

		if (name != null)
			builder.name(name);

		if (lore != null)
			builder.lore(lore);

		return builder;
	}

	/**
	 * 便捷方法，获取现有物品堆的创建器
	 *
	 * @param item 现有物品堆
	 * @return 新的物品创建器
	 */
	public static ItemCreator of(final ItemStack item) {
		final ItemCreator builder = new ItemCreator();
		final ItemMeta meta = item.getItemMeta();

		if (meta != null && meta.getLore() != null)
			builder.lore(meta.getLore());

		return builder.item(item);
	}

	/**
	 * 根据材质获取新的物品创建器
	 * @deprecated 为了即将到来的 Foundation v7 迁移，请改为调用 {@link #of(CompMaterial)}。
	 *
	 * @param mat 现有材质
	 * @return 新的物品创建器
	 */
	@Deprecated
	public static ItemCreator fromMaterial(final CompMaterial mat) {
		return of(mat);
	}

	/**
	 * 根据材质获取新的物品创建器
	 *
	 * @param mat 现有材质
	 * @return 新的物品创建器
	 */
	public static ItemCreator of(final CompMaterial mat) {
		Valid.checkNotNull(mat, "Material cannot be null!");

		return new ItemCreator().material(mat);
	}
}