package top.brmc.devlib.menu.model;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import top.brmc.devlib.Common;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 一个基于 Bukkit API 的库，可根据名称、base64 字符串
 * 和材质 URL 创建玩家头颅。
 * <p>
 * 不使用任何 NMS 代码，应能兼容所有版本。
 *
 * @author Dean B on 12/28/2016.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkullCreator {

	// some reflection stuff to be used when setting a skull's profile
	private static Field blockProfileField;
	private static Method metaSetProfileMethod;
	private static Field metaProfileField;

	/**
	 * 创建玩家头颅，应同时兼容旧版和新版 Bukkit API。
	 *
	 * @return
	 */
	public static ItemStack createSkull() {
		try {
			return new ItemStack(Material.valueOf("PLAYER_HEAD"));

		} catch (final IllegalArgumentException e) {
			return new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (byte) 3);
		}
	}

	/**
	 * 根据玩家名称创建带有对应皮肤的玩家头颅物品。
	 *
	 * @param name 玩家名称。
	 * @return 该玩家的头颅。
	 */
	public static ItemStack itemFromName(final String name) {
		return itemWithName(createSkull(), name);
	}

	/**
	 * 根据玩家 UUID 创建带有对应皮肤的玩家头颅物品。
	 *
	 * @param id 玩家 UUID。
	 * @return 该玩家的头颅。
	 */
	public static ItemStack itemFromUuid(final UUID id) {
		return itemWithUuid(createSkull(), id);
	}

	/**
	 * 创建使用 Mojang URL 所指皮肤的玩家头颅物品。
	 *
	 * @param url Mojang URL。
	 * @return 该玩家的头颅。
	 */
	public static ItemStack itemFromUrl(final String url) {
		return itemWithUrl(createSkull(), url);
	}

	/**
	 * 根据 base64 字符串创建带有对应皮肤的玩家头颅物品。
	 *
	 * @param base64 Mojang URL。
	 * @return 该玩家的头颅。
	 */
	public static ItemStack itemFromBase64(final String base64) {
		return itemWithBase64(createSkull(), base64);
	}

	/**
	 * 修改头颅，使其使用给定名称玩家的皮肤。
	 *
	 * @param item 要应用名称的物品，必须是玩家头颅。
	 * @param name 玩家名称。
	 * @return 该玩家的头颅。
	 */
	public static ItemStack itemWithName(@NonNull final ItemStack item, @NonNull final String name) {
		final SkullMeta meta = (SkullMeta) item.getItemMeta();

		meta.setOwner(name);
		item.setItemMeta(meta);

		return item;
	}

	/**
	 * 修改头颅，使其使用给定 UUID 玩家的皮肤。
	 *
	 * @param item 要应用名称的物品，必须是玩家头颅。
	 * @param id   玩家 UUID。
	 * @return 该玩家的头颅。
	 */
	public static ItemStack itemWithUuid(@NonNull final ItemStack item, @NonNull final UUID id) {
		final SkullMeta meta = (SkullMeta) item.getItemMeta();

		try {
			meta.setOwningPlayer(Remain.getOfflinePlayerByUUID(id));

		} catch (final Throwable t) {
			meta.setOwner(Remain.getOfflinePlayerByUUID(id).getName());
		}

		item.setItemMeta(meta);

		return item;
	}

	/**
	 * 修改头颅，使其使用给定 Mojang URL 的皮肤。
	 *
	 * @param item 要应用皮肤的物品，必须是玩家头颅。
	 * @param url  Mojang 皮肤的 URL。
	 * @return 与该 URL 关联的头颅。
	 */
	public static ItemStack itemWithUrl(@NonNull final ItemStack item, @NonNull final String url) {
		return itemWithBase64(item, urlToBase64(url));
	}

	/**
	 * 修改头颅，使其使用给定 base64 字符串对应的皮肤。
	 *
	 * @param item   要写入 base64 的 ItemStack，必须是玩家头颅。
	 * @param base64 包含材质的 base64 字符串。
	 * @return 带有自定义材质的头颅。
	 */
	public static ItemStack itemWithBase64(@NonNull final ItemStack item, @NonNull final String base64) {
		if (!(item.getItemMeta() instanceof SkullMeta))
			return null;

		final SkullMeta meta = (SkullMeta) item.getItemMeta();

		mutateItemMeta(meta, base64);

		item.setItemMeta(meta);

		return item;
	}

	/**
	 * 修改头颅元数据，使其使用给定 Mojang URL 的皮肤。
	 *
	 * @param meta
	 * @param url  Mojang 皮肤的 URL。
	 * @return
	 */
	public static SkullMeta metaWithUrl(@NonNull final SkullMeta meta, @NonNull final String url) {
		final String base64 = urlToBase64(url);

		mutateItemMeta(meta, base64);

		return meta;
	}

	/**
	 * 将方块设置为给定 UUID 对应的头颅。
	 *
	 * @param block 要设置的方块。
	 * @param id    要设置成的玩家。
	 */
	public static void blockWithUuid(@NonNull final Block block, @NonNull final UUID id) {
		setToSkull(block);

		final Skull state = (Skull) block.getState();
		state.setRawData((byte) 0x1);

		try {
			state.setOwningPlayer(Remain.getOfflinePlayerByUUID(id));

		} catch (final Throwable t) {
			state.setOwner(Remain.getOfflinePlayerByUUID(id).getName());
		}

		state.update(false, false);
	}

	/**
	 * 将方块设置为使用给定 Mojang URL 皮肤的头颅。
	 *
	 * @param block 要设置的方块。
	 * @param url   要使用的 Mojang URL。
	 */
	public static void blockWithUrl(@NonNull final Block block, @NonNull final String url) {
		blockWithBase64(block, urlToBase64(url));
	}

	/**
	 * 将方块设置为使用给定 base64 字符串皮肤的头颅。
	 *
	 * @param block  要设置的方块。
	 * @param base64 要使用的 base64。
	 */
	public static void blockWithBase64(@NonNull final Block block, @NonNull final String base64) {
		setToSkull(block);

		final Skull state = (Skull) block.getState();
		mutateBlockState(state, base64);

		state.update(false, false);
	}

	private static void setToSkull(final Block block) {

		try {
			block.setType(Material.valueOf("PLAYER_HEAD"), false);

		} catch (final IllegalArgumentException e) {
			block.setType(Material.valueOf("SKULL"), false);
			final Skull state = (Skull) block.getState();
			state.setSkullType(SkullType.PLAYER);
			state.setRawData((byte) 0x1);
			state.update(false, false);
		}
	}

	private static String urlToBase64(final String url) {
		Valid.checkBoolean(url.startsWith("http://") || url.startsWith("https://"), "URL for skull must start with http:// or https://, given: " + url);

		final URI actualUrl;
		try {
			actualUrl = new URI(url);
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
		final String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + actualUrl.toString() + "\"}}}";
		return Base64.getEncoder().encodeToString(toEncode.getBytes());
	}

	private static Object makeProfile(final String b64) {
		// random uuid based on the b64 string
		final UUID id = new UUID(
				b64.substring(b64.length() - 20).hashCode(),
				b64.substring(b64.length() - 10).hashCode());

		try {
			Class<?> gameProfileClass = ReflectionUtil.lookupClass("com.mojang.authlib.GameProfile");
			Class<?> propertyClass = ReflectionUtil.lookupClass("com.mojang.authlib.properties.Property");

			Object fakeProfileInstance = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(id, "aaaaa");
			Object propertyInstance = propertyClass.getConstructor(String.class, String.class).newInstance("textures", b64);

			Method getProperties = fakeProfileInstance.getClass().getMethod("getProperties");
			Object propertyMap = getProperties.invoke(fakeProfileInstance);

			Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
			putMethod.invoke(propertyMap,"textures", propertyInstance);

			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_21) && MinecraftVersion.getSubversion() >= 1) {
				// For Minecraft 1.21.1 and later, create a ResolvableProfile
				Class<?> resolvableProfileClass = ReflectionUtil.lookupClass("net.minecraft.world.item.component.ResolvableProfile");
				Object fakeResolvableProfileInstance = resolvableProfileClass.getConstructor(gameProfileClass).newInstance(fakeProfileInstance);

				return fakeResolvableProfileInstance;
			} else {
				// For 1.21 and older versions, return the GameProfile instance
				return fakeProfileInstance;
			}

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex);

			return null;
		}
	}

	/**
	 * 修改头颅方块
	 *
	 * @param block
	 * @param b64
	 */
	public static void mutateBlockState(final Skull block, final String b64) {
		try {
			if (blockProfileField == null) {
				blockProfileField = block.getClass().getDeclaredField("profile");
				blockProfileField.setAccessible(true);
			}
			blockProfileField.set(block, makeProfile(b64));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static void mutateItemMeta(final SkullMeta meta, final String b64) {
		try {
			if (metaSetProfileMethod == null) {
				metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", ReflectionUtil.lookupClass("com.mojang.authlib.GameProfile"));
				metaSetProfileMethod.setAccessible(true);
			}
			metaSetProfileMethod.invoke(meta, makeProfile(b64));
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			// if in an older API where there is no setProfile method,
			// we set the profile field directly.
			try {
				if (metaProfileField == null) {
					metaProfileField = meta.getClass().getDeclaredField("profile");
					metaProfileField.setAccessible(true);
				}
				metaProfileField.set(meta, makeProfile(b64));

			} catch (NoSuchFieldException | IllegalAccessException ex2) {
				ex2.printStackTrace();
			}
		}
	}

	/**
	 * 将头颅旋转到指定的方块朝向
	 *
	 * @param skull
	 * @param blockFace
	 */
	public static void rotateSkull(final Skull skull, final BlockFace blockFace) {
		skull.setRotation(blockFace);
		skull.update(true);
	}
}