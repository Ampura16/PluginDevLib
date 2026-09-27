package top.brmc.devlib;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.Statistic.Type;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.jsonsimple.JSONObject;
import top.brmc.devlib.jsonsimple.JSONParser;
import top.brmc.devlib.menu.Menu;
import top.brmc.devlib.model.HookManager;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.CompAttribute;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.remain.CompProperty;
import top.brmc.devlib.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 管理玩家的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerUtil {

	/**
	 * 玩家物品栏大小，即 3 行 + 快捷栏 1 行，也就是 9 * 4 = 36。
	 */
	public static final int PLAYER_INV_SIZE = 36;

	/**
	 * 存储方块朝向，供后续转换使用
	 */
	private static final BlockFace[] FACE_AXIS = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
	private static final BlockFace[] FACE_RADIAL = { BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST };

	/**
	 * 存储当前待处理的标题动画任务列表，用于将标题恢复为原始标题
	 */
	private static final Map<UUID, BukkitTask> titleRestoreTasks = new ConcurrentHashMap<>();

	/**
	 * 存储临时保存的玩家物品栏、生命值、属性及其他状态
	 */
	private static final Map<UUID, SerializedMap> storedPlayerStates = new HashMap<>();

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 在主线程用带颜色的消息踢出玩家
	 *
	 * @param player
	 * @param message
	 */
	public static void kick(final Player player, final String... message) {
		if (Bukkit.isPrimaryThread())
			player.kickPlayer(Common.colorize(message));

		else
			Common.runLater(() -> player.kickPlayer(Common.colorize(message)));
	}

	/**
	 * 返回玩家的连接延迟（ping），单位毫秒
	 *
	 * @param player
	 * @return
	 */
	public static int getPing(final Player player) {
		return Remain.getPing(player);
	}

	/**
	 * 将玩家注视方向转换为方块朝向
	 * 来源：https://bukkit.org/threads/400099/
	 *
	 * @param player
	 * @return
	 */
	public static BlockFace getFacing(final Player player) {
		return getFacing(player.getLocation().getYaw(), false);
	}

	/**
	 * 将玩家注视方向转换为方块朝向
	 * 来源：https://bukkit.org/threads/400099/
	 *
	 * @param player
	 * @param useSubDirections
	 * @return
	 */
	public static BlockFace getFacing(final Player player, final boolean useSubDirections) {
		return getFacing(player.getLocation().getYaw(), useSubDirections);
	}

	/**
	 * 将给定 yaw 转换为方块朝向
	 * 来源：https://bukkit.org/threads/400099/
	 *
	 * @param yaw
	 * @param useSubDirections
	 * @return
	 */
	public static BlockFace getFacing(final float yaw, final boolean useSubDirections) {
		if (useSubDirections)
			return FACE_RADIAL[Math.round(yaw / 45F) & 0x7].getOppositeFace();

		return FACE_AXIS[Math.round(yaw / 90F) & 0x3].getOppositeFace();
	}

	/**
	 * 从 BlockFace 返回 yaw
	 *
	 * @param face
	 * @param useSubDirections
	 * @return
	 */
	public static int getFacing(final BlockFace face, final boolean useSubDirections) {
		return (useSubDirections ? face.ordinal() * 45 : face.ordinal() * 90) - 180;
	}

	/**
	 * 将给定 yaw 转换为最接近的有效方块朝向，再转回 yaw
	 *
	 * 用于将实体对齐朝向 4 个或 8 个方向之一，而无需你
	 * 站得笔直。
	 *
	 * @param yaw
	 * @param useSubDirections
	 * @return
	 */
	public static float alignYaw(final float yaw, final boolean useSubDirections) {
		final BlockFace face = getFacing(yaw, useSubDirections);

		return getFacing(face, useSubDirections);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Statistics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回玩家在服务器上花费的总时长。
	 * 若删除主世界文件夹中的 playerdata 文件夹，该值会重置。
	 *
	 * **Minecraft 1.12 及更早版本返回 tick 值，否则返回
	 * 分钟数！**
	 *
	 * @param player
	 * @return
	 */
	public static long getPlayTimeTicksOrSeconds(final OfflinePlayer player) {
		final Statistic playTime = Remain.getPlayTimeStatisticName();

		return getStatistic(player, playTime);
	}

	/**
	 * 返回所有玩过的离线玩家的统计数据
	 *
	 * @param statistic
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic) {
		return getStatistics(statistic, null, null);
	}

	/**
	 * 返回所有玩过的离线玩家的统计数据
	 *
	 * @param statistic
	 * @param material
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic, final Material material) {
		return getStatistics(statistic, material, null);
	}

	/**
	 * 返回所有玩过的离线玩家的统计数据
	 *
	 * @param statistic
	 * @param entityType
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic, final EntityType entityType) {
		return getStatistics(statistic, null, entityType);
	}

	/**
	 * 返回所有玩过的离线玩家的统计数据
	 *
	 * @param statistic
	 * @param material
	 * @param entityType
	 * @return
	 */
	public static TreeMap<Long, OfflinePlayer> getStatistics(final Statistic statistic, final Material material, final EntityType entityType) {
		final TreeMap<Long, OfflinePlayer> statistics = new TreeMap<>(Collections.reverseOrder());

		for (final OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
			final long time = getStatistic(offline, statistic, material, entityType);

			statistics.put(time, offline);
		}

		return statistics;
	}

	/**
	 * 返回在线玩家的某项统计数据
	 *
	 * @param player
	 * @param statistic
	 * @return
	 */
	public static long getStatistic(final OfflinePlayer player, final Statistic statistic) {
		return getStatistic(player, statistic, null, null);
	}

	/**
	 * 返回在线玩家的某项统计数据
	 *
	 * @param player
	 * @param statistic
	 * @param material
	 * @return
	 */
	public static long getStatistic(final OfflinePlayer player, final Statistic statistic, final Material material) {
		return getStatistic(player, statistic, material, null);
	}

	/**
	 * 返回在线玩家的某项统计数据
	 *
	 * @param player
	 * @param statistic
	 * @param entityType
	 * @return
	 */
	public static long getStatistic(final OfflinePlayer player, final Statistic statistic, final EntityType entityType) {
		return getStatistic(player, statistic, null, entityType);
	}

	/**
	 * 返回在线玩家的某项统计数据
	 *
	 * @param player
	 * @param statistic
	 * @return
	 */
	private static long getStatistic(final OfflinePlayer player, final Statistic statistic, final Material material, final EntityType entityType) {
		// Return live statistic for up to date data and best performance if possible
		if (player.isOnline()) {
			final Player online = player.getPlayer();

			if (statistic.getType() == Type.UNTYPED)
				return online.getStatistic(statistic);

			else if (statistic.getType() == Type.ENTITY)
				return online.getStatistic(statistic, entityType);

			return online.getStatistic(statistic, material);
		}

		// Otherwise read his stats file
		return getStatisticFile(player, statistic, material, entityType);
	}

	// Read json file for the statistic
	private static long getStatisticFile(final OfflinePlayer player, final Statistic statistic, final Material material, final EntityType entityType) {
		final File worldFolder = new File(Bukkit.getServer().getWorlds().get(0).getWorldFolder(), "stats");
		final File statFile = new File(worldFolder, player.getUniqueId().toString() + ".json");

		if (statFile.exists())
			try {
				final JSONObject json = (JSONObject) JSONParser.deserialize(new FileReader(statFile));
				final String name = Remain.getNMSStatisticName(statistic, material, entityType);

				JSONObject section = json.getObject("stats");
				long result = 0;

				for (String part : name.split("\\:")) {
					part = part.replace(".", ":");

					if (section != null) {
						final JSONObject nextSection = section.getObject(part);

						if (nextSection == null) {
							result = Long.parseLong(section.containsKey(part) ? section.get(part).toString() : "0");
							break;
						}

						section = nextSection;
					}
				}

				return result;

			} catch (final Throwable t) {
				throw new FoException(t);
			}

		return 0;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Permissions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回给定发送者是否拥有某权限
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public static boolean hasPerm(final Permissible sender, final String permission) {
		Valid.checkNotNull(sender, "Cannot call PlayerUtil#hasPerm() for null sender!");

		if (permission == null) {
			Common.log("THIS IS NOT AN ACTUAL ERROR, YOUR PLUGIN WILL WORK FINE");
			Common.log("Internal check got null permission as input, this is no longer allowed.");
			Common.log("We'll return true to prevent errors. Contact developers of " + SimplePlugin.getNamed());
			Common.log("to get it solved and include the fake error below:");

			new Throwable().printStackTrace();

			return true;
		}

		Valid.checkBoolean(!permission.contains("{plugin_name}") && !permission.contains("{plugin_name_lower}"),
				"Found {plugin_name} variable calling hasPerm(" + sender + ", " + permission + ")." + "This is now disallowed, contact plugin authors to put " + SimplePlugin.getNamed().toLowerCase() + " in their permission.");

		return sender.hasPermission(permission);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Inventory
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 将玩家几乎所有可拥有的旗标，例如
	 * 飞行等，恢复正常
	 * <p>
	 * 还会将游戏模式设为生存
	 * <p>
	 * 典型用法：小游戏插件 - 让玩家加入竞技场前调用
	 * <p>
	 * 甚至会禁用 Essentials 上帝模式并移除隐身（支持大多数隐身插件）。
	 *
	 * @param player
	 * @param cleanInventory
	 */
	public static void normalize(final Player player, final boolean cleanInventory) {
		normalize(player, cleanInventory, true);
	}

	/**
	 * 将玩家几乎所有可拥有的旗标，例如
	 * 飞行等，恢复正常
	 * <p>
	 * 还会将游戏模式设为生存
	 * <p>
	 * 典型用法：小游戏插件 - 让玩家加入竞技场前调用
	 * <p>
	 * 甚至会禁用 Essentials 上帝模式。
	 *
	 * @param player
	 * @param cleanInventory
	 * @param removeVanish   是否移除玩家的隐身？支持大多数隐身插件
	 */
	public static void normalize(final Player player, final boolean cleanInventory, final boolean removeVanish) {
		HookManager.setGodMode(player, false);

		player.setGameMode(GameMode.SURVIVAL);

		if (cleanInventory) {
			cleanInventoryAndFood(player);

			try {
				CompAttribute.MAX_HEALTH.set(player, 20);
				CompAttribute.ATTACK_SPEED.set(player, 4.0);

			} catch (final Throwable t) {
				try {
					player.setMaxHealth(20);

				} catch (final Throwable tt) {

					try {
						player.resetMaxHealth();
					} catch (final Throwable ttt) {
						// Minecraft 1.2.5 lol
					}
				}
			}

			try {
				player.setHealth(20);

			} catch (final Throwable t) {
				// Try attribute way

				try {
					final double maxHealthAttr = CompAttribute.MAX_HEALTH.get(player);

					player.setHealth(maxHealthAttr);

				} catch (final Throwable tt) {
					// silence if a third party plugin is controlling health
				}
			}

			player.setHealthScaled(false);

			for (final PotionEffect potion : player.getActivePotionEffects())
				player.removePotionEffect(potion.getType());
		}

		player.setTotalExperience(0);
		player.setLevel(0);
		player.setExp(0F);

		player.resetPlayerTime();
		player.resetPlayerWeather();

		player.setFallDistance(0);

		CompProperty.INVULNERABLE.apply(player, false);
		CompProperty.GLOWING.apply(player, false);
		CompProperty.SILENT.apply(player, false);

		player.setAllowFlight(false);
		player.setFlying(false);

		player.setFlySpeed(0.1F);
		player.setWalkSpeed(0.2F);

		player.setCanPickupItems(true);

		player.setVelocity(new Vector(0, 0, 0));
		player.eject();

		EntityUtil.removeVehiclesAndPassengers(player);

		if (removeVanish)
			try {
				if (player.hasMetadata("vanished")) {
					final Plugin plugin = player.getMetadata("vanished").get(0).getOwningPlugin();

					player.removeMetadata("vanished", plugin);
				}

				for (final Player other : Remain.getOnlinePlayers())
					if (!other.getName().equals(player.getName()) && !other.canSee(player))
						other.showPlayer(player);

			} catch (final NoSuchMethodError err) {
				/* old MC */

			} catch (final Exception ex) {
				ex.printStackTrace();
			}
	}

	/*
	 * Cleans players inventory and restores food levels
	 */
	private static void cleanInventoryAndFood(final Player player) {
		player.getInventory().setArmorContents(null);
		player.getInventory().setContents(new ItemStack[player.getInventory().getContents().length]);

		try {
			player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);
		} catch (final NoSuchMethodError err) {
			/* old MC */
		}

		player.setFireTicks(0);
		player.setFoodLevel(20);
		player.setExhaustion(0);
		player.setSaturation(10);

		player.setVelocity(new Vector(0, 0, 0));
	}

	/**
	 * 若玩家普通物品栏和盔甲栏都为空则返回 true
	 *
	 * @param player
	 * @return
	 */
	public static boolean hasEmptyInventory(final Player player) {
		final ItemStack[] inv = player.getInventory().getContents();
		final ItemStack[] armor = player.getInventory().getArmorContents();

		final Object[] everything = Common.joinArrays(inv, armor);

		for (final Object i : everything)
			if (i instanceof ItemStack)
				if (((ItemStack) i).getType() != Material.AIR)
					return false;

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Player states
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 将玩家快照设为存储在本地缓存中
	 *
	 * @param player
	 */
	public static void storeState(final Player player) {
		Valid.checkBoolean(!hasStoredState(player), "Player " + player.getName() + " already has a stored state!");

		final SerializedMap data = SerializedMap.ofArray(
				"gameMode", player.getGameMode(),
				"content", player.getInventory().getContents(),
				"armorContent", player.getInventory().getArmorContents(),
				"maxHealth", Remain.getMaxHealth(player),
				"health", Remain.getHealth(player),
				"healthScaled", player.isHealthScaled(),
				"remainingAir", player.getRemainingAir(),
				"maximumAir", player.getMaximumAir(),
				"fallDistance", player.getFallDistance(),
				"fireTicks", player.getFireTicks(),
				"totalExp", player.getTotalExperience(),
				"level", player.getLevel(),
				"exp", player.getExp(),
				"foodLevel", player.getFoodLevel(),
				"exhaustion", player.getExhaustion(),
				"saturation", player.getSaturation(),
				"flySpeed", player.getFlySpeed(),
				"walkSpeed", player.getWalkSpeed(),
				"potionEffects", player.getActivePotionEffects());

		// Attributes
		final Map<CompAttribute, Double> attributes = new HashMap<>();

		for (final CompAttribute attribute : CompAttribute.values()) {
			final Double value = attribute.get(player);

			if (value != null)
				attributes.put(attribute, value);
		}

		data.put("attributes", attributes);

		// From now on we have to surround each method with try-catch since
		// those are not available in older MC versions

		try {
			data.put("extraContent", player.getInventory().getExtraContents());
		} catch (final Throwable t) {
		}

		try {
			data.put("invulnerable", player.isInvulnerable());
		} catch (final Throwable t) {
		}

		try {
			data.put("silent", player.isSilent());
		} catch (final Throwable t) {
		}

		try {
			data.put("glowing", player.isGlowing());
		} catch (final Throwable t) {
		}

		storedPlayerStates.put(player.getUniqueId(), data);
	}

	/**
	 * 恢复玩家物品栏和属性
	 *
	 * @param player
	 */
	public static void restoreState(final Player player) {
		final SerializedMap data = storedPlayerStates.remove(player.getUniqueId());
		Valid.checkNotNull(data, "Player " + player.getName() + " does not have a stored game state!");

		player.setGameMode(data.get("gameMode", GameMode.class));
		player.getInventory().setContents((ItemStack[]) data.getObject("content"));
		player.getInventory().setArmorContents((ItemStack[]) data.getObject("armorContent"));
		player.setMaxHealth(data.getInteger("maxHealth"));
		player.setHealth(data.getInteger("health"));
		player.setHealthScaled(data.getBoolean("healthScaled"));
		player.setRemainingAir(data.getInteger("remainingAir"));
		player.setMaximumAir(data.getInteger("maximumAir"));
		player.setFallDistance(data.getFloat("fallDistance"));
		player.setFireTicks(data.getInteger("fireTicks"));
		player.setTotalExperience(data.getInteger("totalExp"));
		player.setLevel(data.getInteger("level"));
		player.setExp(data.getFloat("exp"));
		player.setFoodLevel(data.getInteger("foodLevel"));
		player.setExhaustion(data.getFloat("exhaustion"));
		player.setSaturation(data.getFloat("saturation"));
		player.setFlySpeed(data.getFloat("flySpeed"));
		player.setWalkSpeed(data.getFloat("walkSpeed"));

		// Remove old potion effects
		for (final PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());

		// And add news
		for (final PotionEffect effect : data.getList("potionEffects", PotionEffect.class))
			player.addPotionEffect(effect);

		// Attributes
		final Map<CompAttribute, Double> attributes = (Map<CompAttribute, Double>) data.getObject("attributes");

		for (final Entry<CompAttribute, Double> entry : attributes.entrySet())
			entry.getKey().set(player, entry.getValue());

		// From now on we have to surround each method with try-catch since
		// those are not available in older MC versions

		try {
			player.getInventory().setExtraContents((ItemStack[]) data.getObject("extraContent"));
		} catch (final Throwable t) {
		}

		try {
			player.setInvulnerable(data.getBoolean("invulnerable"));
		} catch (final Throwable t) {
		}

		try {
			player.setSilent(data.getBoolean("silent"));
		} catch (final Throwable t) {
		}

		try {
			player.setGlowing(data.getBoolean("glowing"));
		} catch (final Throwable t) {
		}
	}

	/**
	 * 若玩家有已存储的物品栏和属性快照则返回 true
	 * @param player
	 *
	 * @return
	 */
	public static boolean hasStoredState(final Player player) {
		return storedPlayerStates.containsKey(player.getUniqueId());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Vanish
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回玩家是否隐身，见 {@link #isVanished(Player)}，或另一玩家是否能看见他
	 *
	 * @param player
	 * @param otherPlayer
	 * @return
	 */
	public static boolean isVanished(final Player player, final Player otherPlayer) {
		if (otherPlayer != null && !otherPlayer.canSee(player))
			return true;

		return isVanished(player);
	}

	/**
	 * 若玩家隐身则返回 true。我们检查大多数插件（CMI、Essentials 等）支持的 "vanished"
	 * 元数据值
	 *
	 * 隐身药水或旁观者模式不会返回 true。
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(final Player player) {
		final List<MetadataValue> list = player.getMetadata("vanished");

		for (final MetadataValue meta : list)
			if (meta.asBoolean())
				return true;

		return false;
	}

	/**
	 * 使用元数据、Essentials、CMI 和 NMS 隐形更新玩家的隐身状态。
	 *
	 * @param player
	 * @param vanished
	 */
	public static void setVanished(final Player player, final boolean vanished) {

		// Hook into other plugins
		HookManager.setVanished(player, vanished);

		// Clear any previous metadata
		for (final Iterator<MetadataValue> it = player.getMetadata("vanished").iterator(); it.hasNext();) {
			final MetadataValue meta = it.next();

			if (meta.asBoolean())
				meta.invalidate();
		}

		// Re-add metadata if vanished
		if (vanished)
			player.setMetadata("vanished", new FixedMetadataValue(SimplePlugin.getInstance(), true));

		// NMS
		Remain.setInvisible(player, vanished);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Nicks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回匹配给定昵称且未隐身的玩家
	 *
	 * @param name
	 * @return
	 */
	public static Player getPlayerByNickNoVanish(final String name) {
		return getPlayerByNick(name, false);
	}

	/**
	 * 返回给定名称或昵称对应的玩家
	 *
	 * @param name
	 * @param ignoreVanished
	 * @return
	 */
	public static Player getPlayerByNick(final String name, final boolean ignoreVanished) {
		final Player found = lookupNickedPlayer0(name);

		if (ignoreVanished && found != null && PlayerUtil.isVanished(found))
			return null;

		return found;
	}

	private static Player lookupNickedPlayer0(final String name) {
		Player found = null;
		int delta = Integer.MAX_VALUE;

		for (final Player player : Remain.getOnlinePlayers()) {

			if (player.getName().equalsIgnoreCase(name))
				return player;

			final String nick = HookManager.getNickColorless(player);

			if (nick.toLowerCase().startsWith(name.toLowerCase())) {
				final int curDelta = Math.abs(nick.length() - name.length());

				if (curDelta < delta) {
					found = player;
					delta = curDelta;
				}

				if (curDelta == 0)
					break;
			}
		}

		return found;
	}

	/**
	 * 执行异步玩家查找，然后在同步任务中运行操作
	 *
	 * @param name
	 * @param syncCallback
	 */
	public static void lookupOfflinePlayerAsync(final String name, final Consumer<OfflinePlayer> syncCallback) {
		Common.runAsync(() -> {
			// If the given name is a nick, try to get the real name
			final String parsedName = HookManager.getNameFromNick(name);
			final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(parsedName);

			Common.runLater(() -> syncCallback.accept(offlinePlayer));
		});
	}

	// ----------------------------------------------------------------------------------------------------
	// Animation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 向玩家发送动画标题。颜色会被替换。
	 *
	 * @param menu           该菜单
	 * @param player         该玩家
	 * @param temporaryTitle 动画标题
	 * @param oldTitle       要恢复的旧标题
	 * @param duration       持续时间（tick）
	 */
	public static void updateInventoryTitle(final Menu menu, final Player player, final String temporaryTitle, final String oldTitle, final int duration) {
		Valid.checkNotNull(menu, "Menu == null");
		Valid.checkNotNull(player, "Player == null");
		Valid.checkNotNull(temporaryTitle, "Title == null");
		Valid.checkNotNull(oldTitle, "Old Title == null");

		// Send the packet
		updateInventoryTitle(player, MinecraftVersion.atLeast(V.v1_13) ? temporaryTitle.replace("%", "%%") : temporaryTitle);

		// Prevent flashing titles
		BukkitTask pending = titleRestoreTasks.get(player.getUniqueId());

		if (pending != null)
			pending.cancel();

		pending = Common.runLater(duration, () -> {
			final Menu futureMenu = Menu.getMenu(player);

			if (futureMenu != null && futureMenu.getClass().getName().equals(menu.getClass().getName()))
				updateInventoryTitle(player, oldTitle);
		});

		final UUID uid = player.getUniqueId();

		titleRestoreTasks.put(uid, pending);

		// Prevent overloading the map so remove the key afterwards
		Common.runLater(duration + 1, () -> {
			if (titleRestoreTasks.containsKey(uid))
				titleRestoreTasks.remove(uid);
		});
	}

	/**
	 * 更新玩家物品栏标题而不关闭窗口
	 *
	 * @param player 该玩家
	 * @param title  新标题
	 */
	public static void updateInventoryTitle(final Player player, final String title) {
		Remain.updateInventoryTitle(player, title);
	}

	// ----------------------------------------------------------------------------------------------------
	// Inventory manipulation
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 尝试检索与给定物品相似（见 {@link ItemUtil#isSimilar(ItemStack, ItemStack)}）的
	 * 第一个物品。
	 *
	 * @param player
	 * @param item   找到的物品，若没有则为 null
	 * @return
	 */
	public static ItemStack getFirstItem(final Player player, final ItemStack item) {
		for (final ItemStack otherItem : player.getInventory().getContents())
			if (otherItem != null && ItemUtil.isSimilar(otherItem, item))
				return otherItem;

		return null;
	}

	/**
	 * 拿走给定数量的给定材质，若玩家身上足够拿则返回 true
	 * （否则不做任何操作）
	 *
	 * @param player
	 * @param material
	 * @param amount
	 * @return
	 */
	public static boolean take(final Player player, final CompMaterial material, int amount) {
		if (!containsAtLeast(player, amount, material))
			return false;

		final Inventory inventory = player.getInventory();
		final ItemStack[] content = inventory.getContents();

		for (int slot = 0; slot < content.length; slot++) {
			final ItemStack item = content[slot];

			if (item != null && material.is(item)) {
				final int itemAmount = item.getAmount();
				final int newAmount = itemAmount - amount;

				if (newAmount < 0) {
					amount = amount - itemAmount;

					content[slot] = null;
				}

				else {
					item.setAmount(newAmount);

					content[slot] = item;
					break;
				}
			}
		}

		inventory.setContents(content);

		return true;
	}

	/**
	 * 扫描物品栏，移除找到的第一个匹配给定材质的物品中的
	 * 一件
	 *
	 * @param player
	 * @param material
	 * @return
	 */
	public static boolean takeFirstOnePiece(final Player player, final CompMaterial material) {

		for (final ItemStack item : player.getInventory().getContents())
			if (item != null && material.is(item)) {
				takeOnePiece(player, item);

				return true;
			}

		return false;
	}

	/**
	 * 移除给定物品堆中的一件，
	 * 若物品只有 1 个则将槽位设为空气
	 * <p>
	 * 这会将给定物品堆的数量设为当前数量 -1，
	 * 且不会自动移除物品
	 *
	 * @param player
	 * @param item
	 */
	public static void takeOnePiece(final Player player, final ItemStack item) {
		Remain.takeItemOnePiece(player, item);
	}

	/**
	 * 返回玩家是否拥有足够数量的给定材质
	 *
	 * @param player
	 * @param atLeastSize
	 * @param material
	 * @return
	 */
	public static boolean containsAtLeast(final Player player, final int atLeastSize, final CompMaterial material) {
		int foundSize = 0;

		for (final ItemStack item : player.getInventory().getContents())
			if (item != null && item.getType() == material.getMaterial())
				foundSize += item.getAmount();

		return foundSize >= atLeastSize;
	}

	/**
	 * 尝试搜索并将第一个相似物品堆替换为新的
	 *
	 * @param inv
	 * @param search
	 * @param replaceWith
	 * @return 若替换成功则返回 true
	 */
	public static boolean updateInvSlot(final Inventory inv, final ItemStack search, final ItemStack replaceWith) {
		Valid.checkNotNull(inv, "Inv = null");

		for (int i = 0; i < inv.getSize(); i++) {
			final ItemStack slot = inv.getItem(i);

			if (slot != null && ItemUtil.isSimilar(slot, search)) {
				inv.setItem(i, replaceWith);

				return true;
			}
		}

		return false;
	}

	/**
	 * 尝试向玩家物品栏添加物品，
	 * 若全部添加成功则返回 true。若玩家
	 * 物品栏已满，我们会在附近掉落物品并返回 false。
	 *
	 * @param player
	 * @param items
	 * @return 若物品栏已满、部分物品掉在地上则返回 false，比如话筒
	 */
	public static boolean addItemsOrDrop(final Player player, final ItemStack... items) {
		final Map<Integer, ItemStack> leftovers = addItems(player.getInventory(), items);

		final World world = player.getWorld();
		final Location location = player.getLocation();

		for (final ItemStack leftover : leftovers.values()) {
			final Item item = world.dropItem(location, leftover);

			item.setPickupDelay(2 * 20);
		}

		return leftovers.isEmpty();
	}

	/**
	 * 尝试向物品栏添加物品，
	 * 返回未能存下的部分
	 *
	 * @param inventory
	 * @param items
	 * @return
	 */
	public static Map<Integer, ItemStack> addItems(final Inventory inventory, final Collection<ItemStack> items) {
		return addItems(inventory, items.toArray(new ItemStack[items.size()]));
	}

	/**
	 * 尝试向物品栏添加物品，
	 * 返回未能存下的部分
	 *
	 * @param inventory
	 * @param items
	 * @return
	 */
	public static Map<Integer, ItemStack> addItems(final Inventory inventory, final ItemStack... items) {
		return addItems(inventory, 0, items);
	}

	/**
	 * 尝试向物品栏添加物品，
	 * 返回未能存下的部分
	 * <p>
	 * 将 oversizedStack 设为低于正常堆叠大小以禁用超大堆叠
	 *
	 * @param inventory
	 * @param oversizedStacks
	 * @param items
	 * @return
	 */
	private static Map<Integer, ItemStack> addItems(final Inventory inventory, final int oversizedStacks, final ItemStack... items) {
		if (isCombinedInv(inventory)) {
			final Inventory fakeInventory = makeTruncatedInv((PlayerInventory) inventory);
			final Map<Integer, ItemStack> overflow = addItems(fakeInventory, oversizedStacks, items);
			for (int i = 0; i < fakeInventory.getContents().length; i++)
				inventory.setItem(i, fakeInventory.getContents()[i]);
			return overflow;
		}

		final Map<Integer, ItemStack> left = new HashMap<>();

		// combine items
		final ItemStack[] combined = new ItemStack[items.length];
		for (final ItemStack item : items) {
			if (item == null || item.getAmount() < 1)
				continue;
			for (int j = 0; j < combined.length; j++) {
				if (combined[j] == null) {
					combined[j] = item.clone();
					break;
				}
				if (combined[j].isSimilar(item)) {
					combined[j].setAmount(combined[j].getAmount() + item.getAmount());
					break;
				}
			}
		}

		for (int i = 0; i < combined.length; i++) {
			final ItemStack item = combined[i];
			if (item == null || item.getType() == Material.AIR)
				continue;

			while (true) {
				// Do we already have a stack of it?
				final int maxAmount = oversizedStacks > item.getType().getMaxStackSize() ? oversizedStacks : item.getType().getMaxStackSize();
				final int firstPartial = firstPartial(inventory, item, maxAmount);

				// Drat! no partial stack
				if (firstPartial == -1) {
					// Find a free spot!
					final int firstFree = inventory.firstEmpty();

					if (firstFree == -1) {
						// No space at all!
						left.put(i, item);
						break;
					}

					// More than a single stack!
					if (item.getAmount() > maxAmount) {
						final ItemStack stack = item.clone();
						stack.setAmount(maxAmount);
						inventory.setItem(firstFree, stack);
						item.setAmount(item.getAmount() - maxAmount);
					} else {
						// Just store it
						inventory.setItem(firstFree, item);
						break;
					}

				} else {
					// So, apparently it might only partially fit, well lets do just that
					final ItemStack partialItem = inventory.getItem(firstPartial);

					final int amount = item.getAmount();
					final int partialAmount = partialItem.getAmount();

					// Check if it fully fits
					if (amount + partialAmount <= maxAmount) {
						partialItem.setAmount(amount + partialAmount);
						break;
					}

					// It fits partially
					partialItem.setAmount(maxAmount);
					item.setAmount(amount + partialAmount - maxAmount);
				}
			}
		}
		return left;
	}

	// ----------------------------------------------------------------------------------------------------
	// Utility
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 返回第一个相似物品堆
	 *
	 * @param inventory
	 * @param item
	 * @param maxAmount
	 * @return
	 */
	private static int firstPartial(final Inventory inventory, final ItemStack item, final int maxAmount) {
		if (item == null)
			return -1;
		final ItemStack[] stacks = inventory.getContents();
		for (int i = 0; i < stacks.length; i++) {
			final ItemStack cItem = stacks[i];
			if (cItem != null && cItem.getAmount() < maxAmount && cItem.isSimilar(item))
				return i;
		}
		return -1;
	}

	/**
	 * 创建大小为 {@link #PLAYER_INV_SIZE} 的新物品栏
	 *
	 * @param playerInventory
	 * @return
	 */
	private static Inventory makeTruncatedInv(final PlayerInventory playerInventory) {
		final Inventory fake = Bukkit.createInventory(null, PLAYER_INV_SIZE);
		fake.setContents(Arrays.copyOf(playerInventory.getContents(), fake.getSize()));

		return fake;
	}

	/**
	 * 若该物品栏是合并的玩家物品栏则返回 true
	 *
	 * @param inventory
	 * @return
	 */
	private static boolean isCombinedInv(final Inventory inventory) {
		return inventory instanceof PlayerInventory && inventory.getContents().length > PLAYER_INV_SIZE;
	}
}
