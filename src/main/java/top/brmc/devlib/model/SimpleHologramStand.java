package top.brmc.devlib.model;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Consumer;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.remain.CompMaterial;

import lombok.Getter;

/**
 *
 */
@Getter
public class SimpleHologramStand extends SimpleHologram {

	/**
	 * 此全息图所显示的物品或材质
	 */
	private final Object itemOrMaterial;

	/**
	 * 此盔甲架是否为小型？
	 */
	private boolean small;

	/**
	 * 此盔甲架是否发光？
	 */
	private boolean glowing;

	/**
	 * 使用盔甲架创建一个显示给定物品堆的简单全息图
	 *
	 * @param spawnLocation
	 * @param item
	 */
	public SimpleHologramStand(final Location spawnLocation, final ItemStack item) {
		super(spawnLocation);

		this.itemOrMaterial = item;
	}

	/**
	 * 使用盔甲架创建一个显示给定材质的简单全息图
	 *
	 * @param spawnLocation
	 * @param material
	 */
	public SimpleHologramStand(final Location spawnLocation, final CompMaterial material) {
		super(spawnLocation);

		this.itemOrMaterial = material;
	}

	/**
	 * @see top.brmc.devlib.model.SimpleHologram#createEntity()
	 */
	@Override
	protected final Entity createEntity() {

		final ItemCreator item;

		if (this.itemOrMaterial instanceof ItemStack)
			item = ItemCreator.of((ItemStack) this.itemOrMaterial);
		else
			item = ItemCreator.of((CompMaterial) this.itemOrMaterial);

		if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_11)) {
			final Consumer<ArmorStand> consumer = armorStand -> {
				armorStand.setGravity(false);
				armorStand.setHelmet(item.glow(this.glowing).make());
				armorStand.setVisible(false);
				armorStand.setSmall(this.small);
			};

			return this.getLastTeleportLocation().getWorld().spawn(this.getLastTeleportLocation(), ArmorStand.class, consumer);
		} else {
			final ArmorStand armorStand = this.getLastTeleportLocation().getWorld().spawn(this.getLastTeleportLocation(), ArmorStand.class);

			armorStand.setGravity(false);
			armorStand.setHelmet(item.glow(this.glowing).make());
			armorStand.setVisible(false);
			armorStand.setSmall(this.small);

			return armorStand;
		}
	}

	/**
	 * @param glowing
	 * @return
	 */
	public final SimpleHologram setGlowing(final boolean glowing) {
		this.glowing = glowing;

		return this;
	}

	/**
	 * @param small 要设置的小型状态
	 * @return
	 */
	public final SimpleHologram setSmall(final boolean small) {
		this.small = small;

		return this;
	}
}