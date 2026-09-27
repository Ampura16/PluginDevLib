package top.brmc.devlib.remain;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;
import top.brmc.devlib.ChatUtil;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.ValidCore;
import top.brmc.devlib.remain.nbt.NBTEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 用于为某些 Bukkit 类（如物品或实体）
 * 应用“属性”的便捷类
 * <p>
 * 它本质上是调用那些并非所有 MC 版本都提供的方法，
 * 从而避免你的插件出错。
 * <p>
 * 如果这些方法不可用，则不会应用任何内容。
 */
@RequiredArgsConstructor
public enum CompProperty {

	// ItemMeta
	/**
	 * ItemMeta 的不可破坏属性
	 */
	UNBREAKABLE(ItemMeta.class, boolean.class),

	// Entity
	/**
	 * 实体发光属性，目前仅支持白色
	 */
	GLOWING(Entity.class, boolean.class),

	/**
	 * 实体 AI 寻路属性
	 */
	AI(Entity.class, boolean.class),

	/**
	 * 实体重力属性
	 */
	GRAVITY(Entity.class, boolean.class),

	/**
	 * 静音实体属性，控制实体是否发出声音
	 */
	SILENT(Entity.class, boolean.class),

	/**
	 * 实体无敌模式属性
	 */
	INVULNERABLE(Entity.class, boolean.class),

	/**
	 * 设置此实体是否会与其他实体发生碰撞。
	 */
	COLLIDABLE(LivingEntity.class, boolean.class);

	/**
	 * 此枚举适用的类，例如 {@link Entity}
	 */
	@Getter
	private final Class<?> requiredClass;

	/**
	 * “setter”字段的类型，例如 setSilent 方法接受 boolean
	 */
	private final Class<?> setterMethodType;

	private final Map<Class<?>, Boolean> isAvailable = new HashMap<>();
	private final Map<Class<?>, Method> cachedMethods = new HashMap<>();

	/**
	 * 将该属性应用到实体。类必须与此属性的 {@link #getRequiredClass()} 兼容。
	 * <p>
	 * 示例：SILENT.apply(myZombieEntity, true)
	 *
	 * @param instance
	 * @param key
	 */
	public void apply(final Object instance, final Object key) {
		ValidCore.checkNotNull(instance, "instance is null!");
		ValidCore.checkBoolean(this.requiredClass.isAssignableFrom(instance.getClass()), this + " accepts " + this.requiredClass.getSimpleName() + ", not " + instance.getClass().getSimpleName());

		final Method method = this.getMethod(instance.getClass());

		if (method == null)
			this.applyLegacy(instance, key);

		else
			try {
				ReflectionUtil.invoke(method, instance, key);

			} catch (final Throwable t) {
				if (MinecraftVersion.olderThan(V.values()[0]))
					this.applyLegacy(instance, key);
				else
					// Print error when on latest MC version
					t.printStackTrace();
			}
	}

	private void applyLegacy(@NonNull final Object instance, @NonNull final Object key) {
		ValidCore.checkBoolean(Bukkit.isPrimaryThread(), "Cannot call CompProperty." + this + ".applyLegacy(" + instance.getClass().getSimpleName() + ") async on " + instance);

		if (instance instanceof Entity) {
			final NBTEntity nbtEntity = new NBTEntity((Entity) instance);
			final boolean has = Boolean.parseBoolean(key.toString());

			if (this == INVULNERABLE)
				nbtEntity.setInteger("Invulnerable", has ? 1 : 0);

			else if (this == AI)
				nbtEntity.setInteger("NoAI", has ? 0 : 1);

			else if (this == CompProperty.GRAVITY)
				nbtEntity.setInteger("NoGravity", has ? 0 : 1);
		}

		if (instance instanceof ItemMeta)
			if (this == UNBREAKABLE)
				try {
					final boolean has = Boolean.parseBoolean(key.toString());

					final Method spigotMethod = instance.getClass().getMethod("spigot");
					spigotMethod.setAccessible(true);

					final Object spigot = spigotMethod.invoke(instance);

					final Method setUnbreakable = spigot.getClass().getMethod("setUnbreakable", boolean.class);
					setUnbreakable.setAccessible(true);

					setUnbreakable.invoke(spigot, has);

				} catch (final Throwable t) {
					if (MinecraftVersion.atLeast(V.v1_8))
						t.printStackTrace();
				}
	}

	/**
	 * 此属性能否在本服务器上用于给定类？类必须与 {@link #getRequiredClass()} 兼容
	 * <p>
	 * 例如类为 {@link Entity}
	 *
	 * @param clazz
	 * @return
	 */
	public boolean isAvailable(final Class<?> clazz) {

		if (this.isAvailable.containsKey(clazz))
			return this.isAvailable.get(clazz);

		return this.getMethod(clazz) != null;
	}

	// Automatically returns the correct getter or setter method for class
	private Method getMethod(final Class<?> clazz) {

		if (this.isAvailable.containsKey(clazz) && !this.isAvailable.get(clazz))
			return null;

		Method method = this.cachedMethods.get(clazz);

		if (method == null)
			try {
				method = clazz.getMethod("set" + (this.toString().equals("AI") ? "AI" : ChatUtil.capitalize(this.toString().toLowerCase())), this.setterMethodType);
				method.setAccessible(true);

				this.isAvailable.put(clazz, true);
				this.cachedMethods.put(clazz, method);

			} catch (final Throwable t) {
				this.isAvailable.put(clazz, false);

				return null;
			}

		return method;
	}
}