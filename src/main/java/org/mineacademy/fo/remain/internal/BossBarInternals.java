package org.mineacademy.fo.remain.internal;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.SimpleTask;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompBarColor;
import org.mineacademy.fo.remain.CompBarStyle;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;

/**
 * 处理 Boss 栏跨服务端兼容性的类基于
 * SoThatsIt 的代码。
 * <p>
 * http://forums.bukkit.org/threads/tutorial-utilizing-the-boss-health-bar.158018/page-2#post-1760928
 */
public final class BossBarInternals implements Listener {

	/**
	 * 单例实例
	 */
	@Getter
	private static BossBarInternals instance = new BossBarInternals();

	/**
	 * 虚拟末影龙类
	 */
	private final Class<?> entityClass;

	/**
	 * 当前正在查看 Boss 栏的玩家
	 */
	private final HashMap<UUID, NMSDragon> players = new HashMap<>();

	/**
	 * 当前正在运行的计时器（用于临时 Boss 栏）
	 */
	private final HashMap<UUID, SimpleTask> timers = new HashMap<>();

	// Singleton
	private BossBarInternals() {

		if (MinecraftVersion.olderThan(V.v1_6))
			this.entityClass = null;

		else if (Remain.isProtocol18Hack())
			this.entityClass = NMSDragon_v1_8Hack.class;

		else if (MinecraftVersion.equals(V.v1_6))
			this.entityClass = NMSDragon_v1_6.class;

		else if (MinecraftVersion.equals(V.v1_7))
			this.entityClass = NMSDragon_v1_7.class;

		else if (MinecraftVersion.equals(V.v1_8))
			this.entityClass = NMSDragon_v1_8.class;

		else
			this.entityClass = NMSDragon_v1_9.class;

		if (MinecraftVersion.atLeast(V.v1_6)) {
			Valid.checkNotNull(this.entityClass, "Failed to load Boss bar on Minecraft " + MinecraftVersion.getFullVersion() + "!");

			Common.registerEvents(this);

			if (Remain.isProtocol18Hack())
				Common.runTimer(5, () -> {
					for (final UUID uuid : this.players.keySet()) {
						final Player player = Remain.getPlayerByUUID(uuid);

						Remain.sendPacket(player, this.players.get(uuid).getTeleportPacket(this.getDragonLocation(player.getLocation())));
					}
				});
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPluginDisable(final PluginDisableEvent event) {
		if (event.getPlugin().equals(SimplePlugin.getInstance()))
			this.stop();
	}

	// Removes bars from all players
	private void stop() {
		for (final Player player : Remain.getOnlinePlayers())
			this.removeBar(player);

		this.players.clear();

		for (final SimpleTask task : this.timers.values())
			task.cancel();

		this.timers.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		this.removeBar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerKick(final PlayerKickEvent event) {
		this.removeBar(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		this.handleTeleport(event.getPlayer(), event.getTo().clone());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerRespawnEvent event) {
		this.handleTeleport(event.getPlayer(), event.getRespawnLocation().clone());
	}

	// Fixes bar disappearing on teleport
	private void handleTeleport(final Player player, final Location loc) {
		if (!this.hasBar(player))
			return;

		final NMSDragon oldDragon = this.getDragon(player, "");

		if (oldDragon instanceof NMSDragon_v1_9 || oldDragon == null)
			return;

		Common.runLater(2, () -> {
			if (!this.hasBar(player))
				return;

			final float health = oldDragon.getHealth();
			final String message = oldDragon.getName();

			Remain.sendPacket(player, this.getDragon(player, "").getDestroyPacket());

			this.players.remove(player.getUniqueId());

			final NMSDragon dragon = this.addDragon(player, loc, message);
			dragon.setHealthF(health);

			this.sendDragon(dragon, player);
		});
	}

	/**
	 * 为给定玩家设置一条消息。<br>
	 * 它会一直保留，直到玩家下线或被其他插件
	 * 覆盖。<br>
	 * 此方法会按给定百分比显示血条，并
	 * 取消所有正在运行的计时器。
	 *
	 * @param player  应看到该消息的玩家。
	 * @param message 向玩家显示的消息。<br>
	 *                由于 Minecraft 的限制，此消息不能超过
	 *                64 个字符。<br>
	 *                超出部分会被自动截断。
	 * @param percent 血条填充的百分比。<br>
	 *                该值必须介于 0F（含）到 100F
	 *                （含）之间。
	 * @param color
	 * @param style
	 * @throws IllegalArgumentException 如果百分比不在有效
	 *                                  范围内。
	 */
	public void setMessage(Player player, String message, float percent, CompBarColor color, CompBarStyle style) {
		Valid.checkBoolean(0F <= percent && percent <= 100F, "Percent must be between 0F and 100F, but was: " + percent);

		if (this.entityClass == null)
			return;

		if (this.hasBar(player))
			this.removeBar(player);

		message = Common.colorize(message);

		final NMSDragon dragon = this.getDragon(player, message);

		dragon.setName(cleanMessage(message));
		dragon.setHealthF(percent / 100f * dragon.getMaxHealth());

		if (color != null)
			dragon.barColor = color;

		if (style != null)
			dragon.barStyle = style;

		this.cancelTimer(player);

		this.sendDragon(dragon, player);
	}

	/**
	 * 为给定玩家设置一条消息。<br>
	 * 它会一直保留，直到玩家下线或被其他插件
	 * 覆盖。<br>
	 * 此方法会把血条用作递减计时器，之前
	 * 启动的所有计时器都会被取消。<br>
	 * 计时器从满血条开始。<br>
	 * 血条归零时会被自动移除。
	 *
	 * @param player  应看到该计时器/消息的玩家。
	 * @param message 向玩家显示的消息。<br>
	 *                由于 Minecraft 的限制，此消息不能超过
	 *                64 个字符。<br>
	 *                超出部分会被自动截断。
	 * @param seconds 计时器显示的秒数。<br>
	 *                支持大于等于 1 的值。
	 * @param color
	 * @param style
	 * @throws IllegalArgumentException 如果秒数小于等于零。
	 */
	public void setMessage(final Player player, String message, final int seconds, final CompBarColor color, final CompBarStyle style) {
		Valid.checkBoolean(seconds > 0, "Seconds must be > 1 ");

		if (this.entityClass == null)
			return;

		if (this.hasBar(player))
			this.removeBar(player);

		message = Common.colorize(message);

		final NMSDragon dragon = this.getDragon(player, message);

		dragon.setName(cleanMessage(message));
		dragon.setHealthF(dragon.getMaxHealth());

		if (color != null)
			dragon.barColor = color;
		if (style != null)
			dragon.barStyle = style;

		final float dragonHealthMinus = dragon.getMaxHealth() / seconds;

		this.cancelTimer(player);

		this.timers.put(player.getUniqueId(), Common.runTimer(20, 20, () -> {
			final NMSDragon drag = this.getDragon(player, "");
			drag.setHealthF(drag.getHealth() - dragonHealthMinus);

			if (drag.getHealth() <= 1) {
				this.removeBar(player);
				this.cancelTimer(player);
			} else
				this.sendDragon(drag, player);
		}));

		this.sendDragon(dragon, player);
	}

	/**
	 * 从给定玩家处移除 Boss 栏
	 *
	 * @param player
	 */
	public void removeBar(final Player player) {

		if (this.entityClass == null)
			return;

		if (!this.hasBar(player))
			return;

		final NMSDragon dragon = this.getDragon(player, "");

		if (dragon instanceof NMSDragon_v1_9)
			((NMSDragon_v1_9) dragon).removePlayer(player);
		else
			Remain.sendPacket(player, this.getDragon(player, "").getDestroyPacket());

		this.players.remove(player.getUniqueId());

		this.cancelTimer(player);
	}

	private boolean hasBar(final Player player) {
		return this.players.containsKey(player.getUniqueId());
	}

	private static String cleanMessage(String message) {
		if (message.length() > 64)
			message = message.substring(0, 63);

		return message;
	}

	private void cancelTimer(final Player player) {
		final SimpleTask task = this.timers.remove(player.getUniqueId());

		if (task != null)
			task.cancel();
	}

	private void sendDragon(final NMSDragon dragon, final Player player) {
		if (dragon instanceof NMSDragon_v1_9) {
			final NMSDragon_v1_9 bar = (NMSDragon_v1_9) dragon;

			bar.addPlayer(player);
			bar.setProgress(dragon.getHealth() / dragon.getMaxHealth());

		} else {
			Remain.sendPacket(player, dragon.getMetaPacket(dragon.getWatcher()));
			Remain.sendPacket(player, dragon.getTeleportPacket(this.getDragonLocation(player.getLocation())));
		}
	}

	private NMSDragon getDragon(final Player player, final String message) {
		if (this.hasBar(player))
			return this.players.get(player.getUniqueId());

		return this.addDragon(player, cleanMessage(message));
	}

	private NMSDragon addDragon(final Player player, final String message) {
		return this.addDragon(player, player.getLocation(), message);
	}

	private NMSDragon addDragon(final Player player, final Location loc, final String message) {
		final NMSDragon dragon = this.newDragon(message, this.getDragonLocation(loc));

		if (dragon instanceof NMSDragon_v1_9)
			((NMSDragon_v1_9) dragon).addPlayer(player);

		else
			Remain.sendPacket(player, dragon.getSpawnPacket());

		this.players.put(player.getUniqueId(), dragon);

		return dragon;
	}

	private Location getDragonLocation(Location loc) {
		final float pitch = loc.getPitch();

		if (pitch >= 55)
			loc.add(0, -3, 0);
		else if (pitch <= -55)
			loc.add(0, 3, 0);
		else
			loc = loc.getBlock().getRelative(getDirection(loc), Bukkit.getViewDistance() * 8).getLocation();

		loc.subtract(0, 10, 0);

		return loc;
	}

	private static BlockFace getDirection(final Location loc) {
		final float dir = Math.round(loc.getYaw() / 90);
		if (dir == -4 || dir == 0 || dir == 4)
			return BlockFace.SOUTH;
		if (dir == -1 || dir == 3)
			return BlockFace.EAST;
		if (dir == -2 || dir == 2)
			return BlockFace.NORTH;
		if (dir == -3 || dir == 1)
			return BlockFace.WEST;
		return null;
	}

	private NMSDragon newDragon(final String message, final Location loc) {
		NMSDragon fakeDragon = null;

		try {
			fakeDragon = (NMSDragon) this.entityClass.getConstructor(String.class, Location.class).newInstance(message, loc);
		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return fakeDragon;
	}
}
