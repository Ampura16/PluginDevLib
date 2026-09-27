package top.brmc.devlib.region;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import top.brmc.devlib.BlockUtil;
import top.brmc.devlib.Common;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.model.ConfigSerializable;
import top.brmc.devlib.remain.CompMaterial;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * 表示一个长方体区域
 */
public class Region implements ConfigSerializable {

	/**
	 * 区域名称，未指定则为 null
	 */
	@Getter
	@Setter
	private String name;

	/**
	 * 区域的第一个位置
	 */
	private Location primary;

	/**
	 * 区域的第二个位置
	 */
	private Location secondary;

	/**
	 * 创建一个新区域
	 *
	 * @param primary
	 * @param secondary
	 */
	public Region(final Location primary, final Location secondary) {
		this(null, primary, secondary);
	}

	/**
	 * 创建一个带名称的新区域
	 *
	 * @param name
	 * @param primary
	 * @param secondary
	 */
	public Region(final String name, final Location primary, final Location secondary) {
		this.name = name;

		if (primary != null) {
			Valid.checkNotNull(primary.getWorld(), "Primary location lacks a world!");

			this.primary = primary;
		}

		if (secondary != null) {
			Valid.checkNotNull(secondary.getWorld(), "Primary location lacks a world!");

			this.secondary = secondary;
		}
	}

	/*
	 * Change primary/secondary around to make secondary always the lowest point
	 */
	private Location[] getCorrectedPoints() {
		if (this.primary == null || this.secondary == null)
			return null;

		Valid.checkBoolean(this.primary.getWorld().getName().equals(this.secondary.getWorld().getName()), "Points must be in one world! Primary: " + this.primary + " != secondary: " + this.secondary);

		final int x1 = this.primary.getBlockX(), x2 = this.secondary.getBlockX(),
				y1 = this.primary.getBlockY(), y2 = this.secondary.getBlockY(),
				z1 = this.primary.getBlockZ(), z2 = this.secondary.getBlockZ();

		final Location primary = this.primary.clone();
		final Location secondary = this.secondary.clone();

		primary.setX(Math.min(x1, x2));
		primary.setY(Math.min(y1, y2));
		primary.setZ(Math.min(z1, z2));

		secondary.setX(Math.max(x1, x2));
		secondary.setY(Math.max(y1, y2));
		secondary.setZ(Math.max(z1, z2));

		return new Location[] { primary, secondary };
	}

	/**
	 * 计算此区域中心的大致位置
	 *
	 * @return
	 */
	public final Location getCenter() {
		Valid.checkBoolean(this.isWhole(), "Cannot perform getCenter on a non-complete region: " + this.toString());

		final Location[] centered = this.getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		return new Location(primary.getWorld(),
				(primary.getX() + secondary.getX()) / 2,
				(primary.getY() + secondary.getY()) / 2,
				(primary.getZ() + secondary.getZ()) / 2);
	}

	/**
	 * 若已设置第一个位置则返回 true
	 *
	 * @return
	 */
	public final boolean hasPrimary() {
		return this.primary != null;
	}

	/**
	 * 若已设置第二个位置则返回 true
	 *
	 * @return
	 */
	public final boolean hasSecondary() {
		return this.secondary != null;
	}

	/**
	 * 返回给定位置是否等于第一个位置。
	 * 若此区域未设置第一个位置则返回 false。
	 *
	 * @param location
	 * @return
	 */
	public final boolean isPrimary(Location location) {
		return this.primary != null && Valid.locationEquals(this.primary, location);
	}

	/**
	 * 返回给定位置是否等于第二个位置。
	 * 若此区域未设置第二个位置则返回 false。
	 *
	 * @param location
	 * @return
	 */
	public final boolean isSecondary(Location location) {
		return this.secondary != null && Valid.locationEquals(this.secondary, location);
	}

	/**
	 * 返回第一个位置的副本
	 *
	 * @return 第一个位置
	 */
	public final Location getPrimary() {
		return this.primary == null ? null : this.primary.clone();
	}

	/**
	 * 返回第二个位置的副本
	 *
	 * @return 第二个位置
	 */
	public final Location getSecondary() {
		return this.secondary == null ? null : this.secondary.clone();
	}

	/**
	 * 统计此区域内的所有方块
	 *
	 * @return
	 */
	public final List<Block> getBlocks() {
		Valid.checkBoolean(this.isWhole(), "Cannot perform getBlocks on a non-complete region: " + this.toString());
		final Location[] centered = this.getCorrectedPoints();

		return BlockUtil.getBlocks(centered[0], centered[1]);
	}

	/**
	 * 返回表示长方体区域边界框的位置，
	 * 用于渲染粒子效果
	 *
	 * @return
	 */
	public final Set<Location> getBoundingBox() {
		Valid.checkBoolean(this.isWhole(), "Cannot perform getBoundingBox on a non-complete region: " + this.toString());

		return BlockUtil.getBoundingBox(this.primary, this.secondary);
	}

	/**
	 * 统计此区域内的所有实体
	 *
	 * @return
	 */
	public final List<Entity> getEntities() {
		Valid.checkBoolean(this.isWhole(), "Cannot perform getEntities on a non-complete region: " + this.toString());

		final List<Entity> found = new LinkedList<>();

		final Location[] centered = this.getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		final int xMin = (int) primary.getX() >> 4;
		final int xMax = (int) secondary.getX() >> 4;
		final int zMin = (int) primary.getZ() >> 4;
		final int zMax = (int) secondary.getZ() >> 4;

		for (int cx = xMin; cx <= xMax; ++cx)
			for (int cz = zMin; cz <= zMax; ++cz)
				for (final Entity entity : this.getWorld().getChunkAt(cx, cz).getEntities())
					if (entity.isValid() && entity.getLocation() != null && this.isWithin(entity.getLocation()))
						found.add(entity);

		return found;
	}

	/**
	 * 获取此区域所在的世界
	 *
	 * @return
	 */
	public final World getWorld() {
		if (!this.isWhole())
			return null;

		if (this.primary != null && this.secondary == null)
			return Bukkit.getWorld(this.primary.getWorld().getName());

		if (this.secondary != null && this.primary == null)
			return Bukkit.getWorld(this.secondary.getWorld().getName());

		Valid.checkBoolean(this.primary.getWorld().getName().equals(this.secondary.getWorld().getName()), "Worlds of this region not the same: " + this.primary.getWorld() + " != " + this.secondary.getWorld());
		return Bukkit.getWorld(this.primary.getWorld().getName());
	}

	/**
	 * 若给定点位于此区域内则返回 true
	 *
	 * @param location
	 * @return
	 */
	public final boolean isWithin(@NonNull final Location location) {
		Valid.checkBoolean(this.isWhole(), "Cannot perform isWithin on a non-complete region: " + this.toString());

		if (!location.getWorld().getName().equals(this.primary.getWorld().getName()))
			return false;

		final Location[] centered = this.getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		final int x = (int) location.getX();
		final int y = (int) location.getY();
		final int z = (int) location.getZ();

		return x >= primary.getX() && x <= secondary.getX()
				&& y >= primary.getY() && y <= secondary.getY()
				&& z >= primary.getZ() && z <= secondary.getZ();
	}

	/**
	 * 将玩家传送到区域中心
	 *
	 * @param player
	 */
	public void teleportToCenter(Player player) {
		Valid.checkNotNull(this.isWhole(), "Cannot call teleportToCenter() on a non-complete region: " + this.toString());

		final Location toTeleportLocation = this.getCenter().clone();
		final Location playerLocation = player.getLocation();

		toTeleportLocation.setYaw(playerLocation.getYaw());
		toTeleportLocation.setPitch(playerLocation.getPitch());

		player.teleport(this.getHighestLocation(toTeleportLocation));
	}

	/**
	 * 获取区域边界内给定 x、z 坐标处最高的非空气方块位置。
	 *
	 * @param location 用于查找最高方块位置的基准位置。只使用其 x 和 z 坐标，
	 *                 y 会被忽略并由此方法重新计算。
	 * @return 重新计算后的 Location 对象，指向垂直方向最高的非空气方块（再加一格高度）。
	 * @throws FoException 若区域边界未设置。
	 */
	public final Location getHighestLocation(Location location) {
		Valid.checkNotNull(this.isWhole(), "Cannot call getHighestLocation() on a non-complete region: " + this.toString());

		final int x = location.getBlockX();
		final int z = location.getBlockZ();

		final boolean sameHeight = this.getPrimary().getY() == this.getSecondary().getY();
		int y = (int) Math.max(this.getPrimary().getY(), this.getSecondary().getY()) - 1;

		if (sameHeight)
			y = y + 1;

		highestAvailableLookup:
		{
			for (; sameHeight ? y < location.getWorld().getMaxHeight() : y > 1; y = y + (sameHeight ? 1 : -1)) {
				final Block block = location.getWorld().getBlockAt(x, y, z);

				if (sameHeight) {
					if (CompMaterial.isAir(block)) {
						location.setY(y - 1);

						break highestAvailableLookup;
					}

				} else {
					if (!CompMaterial.isAir(block)) {
						location.setY(y);

						break highestAvailableLookup;
					}
				}
			}

			location.setY(y);
		}

		return location.add(0, 1, 0);
	}

	/**
	 * 若给定位置的 X 和 Z 坐标（不考虑高度）位于此区域内
	 * 则返回 true。
	 *
	 * @param location
	 * @return
	 */
	public final boolean isWithinXZ(@NonNull final Location location) {
		Valid.checkBoolean(this.isWhole(), "Cannot perform isWithinXZ on a non-complete region: " + this.toString());

		if (!location.getWorld().getName().equals(this.primary.getWorld().getName()))
			return false;

		final Location[] centered = this.getCorrectedPoints();
		final Location primary = centered[0];
		final Location secondary = centered[1];

		final int x = (int) location.getX();
		final int z = (int) location.getZ();

		return x >= primary.getX() && x <= secondary.getX()
				&& z >= primary.getZ() && z <= secondary.getZ();
	}

	/**
	 * 若区域的两个点都已设置则返回 true
	 *
	 * @return
	 */
	public final boolean isWhole() {
		return this.primary != null && this.secondary != null;
	}

	/**
	 * 设置区域的第一个点
	 *
	 * @param primary
	 */
	public final void setPrimary(final Location primary) {
		this.primary = primary;
	}

	/**
	 * 设置区域的第二个点
	 *
	 * @param secondary
	 */
	public final void setSecondary(final Location secondary) {
		this.secondary = secondary;
	}

	/**
	 * 根据点击类型设置位置。LEFT = 第一个点，RIGHT = 第二个点
	 *
	 * @param location
	 * @param click
	 */
	public final void setLocation(Location location, ClickType click) {
		this.setLocation(location, click, false);
	}

	/**
	 * 设置第一个和/或第二个位置点（若它们不为
	 * null）。
	 *
	 * @param primary
	 * @param secondary
	 */
	public final void updateLocation(@Nullable Location primary, @Nullable Location secondary) {
		if (primary != null)
			this.setPrimary(primary);

		if (secondary != null)
			this.setSecondary(secondary);
	}

	/**
	 * 根据点击类型设置位置。LEFT = 第一个点，RIGHT = 第二个点
	 *
	 * 如果给定位置点已存在，则将其移除；如果
	 * 不存在，则放置它，从而形成开/关切换效果。
	 *
	 * @param location
	 * @param click
	 * @return 若设置了位置则为 true，若被移除（或 location 参数为 null）则为 null
	 */
	public final boolean toggleLocation(Location location, ClickType click) {
		return this.setLocation(location, click, true);
	}

	/*
	 * Helper method to set location from click type, removing old one if toggle mode
	 */
	private boolean setLocation(Location location, ClickType click, boolean toggle) {
		final boolean isPrimary = click == ClickType.LEFT;

		if (isPrimary) {
			if (location == null || (this.hasPrimary() && this.isPrimary(location) && toggle)) {
				this.setPrimary(null);

				return false;

			} else {
				this.setPrimary(location);

				return true;
			}

		} else if (location == null || (this.hasSecondary() && this.isSecondary(location) && toggle)) {
			this.setSecondary(null);

			return false;

		} else {
			this.setSecondary(location);

			return true;
		}
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Region) {
			final Region otherRegion = (Region) obj;

			if ((otherRegion.name != null && this.name == null) || (otherRegion.name == null && this.name != null))
				return false;

			if ((otherRegion.name != null && !otherRegion.name.equals(this.name)) || (otherRegion.name != null && !this.name.equals(otherRegion.name)))
				return false;

			return Valid.locationEquals(otherRegion.getPrimary(), this.primary) && Valid.locationEquals(otherRegion.getSecondary(), this.secondary);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.primary, this.secondary);
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName() + "{name=" + this.name + ",location=" + Common.shortLocation(this.primary) + " - " + Common.shortLocation(this.secondary) + "}";
	}

	@Override
	public Region clone() {
		return new Region(
				this.name != null ? new String(this.name) : null,
				this.primary != null ? this.primary.clone() : null,
				this.secondary != null ? this.secondary.clone() : null);
	}

	/**
	 * 将区域数据保存为映射，可存入 yaml 或 json 文件
	 */
	@Override
	public final SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIfExist("Name", this.name);
		map.putIfExist("Primary", this.primary);
		map.putIfExist("Secondary", this.secondary);

		return map;
	}

	/**
	 * 如果 yaml/json 文件中保存的映射包含 Primary 和 Secondary 键，则将其转换为区域
	 *
	 * @param map
	 * @return
	 */
	public static Region deserialize(final SerializedMap map) {
		Valid.checkBoolean(map.containsKey("Primary") && map.containsKey("Secondary"), "The region must have Primary and a Secondary location");

		final String name = map.getString("Name");
		final Location prim = map.getLocation("Primary");
		final Location sec = map.getLocation("Secondary");

		return new Region(name, prim, sec);
	}
}