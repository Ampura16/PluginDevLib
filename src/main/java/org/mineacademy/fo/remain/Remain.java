package org.mineacademy.fo.remain;

import static org.mineacademy.fo.ReflectionUtil.getNMSClass;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.Statistic.Type;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.EntityUtil;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.ReflectionException;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.UUIDToNameConverter;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.internal.BossBarInternals;
import org.mineacademy.fo.remain.internal.ChatInternals;
import org.mineacademy.fo.remain.nbt.NBTEntity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

/**
 * 我们主要的跨版本兼容类。
 * <p>
 * 在这里查找各种方法，让你的插件
 * 兼容从 MC 1.8.8 到最新的版本。
 */
public final class Remain {

	/**
	 * 用于匹配编码后 HEX 颜色 &x&F&F&F&F&F&F 的正则
	 */
	private static final Pattern RGB_HEX_ENCODED_REGEX = Pattern.compile("(?i)(§x)((§[0-9A-F]){6})");

	/**
	 * Google Json 实例
	 */
	private final static Gson gson = new Gson();

	/**
	 * NMS 的完整包名。
	 */
	private static final String NMS = "net.minecraft.server";

	/**
	 * Craftbukkit 的包名。
	 */
	private static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";

	// ----------------------------------------------------------------------------------------------------
	// Methods below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 为提高性能而在此存储的获取玩家方法
	 */
	private static Method getPlayersMethod;

	/**
	 * 为提高性能而在此存储的获取玩家生命值方法
	 */
	private static Method getHealthMethod;

	/**
	 * CraftPlayer.getHandle 方法
	 */
	private static Method getHandle;

	/**
	 * EntityPlayer.playerConnection 方法
	 */
	private static Field fieldPlayerConnection;

	/**
	 * 在旧版 MC 上获取实体是否无敌
	 */
	private static Field fieldEntityInvulnerable;

	/**
	 * PlayerConnection.sendPacket 方法
	 */
	private static Method sendPacket;

	// ----------------------------------------------------------------------------------------------------
	// Flags below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 当前服务器版本获取的玩家列表是否为集合？
	 */
	private static boolean isGetPlayersCollection = false;

	/**
	 * 当前服务器版本获取的玩家生命值是否为 double？
	 */
	private static boolean isGetHealthDouble = false;

	/**
	 * 当前服务器版本是否支持发送 fadeIn、stay 和 fadeOut 参数的标题 API？
	 */
	private static boolean hasExtendedPlayerTitleAPI = false;

	/**
	 * 当前服务器版本是否支持粒子 API？
	 */
	private static boolean hasParticleAPI = true;

	/**
	 * 当前服务器版本是否支持原生计分板 API？
	 */
	private static boolean newScoreboardAPI = true;

	/**
	 * 当前服务器版本是否支持书本事件？
	 */
	private static boolean hasBookEvent = true;

	/**
	 * 当前服务器版本是否支持获取背包位置？
	 */
	private static boolean hasInventoryLocation = true;

	/**
	 * 当前服务器版本是否支持永久计分板标签？
	 */
	private static boolean hasScoreboardTags = true;

	/**
	 * 当前服务器版本是否支持刷怪蛋元数据？
	 */
	private static boolean hasSpawnEggMeta = true;

	/**
	 * 当前服务器版本是否支持进度？
	 */
	private static boolean hasAdvancements = true;

	/**
	 * 能否调用 {@link YamlConfiguration#load(java.io.Reader)}
	 */
	private static boolean hasYamlReaderLoad = true;

	/**
	 * 是否存在 org/bukkit/inventory/meta/ItemMeta 类？MC 1.4.7+
	 */
	private static boolean hasItemMeta = true;

	/**
	 * 返回 {@link Entity#addPassenger(Entity)} 方法是否可用。
	 */
	private static boolean hasAddPassenger = true;

	/**
	 * 若存在过于复杂的 io.papermc.paper.event.player.AsyncChatEvent 则返回 true
	 */
	private static boolean hasAdventureChatEvent = true;

	/**
	 * 若 PlayerInventory 类有 getExtraContents 方法则返回 true
	 */
	private static boolean hasPlayerExtraInventoryContent = true;

	/**
	 * 若 Player 有 openSign 方法则返回 true。
	 */
	private static boolean hasPlayerOpenSignMethod = true;

	/**
	 * 为旧版 MC 存储玩家冷却时间
	 */
	private final static StrictMap<UUID /*Player*/, StrictMap<Material, Integer>> cooldowns = new StrictMap<>();

	/**
	 * 内部私有的节路径数据类
	 */
	private static Class<?> sectionPathDataClass = null;

	/**
	 * server.properties 中的 server-name（新版 Minecraft 已缺失，因此我们需要重新添加回来）
	 */
	private static String serverName;

	/**
	 * 若运行在 Paper 上则返回 true
	 */
	private static boolean isPaper = false;

	/**
	 * 若这是 Folia 服务器则返回 true
	 */
	private static boolean isFolia = false;

	/**
	 * 若此服务器是 Thermos 则返回 true
	 */
	private static boolean isThermos = false;

	/**
	 * 若运行在 Mojang 重映射的服务器上则返回 true
	 */
	private static boolean isUsingMojangMappings = false;

	/**
	 * 必须在你的插件中手动解冻，解决 https://github.com/kangarko/ChatControl-Red/issues/2662
	 */
	@Getter
	private static boolean enchantRegistryUnfrozen = false;

	/**
	 * Bukkit 1.4 至 1.20.4 中使用的安全 NMS 前缀。
	 *
	 * @deprecated 仅供内部使用，在 Minecraft 1.20.5 及更高版本上已不再需要
	 */
	@Deprecated
	@Getter
	private static String nmsVersion = "";

	// Singleton
	private Remain() {
	}

	/**
	 * 在设置插件时自动初始化所有字段和方法
	 */
	static {
		// Initialize safeguard prefix first
		{
			final String packageName = Bukkit.getServer() == null ? "" : Bukkit.getServer().getClass().getPackage().getName();
			final String curr = packageName.substring(packageName.lastIndexOf('.') + 1);

			nmsVersion = !"craftbukkit".equals(curr) && !"".equals(packageName) ? curr : "";
		}

		final boolean atLeast1_4 = MinecraftVersion.atLeast(V.v1_4);

		try {
			Class.forName("net.md_5.bungee.chat.ComponentSerializer");

		} catch (final Throwable ex) {
			throw new FoException("Your server " + Bukkit.getName() + " lacks libraries required for " + SimplePlugin.getNamed() + " to run. Install BungeeChatAPI from: https://mineacademy.org/plugins#misc");
		}

		try {
			Class.forName("co.aikar.timings.Timing");

			isPaper = true;
		} catch (final Throwable e) {
		}

		isFolia = ReflectionUtil.isClassAvailable("io.papermc.paper.threadedregions.RegionizedServer");

		try {
			Class.forName("thermos.ThermosRemapper");

			isThermos = true;
		} catch (final Throwable tt) {
		}

		try {
			Class.forName("net.minecraft.server.level.ServerPlayer");

			isUsingMojangMappings = true;
		} catch (final ClassNotFoundException ex) {
		}

		try {
			World.class.getMethod("spawnParticle", org.bukkit.Particle.class, Location.class, int.class);
		} catch (final Throwable ex) {
			hasParticleAPI = false;
		}

		try {
			Objective.class.getMethod("getScore", String.class);
		} catch (final Throwable e) {
			newScoreboardAPI = false;
		}

		try {
			Class.forName("org.bukkit.event.player.PlayerEditBookEvent").getName();
		} catch (final Throwable ex) {
			hasBookEvent = false;
		}

		try {
			Inventory.class.getMethod("getLocation");
		} catch (final Throwable ex) {
			hasInventoryLocation = false;
		}

		try {
			Entity.class.getMethod("getScoreboardTags");
		} catch (final Throwable ex) {
			hasScoreboardTags = false;
		}

		try {
			Class.forName("org.bukkit.inventory.meta.SpawnEggMeta");
		} catch (final Throwable err) {
			hasSpawnEggMeta = false;
		}

		try {
			Class.forName("org.bukkit.advancement.Advancement");
			Class.forName("org.bukkit.NamespacedKey");
		} catch (final Throwable err) {
			hasAdvancements = false;
		}

		try {
			YamlConfiguration.class.getMethod("load", java.io.Reader.class);
		} catch (final Throwable err) {
			hasYamlReaderLoad = false;
		}

		try {
			org.bukkit.inventory.ItemStack.class.getMethod("getItemMeta");
		} catch (final Throwable ex) {
			hasItemMeta = false;
		}

		try {
			Entity.class.getMethod("addPassenger", Entity.class);
		} catch (final Throwable ex) {
			hasAddPassenger = false;
		}

		try {
			Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
		} catch (final Throwable t) {
			hasAdventureChatEvent = false;
		}

		final Method getExtraContents = ReflectionUtil.getMethod(PlayerInventory.class, "getExtraContents");

		if (getExtraContents == null)
			hasPlayerExtraInventoryContent = false;

		try {
			sectionPathDataClass = ReflectionUtil.lookupClass("org.bukkit.configuration.SectionPathData");
		} catch (final Throwable ex) {
		}

		try {
			Player.class.getMethod("openSign", org.bukkit.block.Sign.class);
		} catch (final Throwable ex) {
			hasPlayerOpenSignMethod = false;
		}

		try {
			getPlayersMethod = Bukkit.class.getMethod("getOnlinePlayers");
			isGetPlayersCollection = getPlayersMethod.getReturnType() == Collection.class;

			getHealthMethod = LivingEntity.class.getMethod("getHealth");
			isGetHealthDouble = getHealthMethod.getReturnType() == double.class;

			hasExtendedPlayerTitleAPI = MinecraftVersion.atLeast(V.v1_11);

			CompParticle.CRIT.getClass();

			ChatInternals.init();

			getHandle = getOBCClass("entity.CraftPlayer").getMethod("getHandle");

			if (MinecraftVersion.atLeast(V.v1_21))
				fieldPlayerConnection = ReflectionUtil.lookupClass("net.minecraft.server.level.ServerPlayer")
						.getField("connection");
			else
				fieldPlayerConnection = getNMSClass("EntityPlayer", "net.minecraft.server.level.EntityPlayer")
						.getField(MinecraftVersion.atLeast(V.v1_20) ? "c" : MinecraftVersion.atLeast(V.v1_17) ? "b" : atLeast1_4 ? "playerConnection" : "netServerHandler");

			if (MinecraftVersion.atLeast(V.v1_21))
				sendPacket = ReflectionUtil.lookupClass("net.minecraft.server.network.ServerCommonPacketListenerImpl")
						.getMethod("send", ReflectionUtil.lookupClass("net.minecraft.network.protocol.Packet"));
			else
				sendPacket = getNMSClass(atLeast1_4 ? "PlayerConnection" : "NetServerHandler", "net.minecraft.server.network.PlayerConnection")
						.getMethod(MinecraftVersion.atLeast(V.v1_18) ? "a" : "sendPacket", getNMSClass("Packet", "net.minecraft.network.protocol.Packet"));

			if (MinecraftVersion.olderThan(V.v1_12))
				try {
					fieldEntityInvulnerable = ReflectionUtil.getNMSClass("Entity").getDeclaredField("invulnerable");
					fieldEntityInvulnerable.setAccessible(true);
				} catch (final Throwable t) {
					// Unavailable
				}

		} catch (final Throwable t) {
			if (!isThermos && !isUsingMojangMappings && MinecraftVersion.atLeast(V.v1_7)) {
				Bukkit.getLogger().warning("Unable to setup reflection. Plugin will partially function.");
				Bukkit.getLogger().warning("Ignore this if using Cauldron. Otherwise report the errors below to the developers of " + SimplePlugin.getNamed() + ".");

				t.printStackTrace();
			}
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 返回 Minecraft 的 World 类
	 *
	 * @param world
	 * @return
	 */
	public static Object getHandleWorld(final World world) {
		final Method handle = ReflectionUtil.getMethod(world.getClass(), "getHandle");
		Valid.checkNotNull(handle, "Cannot call getHandle() for " + world.getClass() + " (" + world + ")");

		return ReflectionUtil.invoke(handle, world);
	}

	/**
	 * 返回 Minecraft 的 Entity 类
	 *
	 * @param entity
	 * @return
	 */
	public static Object getHandleEntity(final Object entity) {
		final String methodName = entity instanceof BlockState ? "getTileEntity" : "getHandle";
		final Method handle = ReflectionUtil.getMethod(entity.getClass(), methodName);
		Valid.checkNotNull(handle, "Cannot call " + methodName + "() for " + entity.getClass() + " (" + entity + ")");

		return ReflectionUtil.invoke(handle, entity);
	}

	/**
	 * 获取服务器句柄
	 *
	 * @return
	 */
	public static Object getHandleServer() {
		final org.bukkit.Server server = Bukkit.getServer();
		final Method handle = ReflectionUtil.getMethod(server.getClass(), "getServer");
		Valid.checkNotNull(handle, "Cannot call getServer() for " + server.getClass() + " (" + server + ")");

		return ReflectionUtil.invoke(handle, server);
	}

	/**
	 * 若正在运行 1.8 协议 hack 则返回 true
	 *
	 * @return
	 */
	public static boolean isProtocol18Hack() {
		if (MinecraftVersion.newerThan(V.v1_9))
			return false;

		try {
			getNMSClass("PacketPlayOutEntityTeleport", "N/A").getConstructor(int.class, int.class, int.class, int.class, byte.class, byte.class, boolean.class, boolean.class);

		} catch (final Throwable t) {
			return false;
		}

		return true;
	}

	/**
	 * 高级：向玩家发送数据包
	 *
	 * @param player 玩家
	 * @param packet 数据包
	 */
	public static void sendPacket(final Player player, final Object packet) {
		try {
			final Object playerConnection = getPlayerConnection(player);

			if (playerConnection != null)
				sendPacket.invoke(playerConnection, packet);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Error sending packet " + packet.getClass() + " to player " + player.getName());
		}
	}

	/**
	 * 返回 NMS 中 EntityPlayer 的 playerConnection 字段
	 *
	 * @param player
	 * @return
	 */
	public static Object getPlayerConnection(Player player) {
		if (getHandle == null || fieldPlayerConnection == null || sendPacket == null) {
			Common.log("Cannot get player connection on your server sofware (known to be broken on Cauldron).");

			return null;
		}

		try {
			final Object handle = getHandle.invoke(player);
			final Object playerConnection = fieldPlayerConnection.get(handle);

			return playerConnection;

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Error getting player connection for player " + player.getName());
		}
	}

	/**
	 * 返回接收实体类型的 World#isEnabled() 方法
	 *
	 * @return
	 */
	public static Method getIsEnabledFeatureWorldMethod() {
		final boolean hasFeatureClass = ReflectionUtil.isClassAvailable("io.papermc.paper.world.flag.FeatureDependant") && ReflectionUtil.isClassAvailable("io.papermc.paper.world.flag.FeatureFlagSetHolder");

		if (hasFeatureClass) {
			final Class<?> featureFlagSetHolderClass = ReflectionUtil.lookupClass("io.papermc.paper.world.flag.FeatureFlagSetHolder");
			final Class<?> featureDependent = ReflectionUtil.lookupClass("io.papermc.paper.world.flag.FeatureDependant");

			return ReflectionUtil.getMethod(featureFlagSetHolderClass, "isEnabled", featureDependent);
		}

		return null;
	}

	// ----------------------------------------------------------------------------------------------------
	// Compatibility methods below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 返回实体的生命值
	 *
	 * @param entity 实体
	 * @return 生命值
	 */
	public static int getHealth(final LivingEntity entity) {
		return isGetHealthDouble ? (int) entity.getHealth() : getHealhLegacy(entity);
	}

	/**
	 * 返回实体的最大生命值
	 *
	 * @param entity
	 * @return
	 */
	public static int getMaxHealth(final LivingEntity entity) {
		return isGetHealthDouble ? (int) entity.getMaxHealth() : getMaxHealhLegacy(entity);
	}

	/**
	 * 返回所有在线玩家
	 *
	 * @return 在线玩家
	 */
	public static Collection<? extends Player> getOnlinePlayers() {
		return isGetPlayersCollection ? Bukkit.getOnlinePlayers() : Arrays.asList(getPlayersLegacy());
	}

	/**
	 * 返回玩家的视距
	 *
	 * @param player
	 * @return
	 */
	public static int getViewDistance(Player player) {
		try {
			return player.getClientViewDistance();

		} catch (final NoSuchMethodError err) {
			final Method getViewDistance = ReflectionUtil.getMethod(player.spigot().getClass(), "getViewDistance");

			return ReflectionUtil.invoke(getViewDistance, player.spigot());
		}
	}

	/**
	 * 在给定方块位置生成下落方块
	 *
	 * @param block
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Block block) {
		return spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5) /* fix alignment */, block.getType(), block.getData());
	}

	/**
	 * 在该位置生成下落方块
	 *
	 * @param loc
	 * @param block
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Location loc, final Block block) {
		if (MinecraftVersion.atLeast(V.v1_13))
			return loc.getWorld().spawnFallingBlock(loc, block.getBlockData());
		else
			try {
				return (FallingBlock) loc.getWorld().getClass().getMethod("spawnFallingBlock", Location.class, int.class, byte.class).invoke(loc.getWorld(), loc, ReflectionUtil.invoke("getTypeId", block), block.getData());
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();

				return null;
			}
	}

	/**
	 * 生成下落方块
	 *
	 * @param loc
	 * @param material
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Location loc, final Material material) {
		return spawnFallingBlock(loc, material, (byte) 0);
	}

	/**
	 * 生成下落方块。
	 *
	 * @param loc
	 * @param material
	 * @param data
	 * @return
	 */
	public static FallingBlock spawnFallingBlock(final Location loc, final Material material, final byte data) {
		if (MinecraftVersion.atLeast(V.v1_13))
			return loc.getWorld().spawnFallingBlock(loc, material, data);
		else
			try {
				return (FallingBlock) loc.getWorld().getClass().getMethod("spawnFallingBlock", Location.class, int.class, byte.class).invoke(loc.getWorld(), loc, material.getId(), data);
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();

				return null;
			}
	}

	/**
	 * 尝试掉落物品，并允许在其生成之前为物品
	 * 应用属性
	 *
	 * @param location
	 * @param item
	 * @param modifier
	 * @return 物品
	 * @deprecated 请使用 {@link EntityUtil#dropItem(Location, ItemStack, Consumer)}
	 */
	@Deprecated
	public static Item spawnItem(final Location location, final ItemStack item, final Consumer<Item> modifier) {
		try {

			final Class<?> nmsWorldClass = getNMSClass("World", "net.minecraft.world.level.World");
			final Class<?> nmsStackClass = getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Class<?> nmsEntityClass = getNMSClass("Entity", "net.minecraft.world.entity.Entity");
			final Class<?> nmsItemClass = getNMSClass("EntityItem", "net.minecraft.world.entity.item.EntityItem");

			final Constructor<?> entityConstructor = nmsItemClass.getConstructor(nmsWorldClass, double.class, double.class, double.class, nmsStackClass);

			final Object nmsWorld = location.getWorld().getClass().getMethod("getHandle").invoke(location.getWorld());
			final Method asNmsCopy = getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			final Object nmsEntity = entityConstructor.newInstance(nmsWorld, location.getX(), location.getY(), location.getZ(), asNmsCopy.invoke(null, item));

			final Class<?> craftItemClass = getOBCClass("entity.CraftItem");
			final Class<?> craftServerClass = getOBCClass("CraftServer");

			final Object bukkitItem = craftItemClass.getConstructor(craftServerClass, nmsItemClass).newInstance(Bukkit.getServer(), nmsEntity);
			Valid.checkBoolean(bukkitItem instanceof Item, "Failed to make an dropped item, got " + bukkitItem.getClass().getSimpleName());

			// Default delay to 750ms
			try {
				((Item) bukkitItem).setPickupDelay(15);
			} catch (final Throwable t) {
				// unsupported
			}

			if (modifier != null)
				modifier.accept((Item) bukkitItem);

			{ // add to the world + call event
				final Method addEntity = location.getWorld().getClass().getMethod("addEntity", nmsEntityClass, SpawnReason.class);
				addEntity.invoke(location.getWorld(), nmsEntity, SpawnReason.CUSTOM);
			}

			return (Item) bukkitItem;

		} catch (final ReflectiveOperationException ex) {
			Common.error(ex, "Error spawning item " + item.getType() + " at " + location);

			return null;
		}
	}

	/**
	 * 返回给定物品堆的 NMS 副本
	 *
	 * @param itemStack
	 * @return
	 */
	public static Object asNMSCopy(ItemStack itemStack) {
		try {
			final Method asNmsCopy = getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);

			return asNmsCopy.invoke(null, itemStack);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to convert item to NMS item: " + itemStack);

			return null;
		}
	}

	/**
	 * 设置世界中某个方块的数据值。
	 *
	 * @param block
	 * @param data
	 */
	public static void setData(final Block block, final int data) {
		try {
			Block.class.getMethod("setData", byte.class).invoke(block, (byte) data);

		} catch (final NoSuchMethodException ex) {
			block.setBlockData(Bukkit.getUnsafe().fromLegacy(block.getType(), (byte) data), true);

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 设置方块类型及其数据值，并应用物理效果。
	 *
	 * @param block
	 * @param material
	 * @param data
	 */
	public static void setTypeAndData(final Block block, final Material material, final byte data) {
		setTypeAndData(block, CompMaterial.fromLegacy(material.name(), data));
	}

	/**
	 * 设置方块类型及其数据值。
	 *
	 * @param block
	 * @param material
	 */
	public static void setTypeAndData(final Block block, final CompMaterial material) {
		if (MinecraftVersion.atLeast(V.v1_13))
			block.setType(material.getMaterial());
		else
			try {
				block.getClass().getMethod("setTypeIdAndData", int.class, byte.class, boolean.class).invoke(block, material.getId(), material.getData(), true);
			} catch (final ReflectiveOperationException ex) {
				ex.printStackTrace();
			}
	}

	/**
	 * 尝试在初始方块放置床方块，并在朝向方向放置另一半床头方块
	 *
	 * 使用 {@link PlayerUtil#getFacing(Player)} 获取玩家所看的方向
	 *
	 * @param initialLocation
	 * @param facing
	 */
	public static void setBed(Location initialLocation, BlockFace facing) {
		setBed(initialLocation.getBlock(), facing);
	}

	/**
	 * 尝试在初始方块放置床方块，并在朝向方向放置另一半床头方块
	 *
	 * 使用 {@link PlayerUtil#getFacing(Player)} 获取玩家所看的方向
	 *
	 * @param initialBlock
	 * @param facing
	 */
	public static void setBed(Block initialBlock, BlockFace facing) {

		if (MinecraftVersion.atLeast(V.v1_13))
			for (final Bed.Part part : Bed.Part.values()) {
				initialBlock.setBlockData(Bukkit.createBlockData(CompMaterial.WHITE_BED.getMaterial(), data -> {
					((Bed) data).setPart(part);
					((Bed) data).setFacing(facing);
				}));

				initialBlock = initialBlock.getRelative(facing.getOppositeFace());
			}

		else {
			initialBlock = initialBlock.getRelative(facing);

			final Material bedMaterial = Material.valueOf("BED_BLOCK");
			final Block bedFootBlock = initialBlock.getRelative(facing.getOppositeFace());

			final BlockState bedFootState = bedFootBlock.getState();
			bedFootState.setType(bedMaterial);

			final org.bukkit.material.Bed bedFootData = new org.bukkit.material.Bed(bedMaterial);
			bedFootData.setHeadOfBed(false);
			bedFootData.setFacingDirection(facing);

			bedFootState.setData(bedFootData);
			bedFootState.update(true);

			final BlockState bedHeadState = initialBlock.getState();
			bedHeadState.setType(bedMaterial);

			final org.bukkit.material.Bed bedHeadData = new org.bukkit.material.Bed(bedMaterial);
			bedHeadData.setHeadOfBed(true);
			bedHeadData.setFacingDirection(facing);

			bedHeadState.setData(bedHeadData);
			bedHeadState.update(true);
		}
	}

	/**
	 * 将 json 字符串转换为旧式带颜色的文本
	 *
	 * @param json
	 * @return
	 * @throws InteractiveTextFoundException
	 */
	public static String toLegacyText(final String json) throws InteractiveTextFoundException {
		return toLegacyText(json, true);
	}

	/**
	 * 将 JSON（IChatBaseComponent）格式的聊天消息转换为单行的旧式
	 * 带颜色代码的消息。例如 {text:"Hello world",color="red"} 会转换为
	 * &cHello world
	 * @param json
	 *
	 * @param denyEvents 若发现悬停/点击事件，是否抛出
	 *                   异常。
	 * @return
	 * @throws InteractiveTextFoundException 若发现点击/悬停事件。这类
	 *                                       事件会被移除，因此
	 *                                       包含它们的消息不应被
	 *                                       解包
	 */
	public static String toLegacyText(final String json, final boolean denyEvents) throws InteractiveTextFoundException {
		final StringBuilder text = new StringBuilder();

		// Translate options does not want to work well with ChatControl
		if (json.contains("\"translate\""))
			return text.append("").toString();

		try {
			for (final BaseComponent comp : ComponentSerializer.parse(json)) {
				if ((comp.getHoverEvent() != null || comp.getClickEvent() != null) && denyEvents)
					throw new InteractiveTextFoundException();

				text.append(comp.toLegacyText());
			}

		} catch (final Throwable throwable) {

			// Do not catch our own exception
			if (throwable instanceof InteractiveTextFoundException)
				throw throwable;
		}

		return text.toString();
	}

	/**
	 * 以 JSON 形式返回给定列表
	 *
	 * @param list
	 * @return
	 */
	public static String toJson(final Collection<String> list) {
		return gson.toJson(list);
	}

	/**
	 * 将给定 json 转换为列表
	 *
	 * @param json
	 * @return
	 */
	public static List<String> fromJsonList(String json) {
		return gson.fromJson(json, List.class);
	}

	/**
	 * 将带颜色代码的聊天消息转换为 Json 聊天组件，例如 &6Hello
	 * world 会转换为 {text:"Hello world",color="gold"}
	 * @param message
	 * @return
	 */
	public static String toJson(final String message) {
		return toJson(TextComponent.fromLegacyText(message));
	}

	/**
	 * 将基础组件转换为 json
	 *
	 * @param comps
	 * @return
	 */
	public static String toJson(final BaseComponent... comps) {
		String json;

		try {
			json = ComponentSerializer.toString(comps);

		} catch (final Throwable t) {
			json = new Gson().toJson(new TextComponent(comps).toLegacyText());
		}

		return json;
	}

	/**
	 * 将 {@link org.bukkit.inventory.ItemStack} 转换为 Json 字符串，
	 * 以便通过 {@link net.md_5.bungee.api.chat.BaseComponent} 发送。
	 *
	 * @param item 要转换的物品
	 * @return 物品的 Json 字符串表示
	 */
	public static String toJson(ItemStack item) {
		if (MinecraftVersion.atLeast(V.v1_4)) {
			// ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
			final Class<?> craftItemstack = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
			final Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemstack, "asNMSCopy", ItemStack.class);

			Valid.checkNotNull(asNMSCopyMethod, "Unable to find " + craftItemstack + "#asNMSCopy() method for server version " + Bukkit.getBukkitVersion());

			// NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
			final Class<?> nmsItemStack = ReflectionUtil.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Object nmsItemStackObj = ReflectionUtil.invoke(asNMSCopyMethod, null, item);

			if (MinecraftVersion.newerThan(V.v1_20) || (MinecraftVersion.atLeast(V.v1_20) && MinecraftVersion.getSubversion() > 4)) {
				if (Remain.isPaper()) {
					final Class<?> providerClass = ReflectionUtil.lookupClass("net.minecraft.core.HolderLookup$Provider");
					final Method saveMethod = ReflectionUtil.getMethod(nmsItemStack, "saveOptional", providerClass);

					final Object registryAccess = ReflectionUtil.invoke("registryAccess", Remain.getHandleServer());
					final Object compoundTag = ReflectionUtil.invoke(saveMethod, nmsItemStackObj, registryAccess);

					return compoundTag.toString();
				} else
					// Spigot has different mappings so we just give up and render the base item
					return "{Count:" + item.getAmount() + "b,id:\"" + item.getType().getKey().toString() + "\"}";

			} else {
				final Class<?> nbtTagCompound = ReflectionUtil.getNMSClass("NBTTagCompound", "net.minecraft.nbt.NBTTagCompound");
				final Method saveItemstackMethod = ReflectionUtil.getMethod(nmsItemStack, MinecraftVersion.equals(V.v1_18) || MinecraftVersion.equals(V.v1_19) || (MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() < 5) ? "b" : "save", nbtTagCompound);

				Valid.checkNotNull(saveItemstackMethod, "Unable to find " + nmsItemStack + "#save() method for server version " + Bukkit.getBukkitVersion());

				final Object nmsNbtTagCompoundObj = ReflectionUtil.instantiate(nbtTagCompound);
				final Object itemAsJsonObject = ReflectionUtil.invoke(saveItemstackMethod, nmsItemStackObj, nmsNbtTagCompoundObj);

				return itemAsJsonObject.toString();
			}
		}

		return item.getType().toString();
	}

	/**
	 * 将 json 转换为基础组件数组
	 *
	 * @param json
	 * @return
	 */
	public static BaseComponent[] toComponent(final String json) {
		try {
			return ComponentSerializer.parse(json);

		} catch (final Throwable t) {
			Common.throwError(t,
					"Failed to call toComponent!",
					"Json: " + json,
					"Error: %error%");

			return null;
		}
	}

	/**
	 * 向发送者发送 JSON 组件
	 *
	 * @param sender
	 * @param json
	 * @param placeholders
	 */
	public static void sendJson(final CommandSender sender, final String json, final SerializedMap placeholders) {
		try {
			final BaseComponent[] components = ComponentSerializer.parse(json);

			if (MinecraftVersion.atLeast(V.v1_16))
				replaceHexPlaceholders(Arrays.asList(components), placeholders);

			sendComponent(sender, components);

		} catch (final RuntimeException ex) {
			Common.error(ex, "Malformed JSON when sending message to " + sender.getName() + " with JSON: " + json);
		}
	}

	/*
	 * A helper Method for MC 1.16+ to partially solve the issue of HEX colors in JSON
	 *
	 * BaseComponent does not support colors when in text, they must be set at the color level
	 */
	private static void replaceHexPlaceholders(final List<BaseComponent> components, final SerializedMap placeholders) {

		for (final BaseComponent component : components) {
			if (component instanceof TextComponent) {
				final TextComponent textComponent = (TextComponent) component;
				String text = textComponent.getText();

				for (final Map.Entry<String, Object> entry : placeholders.entrySet()) {
					String key = entry.getKey();
					String value = Common.simplify(entry.getValue());

					// Detect HEX in placeholder
					final Matcher match = RGB_HEX_ENCODED_REGEX.matcher(text);

					while (match.find()) {

						// Find the color
						final String color = "#" + match.group(2).replace(ChatColor.COLOR_CHAR + "", "");

						// Remove it from chat and bind it to TextComponent instead
						value = match.replaceAll("");
						textComponent.setColor(net.md_5.bungee.api.ChatColor.of(color));
					}

					key = key.charAt(0) != '{' ? "{" + key : key;
					key = key.charAt(key.length() - 1) != '}' ? key + "}" : key;

					text = text.replace(key, value);
					textComponent.setText(text);
				}
			}

			if (component.getExtra() != null)
				replaceHexPlaceholders(component.getExtra(), placeholders);

			if (component.getHoverEvent() != null)
				replaceHexPlaceholders(Arrays.asList(component.getHoverEvent().getValue()), placeholders);
		}
	}

	/**
	 * 向发送者发送 JSON 组件
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final CommandSender sender, final String json) {
		try {
			sendComponent(sender, ComponentSerializer.parse(json));

		} catch (final Throwable t) {

			// Silence a bug in md_5's library
			if (t.toString().contains("missing 'text' property"))
				return;

			Common.throwError(t, "Malformed JSON when sending message to " + sender.getName() + " with JSON: " + json);
		}
	}

	/**
	 * 向发送者发送 JSON 组件
	 *
	 * @param sender
	 * @param comps
	 */
	public static void sendComponent(final CommandSender sender, final Object comps) {
		BungeeChatProvider.sendComponent(sender, comps);
	}

	/**
	 * 向玩家发送标题（1.8+），持续三秒
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 */
	public static void sendTitle(final Player player, final String title, final String subtitle) {
		sendTitle(player, 20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * 向玩家发送标题（1.8+），文本会被着色。
	 *
	 * @param player   玩家
	 * @param fadeIn   标题淡入时长（刻）
	 * @param stay     标题停留时长（刻）
	 * @param fadeOut  淡出时长（刻）
	 * @param title    标题，会被着色
	 * @param subtitle 副标题，会被着色
	 */
	public static void sendTitle(final Player player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		if (MinecraftVersion.newerThan(V.v1_7))
			if (hasExtendedPlayerTitleAPI)
				player.sendTitle(Common.colorize(title), Common.colorize(subtitle), fadeIn, stay, fadeOut);
			else
				ChatInternals.sendTitleLegacy(player, fadeIn, stay, fadeOut, title, subtitle);
		else {
			Common.tell(player, title);
			Common.tell(player, subtitle);
		}
	}

	/**
	 * 重置正在向玩家显示的标题（1.8+）
	 *
	 * @param player 玩家
	 */
	public static void resetTitle(final Player player) {
		if (hasExtendedPlayerTitleAPI)
			player.resetTitle();
		else
			ChatInternals.resetTitleLegacy(player);
	}

	/**
	 * 设置 Tab 列表的页眉和/或页脚。页眉或页脚可以为 null。（1.8+）
	 * 文本会被着色。
	 *
	 * @param player 玩家
	 * @param header 页眉
	 * @param footer 页脚
	 */
	public static void sendTablist(final Player player, final String header, final String footer) {
		Valid.checkBoolean(MinecraftVersion.newerThan(V.v1_7), "Sending tab list requires Minecraft 1.8x or newer!");

		if (MinecraftVersion.atLeast(V.v1_13))
			player.setPlayerListHeaderFooter(Common.colorize(header), Common.colorize(footer));
		else
			ChatInternals.sendTablistLegacy(player, header, footer);
	}

	/**
	 * 在玩家生命值和饥饿值栏上方显示消息。（1.8+）文本会
	 * 被着色。
	 *
	 * @param player 玩家
	 * @param text   文本
	 */
	public static void sendActionBar(final Player player, final String text) {
		if (!MinecraftVersion.newerThan(V.v1_7)) {
			Common.tell(player, text);
			return;
		}

		try {
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Common.colorize(text)));

		} catch (final NoSuchMethodError err) {
			ChatInternals.sendActionBarLegacy(player, text);
		}
	}

	/**
	 * 以百分比形式发送 Boss 栏
	 *
	 * @param player
	 * @param message
	 * @param percent
	 */
	public static void sendBossbarPercent(final Player player, final String message, final float percent) {
		sendBossbarPercent(player, message, percent, null, null);
	}

	/**
	 * 以百分比形式发送 Boss 栏
	 *
	 * @param player
	 * @param message
	 * @param percent
	 * @param color
	 * @param style
	 */
	public static void sendBossbarPercent(final Player player, final String message, final float percent, final CompBarColor color, final CompBarStyle style) {
		BossBarInternals.getInstance().setMessage(player, message, percent, color, style);
	}

	/**
	 * 发送仅持续有限时间的 Boss 栏
	 *
	 * @param player
	 * @param message
	 * @param seconds
	 */
	public static void sendBossbarTimed(final Player player, final String message, final int seconds) {
		sendBossbarTimed(player, message, seconds, null, null);
	}

	/**
	 * 发送仅持续有限时间的 Boss 栏
	 *
	 * @param player
	 * @param message
	 * @param seconds
	 * @param color
	 * @param style
	 */
	public static void sendBossbarTimed(final Player player, final String message, final int seconds, final CompBarColor color, final CompBarStyle style) {
		BossBarInternals.getInstance().setMessage(player, message, seconds, color, style);
	}

	/**
	 * 尝试移除玩家的 Boss 栏。
	 * <p>
	 * 仅当你通过此类中的方法渲染它时才有效！
	 *
	 * @param player
	 */
	public static void removeBossbar(final Player player) {
		BossBarInternals.getInstance().removeBar(player);
	}

	/**
	 * 在给定方块处广播箱子打开动画，
	 * 该方块必须是箱子！
	 *
	 * @param block
	 */
	public static void sendChestClose(Block block) {
		sendChestAction(block, 0);
	}

	/**
	 * 在给定方块处广播箱子打开动画，
	 * 该方块必须是箱子！
	 *
	 * @param block
	 */
	public static void sendChestOpen(Block block) {
		sendChestAction(block, 1);
	}

	/*
	 * A helper method
	 */
	private static void sendChestAction(Block block, int action) {

		final BlockState state = block.getState();
		Valid.checkBoolean(state instanceof Chest, "You can only send chest action packet for chests not " + block);

		try {
			if (action == 1)
				((Chest) state).open();
			else
				((Chest) state).close();

		} catch (final NoSuchMethodError t) {
			final Location location = block.getLocation();

			final Class<?> blockClass = getNMSClass("Block");
			final Class<?> blocks = getNMSClass("Blocks");

			final Object position = ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("BlockPosition", double.class, double.class, double.class), location.getX(), location.getY(), location.getZ());
			final Object packet = ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("PacketPlayOutBlockAction",
					ReflectionUtil.getNMSClass("BlockPosition"), blockClass, int.class, int.class), position, ReflectionUtil.getStaticFieldContent(blocks, "CHEST"), 1, action);

			for (final Player player : getOnlinePlayers())
				sendPacket(player, packet);
		}
	}

	/**
	 * 返回给定位置的生物群系
	 *
	 * @param block
	 * @return
	 */
	public static Biome getBiome(final Block block) {
		try {
			final Method getBiome = ReflectionUtil.getMethod(Block.class, "getBiome");

			return ReflectionUtil.invoke(getBiome, block);

		} catch (final NoSuchMethodError err) {
			return getBiome(block.getLocation());
		}
	}

	/**
	 * 根据给定标签创建新的插件命令
	 *
	 * @param label
	 * @return
	 */
	public static PluginCommand newCommand(final String label) {
		try {
			final Constructor<PluginCommand> con = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
			con.setAccessible(true);

			return con.newInstance(label, SimplePlugin.getInstance());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Unable to create command: /" + label);
		}
	}

	/**
	 * 生成新 {@link NamespacedKey} 的快捷方法。需要 MC 1.13+
	 *
	 * @param name
	 * @return
	 */
	public static NamespacedKey newNamespaced(String name) {
		return new NamespacedKey(SimplePlugin.getInstance(), name);
	}

	/**
	 * 生成新 {@link NamespacedKey} 的快捷方法。需要 MC 1.13+
	 *
	 * 名称会以 YOURPLUGIN_RANDOM 格式随机分配，其中 YOURPLUGIN
	 * 是你的插件名称，RANDOM 是 16 个随机字母。
	 *
	 * @return
	 */
	public static NamespacedKey newNamespaced() {
		return new NamespacedKey(SimplePlugin.getInstance(), SimplePlugin.getNamed() + "_" + RandomUtil.nextString(16));
	}

	/**
	 * 设置自定义命令名称
	 *
	 * @param command
	 * @param name
	 */
	public static void setCommandName(final PluginCommand command, final String name) {
		try {
			command.setName(name);
		} catch (final NoSuchMethodError ex) {
		}
	}

	/**
	 * 将现有命令注入命令映射
	 *
	 * @param command
	 */
	public static void registerCommand(final Command command) {
		final CommandMap commandMap = getCommandMap();
		commandMap.register(command.getLabel(), command);

		Valid.checkBoolean(command.isRegistered(), "Command /" + command.getLabel() + " could not have been registered properly!");
	}

	/**
	 * 按标签从命令映射中移除命令，包括所有别名
	 *
	 * @param label 标签
	 */
	public static void unregisterCommand(final String label) {
		unregisterCommand(label, true);
	}

	/**
	 * 按标签从命令映射中移除命令，可选择同时移除
	 * 别名
	 *
	 * @param label          标签
	 * @param removeAliases 是否同时移除别名？
	 */
	public static void unregisterCommand(final String label, final boolean removeAliases) {
		try {
			// Unregister the commandMap from the command itself.
			final PluginCommand command = Bukkit.getPluginCommand(label);

			if (command != null) {
				final Field commandField = Command.class.getDeclaredField("commandMap");
				commandField.setAccessible(true);

				if (command.isRegistered())
					command.unregister((CommandMap) commandField.get(command));
			}

			// Delete command + aliases from server's command map.
			final Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
			f.setAccessible(true);

			final Map<String, Command> cmdMap = (Map<String, Command>) f.get(getCommandMap());

			cmdMap.remove(label);

			if (command != null && removeAliases)
				for (final String alias : command.getAliases())
					cmdMap.remove(alias);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Failed to unregister command /" + label);
		}
	}

	/**
	 * 返回服务器的命令映射
	 *
	 * @return
	 */
	public static SimpleCommandMap getCommandMap() {
		final Class<?> craftServer = getOBCClass("CraftServer");

		try {
			return (SimpleCommandMap) craftServer.getDeclaredMethod("getCommandMap").invoke(Bukkit.getServer());

		} catch (final ReflectiveOperationException ex) {

			try {
				return ReflectionUtil.getFieldContent(Bukkit.getServer(), "commandMap");

			} catch (final Throwable ex2) {
				throw new FoException(ex2, "Unable to get the command map");
			}
		}
	}

	/**
	 * 返回背包的位置
	 *
	 * @param inv 背包
	 * @return 位置
	 */
	public static Location getLocation(final Inventory inv) {
		if (hasInventoryLocation)
			try {
				return inv.getLocation();

			} catch (final NullPointerException ex) { // EnderChest throws this
				return null;
			}

		return inv.getHolder() instanceof BlockState ? ((BlockState) inv.getHolder()).getLocation() : !inv.getViewers().isEmpty() ? inv.getViewers().iterator().next().getLocation() : null;
	}

	/**
	 * 返回给定位置的生物群系
	 *
	 * @param loc
	 * @return
	 */
	public static Biome getBiome(Location loc) {
		try {
			return loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

		} catch (final NoSuchMethodError err) {
			return loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockZ());
		}
	}

	/**
	 * 返回玩家 Minecraft 客户端的语言
	 * <p>
	 * 参见 {@link Player#getLocale()}
	 * <p>
	 * 若你的 MC 版本不支持则返回 null
	 *
	 * @param player
	 * @return
	 */
	public static String getLocale(final Player player) {
		try {
			return player.getLocale();

		} catch (final Throwable t) {
			try {
				final Player.Spigot spigot = player.spigot();
				final Method method = ReflectionUtil.getMethod(spigot.getClass(), "getLocale");

				return (String) ReflectionUtil.invoke(method, spigot);

			} catch (final Throwable tt) {
				return null;
			}
		}
	}

	/**
	 * 返回给定统计项的 NMS 统计名称
	 *
	 * @param stat
	 * @param mat
	 * @param en
	 * @return
	 */
	public static String getNMSStatisticName(final Statistic stat, final Material mat, final EntityType en) {
		final Class<?> craftStatistic = getOBCClass("CraftStatistic");
		Object nmsStatistic = null;

		try {
			if (stat.getType() == Type.UNTYPED)
				nmsStatistic = craftStatistic.getMethod("getNMSStatistic", stat.getClass()).invoke(null, stat);

			else if (stat.getType() == Type.ENTITY)
				nmsStatistic = craftStatistic.getMethod("getEntityStatistic", stat.getClass(), en.getClass()).invoke(null, stat, en);

			else
				nmsStatistic = craftStatistic.getMethod("getMaterialStatistic", stat.getClass(), mat.getClass()).invoke(null, stat, mat);

			Valid.checkNotNull(nmsStatistic, "Could not get NMS statistic from Bukkit's " + stat);

			if (MinecraftVersion.equals(V.v1_8)) {
				final Field f = nmsStatistic.getClass().getField("name");
				f.setAccessible(true);
				return f.get(nmsStatistic).toString();
			}

			return (String) nmsStatistic.getClass().getMethod(MinecraftVersion.atLeast(V.v1_18) ? "d" : "getName").invoke(nmsStatistic);
		} catch (final Throwable t) {
			throw new FoException(t, "Error getting NMS statistic name from " + stat);
		}
	}

	/**
	 * 2 刻后尝试通过原生方法或反射让玩家重生
	 *
	 * @param player
	 */
	public static void respawn(final Player player) {
		respawn(player, 2);
	}

	/**
	 * 尝试通过原生方法或反射让玩家重生
	 *
	 * @param player
	 * @param delayTicks 重生前等待的时长，最少 1 刻
	 */
	public static void respawn(final Player player, final int delayTicks) {
		Common.runLater(delayTicks, () -> {
			try {
				player.spigot().respawn();

			} catch (final NoSuchMethodError err) {
				try {
					final Object respawnEnum = getNMSClass("EnumClientCommand", "N/A").getEnumConstants()[0];
					final Constructor<?>[] constructors = getNMSClass("PacketPlayInClientCommand", "N/A").getConstructors();

					for (final Constructor<?> constructor : constructors) {
						final Class<?>[] args = constructor.getParameterTypes();
						if (args.length == 1 && args[0] == respawnEnum.getClass()) {
							final Object packet = getNMSClass("PacketPlayInClientCommand", "N/A").getConstructor(args).newInstance(respawnEnum);

							sendPacket(player, packet);
							break;
						}
					}

				} catch (final Throwable e) {
					throw new FoException(e, "Failed to send respawn packet to " + player.getName());
				}
			}
		});
	}

	// ----------------------------------------------------------------------------------------------------
	// NMS-related
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 自动查找类：对于旧版 MC，在 oldName 中填写类型（例如 EntityPlayer），
	 * 我们会自动找到正确的 NMS 导入；若使用 MC 1.17+，则填写
	 * 完整类路径（例如 net.minecraft.server.level.EntityPlayer），我们会改用它。
	 *
	 * @param oldName
	 * @param fullName1_17
	 * @return
	 */
	public static Class<?> getNMSClass(final String oldName, final String fullName1_17) {
		return MinecraftVersion.atLeast(V.v1_17) ? ReflectionUtil.lookupClass(fullName1_17) : getNMSClass(oldName);
	}

	/**
	 * 在 net.minecraft.server 包中查找类，会自动添加版本号
	 * （1.20.5+ 上为空）。
	 *
	 * @deprecated Minecraft 1.17+ 的路径名不同，
	 *             请改用 {@link #getNMSClass(String, String)}
	 *
	 * @param name
	 * @return
	 */
	@Deprecated
	public static Class<?> getNMSClass(final String name) {
		String safeguardPrefix = Remain.getNmsVersion();

		if (!safeguardPrefix.isEmpty())
			safeguardPrefix += ".";

		return ReflectionUtil.lookupClass(NMS + "." + safeguardPrefix + name);
	}

	/**
	 * 在 org.bukkit.craftbukkit 包中查找类，会自动添加版本号
	 * （1.20.5+ 上为空）。
	 *
	 * @param name
	 * @return
	 */
	public static Class<?> getOBCClass(final String name) {
		String version = Remain.getNmsVersion();

		if (!version.isEmpty())
			version += ".";

		return ReflectionUtil.lookupClass(CRAFTBUKKIT + "." + version + name);
	}

	/**
	 * 返回给定 NMS 类名（例如 EntityZombie）的构造器。
	 *
	 * @param nmsClassPath
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructorNMS(final String nmsClassPath, final Class<?>... params) {
		return ReflectionUtil.getConstructor(getNMSClass(nmsClassPath), params);
	}

	/**
	 * 使用参数创建给定 NMS 类的新实例。
	 *
	 * @param <T>
	 * @param nmsPath
	 * @param params
	 * @return
	 */
	public static <T> T instantiateNMS(final String nmsPath, final Object... params) {
		return (T) ReflectionUtil.instantiate(getNMSClass(nmsPath), params);
	}

	/**
	 * 为玩家打开告示牌。在旧版本上，需要 ProtocolLib
	 * 才能在更新告示牌后保存编辑内容。
	 *
	 * @param player
	 * @param signBlock
	 */
	public static void openSign(Player player, Block signBlock) {
		final BlockState state = signBlock.getState();
		Valid.checkBoolean(state instanceof Sign, "Block is not a sign: " + signBlock);

		final Sign sign = (Sign) state;

		if (hasPlayerOpenSignMethod) {
			player.openSign(sign);

		} else {
			final Class<?> chatComponentClass = ReflectionUtil.getNMSClass("IChatBaseComponent");
			final Class<?> blockPositionClass = ReflectionUtil.getNMSClass("BlockPosition");

			final Object blockPosition = ReflectionUtil.instantiate(ReflectionUtil.getConstructor(blockPositionClass, int.class, int.class, int.class), signBlock.getX(), signBlock.getY(), signBlock.getZ());
			final Object[] chatComponent = (Object[]) java.lang.reflect.Array.newInstance(chatComponentClass, 4);

			for (int i = 0; i < 4; i++)
				chatComponent[i] = Remain.toIChatBaseComponentPlain(sign.getLine(i));

			final Object nmsSign = Remain.getHandleEntity(sign);
			final Object nmsPlayer = Remain.getHandleEntity(player);

			// Set the sign to be editable and assign the editing player to it
			ReflectionUtil.setDeclaredField(nmsSign, "isEditable", true);
			ReflectionUtil.setDeclaredField(nmsSign, "h", nmsPlayer);

			CompMetadata.setTempMetadata(player, FoConstants.NBT.METADATA_OPENED_SIGN, sign.getLocation());
			Remain.sendPacket(player, ReflectionUtil.instantiate(ReflectionUtil.getConstructorNMS("PacketPlayOutOpenSignEditor", blockPositionClass), blockPosition));
		}
	}

	/**
	 * 为玩家打开书本，前提是该书为 WRITTEN_BOOK
	 *
	 * @param player
	 * @param book
	 */
	public static void openBook(Player player, ItemStack book) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_8), "Opening books is only supported on MC 1.8 and greater");
		Valid.checkBoolean(book.getItemMeta() instanceof org.bukkit.inventory.meta.BookMeta, "openBook method called for not a book item: " + book);
		Valid.checkBoolean(CompMaterial.fromMaterial(book.getType()) == CompMaterial.WRITTEN_BOOK, "Can only call openBook for WRITTEN_BOOK! Got: " + book);

		// Fix "Invalid book tag" error when author/title is empty
		final org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();

		if (meta.getAuthor() == null)
			meta.setAuthor("");

		if (meta.getTitle() == null)
			meta.setTitle("");

		if (meta.getPageCount() == 0)
			meta.setPages(""); // Empty book

		book.setItemMeta(meta);

		try {
			player.openBook(book);

		} catch (final NoSuchMethodError ex) {
			final ItemStack oldItem = player.getItemInHand();

			// Set the book temporarily to hands
			player.setItemInHand(book);

			final Object craftPlayer = getHandleEntity(player);
			final Object nmsItemstack = asNMSCopy(book);

			Common.runLater(() -> {
				final Method openInventory = ReflectionUtil.getMethod(craftPlayer.getClass(), "openBook", nmsItemstack.getClass());
				ReflectionUtil.invoke(openInventory, craftPlayer, nmsItemstack);

				// Reset hands
				player.setItemInHand(oldItem);
			});
		}
	}

	/**
	 * 在不关闭窗口的情况下更新玩家的背包标题
	 *
	 * @param player 玩家
	 * @param title  新标题
	 * @deprecated 请使用 {@link PlayerUtil#updateInventoryTitle(Player, String)}
	 */
	@Deprecated
	public static void updateInventoryTitle(final Player player, String title) {
		try {
			final Object view = ReflectionUtil.invoke("getOpenInventory", player);
			final Method setTitle = ReflectionUtil.getMethod(view.getClass(), "setTitle", String.class);

			if (setTitle == null)
				throw new NoSuchMethodError();

			ReflectionUtil.invoke(setTitle, view, Common.colorize(title));

		} catch (final NoSuchMethodError err) {

			final Inventory topInventory = Remain.getTopInventoryFromOpenInventory(player);

			try {
				if (MinecraftVersion.atLeast(V.v1_17)) {
					final String nmsVersion = getNmsVersion();

					final boolean is1_17 = MinecraftVersion.equals(V.v1_17);
					final boolean is1_18 = MinecraftVersion.equals(V.v1_18);
					final boolean is1_19 = MinecraftVersion.equals(V.v1_19);

					final Object nmsPlayer = Remain.getHandleEntity(player);
					final Object chatComponent = toIChatBaseComponentPlain(ChatColor.translateAlternateColorCodes('&', title));

					final int inventorySize = topInventory.getSize() / 9;
					String containerName;

					if (inventorySize == 1)
						containerName = "a";

					else if (inventorySize == 2)
						containerName = "b";

					else if (inventorySize == 3)
						containerName = "c";

					else if (inventorySize == 4)
						containerName = "d";

					else if (inventorySize == 5)
						containerName = "e";

					else if (inventorySize == 6)
						containerName = "f";
					else
						throw new FoException("Cannot generate NMS container class to update inventory of size " + inventorySize);

					final Object container = ReflectionUtil.getStaticFieldContent(ReflectionUtil.lookupClass("net.minecraft.world.inventory.Containers"), containerName);

					final Constructor<?> packetConstructor = ReflectionUtil.getConstructor(
							"net.minecraft.network.protocol.game.PacketPlayOutOpenWindow",
							int.class,
							container.getClass(),
							ReflectionUtil.lookupClass("net.minecraft.network.chat.IChatBaseComponent"));

					String activeContainerName;

					if (is1_17)
						activeContainerName = "bV";

					else if (is1_18)
						activeContainerName = nmsVersion.contains("R2") ? "bV" : "bW";

					else if (is1_19)
						activeContainerName = nmsVersion.contains("R3") ? "bP" : "bU";

					else
						activeContainerName = "bR";

					final Object activeContainer = ReflectionUtil.getFieldContent(nmsPlayer, activeContainerName);
					final int windowId = ReflectionUtil.getFieldContent(activeContainer, "j");
					Remain.sendPacket(player, ReflectionUtil.instantiate(packetConstructor, windowId, container, chatComponent));

					// Re-initialize the menu internally
					Method method = ReflectionUtil.getMethod(nmsPlayer.getClass(), "initMenu", ReflectionUtil.lookupClass("net.minecraft.world.inventory.Container"));

					if (method == null)
						method = ReflectionUtil.getMethod(nmsPlayer.getClass(), "a", ReflectionUtil.lookupClass("net.minecraft.world.inventory.Container"));

					if (method != null)
						ReflectionUtil.invoke(method, nmsPlayer, activeContainer);

					return;
				}

				if (MinecraftVersion.olderThan(V.v1_9) && title.length() > 32)
					title = title.substring(0, 32);

				final Object entityPlayer = getHandleEntity(player);
				final Object activeContainer = entityPlayer.getClass().getField("activeContainer").get(entityPlayer);
				final Object windowId = activeContainer.getClass().getField("windowId").get(activeContainer);

				final Object packetOpenWindow;

				if (MinecraftVersion.atLeast(V.v1_8)) {
					final Constructor<?> chatMessageConst = getNMSClass("ChatMessage", "net.minecraft.network.chat.ChatMessage").getConstructor(String.class, Object[].class);
					final Object chatMessage = chatMessageConst.newInstance(ChatColor.translateAlternateColorCodes('&', title), new Object[0]);

					if (MinecraftVersion.newerThan(V.v1_13)) {
						final int inventorySize = topInventory.getSize() / 9;

						if (inventorySize < 1 || inventorySize > 6) {
							Common.log("Cannot update title for " + player.getName() + " as his inventory has non typical size: " + inventorySize + " rows");

							return;
						}

						final Class<?> containersClass = getNMSClass("Containers", "net.minecraft.world.inventory.Containers");
						final Constructor<?> packetConst = getNMSClass("PacketPlayOutOpenWindow", "net.minecraft.network.protocol.game.PacketPlayOutOpenWindow")
								.getConstructor(/*windowID*/int.class, /*containers*/containersClass, /*msg*/getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"));

						final String containerName = "GENERIC_9X" + inventorySize;

						final Object container = containersClass.getField(containerName).get(null);

						packetOpenWindow = packetConst.newInstance(windowId, container, chatMessage);

					} else {
						final Constructor<?> packetConst = getNMSClass("PacketPlayOutOpenWindow", "N/A").getConstructor(int.class, String.class, getNMSClass("IChatBaseComponent", "net.minecraft.network.chat.IChatBaseComponent"), int.class);

						packetOpenWindow = packetConst.newInstance(windowId, "minecraft:chest", chatMessage, topInventory.getSize());
					}
				} else {
					final Constructor<?> openWindow = ReflectionUtil.getConstructor(
							getNMSClass(MinecraftVersion.atLeast(V.v1_7) ? "PacketPlayOutOpenWindow" : "Packet100OpenWindow", "N/A"), int.class, int.class, String.class, int.class, boolean.class);

					packetOpenWindow = ReflectionUtil.instantiate(openWindow, windowId, 0, ChatColor.translateAlternateColorCodes('&', title), topInventory.getSize(), true);
				}

				sendPacket(player, packetOpenWindow);
				entityPlayer.getClass().getMethod("updateInventory", getNMSClass("Container", "net.minecraft.world.inventory.Container")).invoke(entityPlayer, activeContainer);

			} catch (final ReflectiveOperationException ex) {
				Common.error(ex, "Error updating " + player.getName() + " inventory title to '" + title + "'");
			}
		}
	}

	/**
	 * 向某个位置发送虚假的方块更新，并在一段时间后将其恢复为
	 * 真实方块。
	 *
	 * @param delayTicks 恢复之前的间隔
	 * @param player     玩家
	 * @param location   位置
	 * @param material   材质
	 */
	public static void sendBlockChange(final int delayTicks, final Player player, final Location location, final CompMaterial material) {
		Common.runLater(delayTicks, () -> sendBlockChange0(player, location, material));
	}

	private static void sendBlockChange0(final Player player, final Location location, final CompMaterial material) {
		try {
			player.sendBlockChange(location, material.getMaterial().createBlockData());
		} catch (final NoSuchMethodError ex) {
			player.sendBlockChange(location, material.getMaterial(), material.getData());
		}
	}

	/**
	 * 向玩家发送给定方块的方块更新数据包，通常用于
	 * 将其重置回真实状态
	 *
	 * @param delayTicks
	 * @param player
	 * @param block
	 */
	public static void sendBlockChange(final int delayTicks, final Player player, final Block block) {
		Common.runLater(delayTicks, () -> sendBlockChange0(player, block));
	}

	private static void sendBlockChange0(final Player player, final Block block) {
		try {
			player.sendBlockChange(block.getLocation(), block.getBlockData());
		} catch (final NoSuchMethodError ex) {
			player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
		}
	}

	/**
	 * 返回玩家在此服务器上的游玩时长（从你的世界统计文件中读取），
	 * 单位为分钟
	 *
	 * @param player
	 * @return
	 */
	public static long getPlaytimeMinutes(final OfflinePlayer player) {
		return getPlaytimeSeconds(player) / 60;
	}

	/**
	 * 返回玩家在此服务器上的游玩时长（从你的世界统计文件中读取），
	 * 单位为秒。
	 *
	 * @param player
	 * @return
	 */
	public static long getPlaytimeSeconds(final OfflinePlayer player) {
		final long value = PlayerUtil.getStatistic(player, getPlayTimeStatisticName());

		return value / 20;
	}

	/**
	 * MC <1.13 返回 PLAY_ONE_TICK，1.13+ 返回 PLAY_ONE_MINUTE
	 *
	 * @return
	 */
	public static Statistic getPlayTimeStatisticName() {
		return Statistic.valueOf(MinecraftVersion.olderThan(V.v1_13) ? "PLAY_ONE_TICK" : "PLAY_ONE_MINUTE");
	}

	/**
	 * 返回游玩时间统计是否以刻为单位
	 *
	 * @return
	 */
	public static boolean isPlaytimeStatisticTicks() {
		return MinecraftVersion.olderThan(V.v1_13);
	}

	/**
	 * 自从 Minecraft 引入双持后，每次交互会为每只手各触发一次
	 * 事件。返回该事件是否为主手触发。
	 * <p>
	 * 向后兼容。
	 *
	 * @param event 事件
	 * @return 该事件是否仅为主手触发
	 */
	public static boolean isInteractEventPrimaryHand(final PlayerInteractEvent event) {

		if (MinecraftVersion.olderThan(V.v1_9))
			return true;

		try {
			return event.getHand() != null && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;

		} catch (final NoSuchMethodError err) {
			return true; // Older MC, always true since there was no off-hand
		}
	}

	/**
	 * 参见 {@link #isInteractEventPrimaryHand(PlayerInteractEvent)}
	 *
	 * @param e
	 * @return
	 */
	public static boolean isInteractEventPrimaryHand(final PlayerInteractEntityEvent e) {

		if (MinecraftVersion.olderThan(V.v1_9))
			return true;

		try {
			return e.getHand() != null && e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND;

		} catch (final NoSuchMethodError err) {
			return true; // Older MC, always true since there was no off-hand
		}
	}

	/**
	 * 返回计分板分数
	 *
	 * @param obj
	 * @param entry
	 * @return
	 */
	public static Score getScore(final Objective obj, String entry) {
		Valid.checkNotNull(obj, "Objective cannot be null");

		entry = Common.colorize(entry);

		try {
			return obj.getScore(entry);

		} catch (final NoSuchMethodError err) {
			return obj.getScore(Bukkit.getOfflinePlayer(entry));
		}
	}

	/**
	 * 尝试通过 uuid 查找离线玩家
	 *
	 * @param id
	 * @return
	 */
	public static OfflinePlayer getOfflinePlayerByUUID(final UUID id) {
		try {
			return Bukkit.getOfflinePlayer(id);

		} catch (final NoSuchMethodError err) {
			if (Bukkit.isPrimaryThread())
				Common.log("getOfflinePlayerByUUID required two blocking calls on main thread - please notify " + SimplePlugin.getNamed() + " plugin authors.");

			final UUIDToNameConverter f = new UUIDToNameConverter(id);

			try {
				final String name = f.call();

				return Bukkit.getOfflinePlayer(name);
			} catch (final Throwable t) {
				return null;
			}
		}
	}

	/**
	 * 尝试通过 uuid 查找在线玩家
	 *
	 * @param id
	 *
	 * @return 若离线则为 null，否则为玩家
	 */
	public static Player getPlayerByUUID(final UUID id) {
		try {
			final Player player = Bukkit.getPlayer(id);

			return player != null && player.isOnline() ? player : null;

		} catch (final NoSuchMethodError err) {
			for (final Player online : getOnlinePlayers())
				if (online.getUniqueId().equals(id))
					return online;

			return null;
		}
	}

	/**
	 * 获取事件的最终伤害
	 *
	 * @param event
	 * @return
	 */
	public static double getFinalDamage(final EntityDamageEvent event) {
		try {
			return event.getFinalDamage();

		} catch (final NoSuchMethodError err) {
			return event.getDamage();
		}
	}

	/**
	 * 返回实际被点击的背包（底部或顶部背包；
	 * 若点击在外部则为 null）
	 *
	 * @param event 背包点击事件
	 * @return 实际被点击的背包，底部或顶部；若点击在外部
	 * 则为 null
	 */
	public static Inventory getClickedInventory(final InventoryClickEvent event) {
		final int slot = event.getRawSlot();

		if (slot < 0)
			return null;

		final Inventory topInventory = invokeInventoryViewMethod(event, "getTopInventory");
		final Inventory bottomInventory = invokeInventoryViewMethod(event, "getBottomInventory");

		return topInventory != null && slot < topInventory.getSize() ? topInventory : bottomInventory;
	}

	/**
	 *
	 * @param <T>
	 * @param event
	 * @param methodName
	 * @return
	 */
	public static <T> T invokeInventoryViewMethod(InventoryEvent event, String methodName) {
		final Object view = ReflectionUtil.invoke("getView", event);

		return ReflectionUtil.invoke(methodName, view);
	}

	/**
	 * 返回玩家已打开背包视图的顶部背包
	 *
	 * @param player
	 * @return
	 */
	public static Inventory getTopInventoryFromOpenInventory(Player player) {
		return invokeOpenInventoryMethod(player, "getTopInventory");
	}

	/**
	 * 返回玩家已打开背包视图的顶部背包
	 *
	 * @param player
	 * @return
	 */
	public static Inventory getBottomInventoryFromOpenInventory(Player player) {
		return invokeOpenInventoryMethod(player, "getBottomInventory");
	}

	/**
	 *
	 * @param <T>
	 * @param player
	 * @param methodName
	 * @return
	 */
	public static <T> T invokeOpenInventoryMethod(Player player, String methodName) {
		final Object view = ReflectionUtil.invoke("getOpenInventory", player);

		return ReflectionUtil.invoke(methodName, view);
	}

	/**
	 * 返回书中的页面列表（新版 MC 还会暴露交互元素）
	 *
	 * @param metaObject
	 * @return
	 */
	public static List<BaseComponent[]> getPages(Object metaObject) {
		Valid.checkBoolean(metaObject instanceof org.bukkit.inventory.meta.BookMeta);
		final org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) metaObject;

		try {
			return meta.spigot().getPages();

		} catch (final NoSuchMethodError ex) {
			final List<BaseComponent[]> list = new ArrayList<>();

			for (final String page : meta.getPages())
				list.add(TextComponent.fromLegacyText(page));

			return list;
		}
	}

	/**
	 * 尝试根据给定列表设置书的页面
	 *
	 * @param metaObject
	 * @param pages
	 */
	public static void setPages(Object metaObject, List<BaseComponent[]> pages) {
		Valid.checkBoolean(metaObject instanceof org.bukkit.inventory.meta.BookMeta);
		final org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) metaObject;

		try {
			meta.spigot().setPages(pages);

		} catch (final NoSuchMethodError ex) {
			try {
				final List<Object> chatComponentPages = (List<Object>) ReflectionUtil.getFieldContent(ReflectionUtil.getOBCClass("inventory.CraftMetaBook"), "pages", meta);

				for (final BaseComponent[] text : pages)
					chatComponentPages.add(toIChatBaseComponent(text));

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 根据给定的纯文本返回 IChatBaseComponent
	 *
	 * @param text
	 * @return
	 */
	public static Object toIChatBaseComponentPlain(String text) {
		return toIChatBaseComponent(TextComponent.fromLegacyText(text));
	}

	/**
	 * 根据给定的组件列表返回 IChatBaseComponent
	 *
	 * @param baseComponents
	 * @return
	 */
	public static Object toIChatBaseComponent(BaseComponent[] baseComponents) {
		return toIChatBaseComponent(toJson(baseComponents));
	}

	/**
	 * 根据给定的 JSON 返回 IChatBaseComponent
	 *
	 * @param json
	 * @return
	 */
	public static Object toIChatBaseComponent(String json) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "Serializing chat components requires Minecraft 1.7.10 and greater");

		final Class<?> chatSerializer = ReflectionUtil.getNMSClass((MinecraftVersion.equals(V.v1_7) ? "" : "IChatBaseComponent$") + "ChatSerializer", "net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");
		final Method a = ReflectionUtil.getMethod(chatSerializer, "a", String.class);

		return ReflectionUtil.invoke(a, null, json);
	}

	/**
	 * 返回实体的名称
	 *
	 * @param entity
	 * @return
	 */
	public static String getName(final Entity entity) {
		try {
			return entity.getName();

		} catch (final NoSuchMethodError t) {
			return entity instanceof Player ? ((Player) entity).getName() : ItemUtil.bountifyCapitalized(entity.getType());
		}
	}

	/**
	 * 为实体设置自定义名称
	 *
	 * @param entity
	 * @param name
	 */
	public static void setCustomName(final Entity entity, final String name) {
		setCustomName(entity, name, true);
	}

	/**
	 * 为实体设置自定义名称
	 *
	 * @param entity
	 * @param name
	 * @param visible
	 */
	public static void setCustomName(final Entity entity, @Nullable final String name, final boolean visible) {
		try {
			entity.setCustomNameVisible(visible);

			if (name != null)
				entity.setCustomName(Common.colorize(name));

		} catch (final NoSuchMethodError er) {
			Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "setCustomName requires Minecraft 1.7.10+");

			final NBTEntity nbt = new NBTEntity(entity);

			nbt.setInteger("CustomNameVisible", visible ? 1 : 0);

			if (name != null)
				nbt.setString("CustomName", Common.colorize(name));
		}
	}

	/**
	 * 如果实体有自定义名称，则将其移除
	 *
	 * @param entity
	 */
	public static void removeCustomName(final Entity entity) {
		try {
			entity.setCustomNameVisible(false);
			entity.setCustomName(null);

		} catch (final NoSuchMethodError er) {
			Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_7), "setCustomName requires Minecraft 1.7.10+");

			final NBTEntity nbt = new NBTEntity(entity);

			nbt.removeKey("CustomNameVisible");
			nbt.removeKey("CustomName");
		}
	}

	/**
	 * 调用 NMS 判断实体是否隐形，适用于任何实体；
	 * 比 Bukkit 更好，因为它具有极强的向下兼容性，且不需要 LivingEntity
	 *
	 * 请谨慎使用：对于旁观者模式和隐身药水也会返回 true
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isInvisible(Entity entity) {
		if (entity instanceof LivingEntity && MinecraftVersion.atLeast(V.v1_16))
			return entity.isInvisible();

		else if (MinecraftVersion.atLeast(V.v1_4)) {
			final Object nmsEntity = getHandleEntity(entity);

			return (boolean) ReflectionUtil.invoke("isInvisible", nmsEntity);
		}

		return false;
	}

	/**
	 * 调用 NMS 设置任何实体的隐形状态；
	 * 比 Bukkit 更好，因为它具有极强的向下兼容性，且不需要 LivingEntity
	 *
	 * @param entity
	 * @param invisible
	 *
	 * @deprecated 请使用 {@link PlayerUtil#setVanished(Player, boolean)}，以便同时为插件禁用隐身
	 */
	@Deprecated
	public static void setInvisible(Object entity, boolean invisible) {
		Valid.checkBoolean(MinecraftVersion.atLeast(V.v1_4), "Entity#setInvisible requires Minecraft 1.4.7 or greater");

		if (entity instanceof LivingEntity && MinecraftVersion.atLeast(V.v1_16))
			((LivingEntity) entity).setInvisible(invisible);

		else {
			final Object nmsEntity = entity.getClass().toString().contains("net.minecraft.server") ? entity : entity instanceof LivingEntity ? getHandleEntity(entity) : null;
			Valid.checkNotNull(nmsEntity, "setInvisible requires either a LivingEntity or a NMS Entity, got: " + entity.getClass());
			final Method setInvisible = ReflectionUtil.getMethod(nmsEntity.getClass(), "setInvisible", boolean.class);

			// https://www.spigotmc.org/threads/how-do-i-make-an-entity-go-invisible-without-using-potioneffects.321227/
			Common.runLater(2, () -> {
				try {
					ReflectionUtil.invoke(setInvisible, nmsEntity, invisible);

				} catch (final Throwable t) {

					// unsupported
					t.printStackTrace();
				}
			});
		}
	}

	/**
	 * 返回给定实体是否无敌
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isInvulnerable(Entity entity) {
		try {
			return entity.isInvulnerable();

		} catch (final NoSuchMethodError ex) {

			if (fieldEntityInvulnerable != null)
				try {
					return (boolean) fieldEntityInvulnerable.get(getHandleEntity(entity));

				} catch (final ReflectiveOperationException exx) {
				}

			return false;
		}
	}

	/**
	 * 设置实体的无敌状态，
	 * 在 1.7.10 等旧版 Minecraft 上可能会失败。
	 *
	 * @param entity
	 * @param invulnerable
	 */
	public static void setInvulnerable(Entity entity, boolean invulnerable) {
		CompProperty.INVULNERABLE.apply(entity, invulnerable);
	}

	/**
	 * 尝试获取第一个材质，失败则返回第二个作为回退
	 *
	 * @param material
	 * @param fallback
	 * @return
	 */
	public static CompMaterial getMaterial(final String material, final CompMaterial fallback) {
		Material mat = null;

		try {
			mat = Material.getMaterial(material);
		} catch (final Throwable t) {
		}

		return mat != null ? CompMaterial.fromMaterial(mat) : fallback;
	}

	/**
	 * 尝试按名称获取新材质，失败则返回旧材质作为回退
	 *
	 * @param newMaterial
	 * @param oldMaterial
	 * @return
	 */
	public static Material getMaterial(final String newMaterial, final String oldMaterial) {
		try {
			return Material.getMaterial(newMaterial);

		} catch (final Throwable t) {
			return Material.getMaterial(oldMaterial);
		}
	}

	/**
	 * 获取玩家的目标方块
	 *
	 * @param en
	 * @param radius
	 * @return
	 */
	public static Block getTargetBlock(final LivingEntity en, final int radius) {
		try {
			return en.getTargetBlock((Set<Material>) null, radius);

		} catch (final Throwable t) {
			if (t instanceof IllegalStateException)
				return null;

			try {
				return (Block) en.getClass().getMethod("getTargetBlock", HashSet.class, int.class).invoke(en, (HashSet<Byte>) null, radius);

			} catch (final ReflectiveOperationException ex2) {
				throw new FoException(t, "Unable to get target block for " + en);
			}
		}
	}

	/**
	 * 发送 "toast" 通知。这是一种进度通知，其首屏无法
	 * 修改。会带来轻微的性能开销。
	 *
	 * @param receiver
	 * @param message
	 */
	public static void sendToast(Player receiver, String message) {
		sendToast(receiver, message, CompMaterial.BOOK, CompToastStyle.TASK);
	}

	/**
	 * 发送 "toast" 通知。这是一种进度通知，其首屏无法
	 * 修改。会带来轻微的性能开销。
	 *
	 * 你可以在此从 Minecraft 预设的界面中选择首屏样式。
	 *
	 * @param receiver
	 * @param message
	 * @param toastStyle
	 */
	public static void sendToast(Player receiver, String message, CompToastStyle toastStyle) {
		sendToast(receiver, message, CompMaterial.BOOK, toastStyle);
	}

	/**
	 * 发送 "toast" 通知。这是一种进度通知，其首屏无法
	 * 修改。会带来轻微的性能开销。
	 *
	 * 你可以在此修改首屏上显示的图标。
	 *
	 * @param receiver
	 * @param message
	 * @param icon
	 */
	public static void sendToast(final Player receiver, final String message, final CompMaterial icon) {
		sendToast(receiver, message, icon, CompToastStyle.TASK);
	}

	/**
	 * 发送 "toast" 通知。这是一种进度通知，可修改的
	 * 地方不多。会带来轻微的性能开销。
	 *
	 * 你可以在此修改首屏上显示的图标。
	 * 你也可以在此从 Minecraft 预设的界面中选择首屏样式。
	 *
	 * @param receiver
	 * @param message
	 * @param icon
	 * @param toastStyle
	 */
	public static void sendToast(final Player receiver, final String message, final CompMaterial icon, final CompToastStyle toastStyle) {
		if (message != null && !message.isEmpty()) {
			final String colorized = Common.colorize(message);

			if (!colorized.isEmpty()) {
				Valid.checkSync("Toasts may only be sent from the main thread");

				if (hasAdvancements)
					new AdvancementAccessor(colorized, icon.toString().toLowerCase(), toastStyle).show(receiver);

				else
					receiver.sendMessage(colorized);
			}
		}
	}

	/**
	 * 向给定接收者发送 "toast" 通知。这是一种进度通知，可修改的
	 * 地方不多。发送的玩家越多，性能开销越大。
	 *
	 * 每个玩家的发送间隔为 0.1 秒
	 *
	 * @param receivers
	 * @param message 你可以在此替换消息中玩家专属的变量
	 * @param icon
	 */
	public static void sendToast(final List<Player> receivers, final Function<Player, String> message, final CompMaterial icon) {
		sendToast(receivers, message, icon, CompToastStyle.GOAL);
	}

	/**
	 * 向给定接收者发送 "toast" 通知。这是一种进度通知，可修改的
	 * 地方不多。发送的玩家越多，性能开销越大。
	 *
	 * 每个玩家的发送间隔为 0.1 秒
	 *
	 * @param receivers
	 * @param message 你可以在此替换消息中玩家专属的变量
	 * @param icon
	 * @param style
	 */
	public static void sendToast(final List<Player> receivers, final Function<Player, String> message, final CompMaterial icon, final CompToastStyle style) {

		if (hasAdvancements)
			Common.runAsync(() -> {
				for (final Player receiver : receivers) {

					// Sleep to mitigate sending not working at once
					Common.sleep(100);

					Common.runLater(() -> {
						final String colorized = Common.colorize(message.apply(receiver));

						if (!colorized.isEmpty()) {
							final AdvancementAccessor accessor = new AdvancementAccessor(colorized, icon.toString().toLowerCase(), style);

							if (receiver.isOnline())
								accessor.show(receiver);
						}
					});
				}
			});
		else
			for (final Player receiver : receivers) {
				final String colorized = Common.colorize(message.apply(receiver));

				if (!colorized.isEmpty())
					receiver.sendMessage(colorized);
			}

	}

	/**
	 * 为给定材质设置可视冷却，参见 {@link Player#setCooldown(Material, int)}
	 * 你仍需自行实现对它的处理
	 * <p>
	 * 旧版 MC 也受支持并由我们处理，
	 * 但没有可视效果
	 *
	 * @param player
	 * @param material
	 * @param cooldownTicks
	 */
	public static void setCooldown(final Player player, final Material material, final int cooldownTicks) {
		try {
			player.setCooldown(material, cooldownTicks);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			cooldown.override(material, cooldownTicks);
			cooldowns.override(player.getUniqueId(), cooldown);
		}
	}

	/**
	 * 参见 {@link Player#hasCooldown(Material)}
	 * <p>
	 * 旧版 MC 也受支持并由我们处理，
	 * 但没有可视效果
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static boolean hasCooldown(final Player player, final Material material) {
		try {
			return player.hasCooldown(material);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			return cooldown.containsKey(material);
		}
	}

	/**
	 * 返回 {@link Player#getCooldown(Material)} 中指定的物品冷却
	 * <p>
	 * 旧版 MC 也受支持并由我们处理，
	 * 但没有可视效果
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static int getCooldown(final Player player, final Material material) {
		try {
			return player.getCooldown(material);

		} catch (final Throwable t) {
			final StrictMap<Material, Integer> cooldown = getCooldown(player);

			return cooldown.getOrDefault(material, 0);
		}
	}

	// Internal method to get a players cooldown map
	private static StrictMap<Material, Integer> getCooldown(final Player player) {
		return cooldowns.getOrDefault(player.getUniqueId(), new StrictMap<>());
	}

	/**
	 * 返回玩家延迟
	 *
	 * @deprecated 请使用 {@link PlayerUtil#getPing(Player)}
	 * @param player
	 * @return
	 */
	@Deprecated
	public static int getPing(Player player) {
		try {
			return player.getPing();

		} catch (final NoSuchMethodError err) {
			final Object entityPlayer = Remain.getHandleEntity(player);

			return (int) ReflectionUtil.getFieldContent(entityPlayer, "ping");
		}
	}

	/**
	 * 通过 UUID 返回实体
	 *
	 * @param uuid
	 * @return
	 */
	public static Entity getEntity(final UUID uuid) {
		Valid.checkSync("Remain#getEntity must be called on the main thread");

		for (final World world : Bukkit.getWorlds())
			for (final Entity entity : world.getEntities())
				if (entity.getUniqueId().equals(uuid))
					return entity;

		return null;
	}

	/**
	 * 尝试从弹射物命中事件中找出被命中的实体。
	 *
	 * @param event
	 * @return
	 */
	public static LivingEntity getHitEntity(ProjectileHitEvent event) {
		try {

			// Try getting the hit entity directly
			if (event.getHitEntity() instanceof LivingEntity)
				return (LivingEntity) event.getHitEntity();

		} catch (final Throwable t) {

			// If this fails, try getting the entity to which the projectile was attached,
			// imperfect, but mostly works.
			final double radius = 0.5;

			for (final Entity nearby : event.getEntity().getNearbyEntities(radius, radius, radius))
				if (nearby instanceof LivingEntity)
					return (LivingEntity) nearby;
		}

		return null;
	}

	/**
	 * 尝试从弹射物命中事件中解析出被命中的方块
	 *
	 * @param event
	 * @return
	 */
	public static Block getHitBlock(ProjectileHitEvent event) {
		try {
			return event.getHitBlock();

		} catch (final Throwable t) {

			final Block entityBlock = event.getEntity().getLocation().getBlock();

			if (!CompMaterial.isAir(entityBlock))
				return entityBlock;

			for (final BlockFace face : Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH)) {
				final Block adjucentBlock = entityBlock.getRelative(face);

				if (!CompMaterial.isAir(adjucentBlock))
					return adjucentBlock;
			}
		}

		return null;
	}

	/**
	 * 返回某位置附近的实体
	 *
	 * @param location
	 * @param radius
	 * @return
	 */
	public static Collection<Entity> getNearbyEntities(final Location location, final double radius) {
		try {
			return location.getWorld().getNearbyEntities(location, radius, radius, radius);

		} catch (final Throwable t) {
			final List<Entity> found = new ArrayList<>();

			for (final Entity nearby : location.getWorld().getEntities())
				if (nearby.getLocation().distance(location) <= radius)
					found.add(nearby);

			return found;
		}
	}

	/**
	 * 从手持物品中扣除一个
	 *
	 * @param player
	 */
	public static void takeHandItem(final Player player) {
		takeItemAndSetAsHand(player, player.getItemInHand());
	}

	/**
	 * 从给定物品中扣除一个并将其设为手持物品
	 *
	 * @param player
	 * @param item
	 */
	public static void takeItemAndSetAsHand(final Player player, final ItemStack item) {
		if (item.getAmount() > 1) {
			item.setAmount(item.getAmount() - 1);
			player.getInventory().setItemInHand(item);

		} else
			player.getInventory().setItemInHand(null);

		player.updateInventory();
	}

	/**
	 * 从玩家背包中扣除 1 个该物品
	 *
	 * @param player
	 * @param item
	 */
	public static void takeItemOnePiece(final Player player, final ItemStack item) {
		if (MinecraftVersion.atLeast(V.v1_15))
			item.setAmount(item.getAmount() - 1);

		else {
			if (item.getAmount() > 1)
				item.setAmount(item.getAmount() - 1);

			// Explanation: For some weird reason there is a bug not removing 1 piece of ItemStack in 1.8.8
			else {
				final ItemStack[] content = player.getInventory().getContents();

				for (int slot = 0; slot < content.length; slot++) {
					final ItemStack slotItem = content[slot];

					if (slotItem != null && slotItem.equals(item)) {
						content[slot] = null;

						break;
					}
				}

				player.getInventory().setContents(content);
			}

			player.updateInventory();
		}
	}

	/**
	 * 尝试为给定物品添加持续 10 分钟的药水效果。
	 *
	 * @param item
	 * @param type
	 * @param level
	 */
	public static void setPotion(final ItemStack item, final PotionEffectType type, final int level) {
		setPotion(item, type, 20 * 60 * 10, level);
	}

	/**
	 * 尝试为给定物品添加药水效果。
	 *
	 * @param item
	 * @param type
	 * @param durationTicks
	 * @param level
	 */
	public static void setPotion(final ItemStack item, final PotionEffectType type, final int durationTicks, final int level) {
		if (hasItemMeta)
			PotionSetter.setPotion(item, type, durationTicks, level);
	}

	/**
	 * 解冻附魔注册表
	 *
	 * @deprecated 已在 {@link SimplePlugin} 内部调用
	 */
	@Deprecated
	public static void unfreezeEnchantRegistry() {
		if (MinecraftVersion.atLeast(V.v1_19)) {
			final boolean mojMap = Remain.isUsingMojangMappings();
			final Object enchantmentRegistry = getEnchantRegistry();

			try {
				// works fine in versions (1.19.3 and up)
				ReflectionUtil.setDeclaredField(enchantmentRegistry, mojMap ? "frozen" : "l", false); // MappedRegistry#frozen
				ReflectionUtil.setDeclaredField(enchantmentRegistry, mojMap ? "unregisteredIntrusiveHolders" : "m", new IdentityHashMap<>()); // MappedRegistry#unregisteredIntrusiveHolders

			} catch (final Throwable t) {
				try {
					// in (1.19 - 1.19.2) the obfuscation is different.
					ReflectionUtil.setDeclaredField(enchantmentRegistry, mojMap ? "frozen" : "ca", false); // MappedRegistry#frozen
					// unregisteredIntrusiveHolders does not exist in this version

				} catch (final Throwable tt) {
					// Unable to unfreeze (i.e. 1.20.2, we only support the latest subversion)
				}
			}

		}

		if (MinecraftVersion.olderThan(V.v1_20)) {
			ReflectionUtil.setStaticField(Enchantment.class, "acceptingNew", true);

			clearLegacyEnchantMap();
		}

		enchantRegistryUnfrozen = true;
	}

	/**
	 * 重新冻结附魔注册表
	 *
	 * @deprecated 已在 {@link SimplePlugin} 内部调用
	 */
	@Deprecated
	public static void freezeEnchantRegistry() {
		if (MinecraftVersion.atLeast(V.v1_19)) {
			final Object enchantmentRegistry = getEnchantRegistry();
			final Method freezeMethod = ReflectionUtil.getDeclaredMethod(enchantmentRegistry.getClass(), Remain.isUsingMojangMappings() ? "freeze" : "l");

			ReflectionUtil.invoke(freezeMethod, enchantmentRegistry);
		}

		if (MinecraftVersion.olderThan(V.v1_20)) {
			clearLegacyEnchantMap();

			ReflectionUtil.invokeStatic(Enchantment.class, "stopAcceptingRegistrations");
		}
	}

	private static void clearLegacyEnchantMap() {
		try {
			final Class<?> enchantCommandClass = ReflectionUtil.lookupClass("org.bukkit.command.defaults.EnchantCommand");

			if (enchantCommandClass != null) {
				final List<String> enchants = ReflectionUtil.getStaticFieldContent(enchantCommandClass, "ENCHANTMENT_NAMES");

				enchants.clear();
			}
		} catch (final Throwable t) {
			// prob unsupported at server level anymore
		}
	}

	/*
	 * Helper to get the registry object
	 */
	private static Object getEnchantRegistry() {
		final Class<?> registryClass = ReflectionUtil.lookupClass("net.minecraft.core.registries.BuiltInRegistries");
		final Object enchantmentRegistry = ReflectionUtil.getStaticFieldContent(registryClass, Remain.isUsingMojangMappings() ? "ENCHANTMENT" : MinecraftVersion.equals(V.v1_19) ? "g" : "f");

		return enchantmentRegistry;
	}

	/**
	 * 尝试返回 I18N 本地化的显示名称；失败则返回
	 * 首字母大写的 Material 名称。
	 * <p>
	 * 需要 PaperSpigot。
	 *
	 * @param item 要获取 I18N 名称的 {@link ItemStack}
	 * @return I18N 本地化名称或 Material 名称
	 */
	public static String getI18NDisplayName(final ItemStack item) {
		try {
			return (String) item.getClass().getDeclaredMethod("getI18NDisplayName").invoke(item);

		} catch (final Throwable t) {
			return ItemUtil.bountifyCapitalized(item.getType());
		}
	}

	/**
	 * 返回 spigot 中配置的最大生命值
	 *
	 * @return 最大生命值，未找到则为 2048
	 */
	public static double getMaxHealth() {
		try {
			final String health = String.valueOf(Class.forName("org.spigotmc.SpigotConfig").getField("maxHealth").get(null));

			return health.contains(".") ? Double.parseDouble(health) : Integer.parseInt(health);

		} catch (final Throwable t) {
			return 2048.0;
		}
	}

	/**
	 * 返回统计数据是否不保存
	 *
	 * @return 若禁用了统计数据保存则为 true；否则或未运行
	 * Spigot 时为 false
	 */
	public static boolean isStatSavingDisabled() {
		try {
			return (boolean) Class.forName("org.spigotmc.SpigotConfig").getField("disableStatSaving").get(null);

		} catch (final ReflectiveOperationException ex) {
			try {
				final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File("spigot.yml"));

				return cfg.isSet("stats.disable-saving") ? cfg.getBoolean("stats.disable-saving") : false;
			} catch (final Throwable t) {
				// No Spigot
			}
		}

		return false;
	}

	/**
	 * 将非受检异常转换为受检异常
	 *
	 * @param throwable
	 */
	public static void sneaky(final Throwable throwable) {
		try {
			SneakyThrow.sneaky(throwable);

		} catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
	}

	/**
	 * 设置游戏规则
	 *
	 * @param world    要设置游戏规则的世界
	 * @param gameRule 游戏规则
	 * @param value    要设置的值（true/false）
	 */
	@SuppressWarnings("rawtypes")
	public static void setGameRule(final World world, final String gameRule, final boolean value) {
		try {
			if (MinecraftVersion.newerThan(V.v1_13)) {
				final GameRule rule = GameRule.getByName(gameRule);

				world.setGameRule(rule, value);
			} else
				world.setGameRuleValue(gameRule, "" + value);

		} catch (final Throwable t) {
			Common.error(t, "Game rule " + gameRule + " not found.");
		}
	}

	/**
	 * 返回服务器名称标识（用于 BungeeCord）
	 *
	 * @return
	 */
	public static String getServerName() {
		if (!hasServerName())
			throw new IllegalArgumentException("Please write a 'server-name' key to your server.properties according to https://mineacademy.org/server-properties (do NOT report this, this is NOT a bug)");

		return serverName;
	}

	/**
	 * 设置服务器名称标识（用于 BungeeCord）
	 *
	 * @param serverName
	 */
	public static void setServerName(String serverName) {
		Remain.serverName = serverName;
	}

	/**
	 * 若 server.properties 中的 server-name 属性被修改过则返回 true
	 *
	 * @return
	 */
	public static boolean hasServerName() {
		if (serverName == null)
			loadServerName();

		return serverName != null && !serverName.isEmpty() && !serverName.contains("mineacademy.org/server-properties") && !"undefined".equals(serverName) && !"Unknown Server".equals(serverName);
	}

	/**
	 * 新版 Minecraft 缺少我们在 BungeeCord 中依赖的 server-name，
	 * 将其恢复回来
	 */
	private static void loadServerName() {
		try {
			// Check server.properties for a valid server-name key
			final File serverProperties = new File(SimplePlugin.getData().getParentFile().getParentFile(), "server.properties");
			final List<String> lines = FileUtil.readLines(serverProperties);

			lines.removeIf(line -> line.equals("server-name=undefined") || line.equals("server-name=Unknown Server"));

			String oldName = "";

			for (final String line : lines)
				if (line.startsWith("server-name=")) {
					oldName = line.replace("server-name=", "");

					break;
				}

			serverName = oldName;

		} catch (final Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * 返回对应的 Java 主版本号，例如 Java 1.8 为 8，Java 11 为 11。
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		return SimplePlugin.getJavaVersion(); // The reason we have one in SimplePlugin is to NOT invoke the Remain class when calling
	}

	/**
	 * 返回服务器的每秒刻数（需要 Paper，否则返回 20）
	 *
	 * @return
	 */
	public static int getTPS() {

		try {
			final Method getTPS = Bukkit.class.getDeclaredMethod("getTPS", double[].class);

			return (int) MathUtil.floor(getTPS == null ? 20 : ((double[]) getTPS.invoke(null))[0]);
		} catch (final ReflectiveOperationException ex) {

			// Unsupported
			return 20;
		}
	}

	/**
	 * 尝试将玩家的渲染距离设为给定值；
	 * 若发生反射异常（例如未使用 PaperSpigot
	 * 或 MC 版本过旧）则返回 false。
	 * @param player
	 * @param viewDistanceChunks
	 *
	 * @return
	 */
	public static boolean setViewDistance(Player player, int viewDistanceChunks) {

		try {
			final Method setViewDistance = Player.class.getDeclaredMethod("setViewDistance", int.class);

			ReflectionUtil.invoke(setViewDistance, player, viewDistanceChunks);
			return true;

		} catch (final ReflectiveOperationException ex) {

			// Not using Paper or old MC version
			return false;
		}
	}

	/**
	 * 将可能是 MC 1.18 SectionPathData 的给定对象转换回其根数据，
	 * 例如 {@link MemorySection}
	 *
	 * @param objectOrSectionPathData
	 * @return
	 *
	 * @deprecated 旧代码，将被移除
	 */
	@Deprecated
	public static Object getRootOfSectionPathData(Object objectOrSectionPathData) {
		if (objectOrSectionPathData != null && objectOrSectionPathData.getClass() == sectionPathDataClass)
			objectOrSectionPathData = ReflectionUtil.invoke("getData", objectOrSectionPathData);

		return objectOrSectionPathData;
	}

	/**
	 * 若给定对象是内存节则返回 true
	 *
	 * @param obj
	 * @return
	 */
	public static boolean isMemorySection(Object obj) {
		return obj != null && sectionPathDataClass == obj.getClass();
	}

	/**
	 * 从对话上下文中获取会话数据映射。
	 *
	 * @param context
	 * @return
	 */
	public static Map<Object, Object> getAllSessionData(ConversationContext context) {
		try {
			return context.getAllSessionData();

		} catch (final NoSuchMethodError err) {
			return ReflectionUtil.getFieldContent(context, "sessionData");
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Getters for various server functions
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 此服务器是否支持原生计分板 API？
	 *
	 * @return 服务器是否支持原生计分板 API
	 */
	public static boolean hasNewScoreboardAPI() {
		return newScoreboardAPI;
	}

	/**
	 * 此服务器是否支持粒子？
	 *
	 * @return 服务器是否支持原生粒子 API
	 */
	public static boolean hasParticleAPI() {
		return hasParticleAPI;
	}

	/**
	 * 此服务器是否支持书本事件？
	 *
	 * @return 服务器是否支持书本事件
	 */
	public static boolean hasBookEvent() {
		return hasBookEvent;
	}

	/**
	 * 此服务器是否支持永久计分板标签？
	 *
	 * @return 服务器是否支持永久计分板标签
	 */
	public static boolean hasScoreboardTags() {
		return hasScoreboardTags;
	}

	/**
	 * 返回服务器版本是否支持 SpawnEggMeta
	 *
	 * @return 若支持刷怪蛋元数据则为 true
	 */
	public static boolean hasSpawnEggMeta() {
		return hasSpawnEggMeta;
	}

	/**
	 * 返回服务器版本是否支持 {@link YamlConfiguration#load(java.io.Reader)}，
	 * 否则你只能使用 {@link InputStream}
	 *
	 * @return
	 */
	public static boolean hasYamlReaderLoad() {
		return hasYamlReaderLoad;
	}

	/**
	 * 返回此 MC 是否可能为 1.3.2 或更高版本
	 *
	 * @return
	 */
	public static boolean hasItemMeta() {
		return hasItemMeta;
	}

	/**
	 * 返回 MC 版本是否为支持 HEX RGB 颜色的 1.16+
	 *
	 * @return
	 */
	public static boolean hasHexColors() {
		return MinecraftVersion.atLeast(V.v1_16);
	}

	/**
	 * 返回 Entity 类是否有 addPassenger 方法
	 *
	 * @return
	 */
	public static boolean hasAddPassenger() {
		return hasAddPassenger;
	}

	/**
	 * 若存在复杂的 io.papermc.paper.event.player.AsyncChatEvent 则返回 true
	 * @return
	 */
	public static boolean hasAdventureChatEvent() {
		return hasAdventureChatEvent;
	}

	/**
	 *
	 * 若玩家背包类有额外的背包内容则返回 true
	 *
	 * @return
	 */
	public static boolean hasPlayerExtraInventoryContent() {
		return hasPlayerExtraInventoryContent;
	}

	/**
	 * 若 Player 类有打开告示牌的方法则返回 true
	 *
	 * @return
	 */
	public static boolean hasPlayerOpenSignMethod() {
		return hasPlayerOpenSignMethod;
	}

	/**
	 * 若这是 Folia 服务器则返回 true
	 *
	 * @return
	 */
	public static boolean isFolia() {
		return isFolia;
	}

	/**
	 * 返回服务器是否运行 Paper（原 PaperSpigot）软件。
	 * <p>
	 * Paper 是 Spigot 的一个分支，兼容大多数 Bukkit 插件。
	 * <p>
	 * 我们使用 getTPS 方法判断是否安装了 Paper。
	 *
	 * @return 若服务器运行 Paper(Spigot) 则为 true
	 */
	public static boolean isPaper() {
		return isPaper;
	}

	/**
	 * 若使用的是 Mojang 映射则返回 true（注意 Foundation 的 NMS
	 * 不支持它们，但会安全地失败）
	 *
	 * @return
	 */
	public static boolean isUsingMojangMappings() {
		return isUsingMojangMappings;
	}

	// ------------------------ Legacy ------------------------

	// return the legacy online player array
	private static Player[] getPlayersLegacy() {
		try {
			return (Player[]) getPlayersMethod.invoke(null);
		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Reflection malfunction");
		}
	}

	// return the legacy get health int method
	private static int getHealhLegacy(final LivingEntity entity) {
		try {
			return (int) getHealthMethod.invoke(entity);
		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Reflection malfunction");
		}
	}

	// return the legacy get health int method
	private static int getMaxHealhLegacy(final LivingEntity entity) {
		try {
			final Object number = LivingEntity.class.getMethod("getMaxHealth").invoke(entity);

			if (number instanceof Double)
				return ((Double) number).intValue();
			if (number instanceof Integer)
				return (Integer) number;

			return (int) Double.parseDouble(number.toString());

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Reflection malfunction");
		}
	}

	// ------------------------ Utility ------------------------

	/**
	 * 当消息包含悬停或点击事件（否则会被移除）时
	 * 抛出。
	 * <p>
	 * 不会检查此类消息。
	 */
	public static class InteractiveTextFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InteractiveTextFoundException() {
		}
	}
}

/**
 * Spigot 的包装器
 */
class SneakyThrow {

	public static void sneaky(final Throwable t) {
		throw SneakyThrow.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
		throw (T) t;
	}
}

/**
 * bungee 聊天组件库的包装器
 */
class BungeeChatProvider {

	/**
	 * 向玩家发送 JSON 组件消息
	 *
	 * @param sender
	 * @param components
	 */
	static void sendComponent(final CommandSender sender, final Object components) {
		if (components instanceof TextComponent)
			sendComponent0(sender, (TextComponent) components);

		else
			sendComponent0(sender, (BaseComponent[]) components);
	}

	private static void sendComponent0(final CommandSender sender, final BaseComponent... components) {

		if (!(sender instanceof Player)) {
			sendAsPlain(sender, components);

			return;
		}

		try {
			((Player) sender).spigot().sendMessage(components);

		} catch (final Throwable ex) {

			if (MinecraftVersion.olderThan(V.v1_7))
				sendAsPlain(sender, components);

			// This is the minimum MC version that supports interactive chat
			else if (MinecraftVersion.equals(V.v1_7)) {
				final Class<?> chatBaseComponentClass = getNMSClass("IChatBaseComponent", "N/A");
				final Class<?> packetClass = getNMSClass("PacketPlayOutChat", "N/A");

				final Object chatBaseComponent = Remain.toIChatBaseComponent(components);
				final Object packet = ReflectionUtil.instantiate(ReflectionUtil.getConstructor(packetClass, chatBaseComponentClass), chatBaseComponent);

				Remain.sendPacket((Player) sender, packet);

			} else {

				// Ignore Cauldron
				if (!Bukkit.getName().contains("Cauldron"))
					Common.throwError(ex, "Failed to send component: " + TextComponent.toLegacyText(components) + " to " + sender.getName());

				sendAsPlain(sender, components);
			}
		}
	}

	private static void sendAsPlain(final CommandSender sender, final BaseComponent... components) {
		final StringBuilder plain = new StringBuilder();

		for (final BaseComponent component : components)
			plain.append(component.toLegacyText().replaceAll(ChatColor.COLOR_CHAR + "x", ""));

		final String message = plain.toString();

		if (!message.isEmpty() && !"none".equals(message)) {
			final String stripped = message.startsWith("[JSON]") ? message.replaceFirst("\\[JSON\\]", "").trim() : message;

			for (final String part : stripped.split("\n"))
				sender.sendMessage(part);
		}
	}
}

/**
 * 进度的包装器
 */
class AdvancementAccessor {

	private final NamespacedKey key;
	private final String icon;
	private final String message;
	private final CompToastStyle toastStyle;

	AdvancementAccessor(final String message, final String icon, CompToastStyle toastStyle) {
		this.key = new NamespacedKey(SimplePlugin.getInstance(), UUID.randomUUID().toString());
		this.message = message;
		this.icon = icon;
		this.toastStyle = toastStyle;
	}

	public void show(final Player player) {
		this.loadAdvancement();
		this.grantAdvancement(player);

		Common.runLater(10, () -> {
			this.revokeAdvancement(player);
			this.removeAdvancement();
		});
	}

	private void loadAdvancement() {
		Bukkit.getUnsafe().loadAdvancement(this.key, this.compileJson0());
	}

	private String compileJson0() {
		final JsonObject json = new JsonObject();

		final JsonObject icon = new JsonObject();
		if (MinecraftVersion.atLeast(V.v1_20)) {
			icon.addProperty("id", this.icon);
		} else
			icon.addProperty("item", this.icon);

		final JsonObject display = new JsonObject();
		display.add("icon", icon);
		display.addProperty("title", this.message);
		display.addProperty("description", "");
		display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/adventure.png");
		display.addProperty("frame", this.toastStyle.getKey());
		display.addProperty("announce_to_chat", false);
		display.addProperty("show_toast", true);
		display.addProperty("hidden", true);

		final JsonObject criteria = new JsonObject();

		final JsonObject trigger = new JsonObject();
		trigger.addProperty("trigger", "minecraft:impossible");

		criteria.add("impossible", trigger);

		json.add("criteria", criteria);
		json.add("display", display);

		return new Gson().toJson(json);
	}

	private void grantAdvancement(final Player plazer) {
		final Advancement adv = this.getAdvancement();
		final AdvancementProgress progress = plazer.getAdvancementProgress(adv);

		if (!progress.isDone())
			progress.getRemainingCriteria().forEach(crit -> progress.awardCriteria(crit));
	}

	private void revokeAdvancement(final Player plazer) {
		final Advancement adv = this.getAdvancement();
		final AdvancementProgress prog = plazer.getAdvancementProgress(adv);

		if (prog.isDone())
			prog.getAwardedCriteria().forEach(crit -> prog.revokeCriteria(crit));
	}

	private void removeAdvancement() {
		Bukkit.getUnsafe().removeAdvancement(this.key);
	}

	private Advancement getAdvancement() {
		return Bukkit.getAdvancement(this.key);
	}
}

class PotionSetter {

	/**
	 * 尝试为给定物品添加某种药水效果
	 *
	 * @param item
	 * @param type
	 * @param durationTicks
	 * @param level
	 */
	public static void setPotion(final ItemStack item, final PotionEffectType type, final int durationTicks, final int level) {
		Valid.checkBoolean(item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta, "Can only use setPotion for items with PotionMeta not: " + item.getItemMeta());

		final org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
		final PotionType wrapped = PotionType.getByEffect(type);

		if (wrapped != null && MinecraftVersion.olderThan(V.v1_20))
			try {
				meta.setBasePotionType(wrapped);

			} catch (final NoSuchMethodError ex) {
			}

		if (level > 0 && wrapped == null) {
			Class<?> potionDataClass = null;

			try {
				potionDataClass = ReflectionUtil.lookupClass("org.bukkit.potion.PotionData");
			} catch (final Exception e) {
			}

			if (potionDataClass != null) {
				final Constructor<?> potionConst = ReflectionUtil.getConstructor(potionDataClass, PotionType.class, boolean.class, boolean.class);
				final Object potionData = ReflectionUtil.instantiate(potionConst, level > 0 && wrapped != null ? wrapped : PotionType.WATER, false, false);
				final Method setBasePotionData = ReflectionUtil.getMethod(meta.getClass(), "setBasePotionData", potionDataClass);

				ReflectionUtil.invoke(setBasePotionData, meta, potionData);
			}
		}

		// For some reason this does not get added so we have to add it manually on top of the lore
		if (MinecraftVersion.olderThan(V.v1_9)) {
			if (item.getData().getData() == 0) {
				final List<String> lore = new ArrayList<>();
				final String potionLine = Common.colorize("&7" + ItemUtil.bountifyCapitalized(type) + " (" + TimeUtil.formatTimeColon(durationTicks / 20) + ")");

				lore.add(potionLine);

				if (meta.getLore() != null)
					for (final String otherLore : meta.getLore())
						if (!otherLore.contains(potionLine))
							lore.add(otherLore);

				item.getData().setData((byte) 45);

				meta.setDisplayName(Common.colorize("&rPotion Of " + ItemUtil.bountifyCapitalized(type)));
				meta.setLore(lore);
			}
		}

		//meta.setMainEffect(type);
		meta.addCustomEffect(new PotionEffect(type, durationTicks, level - 1), true);

		item.setItemMeta(meta);
	}
}