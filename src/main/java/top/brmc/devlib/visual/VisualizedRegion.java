package top.brmc.devlib.visual;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import top.brmc.devlib.BlockUtil;
import top.brmc.devlib.Common;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.collection.StrictMap;
import top.brmc.devlib.model.SimpleRunnable;
import top.brmc.devlib.region.Region;
import top.brmc.devlib.remain.CompParticle;

import lombok.Getter;
import lombok.Setter;

/**
 * 在世界中可视化两个位置的简单方式
 */
public final class VisualizedRegion extends Region {

	/**
	 * @deprecated 无法使用，请改为调用 new VisualizedRegion()
	 */
	@Deprecated
	public final static VisualizedRegion EMPTY = null;

	/**
	 * 可以看到粒子的玩家列表及其粒子颜色（要求 {@link #particle} 为 REDSTONE）
	 */
	private final StrictMap<Player, Color> viewers = new StrictMap<>();

	/**
	 * 负责发送粒子的任务
	 */
	private BukkitTask task;

	/**
	 * 正在发送的粒子
	 */
	@Getter
	@Setter
	private CompParticle particle = CompParticle.VILLAGER_HAPPY;

	/**
	 * 可视化区域时每次显示粒子之间的延迟，越低可见性越好，但 CPU 消耗越大
	 */
	@Getter
	@Setter
	private int delayTicks = 23;

	/**
	 * 创建一个新的可视化空区域
	 */
	public VisualizedRegion() {
		this(null, null);
	}

	/**
	 * 创建一个新的可视化区域
	 *
	 * @param primary
	 * @param secondary
	 */
	public VisualizedRegion(final Location primary, final Location secondary) {
		super(primary, secondary);
	}

	/**
	 * 创建一个可视化区域
	 *
	 * @param name
	 * @param primary
	 * @param secondary
	 */
	public VisualizedRegion(final String name, final Location primary, final Location secondary) {
		super(name, primary, secondary);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Rendering
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * 在给定时长内向给定玩家显示该区域，
	 * 然后隐藏
	 *
	 * @param player
	 * @param durationTicks
	 */
	public void showParticles(Player player, int durationTicks) {
		this.showParticles(player, null, durationTicks);
	}

	/**
	 * 在给定时长内向给定玩家显示该区域，
	 * 然后隐藏
	 *
	 * @param player
	 * @param color
	 * @param durationTicks
	 */
	public void showParticles(Player player, @Nullable Color color, int durationTicks) {
		this.showParticles(player, color);

		Common.runLater(durationTicks, () -> {
			if (this.canSeeParticles(player))
				this.hideParticles(player);
		});
	}

	/**
	 * 向给定玩家显示该区域
	 *
	 * @param player
	 */
	public void showParticles(final Player player) {
		this.showParticles(player, null);
	}

	/**
	 * 向给定玩家显示该区域
	 *
	 * @param player
	 * @param color
	 */
	public void showParticles(final Player player, @Nullable Color color) {
		Valid.checkBoolean(!this.canSeeParticles(player), "Player " + player.getName() + " already sees region " + this);
		Valid.checkBoolean(this.isWhole(), "Cannot show particles of an incomplete region " + this);

		this.viewers.put(player, color);

		if (this.task == null)
			this.startVisualizing();
	}

	/**
	 * 对给定玩家隐藏该区域
	 *
	 * @param player
	 */
	public void hideParticles(final Player player) {
		Valid.checkBoolean(this.canSeeParticles(player), "Player " + player.getName() + " is not seeing region " + this);

		this.viewers.removeWeak(player);

		if (this.viewers.isEmpty() && this.task != null)
			this.stopVisualizing();
	}

	/**
	 * 若给定玩家能看到区域粒子则返回 true
	 *
	 * @param player
	 * @return
	 */
	public boolean canSeeParticles(final Player player) {
		return this.viewers.containsKey(player);
	}

	/*
	 * Starts visualizing this region if it is whole
	 */
	private void startVisualizing() {
		Valid.checkBoolean(this.task == null, "Already visualizing region " + this + "!");
		Valid.checkBoolean(this.isWhole(), "Cannot visualize incomplete region " + this + "!");

		this.task = Common.runTimer(this.delayTicks, new SimpleRunnable() {
			@Override
			public void run() {
				if (VisualizedRegion.this.viewers.isEmpty() || !VisualizedRegion.this.isWhole()) {
					VisualizedRegion.this.stopVisualizing();

					return;
				}

				final Set<Location> blocks = BlockUtil.getBoundingBox(VisualizedRegion.this.getPrimary(), VisualizedRegion.this.getSecondary());

				for (final Location location : blocks)
					for (final Map.Entry<Player, Color> entry : VisualizedRegion.this.viewers.entrySet()) {
						final Player viewer = entry.getKey();
						final Color color = entry.getValue();
						final Location viewerLocation = viewer.getLocation();

						if (viewerLocation.getWorld().equals(location.getWorld()) && viewerLocation.distance(location) < 100)
							if (color != null)
								CompParticle.REDSTONE.spawn(viewer, location, color, 0.5F);

							else
								VisualizedRegion.this.particle.spawn(viewer, location);
					}

			}
		});
	}

	/*
	 * Stops the region from being visualized
	 */
	private void stopVisualizing() {
		Valid.checkNotNull(this.task, "Region " + this + " not visualized");

		this.task.cancel();
		this.task = null;

		this.viewers.clear();
	}

	@Override
	public VisualizedRegion clone() {
		return new VisualizedRegion(
				this.getName() != null ? new String(this.getName()) : null,
				this.getPrimary() != null ? this.getPrimary().clone() : null,
				this.getSecondary() != null ? this.getSecondary().clone() : null);
	}

	/**
	 * 如果 yaml/json 文件中保存的映射包含 Primary 和 Secondary 键，则将其转换为区域
	 *
	 * @param map
	 * @return
	 */
	public static VisualizedRegion deserialize(final SerializedMap map) {

		// Support loading an empty key with "{}" empty map
		if (map.isEmpty())
			return new VisualizedRegion();

		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = map.getLocation("Primary");
		final Location sec = map.getLocation("Secondary");

		return new VisualizedRegion(name, prim, sec);
	}
}
