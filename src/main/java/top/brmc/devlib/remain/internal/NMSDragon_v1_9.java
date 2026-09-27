package top.brmc.devlib.remain.internal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * 表示 Minecraft 1.9 及更新版本的原生 Boss 栏 API
 */
class NMSDragon_v1_9 extends NMSDragon {

	private final BossBar bar;

	public NMSDragon_v1_9(String name, Location loc) {
		super(name, loc);

		this.bar = Bukkit.createBossBar(name, BarColor.PINK, BarStyle.SOLID);
	}

	public final void removePlayer(Player player) {
		this.getBar().removePlayer(player);
	}

	public final void addPlayer(Player player) {
		this.getBar().addPlayer(player);
	}

	public final void setProgress(double progress) {
		this.getBar().setProgress(progress);
	}

	private BossBar getBar() {
		if (this.barColor != null)
			this.bar.setColor(BarColor.valueOf(this.barColor.toString()));

		if (this.barStyle != null)
			this.bar.setStyle(BarStyle.valueOf(this.barStyle.toString()));

		return this.bar;
	}

	@Override
	public Object getSpawnPacket() {
		return null;
	}

	@Override
	public Object getDestroyPacket() {
		return null;
	}

	@Override
	public Object getMetaPacket(Object watcher) {
		return null;
	}

	@Override
	public Object getTeleportPacket(Location loc) {
		return null;
	}

	@Override
	public Object getWatcher() {
		return null;
	}

	@Override
	Object getNMSEntity() {
		return null;
	}
}
