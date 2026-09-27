package top.brmc.devlib.event;

import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import top.brmc.devlib.menu.tool.Rocket;

import lombok.Getter;
import lombok.Setter;

/**
 * {@link Rocket} 爆炸时触发的事件。
 */
@Getter
public final class RocketExplosionEvent extends SimpleEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * 火箭
	 */
	private final Rocket rocket;

	/**
	 * 投射物
	 */
	private final Projectile projectile;

	/**
	 * 此次爆炸的威力
	 */
	@Setter
	private float power;

	/**
	 * 此次爆炸是否破坏方块？
	 */
	@Setter
	private boolean breakBlocks;

	/**
	 * 事件是否已被取消？
	 */
	@Setter
	private boolean cancelled;

	public RocketExplosionEvent(Rocket rocket, Projectile projectile, float power, boolean breakBlocks) {
		this.rocket = rocket;
		this.projectile = projectile;
		this.power = power;
		this.breakBlocks = breakBlocks;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}