package org.mineacademy.fo.enchant;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.remain.CompEquipmentSlot;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 一种将自定义附魔加入 Minecraft 的简单方式
 *
 * 免责声明：Minecraft 并非为自定义附魔设计。附魔名称存储在
 * 客户端，因此自定义附魔不会显示任何内容——Foundation 会尽力
 * 拦截设置槽位的数据包并手动注入描述（需要 ProtocolLib）。
 *
 * 用法：使用前，你需要先在 {@link #registerEnchantmentHandle(Class)} 中注册自定义 NMS 类，
 * 该类需要继承 Bukkit 或 NMS 的 Enchantment 类，
 * 并实现 {@link NmsEnchant}
 *
 * 提示：如果你要为自定义事件注册，请使用 {@link SimpleEnchantment#hasEnchant(ItemStack)}
 * 确认事件确实发生在带有该附魔的物品上——不要使用 BUKKIT 的 CONTAINENCHANTMENT
 * 方法，因为某些 Minecraft 版本（或糟糕的实现）会返回 false——请检查你是否已在 NMS 内置注册表中
 * 正确注册，尤其是 1.19+。
 */
public abstract class SimpleEnchantment implements Listener {

	/*
	 * Cached for performance reasons
	 */
	private static boolean hasNamespacedKeys = MinecraftVersion.atLeast(V.v1_13);

	/**
	 * 用于匹配命名空间的模式
	 */
	private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9._-]+");

	/**
	 * 假附魔描述的默认前缀
	 */
	private static final String FO_ENCHANT_PREFIX = "&r&7";

	/**
	 * 按命名空间键注册的自定义附魔。
	 */
	private static final StrictSet<SimpleEnchantment> registeredEnchantments = new StrictSet<>();

	/**
	 * 用于包装自定义附魔而实例化的类。
	 */
	@Getter
	private static Class<? extends NmsEnchant> handleClass = null;

	/**
	 * 此附魔的名称（例如 Black Nova）
	 */
	private final String name;

	/**
	 * 命名空间格式的名称（例如 black_nova）
	 */
	private final String namespacedName;
	private final String namespacedNameWithPrefix;

	/**
	 * 此附魔的最高等级
	 */
	private final int maxLevel;

	/**
	 * 将此附魔注入 Minecraft 的实际句柄
	 */
	private NmsEnchant handle;

	/**
	 * 内部使用，用于在 MC 1.13 之前的版本中配对附魔
	 */
	@Deprecated
	private int id = -1;

	/**
	 * 使用给定名称创建新附魔
	 *
	 * @param name
	 */
	protected SimpleEnchantment(@NonNull String name, int maxLevel) {
		String namespacedName = new String(name);
		namespacedName = namespacedName.toLowerCase().replace(" ", "_");
		namespacedName = ChatUtil.replaceDiacritic(namespacedName);

		Valid.checkBoolean(VALID_NAMESPACE.matcher(namespacedName).matches(), "Enchant name must only contain English alphabet names: " + name);

		this.name = name;
		this.namespacedName = namespacedName;
		this.namespacedNameWithPrefix = "minecraft:" + this.namespacedName;
		this.maxLevel = maxLevel;

		if (handleClass != null) {
			this.handle = this.assignHandle();
			this.handle.register();

			registeredEnchantments.add(this);
		}
	}

	/*
	 * Private method to register this enchant
	 */
	private NmsEnchant assignHandle() {
		Constructor<?> constructor;

		try {
			constructor = ReflectionUtil.getConstructor(handleClass, SimpleEnchantment.class);

		} catch (final Throwable t) {
			throw new FoException("Please add one public constructor taking SimpleEnchantment as one parameter to your " + handleClass);
		}

		return (NmsEnchant) ReflectionUtil.instantiate(constructor, this);
	}

	// ------------------------------------------------------------------------------------------
	// Events
	// ------------------------------------------------------------------------------------------

	/**
	 * 当攻击者拥有此附魔时自动触发
	 *
	 * @param level   此附魔的等级
	 * @param damager
	 * @param event
	 */
	protected void onDamage(int level, LivingEntity damager, EntityDamageByEntityEvent event) {
	}

	/**
	 * 当玩家使用带有该附魔的物品点击方块/空气时自动触发
	 *
	 * @param level
	 * @param event
	 */
	protected void onInteract(int level, PlayerInteractEvent event) {
	}

	/**
	 * 当玩家手持带有此附魔的物品破坏方块时自动触发
	 *
	 * @param level
	 * @param event
	 */
	protected void onBreakBlock(int level, BlockBreakEvent event) {
	}

	/**
	 * 当弹射物由手持此物品的生物实体射出时
	 * 自动触发
	 *
	 * @param level
	 * @param shooter
	 * @param event
	 */
	protected void onShoot(int level, LivingEntity shooter, ProjectileLaunchEvent event) {
	}

	/**
	 * 当弹射物击中某物，且射击者是手持带有此附魔物品的
	 * 生物实体时自动触发
	 *
	 * @param level
	 * @param shooter
	 * @param event
	 */
	protected void onHit(int level, LivingEntity shooter, ProjectileHitEvent event) {
	}

	// ------------------------------------------------------------------------------------------
	// Our own methods
	// ------------------------------------------------------------------------------------------

	/**
	 * 返回带有此附魔的物品上显示的描述
	 * 返回 null 可隐藏描述
	 * <p>
	 * 由于 Minecraft 并不真正支持自定义附魔，
	 * 我们必须手动添加物品描述
	 *
	 * @param level
	 * @return
	 */
	public String getLore(int level) {
		return this.name + " " + MathUtil.toRoman(level);
	}

	// ------------------------------------------------------------------------------------------
	// Bukkit methods
	// ------------------------------------------------------------------------------------------

	/**
	 * 返回此附魔在当前 Minecraft 版本中是否可用。
	 *
	 * @return
	 */
	public final boolean isAvailable() {
		return this.handle != null;
	}

	/**
	 * 转换为 Bukkit 的 {@link Enchantment} 类
	 *
	 * @return
	 */
	@Nullable
	public final Enchantment toBukkit() {
		if (this.isAvailable()) {
			final Enchantment enchantment = this.handle.toBukkit();
			Valid.checkNotNull(enchantment, "Failed to convert " + this + " into a Bukkit class");

			return enchantment;
		}

		return null;
	}

	/**
	 *
	 * @param item
	 * @return
	 */
	public final boolean hasEnchant(ItemStack item) {
		return SimpleEnchantment.hasEnchantment(item, this);
	}

	/**
	 *
	 * @param item
	 * @param level
	 * @return
	 */
	public final ItemStack applyTo(ItemStack item, int level) {
		if (this.isAvailable()) {
			final ItemMeta meta = item.getItemMeta();

			meta.addEnchant(this.toBukkit(), level, true);
			item.setItemMeta(meta);
		}

		return item;
	}

	// ------------------------------------------------------------------------------------------
	// Overridable methods
	// ------------------------------------------------------------------------------------------

	/**
	 * 此附魔可以应用于哪些物品？默认为 ALL
	 *
	 * @return
	 */
	public SimpleEnchantmentTarget getTarget() {
		return SimpleEnchantmentTarget.BREAKABLE;
	}

	/**
	 * 返回稀有度
	 *
	 * @return
	 */
	public SimpleEnchantmentRarity getRarity() {
		return SimpleEnchantmentRarity.COMMON;
	}

	/**
	 * 获取此附魔的所有生效槽位
	 *
	 * @return
	 */
	public Set<CompEquipmentSlot> getActiveSlots() {
		return Common.newSet(CompEquipmentSlot.values());
	}

	/**
	 * 此附魔与哪些其他附魔冲突？默认对所有附魔返回 false
	 *
	 * @param other
	 * @return
	 */
	public boolean conflictsWith(Enchantment other) {
		return false;
	}

	/**
	 * 哪些物品可以被附魔？默认对所有物品返回 true
	 *
	 * @param item
	 * @return
	 */
	public boolean canEnchantItem(ItemStack item) {
		return true;
	}

	/**
	 * 获取起始等级，默认为 1
	 *
	 * @deprecated 未使用
	 * @return
	 */
	@Deprecated
	public int getStartLevel() {
		return 1;
	}

	/**
	 * 返回给定等级的最低花费
	 *
	 * @deprecated 请使用 {@link #getMinCost()}
	 *
	 * @param level
	 * @return
	 */
	@Deprecated
	public int getMinCost(int level) {
		return this.getMinCost().calculate(level);
	}

	/**
	 * 返回给定等级的最低花费
	 *
	 * @return
	 */
	public Cost getMinCost() {
		return new Cost(1, 1);
	}

	/**
	 * 返回在铁砧上的花费
	 *
	 * @return
	 */
	public int getAnvilCost() {
		return 1;
	}

	/**
	 * 返回给定等级的最高花费
	 *
	 * @deprecated 请使用 {@link #getMaxCost()}
	 *
	 * @param level
	 * @return
	 */
	@Deprecated
	public int getMaxCost(int level) {
		return this.getMaxCost().calculate(level);
	}

	/**
	 * 返回给定等级的最高花费
	 *
	 * @return
	 */
	public Cost getMaxCost() {
		return new Cost(1, 1);
	}

	/**
	 * 返回此附魔能否通过村民交易获得，默认为 true
	 *
	 * @return
	 */
	public boolean isTradeable() {
		return true;
	}

	/**
	 * 返回此附魔能否在附魔台上获得，默认为 true
	 *
	 * @return
	 */
	public boolean isDiscoverable() {
		return true;
	}

	/**
	 * 获取此附魔是否为宝藏附魔，默认为 false
	 *
	 * @return
	 */
	public boolean isTreasure() {
		return false;
	}

	/**
	 * 获取此附魔是否为诅咒附魔，默认为 false
	 *
	 * @return
	 */
	public boolean isCursed() {
		return false;
	}

	/**
	 * 返回此附魔的最高等级
	 *
	 * @return
	 */
	public final int getMaxLevel() {
		return this.maxLevel;
	}

	/**
	 * 返回此附魔的名称
	 *
	 * @return
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * 返回命名空间格式的名称
	 *
	 * 在 MC 1.13+ 上可使用 new NamespacedKey(SimplePlugin.getInstance(), this.name)) 转换为 {@link NamespacedKey}。
	 *
	 * @return
	 */
	public final String getNamespacedName() {
		return this.namespacedName;
	}

	/**
	 * @deprecated 仅供内部使用
	 * @param id
	 */
	@Deprecated
	public final void setLegacyId(int id) {
		this.id = id;
	}

	// ------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------

	/**
	 * 注册一个兼容的 NMS 类来处理附魔
	 *
	 * @param handleClass
	 */
	public static void registerEnchantmentHandle(Class<? extends NmsEnchant> handleClass) {
		Remain.unfreezeEnchantRegistry();

		SimpleEnchantment.handleClass = handleClass;
	}

	/**
	 * 返回给定物品上的附魔及其等级的映射
	 *
	 * @param item
	 * @return
	 */
	public static Map<SimpleEnchantment, Integer> findEnchantments(ItemStack item) {
		final Map<SimpleEnchantment, Integer> map = new HashMap<>();

		if (item == null)
			return map;

		final Map<Enchantment, Integer> vanilla;

		try {
			vanilla = item.hasItemMeta() ? item.getItemMeta().getEnchants() : new HashMap<>();
		} catch (final NoSuchMethodError err) {
			if (Remain.hasItemMeta())
				err.printStackTrace();

			return map;

		} catch (final NullPointerException ex) {
			// Caused if any associated enchant is null, probably by a third party plugin
			return map;
		}

		for (final Entry<Enchantment, Integer> entry : vanilla.entrySet()) {
			final Enchantment enchantment = entry.getKey();
			final int level = entry.getValue();
			final SimpleEnchantment simpleEnchantment = fromBukkit(enchantment);

			if (simpleEnchantment != null)
				map.put(simpleEnchantment, level);
		}

		return map;
	}

	/**
	 * 若物品带有我们的自定义附魔则返回 true
	 *
	 * @param item
	 * @param simpleEnchantment
	 * @return
	 */
	public static boolean hasEnchantment(ItemStack item, @NonNull SimpleEnchantment simpleEnchantment) {
		if (item == null)
			return false;

		final Map<Enchantment, Integer> vanilla;

		try {
			vanilla = item.hasItemMeta() ? item.getItemMeta().getEnchants() : new HashMap<>();

		} catch (final NoSuchMethodError err) {
			if (Remain.hasItemMeta())
				err.printStackTrace();

			return false;

		} catch (final NullPointerException ex) {
			return false;
		}

		for (final Entry<Enchantment, Integer> entry : vanilla.entrySet()) {
			final Enchantment enchantment = entry.getKey();
			final SimpleEnchantment otherSimpleEnchantment = fromBukkit(enchantment);

			if (otherSimpleEnchantment != null && otherSimpleEnchantment.getNamespacedName().equals(simpleEnchantment.getNamespacedName()))
				return true;
		}

		return false;
	}

	/**
	 * 由于 Minecraft 客户端无法显示自定义附魔，我们必须手动添加描述。
	 * <p>
	 * 此方法会在客户端尝试复制物品时，移除给定物品上的假附魔描述。
	 *
	 * @param item
	 * @return 修改后的物品；若物品未被修改（未找到附魔）则为 null
	 * @deprecated 仅供内部使用
	 */
	@Deprecated
	public static ItemStack removeEnchantmentLores(ItemStack item) {
		if (!item.hasItemMeta())
			return null;

		final ItemMeta meta = item.getItemMeta();

		if (meta != null && meta.hasLore()) {
			Map<Enchantment, Integer> enchants = item.getEnchantments();
			Map<Enchantment, Integer> storedEnchants = meta instanceof EnchantmentStorageMeta ? ((EnchantmentStorageMeta) meta).getStoredEnchants() : null;
			if (enchants.isEmpty() && (storedEnchants == null || storedEnchants.isEmpty()))
				return null;

			final List<String> lore = meta.getLore();
			final List<String> newLore = new ArrayList<>(lore.size());
			boolean foEnchanted = false;

			final List<String> colorLess = new ArrayList<>();
			try {
				for (final Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
					final Enchantment enchantment = entry.getKey();
					final SimpleEnchantment simpleEnchantment = fromBukkit(enchantment);

					if (simpleEnchantment != null) {
						final String loreLine = simpleEnchantment.getLore(entry.getValue());

						if (loreLine != null && !loreLine.isEmpty())
							colorLess.add(ChatColor.stripColor(Common.colorize(loreLine)));
					}
				}

				for (final Map.Entry<Enchantment, Integer> entry : storedEnchants.entrySet()) {
					final Enchantment storedEnchantment = entry.getKey();
					final SimpleEnchantment simpleEnchantment = fromBukkit(storedEnchantment);

					if (simpleEnchantment != null) {
						final String loreLine = simpleEnchantment.getLore(entry.getValue());

						if (loreLine != null && !loreLine.isEmpty())
							colorLess.add(ChatColor.stripColor(Common.colorize(loreLine)));
					}
				}

			} catch (final NullPointerException ex) {
				// Some weird problem in third party plugin
			}

			for (final String line : lore) {
				if (colorLess.contains(ChatColor.stripColor(Common.colorize(line)))) {
					foEnchanted = true;
				} else {
					newLore.add(line);
				}
			}

			if (!foEnchanted)
				return null;

			meta.setLore(newLore);
			item.setItemMeta(meta);

			return item;
		}

		return null;
	}


	/**
	 * 由于 Minecraft 客户端无法显示自定义附魔，我们必须手动添加描述。
	 * <p>
	 * 此方法会在给定物品上不存在假附魔描述时添加它。
	 *
	 * @param item
	 * @return 修改后的物品；若物品未被修改（未找到附魔）则为 null
	 * @deprecated 仅供内部使用
	 */
	@Deprecated
	public static ItemStack addEnchantmentLores(ItemStack item) {
		final List<String> customEnchants = new ArrayList<>();

		// Fill in our enchants
		try {
			for (final Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
				final Enchantment enchantment = entry.getKey();
				final SimpleEnchantment simpleEnchantment = fromBukkit(enchantment);

				if (simpleEnchantment != null) {
					final String lore = simpleEnchantment.getLore(entry.getValue());

					if (lore != null && !lore.isEmpty())
						customEnchants.add(Common.colorize(FO_ENCHANT_PREFIX + lore));
				}
			}

			if (Remain.hasItemMeta() && item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();
				if (meta instanceof EnchantmentStorageMeta) {
					for (final Map.Entry<Enchantment, Integer> entry : ((EnchantmentStorageMeta) meta).getStoredEnchants().entrySet()) {
						final Enchantment enchantment = entry.getKey();
						final SimpleEnchantment simpleEnchantment = fromBukkit(enchantment);

						if (simpleEnchantment != null) {
							final String lore = simpleEnchantment.getLore(entry.getValue());

							if (lore != null && !lore.isEmpty())
								customEnchants.add(Common.colorize(FO_ENCHANT_PREFIX + lore));
						}
					}
				}
			}

		} catch (final NullPointerException ex) {
			// Some weird problem in third party plugin
		}

		if (!customEnchants.isEmpty()) {
			final ItemMeta meta = Remain.hasItemMeta() && item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
			final List<String> originalLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
			final List<String> finalLore = new ArrayList<>();

			final List<String> colorlessOriginals = new ArrayList<>();

			for (final String original : originalLore)
				colorlessOriginals.add(ChatColor.stripColor(Common.colorize(original)));

			// Place our enchants
			for (final String customEnchant : customEnchants) {
				final String colorlessEnchant = ChatColor.stripColor(Common.colorize(customEnchant));

				if (!colorlessOriginals.contains(colorlessEnchant))
					finalLore.add(customEnchant);
			}

			// Place the original lore at the bottom
			finalLore.addAll(originalLore);

			// Set the lore
			meta.setLore(finalLore);

			// Update the item stack
			item.setItemMeta(meta);

			return item;
		}

		return null;
	}

	private static SimpleEnchantment fromBukkit(Enchantment bukkitEnchantment) {
		if (hasNamespacedKeys) {
			final String key = bukkitEnchantment.getKey().getNamespace() + ":" + bukkitEnchantment.getKey().getKey();

			for (final SimpleEnchantment simpleEnchantment : registeredEnchantments)
				if (simpleEnchantment.namespacedNameWithPrefix.equals(key))
					return simpleEnchantment;

		} else {
			final String name = bukkitEnchantment.getName();

			for (final SimpleEnchantment simpleEnchantment : registeredEnchantments)
				if (simpleEnchantment.name.equals(name))
					return simpleEnchantment;

			try {
				final int id = ReflectionUtil.invoke("getId", bukkitEnchantment);

				for (final SimpleEnchantment simpleEnchantment : registeredEnchantments)
					if (simpleEnchantment.id == id)
						return simpleEnchantment;

			} catch (final Throwable t) {
				// Unsupported, very old MC
			}
		}

		return null;
	}

	/**
	 * 用于 1.20.5+ 花费处理的包装类
	 */
	@Data
	public static final class Cost {
		private final int base;
		private final int perLevel;

		public Cost(int var0, int var1) {
			this.base = var0;
			this.perLevel = var1;
		}

		public int calculate(int level) {
			return this.base + this.perLevel * (level - 1);
		}
	}

	/**
	 * 监听并执行 {@link SimpleEnchantment} 的事件
	 * <p>
	 * @deprecated 仅供内部使用！
	 */
	@Deprecated
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Listener implements org.bukkit.event.Listener {

		@Getter
		private static final Listener instance = new Listener();

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageByEntityEvent event) {
			final Entity damager = event.getDamager();

			if (damager instanceof LivingEntity)
				this.execute((LivingEntity) damager, (enchant, level) -> enchant.onDamage(level, (LivingEntity) damager, event));
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
		public void onInteract(PlayerInteractEvent event) {
			if (!Remain.isInteractEventPrimaryHand(event))
				return;

			this.execute(event.getPlayer(), (enchant, level) -> enchant.onInteract(level, event));
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onBreakBlock(BlockBreakEvent event) {
			this.execute(event.getPlayer(), (enchant, level) -> enchant.onBreakBlock(level, event));
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onShoot(ProjectileLaunchEvent event) {
			try {
				final ProjectileSource projectileSource = event.getEntity().getShooter();

				if (projectileSource instanceof LivingEntity) {
					final LivingEntity shooter = (LivingEntity) projectileSource;

					this.execute(shooter, (enchant, level) -> enchant.onShoot(level, shooter, event));
					EntityUtil.trackHit(event.getEntity(), hitEvent -> this.execute(shooter, (enchant, level) -> enchant.onHit(level, shooter, hitEvent)));
				}
			} catch (final NoSuchMethodError ex) {
				if (MinecraftVersion.atLeast(V.v1_4))
					ex.printStackTrace();
			}
		}

		private void execute(LivingEntity source, BiConsumer<SimpleEnchantment, Integer> executer) {
			try {
				final ItemStack hand = source instanceof Player ? ((Player) source).getItemInHand() : source.getEquipment().getItemInHand();

				if (hand != null)
					for (final Entry<SimpleEnchantment, Integer> e : SimpleEnchantment.findEnchantments(hand).entrySet())
						executer.accept(e.getKey(), e.getValue());

			} catch (final NoSuchMethodError ex) {
				if (Remain.hasItemMeta())
					ex.printStackTrace();
			}
		}
	}

}