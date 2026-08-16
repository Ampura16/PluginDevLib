package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompParticle;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
public abstract class SimpleHologram {

	/**
	 * 此物品每行描述文字之间的间距
	 */
	@Getter
	@Setter
	private static double loreLineHeight = 0.26D;

	/**
	 * 已创建的动画物品注册表
	 */
	@Getter
	private static Set<SimpleHologram> registeredItems = new HashSet<>();

	/**
	 * 负责调用 {@link #onTick()} 的定时任务
	 */
	private static BukkitTask tickingTask = null;

	/**
	 * 盔甲架名称，每一行会生成另一个隐形盔甲架
	 */
	@Getter
	private final List<ArmorStand> loreEntities = new ArrayList<>();

	/**
	 * 生成位置
	 */
	private final Location lastTeleportLocation;

	/**
	 * 物品上方的描述文字
	 */
	@Getter
	private final List<String> loreLines = new ArrayList<>();

	/**
	 * 在全息图下方生成的可选粒子
	 */
	@Getter
	private final List<Tuple<CompParticle, Object>> particles = new ArrayList<>();

	/**
	 * 显示的实体
	 */
	@Getter
	private Entity entity;

	/*
	 * A private flag to help with teleporting of this entity
	 */
	private Location pendingTeleport = null;

	/*
	 * Constructs a new item and registers it
	 */
	protected SimpleHologram(Location spawnLocation) {
		this.lastTeleportLocation = spawnLocation.clone();

		registeredItems.add(this);

		onReload();
	}

	/**
	 * 重载时重启定时任务
	 *
	 * @deprecated 仅供内部使用，请勿调用
	 */
	@Deprecated
	public static void onReload() {
		if (tickingTask != null)
			tickingTask.cancel();

		tickingTask = scheduleTickingTask();
	}

	/*
	 * Helper method to start main anim ticking task
	 */
	private static BukkitTask scheduleTickingTask() {
		return Common.runTimer(1, () -> {

			for (final Iterator<SimpleHologram> it = registeredItems.iterator(); it.hasNext();) {
				final SimpleHologram model = it.next();

				if (model.isSpawned())
					if (!model.getEntity().isValid() || model.getEntity().isDead()) {
						model.removeLore();
						model.getEntity().remove();

						it.remove();
					} else
						model.tick();
			}
		});
	}

	/**
	 * 生成此全息图实体
	 *
	 * @return
	 */
	public SimpleHologram spawn() {
		Valid.checkBoolean(!this.isSpawned(), this + " is already spawned!");

		this.entity = this.createEntity();
		Valid.checkNotNull(this.entity, "Failed to spawn entity from " + this);

		this.drawLore(this.lastTeleportLocation);

		return this;
	}

	/**
	 * 生成实体的核心实现方法
	 *
	 * @return
	 */
	protected abstract Entity createEntity();

	/*
	 * Set a lore for this armor stand
	 */
	private void drawLore(Location location) {
		if (this.loreLines.isEmpty())
			return;

		if (this.entity instanceof ArmorStand && ((ArmorStand) this.entity).isSmall())
			location = location.add(0, -0.5, 0);

		for (final String loreLine : this.loreLines) {
			final ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

			armorStand.setGravity(false);
			armorStand.setVisible(false);

			Remain.setCustomName(armorStand, loreLine);

			location = location.subtract(0, loreLineHeight, 0);

			this.loreEntities.add(armorStand);
		}
	}

	/*
	 * Iterate the ticking mechanism of this entity
	 */
	private void tick() {

		if (this.pendingTeleport != null) {
			this.entity.teleport(this.pendingTeleport);

			for (final ArmorStand loreEntity : this.loreEntities)
				loreEntity.teleport(this.pendingTeleport);

			this.pendingTeleport = null;
			return;
		}

		this.onTick();

		for (final Tuple<CompParticle, Object> tuple : this.particles) {
			final CompParticle particle = tuple.getKey();
			final Object extra = tuple.getValue();

			if (extra instanceof CompMaterial)
				particle.spawn(this.getLocation(), (CompMaterial) extra);

			else if (extra instanceof Double)
				particle.spawn(this.getLocation(), (double) extra);
		}
	}

	/**
	 * 自动调用，你可以在此为盔甲架制作动画
	 */
	protected void onTick() {
	}

	/**
	 * 若此盔甲架已生成则返回 true
	 *
	 * @return
	 */
	public final boolean isSpawned() {
		return this.entity != null;
	}

	/**
	 * 删除盔甲架上的所有文字
	 */
	public final void removeLore() {
		this.loreEntities.forEach(ArmorStand::remove);
	}

	/**
	 *
	 * @param lore
	 * @return
	 */
	public final SimpleHologram setLore(String... lore) {
		this.loreLines.clear();
		this.loreLines.addAll(Arrays.asList(lore));

		return this;
	}

	/**
	 * 为此全息图添加粒子效果
	 *
	 * @param particle
	 */
	public final void addParticleEffect(CompParticle particle) {
		this.addParticleEffect(particle, null);
	}

	/**
	 * 为此全息图添加粒子效果
	 *
	 * @param particle
	 * @param data
	 */
	public final void addParticleEffect(CompParticle particle, CompMaterial data) {
		this.particles.add(new Tuple<>(particle, data));
	}

	/**
	 * 返回当前盔甲架位置
	 *
	 * @return
	 */
	public final Location getLocation() {
		this.checkSpawned("getLocation");

		return this.entity.getLocation();
	}

	/**
	 * 返回最后已知的传送位置
	 *
	 * @return
	 */
	public final Location getLastTeleportLocation() {
		return this.lastTeleportLocation.clone();
	}

	/**
	 * 将此全息图连同描述文字传送到给定位置
	 *
	 * @param location
	 */
	public final void teleport(Location location) {
		Valid.checkBoolean(this.pendingTeleport == null, this + " is already pending teleport to " + this.pendingTeleport);
		this.checkSpawned("teleport");

		this.lastTeleportLocation.setX(location.getY());
		this.lastTeleportLocation.setY(location.getY());
		this.lastTeleportLocation.setZ(location.getZ());

		this.pendingTeleport = location;
	}

	/**
	 * 删除此盔甲架
	 */
	public final void remove() {
		this.removeLore();

		if (this.entity != null)
			this.entity.remove();

		registeredItems.remove(this);
	}

	/*
	 * A helper method to check if this entity is spawned
	 */
	private void checkSpawned(String method) {
		Valid.checkBoolean(this.isSpawned(), this + " is not spawned, cannot call " + method + "!");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ArmorStandItem{spawnLocation=" + Common.shortLocation(this.lastTeleportLocation) + ", spawned=" + this.isSpawned() + "}";
	}

	/**
	 * 删除服务器上所有浮空物品
	 */
	public static final void deleteAll() {

		for (final Iterator<SimpleHologram> it = registeredItems.iterator(); it.hasNext();) {
			final SimpleHologram item = it.next();

			if (item.isSpawned())
				item.getEntity().remove();

			item.removeLore();
			it.remove();
		}
	}
}
