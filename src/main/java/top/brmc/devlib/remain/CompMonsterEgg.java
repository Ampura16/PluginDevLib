package top.brmc.devlib.remain;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.ValidCore;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.nbt.NBTCompound;
import top.brmc.devlib.remain.nbt.NBTItem;

import lombok.NonNull;

/**
 * 用于操作刷怪蛋的工具类
 */
public final class CompMonsterEgg {

	/**
	 * 我们用来标记自己刷怪蛋的通用标签
	 */
	private static final String TAG = SimplePlugin.getInstance().getName() + "_NbtTag";

	// Prevent new instance, always call static methods
	private CompMonsterEgg() {
	}

	/**
	 * 制作指定类型的刷怪蛋。
	 * @deprecated 为即将到来的 v7 迁移，请改用 {@link #toItemStack(EntityType)}。
	 *
	 * @param type
	 * @return 制作完成的刷怪蛋
	 */
	@Deprecated
	public static ItemStack makeEgg(final EntityType type) {
		return toItemStack(type);
	}

	/**
	 * 制作指定类型的刷怪蛋。
	 *
	 * @param type
	 * @return 制作完成的刷怪蛋
	 */
	public static ItemStack toItemStack(final EntityType type) {
		return toItemStack(type, 1);
	}

	/**
	 * 制作指定数量的刷怪蛋。
	 * @deprecated 为即将到来的 v7 迁移，请改用 {@link #toItemStack(EntityType, int)}。
	 *
	 * @param type
	 * @param count
	 * @return 制作完成的刷怪蛋
	 */
	@Deprecated
	public static ItemStack makeEgg(@NonNull final EntityType type, final int count) {
		return toItemStack(type, count);
	}

	/**
	 * 制作指定数量的刷怪蛋。
	 *
	 * @param type
	 * @param count
	 * @return 制作完成的刷怪蛋
	 */
	public static ItemStack toItemStack(@NonNull final EntityType type, final int count) {
		CompMaterial material = CompEntityType.getSpawnEgg(type);

		if (material == null && MinecraftVersion.atLeast(V.v1_13))
			material = CompMaterial.SHEEP_SPAWN_EGG;

		ItemStack itemStack = new ItemStack(material != null ? material.getMaterial() : Material.valueOf("MONSTER_EGG"), count);

		// For older MC
		if (itemStack.getType().toString().equals("MONSTER_EGG"))
			itemStack = setEntity(itemStack, type);

		return itemStack;
	}

	/**
	 * 从 {@link ItemStack} 中识别 {@link EntityType}
	 *
	 * @deprecated 为即将到来的 v7 迁移，请改用 {@link #lookupEntity(ItemStack)}。
	 * @param item
	 * @return 实体类型；未找到时返回 unknown 或 error
	 */
	@Deprecated
	public static EntityType getEntity(@NonNull final ItemStack item) {
		return lookupEntity(item);
	}

	/**
	 * 从 {@link ItemStack} 中识别 {@link EntityType}
	 *
	 * @param item
	 * @return 实体类型；未找到时返回 unknown 或 error
	 */
	public static EntityType lookupEntity(@NonNull final ItemStack item) {
		ValidCore.checkBoolean(CompMaterial.isMonsterEgg(item.getType()), "Item must be a monster egg not " + item);
		EntityType type = null;

		if (MinecraftVersion.atLeast(V.v1_13))
			type = CompEntityType.fromSpawnEggMaterial(CompMaterial.fromItem(item));

		if (type == null && Remain.hasSpawnEggMeta())
			type = lookupTypeByMeta(item);

		if (type == null && MinecraftVersion.olderThan(V.v1_13))
			type = lookupTypeByData(item);

		if (type == null)
			type = lookupTypeByNbt(item);

		return type != null ? type : CompEntityType.UNKNOWN;
	}

	private static EntityType lookupTypeByMeta(final ItemStack item) {
		final ItemMeta meta = item.getItemMeta();

		return item.hasItemMeta() && meta instanceof SpawnEggMeta ? ((SpawnEggMeta) meta).getSpawnedType() : null;
	}

	private static EntityType lookupTypeByData(final ItemStack item) {
		EntityType type = readItemStackNBTEntity(item);

		if (type == null) {
			if (item.getDurability() != 0)
				type = EntityType.fromId(item.getDurability());

			if (type == null && item.getData().getData() != 0)
				type = EntityType.fromId(item.getData().getData());
		}

		return type;
	}

	private static EntityType readItemStackNBTEntity(final ItemStack item) {
		ValidCore.checkNotNull(item, "Reading entity got null item");

		final NBTItem nbt = new NBTItem(item);
		final String type = nbt.hasKey(TAG) ? nbt.getCompound(TAG).getString("entity") : null;

		return type != null && !type.isEmpty() ? CompEntityType.fromName(type) : null;
	}

	private static EntityType lookupTypeByNbt(@NonNull final ItemStack item) {
		try {
			final Class<?> classNMSItemstack = Remain.getNMSClass("ItemStack", "net.minecraft.world.item.ItemStack");
			final Object stack = Remain.asNMSCopy(item);
			final Object tagCompound = classNMSItemstack.getMethod("getTag").invoke(stack);

			if (tagCompound == null)
				return null;

			ValidCore.checkNotNull(tagCompound, "Spawn egg lacks tag compound: " + item);

			final Method tagGetCompound = tagCompound.getClass().getMethod("getCompound", String.class);
			final Object entityTag = tagGetCompound.invoke(tagCompound, "EntityTag");

			final Method tagGetString = entityTag.getClass().getMethod("getString", String.class);
			String idString = (String) tagGetString.invoke(entityTag, "id");

			if (MinecraftVersion.atLeast(V.v1_11) && idString.startsWith("minecraft:"))
				idString = idString.split("minecraft:")[1];

			return CompEntityType.fromName(idString);

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	/**
	 * 向已有的刷怪蛋物品堆中写入元数据。
	 *
	 * 如果该实体类型没有对应的刷怪蛋，则设为羊的刷怪蛋。
	 *
	 * @param item
	 * @param type
	 * @return 物品堆
	 */
	public static ItemStack setEntity(@NonNull ItemStack item, final EntityType type) {
		ValidCore.checkBoolean(CompMaterial.isMonsterEgg(item.getType()), "Item must be a monster egg not " + item);

		if (MinecraftVersion.atLeast(V.v1_13)) {
			final CompMaterial material = CompEntityType.getSpawnEgg(type);
			item.setType(material == null ? CompMaterial.SHEEP_SPAWN_EGG.getMaterial() : material.getMaterial());

			return item;
		}

		if (Remain.hasSpawnEggMeta())
			item = setTypeByMeta(item, type);

		else
			item = setTypeByData(item, type);

		return item;
	}

	private static ItemStack setTypeByMeta(final ItemStack item, final EntityType type) {
		final SpawnEggMeta meta = (SpawnEggMeta) item.getItemMeta();

		meta.setSpawnedType(type);
		item.setItemMeta(meta);

		return item;
	}

	private static ItemStack setTypeByData(final ItemStack item, final EntityType type) {
		final Integer id = CompEntityType.getId(type);

		if (id != null) {
			item.setDurability(id.shortValue());
			item.getData().setData(id.byteValue());
		}

		return writeEntity0(item, type);
	}

	private static ItemStack writeEntity0(final ItemStack item, final EntityType type) {
		ValidCore.checkNotNull(item, "setting nbt got null item");
		ValidCore.checkNotNull(type, "setting nbt got null entity");

		final NBTItem nbt = new NBTItem(item);
		final NBTCompound tag = nbt.addCompound(TAG);

		tag.setString("entity", type.toString());
		return nbt.getItem();
	}
}