package org.mineacademy.fo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleRunnable;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 管理实体的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityUtil {

	/**
	 * 用于防止重复注册 {@link HitTracking} 监听器。
	 */
	private static boolean registeredHitListener = false;

	/**
	 * 返回中心位置给定三维范围内与给定实体类匹配的最近实体，
	 * 若未找到则返回 null。
	 *
	 * @param <T>
	 * @param center
	 * @param range3D
	 * @param entityClass
	 * @return
	 */
	public static <T extends LivingEntity> T findNearestEntity(Location center, double range3D, Class<T> entityClass) {
		final List<T> found = new ArrayList<>();

		for (final Entity nearby : Remain.getNearbyEntities(center, range3D))
			if (nearby instanceof LivingEntity && entityClass.isAssignableFrom(nearby.getClass()))
				found.add((T) nearby);

		Collections.sort(found, (first, second) -> Double.compare(first.getLocation().distance(center), second.getLocation().distance(center)));

		return found.isEmpty() ? null : found.get(0);
	}

	/**
	 * 仅当实体目标是玩家时返回它，否则返回 null
	 *
	 * @param entity
	 * @return
	 */
	public static Player getTargetPlayer(Entity entity) {
		final Entity target = getTarget(entity);

		if (target == null)
			return null;

		return target instanceof Player && target.getLocation().getWorld().equals(entity.getWorld()) && !HookManager.isNPC(target) ? (Player) target : null;
	}

	/**
	 * 返回给定实体的目标，支持 NPC 实体目标获取，请使用
	 * {@link HookManager#isNPC(Entity)} 检查目标是否为 NPC
	 *
	 * @param entity
	 * @return 目标，若没有或不支持则为 null
	 */
	public static Entity getTarget(Entity entity) {
		Entity target = null;

		try {
			if (entity instanceof Mob)
				target = ((Mob) entity).getTarget();
		} catch (final Throwable t) {
			// Old MC
		}

		if (target == null && entity instanceof Creature)
			target = ((Creature) entity).getTarget();

		if (target == null)
			target = HookManager.getNPCTarget(entity);

		return target;
	}

	/**
	 * 尝试在 y=0 坐标生成实体 1 tick 然后移除，
	 * 以此获取其在 Minecraft 中的默认生命值
	 *
	 * @param type
	 * @return
	 */
	public static double getDefaultHealth(EntityType type) {

		if (type == EntityType.PLAYER)
			return 20;

		final Location location = Bukkit.getWorlds().get(0).getSpawnLocation();
		location.setY(0);

		final Entity entity = location.getWorld().spawnEntity(location, type);
		Valid.checkBoolean(entity instanceof LivingEntity, "Cannot use getDefaultHealth for non-living entity: " + type);

		final double health = Remain.getHealth((LivingEntity) entity);

		entity.remove();
		return health;
	}

	/**
	 * 调整给定位置使其朝向 "facing" 位置
	 *
	 * @param location 原点位置
	 * @param facing 朝向哪里
	 */
	public static void rotateYaw(Location location, Location facing) {
		final float yaw = (float) Math.toDegrees(Math.atan2(facing.getZ() - location.getZ(), facing.getX() - location.getX())) - 90;

		location.setYaw(yaw);
	}

	/**
	 * 尝试移除给定实体上的所有载具和乘骑堆叠
	 *
	 * @param entity
	 */
	public static void removeVehiclesAndPassengers(Entity entity) {

		Entity vehicle = entity.getVehicle();

		while (vehicle != null) {
			final Entity copyOf = vehicle;
			vehicle = vehicle.getVehicle();

			copyOf.remove();
		}

		try {
			for (final Entity passenger : entity.getPassengers())
				passenger.remove();

		} catch (final NoSuchMethodError err) {
			final Entity passenger = entity.getPassenger();

			if (passenger != null)
				passenger.remove();
		}
	}

	/**
	 * 返回该实体是否为生物且具攻击性（非动物）
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isAggressive(Entity entity) {
		if (entity instanceof Ghast || entity instanceof Slime)
			return true;

		if (entity instanceof Wolf && ((Wolf) entity).isAngry())
			return true;

		if (entity instanceof Animals)
			return false;

		return entity instanceof Creature;
	}

	/**
	 * 返回该实体是否为 {@link Creature}、{@link Slime} 或 {@link Wolf}
	 *
	 * @param entity
	 * @return
	 */
	public static boolean isCreature(Entity entity) {
		return entity instanceof Slime ||
				entity instanceof Wolf ||
				entity instanceof Creature;
	}

	/**
	 * 返回该实体是否适合移除（例如掉落物、
	 * 掉落方块、箭、投掷物）
	 *
	 * @param entity
	 * @return
	 */
	public static boolean canBeCleaned(Entity entity) {
		return entity instanceof FallingBlock ||
				entity instanceof Item ||
				entity instanceof Projectile ||
				entity instanceof ExperienceOrb;
	}

	// ----------------------------------------------------------------------------------------------------
	// Dropping
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 尝试掉落物品，允许在物品生成前
	 * 为其设置属性
	 *
	 * @param location
	 * @param item
	 * @param modifier
	 * @return 该物品
	 */
	public static Item dropItem(Location location, ItemStack item, Consumer<Item> modifier) {
		return Remain.spawnItem(location, item, modifier);
	}

	// ----------------------------------------------------------------------------------------------------
	// Tracking
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 运行周期为 1 tick 的计时任务，当给定实体落地时触发你的命中监听器。
	 * 若实体在落地前被移除，则
	 * 什么都不会调用
	 * <p>
	 * 若实体 30 秒后仍在飞行，则什么都不会调用
	 *
	 * @param entity
	 * @param hitGroundListener
	 */
	public static void trackFalling(Entity entity, Runnable hitGroundListener) {
		track(entity, 30 * 20, null, hitGroundListener);
	}

	/**
	 * 运行周期为 1 tick 的计时任务，每 tick 触发你的飞行监听器，
	 * 直到实体被移除或落地
	 * <p>
	 * 若实体 30 秒后仍在飞行，则什么都不会调用
	 *
	 * @param entity
	 * @param flyListener
	 */
	public static void trackFlying(Entity entity, Runnable flyListener) {
		track(entity, 30 * 20, flyListener, null);
	}

	/**
	 * 运行周期为 1 tick 的计时任务，当给定实体落地时触发你的命中监听器。
	 * 若实体在落地前被移除，则
	 * 什么都不会调用
	 * <p>
	 * 飞行监听器每 tick 调用一次
	 *
	 * @param entity
	 * @param timeoutTicks
	 * @param flyListener
	 * @param hitGroundListener
	 */
	public static void track(Entity entity, int timeoutTicks, Runnable flyListener, Runnable hitGroundListener) {
		if (flyListener == null && hitGroundListener == null)
			throw new FoException("Cannot track entity with fly and hit listeners on null!");

		Common.runTimer(1, new SimpleRunnable() {

			private int elapsedTicks = 0;

			@Override
			public void run() {

				// Cancel after the given timeout to save performance
				if (this.elapsedTicks++ > timeoutTicks) {
					this.cancel();

					return;
				}

				// Cancel when invalid
				if (entity == null || entity.isDead() || !entity.isValid()) {
					if (entity instanceof FallingBlock && hitGroundListener != null)
						hitGroundListener.run();

					this.cancel();
					return;
				}

				// Run the hit listener
				if (entity.isOnGround()) {
					if (hitGroundListener != null)
						hitGroundListener.run();

					this.cancel();

				} else if (flyListener != null)
					flyListener.run();
			}
		});
	}

	/**
	 * （无计时任务）开始追踪投掷物的撞击，命中某物时执行命中
	 * 任务。飞行 30 秒后停止追踪
	 * 以节省性能
	 *
	 * @param projectile
	 * @param hitTask
	 */
	public static void trackHit(Projectile projectile, Consumer<ProjectileHitEvent> hitTask) {
		HitTracking.addFlyingProjectile(projectile, hitTask);

		if (!registeredHitListener) {
			Common.registerEvents(new HitTracking());

			registeredHitListener = true;
		}
	}
}

/**
 * 负责追踪投掷物发射与投掷物命中事件之间关联的类
 */
class HitTracking implements Listener {

	/**
	 * 飞行中的投掷物列表，撞击时运行代码，
	 * 30 秒后停止追踪以防止映射过载
	 */
	private static ExpiringMap<UUID, List<Consumer<ProjectileHitEvent>>> flyingProjectiles = ExpiringMap.builder().expiration(30, TimeUnit.SECONDS).build();

	/**
	 * 当注册的投掷物命中某物时调用命中监听器
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHit(ProjectileHitEvent event) {
		final List<Consumer<ProjectileHitEvent>> hitListeners = flyingProjectiles.remove(event.getEntity().getUniqueId());

		if (hitListeners != null)
			for (final Consumer<ProjectileHitEvent> listener : hitListeners)
				listener.accept(event);
	}

	/**
	 * 添加新的飞行中投掷物，它将处于待命中状态，碰撞时执行代码
	 *
	 * @param projectile
	 * @param hitTask
	 */
	static void addFlyingProjectile(Projectile projectile, Consumer<ProjectileHitEvent> hitTask) {
		final UUID uniqueId = projectile.getUniqueId();
		final List<Consumer<ProjectileHitEvent>> listeners = flyingProjectiles.getOrDefault(uniqueId, new ArrayList<>());

		listeners.add(hitTask);
		flyingProjectiles.put(uniqueId, listeners);
	}
}
