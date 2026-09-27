package top.brmc.devlib.event;

import org.bukkit.World;
import org.bukkit.event.HandlerList;
import top.brmc.devlib.model.OfflineRegionScanner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 当 {@link OfflineRegionScanner} 扫描完磁盘上所有离线区域后触发
 */
@Getter
@RequiredArgsConstructor
public final class RegionScanCompleteEvent extends SimpleEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * 该扫描器所处理的世界
	 */
	private final World world;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}