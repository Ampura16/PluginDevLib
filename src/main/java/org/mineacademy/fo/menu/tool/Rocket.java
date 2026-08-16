package org.mineacademy.fo.menu.tool;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerInteractEvent;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.event.RocketExplosionEvent;

import lombok.Getter;

/**
 * 火箭是扩展的 {@link Tool}，
 * 在击中地面时爆炸。
 * <p>
 * 请使用 onExplode 方法来触发
 * 爆炸，或手动调用 {@link RocketExplosionEvent}。
 */
@Getter
public abstract class Rocket extends Tool {

	/**
	 * 被发射的投射物
	 */
	private final Class<? extends Projectile> projectile;

	/**
	 * 发射投射物时的飞行速度
	 */
	private final float flightSpeed;

	/**
	 * 爆炸威力，推荐：2 - 15
	 */
	private final float explosionPower;

	/**
	 * 爆炸是否破坏方块？
	 */
	private final boolean breakBlocks;

	/**
	 * 创建一枚新火箭，飞行速度为 1.5F（略高于正常），
	 * 爆炸威力为 5F（TNT 为 4F）
	 *
	 * @param projectile
	 * @param flightSpeed
	 */
	protected Rocket(Class<? extends Projectile> projectile) {
		this(projectile, 1.5F);
	}

	/**
	 * 创建一枚爆炸威力为 5F（TNT 为 4F）的新火箭
	 *
	 * @param projectile
	 * @param flightSpeed
	 */
	protected Rocket(Class<? extends Projectile> projectile, float flightSpeed) {
		this(projectile, flightSpeed, 5F);
	}

	/**
	 * 创建一枚会破坏方块的新火箭
	 *
	 * @param projectile
	 * @param flightSpeed
	 * @param explosionPower
	 */
	protected Rocket(Class<? extends Projectile> projectile, float flightSpeed, float explosionPower) {
		this(projectile, flightSpeed, explosionPower, true);
	}

	/**
	 * 使用给定的投射物及其速度（1=正常，5=疯狂，10=最大，会出 bug）、
	 * 爆炸威力（1-30，不过超过 15 就已经会出 bug）以及是否破坏方块，创建一枚新火箭
	 * <p>
	 * 爆炸威力参见 https://minecraft.wiki/w/Explosion
	 *
	 * @param projectile
	 * @param flightSpeed
	 */
	protected Rocket(Class<? extends Projectile> projectile, float flightSpeed, float explosionPower, boolean breakBlocks) {
		Valid.checkBoolean(flightSpeed <= 10F, "Rocket cannot have speed over 10");
		Valid.checkBoolean(explosionPower <= 30F, "Rocket cannot have explosion power over 30");

		this.projectile = projectile;
		this.flightSpeed = flightSpeed;
		this.explosionPower = explosionPower;
		this.breakBlocks = breakBlocks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onBlockClick(PlayerInteractEvent e) {
	}

	/**
	 * 若此火箭可以在给定位置发射则返回 true。
	 * 例如，我们用它检查发射火箭的玩家是否在竞技场内
	 *
	 * @param shooter
	 * @param location
	 * @return
	 */
	protected boolean canLaunch(Player shooter, Location location) {
		return true;
	}

	/**
	 * 此火箭被发射时自动调用
	 *
	 * @param projectile
	 * @param shooter
	 */
	protected void onLaunch(Projectile projectile, Player shooter) {
	}

	/**
	 * 从发射投射物到它击中地面为止，
	 * 每个服务器刻都有一个定时任务调用此方法
	 * <p>
	 * 提示：你可以在这里生成特殊的飞行粒子
	 *
	 * @param projectile
	 * @param shooter
	 */
	protected void onFlyTick(Projectile projectile, Player shooter) {
	}

	/**
	 * 检查火箭能否爆炸；若为 false，我们只会移除该实体
	 *
	 * @param projectile
	 * @param shooter
	 * @return
	 */
	protected boolean canExplode(Projectile projectile, Player shooter) {
		return true;
	}

	/**
	 * 此火箭击中地面时自动调用。
	 * <p>
	 * 提示：调用 {@link #explode(Projectile, float, boolean)}
	 *
	 * @param projectile
	 * @param shooter
	 * @param location
	 */
	protected void onExplode(Projectile projectile, Player shooter) {
	}

	/**
	 * 点击空气时也发射火箭（因为 Bukkit 会取消该事件）
	 *
	 * @return true
	 */
	@Override
	protected boolean ignoreCancelled() {
		return false;
	}

	/**
	 * 自动取消点击事件以发射火箭
	 */
	@Override
	protected boolean autoCancel() {
		return true;
	}
}
