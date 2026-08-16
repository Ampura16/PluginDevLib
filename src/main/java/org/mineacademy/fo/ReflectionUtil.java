package org.mineacademy.fo;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * 各种反射方法的工具类
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtil {

	/**
	 * NMS 的完整包名
	 */
	public static final String NMS = "net.minecraft.server";

	/**
	 * Craftbukkit 的包名
	 */
	public static final String CRAFTBUKKIT = "org.bukkit.craftbukkit";

	/**
	 * 优雅失败的兼容枚举类，以便
	 * 插件在那些类型不存在的旧 MC 版本上也能加载，
	 * 即使它们出现在插件默认配置文件中
	 */
	private static final Map<Class<? extends Enum<?>>, Map<String, V>> legacyEnumTypes;

	/**
	 * 反射使用简单缓存以获得最快性能
	 */
	private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, ReflectionData<?>> reflectionDataCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Method[]> methodCache = new ConcurrentHashMap<>();
	private static final Collection<String> classNameGuard = ConcurrentHashMap.newKeySet();

	/**
	 * 将原生 <code>Class</code> 映射到对应的包装 <code>Class</code>。
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();

	/**
	 * 将包装 <code>Class</code> 映射到对应的原生类型。
	 */
	private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<>();

	/**
	 * 为旧 MC 版本自动查找类（例如 oldName 填 EntityPlayer，
	 * 我们会自动找到正确的 NMS 导入）；若使用 MC 1.17+ 则填写
	 * 完整类路径，例如 net.minecraft.server.level.EntityPlayer，我们会改用它。
	 *
	 * @param oldName
	 * @param fullName1_17
	 * @return
	 */
	public static Class<?> getNMSClass(String oldName, String fullName1_17) {
		return MinecraftVersion.atLeast(V.v1_17) ? lookupClass(fullName1_17) : getNMSClass(oldName);
	}

	/**
	 * 在 net.minecraft.server 包中查找类，自动添加
	 * 版本
	 *
	 * @deprecated Minecraft 1.17+ 的路径名不同，
	 *             请改用 {@link #getNMSClass(String, String)}
	 *
	 * @param name
	 * @return
	 */
	@Deprecated
	public static Class<?> getNMSClass(final String name) {
		String version = Remain.getNmsVersion();

		if (!version.isEmpty())
			version += ".";

		return ReflectionUtil.lookupClass(NMS + "." + version + name);
	}

	/**
	 * 在 org.bukkit.craftbukkit 包中查找类，自动添加
	 * 版本
	 *
	 * @param name
	 * @return
	 */
	public static Class<?> getOBCClass(final String name) {
		String version = Remain.getNmsVersion();

		if (!version.isEmpty())
			version += ".";

		return ReflectionUtil.lookupClass(CRAFTBUKKIT + "." + version + name);
	}

	/**
	 * 返回给定 NMS 类名（如 EntityZombie）的构造器
	 *
	 * @param nmsClassPath
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructorNMS(@NonNull final String nmsClassPath, final Class<?>... params) {
		return getConstructor(getNMSClass(nmsClassPath), params);
	}

	/**
	 * 返回给定全限定类路径的构造器，例如
	 * org.mineacademy.boss.BossPlugin
	 *
	 * @param classPath
	 * @param params
	 * @return
	 */
	public static Constructor<?> getConstructor(@NonNull final String classPath, final Class<?>... params) {
		final Class<?> clazz = lookupClass(classPath);

		return getConstructor(clazz, params);
	}

	/**
	 * 返回给定类的构造器
	 * 
	 * @param <T> 
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static <T> Constructor<T> getConstructor(@NonNull final Class<T> clazz, final Class<?>... params) {
		try {
			if (reflectionDataCache.containsKey(clazz))
				return (Constructor<T>) reflectionDataCache.get(clazz).getConstructor(params);

			Constructor<T> constructor;

			try {
				constructor = clazz.getConstructor(params);

			} catch (final NoSuchMethodException err) {
				constructor = clazz.getDeclaredConstructor(params);
			}

			constructor.setAccessible(true);

			return constructor;

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not get constructor of " + clazz + " with parameters " + Common.join(params));
		}
	}

	/**
	 * 获取字段内容
	 *
	 * @param <T>
	 * @param instance
	 * @param field
	 * @return
	 */
	public static <T> T getFieldContent(final Object instance, final String field) {
		return getFieldContent(instance.getClass(), field, instance);
	}

	/**
	 * 获取字段内容
	 *
	 * @param <T>
	 * @param clazz
	 * @param field
	 * @param instance
	 * @return
	 */
	public static <T> T getFieldContent(Class<?> clazz, final String field, final Object instance) {
		final String originalClassName = new String(clazz.getName());

		do
			// note: getDeclaredFields() fails if any of the fields are classes that cannot be loaded
			for (final Field f : clazz.getDeclaredFields())
				if (f.getName().equals(field))
					return (T) getFieldContent(f, instance);
		while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		throw new ReflectionException("No such field " + field + " in " + originalClassName + " or its superclasses");
	}

	/**
	 * 获取字段内容
	 *
	 * @param field
	 * @param instance
	 * @return
	 */
	public static Object getFieldContent(final Field field, final Object instance) {
		try {
			field.setAccessible(true);

			return field.get(instance);

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not get field " + field.getName() + " in instance " + (instance != null ? instance : field).getClass().getSimpleName());
		}
	}

	/**
	 * 获取类及其所有超类的字段
	 *
	 * @param clazz
	 * @return
	 */
	public static Field[] getAllFields(@NonNull Class<?> clazz) {
		final List<Field> list = new ArrayList<>();

		try {
			do
				list.addAll(Arrays.asList(clazz.getDeclaredFields()));
			while (!(clazz = clazz.getSuperclass()).isAssignableFrom(Object.class));

		} catch (final NullPointerException ex) {
			// Pass through - such as interfaces or object itself throw this
		}

		return list.toArray(new Field[0]);
	}

	/**
	 * 按名称获取类中的声明字段
	 *
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getDeclaredField(final Class<?> clazz, final String fieldName) {
		try {

			if (reflectionDataCache.containsKey(clazz))
				return reflectionDataCache.get(clazz).getDeclaredField(fieldName);

			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);

			return field;

		} catch (final ReflectiveOperationException ex) {
			Remain.sneaky(ex);
		}

		return null;
	}

	/**
	 * 将声明字段设为给定值
	 *
	 * @param instance
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setDeclaredField(@NonNull final Object instance, final String fieldName, final Object fieldValue) {
		final Field field = getDeclaredField(instance.getClass(), fieldName);

		try {
			field.set(instance, fieldValue);

		} catch (final ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取静态字段内容的便捷方法。
	 *
	 * @param <T>
	 * @param clazz
	 * @param field
	 * @return
	 */
	public static <T> T getStaticFieldContent(@NonNull final Class<?> clazz, final String field) {
		return getFieldContent(clazz, field, null);
	}

	/**
	 * 将静态字段设为给定值
	 *
	 * @param clazz
	 * @param fieldName
	 * @param fieldValue
	 */
	public static void setStaticField(@NonNull final Class<?> clazz, final String fieldName, final Object fieldValue) {
		try {
			final Field field = getDeclaredField(clazz, fieldName);

			field.set(null, fieldValue);

		} catch (final Throwable t) {
			throw new FoException(t, "Could not set " + fieldName + " in " + clazz + " to " + fieldValue);
		}
	}

	/**
	 * 获取类方法
	 *
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 */
	public static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... args) {
		try {
			final Method method = clazz.getMethod(methodName, args);
			method.setAccessible(true);
			return method;
		} catch (final NoSuchMethodException e) {
		}

		final Method[] methods = methodCache.computeIfAbsent(clazz, k -> clazz.getMethods());
		for (final Method method : methods)
			if (method.getName().equals(methodName) && isClassListEqual(args, method.getParameterTypes())) {
				method.setAccessible(true);

				return method;
			}

		return null;
	}

	// Compares class lists
	private static boolean isClassListEqual(final Class<?>[] first, final Class<?>[] second) {
		if (first.length != second.length)
			return false;

		for (int i = 0; i < first.length; i++)
			if (first[i] != second[i])
				return false;

		return true;
	}

	/**
	 * 获取类方法
	 *
	 * @param clazz
	 * @param methodName
	 * @return
	 */
	public static Method getMethod(final Class<?> clazz, final String methodName) {
		for (final Method method : clazz.getMethods())
			if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
				method.setAccessible(true);

				return method;
			}

		for (final Method method : clazz.getDeclaredMethods())
			if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
				method.setAccessible(true);

				return method;
			}

		return null;
	}

	/**
	 * 获取声明的类方法
	 *
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 */
	public static Method getDeclaredMethod(Class<?> clazz, final String methodName, Class<?>... args) {
		final Class<?> originalClass = clazz;

		while (!clazz.equals(Object.class))
			try {
				final Method method = clazz.getDeclaredMethod(methodName, args);
				method.setAccessible(true);

				return method;

			} catch (final NoSuchMethodException ex) {
				clazz = clazz.getSuperclass();

			} catch (final Throwable t) {
				throw new ReflectionException(t, "Error lookup up method " + methodName + " in class " + originalClass + " and her subclasses");
			}

		throw new ReflectionException("Unable to find method " + methodName + (args != null ? " with params " + Common.join(args) : "") + " in class " + originalClass + " and her subclasses");
	}

	/**
	 * 调用静态方法
	 *
	 * @param <T>
	 * @param clazz
	 * @param methodName
	 * @param params
	 * @return
	 */
	public static <T> T invokeStatic(@NonNull final Class<?> clazz, final String methodName, final Object... params) {
		final Method method = getMethod(clazz, methodName);
		Valid.checkNotNull(method, "Method " + clazz + "." + methodName + "(" + Common.join(params) + ") not found!");

		return invokeStatic(method, params);
	}

	/**
	 * 调用静态方法
	 *
	 * @param <T>
	 * @param method
	 * @param params
	 * @return
	 */
	public static <T> T invokeStatic(@NonNull final Method method, final Object... params) {
		try {
			Valid.checkBoolean(Modifier.isStatic(method.getModifiers()),
					"Method " + method.getName() + " must be static to be invoked through invokeStatic with params: " + Common.join(params));

			return (T) method.invoke(null, params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not invoke static method " + method + " with params " + Common.join(params, ", ", Common::simplify));
		}
	}

	/**
	 * 调用非静态方法
	 *
	 * @param <T>
	 * @param methodName
	 * @param instance
	 * @param params
	 * @return
	 */
	public static <T> T invoke(@NonNull final String methodName, @NonNull final Object instance, final Object... params) {
		final List<Class<?>> args = Common.convert(params, Object::getClass);
		final Method method = getMethod(instance.getClass(), methodName, args.toArray(new Class<?>[args.size()]));
		Valid.checkNotNull(method, "Unable to invoke " + methodName + "(" + Common.join(params) + ") because such method was not found in " + instance.getClass());

		return invoke(method, instance, params);
	}

	/**
	 * 调用非静态方法
	 *
	 * @param <T>
	 * @param method
	 * @param instance
	 * @param params
	 * @return
	 */
	public static <T> T invoke(final Method method, final Object instance, final Object... params) {
		Valid.checkNotNull(method, "Cannot invoke a null method for " + (instance == null ? "static" : instance.getClass().getSimpleName() + "") + " instance '" + instance + "' " + " with params " + Common.join(params, ", "));

		try {
			return (T) method.invoke(instance, params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not invoke method " + method + " on instance " + instance + " with params " + Common.join(params, ", "));
		}
	}

	/**
	 * 按全路径名创建类的新实例
	 *
	 * @param <T>
	 * @param classPath
	 * @return
	 */
	public static <T> T instantiate(final String classPath) {
		final Class<T> clazz = lookupClass(classPath);

		return instantiate(clazz);
	}

	/**
	 * 创建类的新实例
	 *
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz) {
		try {
			final Constructor<T> constructor;

			if (reflectionDataCache.containsKey(clazz))
				constructor = ((ReflectionData<T>) reflectionDataCache.get(clazz)).getDeclaredConstructor();
			else
				constructor = ReflectionUtil.getConstructor(clazz);

			return constructor.newInstance();

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not make instance of: " + clazz);
		}
	}

	/**
	 * 用参数创建给定 NMS 类的新实例，
	 * 注意：不支持 Minecraft 1.17+
	 *
	 * @param <T>
	 * @param nmsPath
	 * @param params
	 * @return
	 */
	public static <T> T instantiateNMS(final String nmsPath, final Object... params) {
		return (T) instantiate(getNMSClass(nmsPath), params);
	}

	/**
	 * 用参数创建类的新实例。
	 *
	 * @param <T>
	 * @param clazz
	 * @param params
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz, final Object... params) {
		try {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Object param : params) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);
				final Class<?> paramClass = param.getClass();

				classes.add(paramClass.isPrimitive() ? wrapperToPrimitive(paramClass) : paramClass);
			}

			final Class<?>[] paramArr = classes.toArray(new Class<?>[0]);
			final Constructor<T> constructor;

			if (reflectionDataCache.containsKey(clazz))
				constructor = ((ReflectionData<T>) reflectionDataCache.get(clazz)).getDeclaredConstructor(paramArr);
			else {
				classCache.put(clazz.getCanonicalName(), clazz);

				constructor = (Constructor<T>) reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new).getDeclaredConstructor(paramArr);
			}

			constructor.setAccessible(true);

			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not make instance of: " + clazz);
		}
	}

	/**
	 * 尝试用给定构造器和参数创建新实例
	 *
	 * @param <T>
	 * @param constructor
	 * @param params
	 * @return
	 */
	public static <T> T instantiate(final Constructor<T> constructor, final Object... params) {
		try {
			constructor.setAccessible(true);

			return constructor.newInstance(params);

		} catch (final ReflectiveOperationException ex) {
			throw new FoException(ex, "Could not make new instance of " + constructor + " with params: " + Common.join(params));
		}
	}

	/**
	 * 若给定绝对类路径可用则返回 true，
	 * 可用于在旧 MC 版本上检查类，如 org.bukkit.entity.Phantom
	 *
	 * @param path
	 * @return
	 */
	public static boolean isClassAvailable(final String path) {
		try {
			if (classCache.containsKey(path))
				return true;

			Class.forName(path);

			return true;

		} catch (final Throwable t) {
			return false;
		}
	}

	/**
	 * Class.forName 的包装
	 *
	 * @param <T>
	 * @param path
	 * @return
	 */
	public static <T> Class<T> lookupClass(final String path) {
		if (classCache.containsKey(path))
			return (Class<T>) classCache.get(path);

		if (classNameGuard.contains(path)) {
			while (classNameGuard.contains(path)) {
				// Wait for other thread
			}

			return lookupClass(path); // Re run method to see if the cached value now exists.
		}

		try {
			classNameGuard.add(path);

			final Class<?> clazz = Class.forName(path);

			classCache.put(path, clazz);
			reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new);

			return (Class<T>) clazz;

		} catch (final ClassNotFoundException ex) {
			throw new ReflectionException("Could not find class: " + path);

		} finally {
			classNameGuard.remove(path);
		}
	}

	/**
	 * 尝试按多个名称查找枚举，典型场景是
	 * 多个 MC 版本中名称变了但枚举类没变。
	 *
	 * 注意：Material 类请使用我们专用的 CompMaterial，不要用本方法。
	 *
	 * @param <T>
	 * @param enumClass
	 * @param names
	 * @return
	 */
	public static <T extends Enum<T>> T lookupLegacyEnum(final Class<T> enumClass, String... names) {

		for (final String name : names) {
			final T foundEnum = lookupEnumSilent(enumClass, name);

			if (foundEnum != null)
				return foundEnum;
		}

		return null;
	}

	/**
	 * 尝试查找枚举，若未找到则抛出显示所有可用
	 * 值的格式化错误
	 *
	 * 字段名会转大写，空格替换为下划线，甚至会
	 * 加复数 S 以尝试检测正确的枚举
	 *
	 * 若已知给定类型只出现在新 MC 版本，我们可能返回 null
	 * 而不是抛错。这是为了防止包含该枚举的默认配置
	 * 在旧 MC 版本上加载时导致插件崩溃。
	 *
	 * @param <E>
	 * @param enumType
	 * @param name
	 *
	 * @return 该枚举，或带异常的错误，见上文
	 */
	@Nullable
	public static <E> E lookupEnum(final Class<E> enumType, final String name) {
		return lookupEnum(enumType, name, enumType.getSimpleName() + " value '" + name + "' is not found on Minecraft " + MinecraftVersion.getFullVersion() + "! Available: {available}");
	}

	/**
	 * 尝试查找枚举，若未找到则抛出显示所有可用
	 * 值的格式化错误。在 errMessage 中用 {available} 获取所有枚举值。
	 *
	 * 字段名会转大写，空格替换为下划线，甚至会
	 * 加复数 S 以尝试检测正确的枚举
	 *
	 * 若已知给定类型只出现在新 MC 版本，我们可能返回 null
	 * 而不是抛错。这是为了防止包含该枚举的默认配置
	 * 在旧 MC 版本上加载时导致插件崩溃。
	 *
	 * @param <E>
	 * @param enumType
	 * @param name
	 * @param errMessage
	 *
	 * @return 该枚举，或带异常的错误，见上文
	 */
	public static <E> E lookupEnum(final Class<E> enumType, String name, final String errMessage) {
		Valid.checkNotNull(enumType, "Type missing for " + name);
		Valid.checkNotNull(name, "Name missing for " + enumType);

		final String rawName = name.toUpperCase().replace(" ", "_");

		// Some compatibility workaround for ChatControl, Boss, CoreArena and other plugins
		// having these values in their default config. This prevents
		// malfunction on plugin's first load, in case it is loaded on an older MC version.
		{
			if (enumType == ChatColor.class && name.contains(ChatColor.COLOR_CHAR + ""))
				return (E) ChatColor.getByChar(name.charAt(1));

			if (enumType == org.bukkit.block.Biome.class)
				if (MinecraftVersion.atLeast(V.v1_13))
					if (rawName.equalsIgnoreCase("ICE_MOUNTAINS"))
						name = "SNOWY_TAIGA";

			if (enumType == EntityType.class) {
				if ((MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() >= 5) || MinecraftVersion.newerThan(V.v1_20)) {
					if (rawName.equals("LIGHTNING"))
						name = "LIGHTNING_BOLT";
					else if (rawName.equals("PRIMED_TNT"))
						name = "TNT";
					else if (rawName.equals("FIREWORK"))
						name = "FIREWORK_ROCKET";
					else if (rawName.equals("ENDER_CRYSTAL"))
						name = "END_CRYSTAL";

				} else {
					if (rawName.equals("LIGHTNING_BOLT"))
						name = "LIGHTNING";
					else if (rawName.equals("TNT"))
						name = "PRIMED_TNT";
					else if (rawName.equals("FIREWORK_ROCKET"))
						name = "FIREWORK";
					else if (rawName.equals("END_CRYSTAL"))
						name = "ENDER_CRYSTAL";
				}

				if (MinecraftVersion.atLeast(V.v1_16)) {
					if (rawName.equals("PIG_ZOMBIE"))
						name = "ZOMBIFIED_PIGLIN";
				} else {
					if (rawName.equals("ZOMBIFIED_PIGLIN"))
						name = "PIG_ZOMBIE";
				}

				if (MinecraftVersion.atLeast(V.v1_14))
					if (rawName.equals("TIPPED_ARROW"))
						name = "ARROW";

				if (MinecraftVersion.olderThan(V.v1_16))
					if (rawName.equals("ZOMBIFIED_PIGLIN"))
						name = "PIG_ZOMBIE";

				if (MinecraftVersion.olderThan(V.v1_9))
					if (rawName.equals("TRIDENT"))
						name = "ARROW";
					else if (rawName.equals("DRAGON_FIREBALL"))
						name = "FIREBALL";

				if (MinecraftVersion.olderThan(V.v1_13))
					if (rawName.equals("DROWNED"))
						name = "ZOMBIE";
					else if (rawName.equals("ZOMBIE_VILLAGER"))
						name = "ZOMBIE";
			}

			if (enumType == DamageCause.class) {
				if (MinecraftVersion.olderThan(V.v1_13))
					if (rawName.equals("DRYOUT"))
						name = "CUSTOM";

				if (MinecraftVersion.olderThan(V.v1_11))
					if (rawName.equals("ENTITY_SWEEP_ATTACK"))
						name = "ENTITY_ATTACK";
					else if (rawName.equals("CRAMMING"))
						name = "CUSTOM";

				if (MinecraftVersion.olderThan(V.v1_9))
					if (rawName.equals("FLY_INTO_WALL"))
						name = "SUFFOCATION";
					else if (rawName.equals("HOT_FLOOR"))
						name = "LAVA";

				if (rawName.equals("DRAGON_BREATH"))
					try {
						DamageCause.valueOf("DRAGON_BREATH");
					} catch (final Throwable t) {
						name = "ENTITY_ATTACK";
					}
			}
		}

		final String oldName = name;

		E result = lookupEnumSilent(enumType, name);

		// Try making the enum uppercased
		if (result == null) {
			name = name.toUpperCase();

			result = lookupEnumSilent(enumType, name);
		}

		// Try replacing spaces with underscores
		if (result == null) {
			name = name.replace(" ", "_");

			result = lookupEnumSilent(enumType, name);
		}

		// Try crunching all underscores (were spaces) all together
		if (result == null)
			result = lookupEnumSilent(enumType, name.replace("_", ""));

		if (result == null) {

			// Return null for legacy types
			final Map<String, V> legacyMap = legacyEnumTypes.get(enumType);

			if (legacyMap != null) {
				final V since = legacyMap.get(rawName);

				if (since != null && MinecraftVersion.olderThan(since))
					return null;
			}

			throw new MissingEnumException(oldName, errMessage.replace("{available}", Common.join(enumType.getEnumConstants(), ", ")));
		}

		return result;
	}

	/**
	 * 不抛异常的 Enum.valueOf 包装
	 *
	 * @param <E>
	 * @param enumClass
	 * @param name
	 * @return 该枚举，若不存在则为 null
	 */
	@SuppressWarnings("rawtypes")
	public static <E> E lookupEnumSilent(final Class<E> enumClass, final String name) {
		try {

			if (enumClass == CompMaterial.class || enumClass == Material.class) {
				final CompMaterial material = CompMaterial.fromString(name);

				if (material != null)
					return enumClass == CompMaterial.class ? (E) material : (E) material.getMaterial();
			}

			// Since we obfuscate our plugins, enum names are changed.
			// Therefore we look up a special fromKey method in some of our enums
			boolean hasKey = false;
			Method method = null;

			try {
				method = enumClass.getDeclaredMethod("fromKey", String.class);

				if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
					hasKey = true;

			} catch (final Throwable t) {
			}

			// Only invoke fromName from non-Bukkit API since this gives unexpected results
			if (method == null && !enumClass.getName().contains("org.bukkit"))
				try {
					method = enumClass.getDeclaredMethod("fromName", String.class);

					if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
						hasKey = true;

				} catch (final Throwable t) {
				}

			if (method == null)
				try {
					method = enumClass.getDeclaredMethod("valueOf", String.class);

					if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
						hasKey = true;

				} catch (final NoSuchMethodException t) {
				}

			if (hasKey)
				return (E) method.invoke(null, name);

			if (enumClass.isEnum())
				return (E) Enum.valueOf((Class<Enum>) enumClass, name);

			return ReflectionUtil.invokeStatic(enumClass, "valueOf", name);

		} catch (final IllegalArgumentException ex) {
			return null;

		} catch (final ReflectiveOperationException ex) {
			return null;
		}
	}

	/**
	 * 获取枚举名称，对枚举和接口类都有效。
	 *
	 * @param enumOrKeyed
	 * @return
	 */
	public static String getEnumName(Object enumOrKeyed) {
		return enumOrKeyed instanceof Enum ? ((Enum<?>) enumOrKeyed).name() : invoke("name", enumOrKeyed);
	}

	/**
	 * 获取枚举常量，对枚举和接口类都有效。
	 *
	 * @param <T>
	 * @param enumOrKeyed
	 * @return
	 */
	public static <T> T[] getEnumValues(Class<T> enumOrKeyed) {
		return enumOrKeyed.isEnum() ? enumOrKeyed.getEnumConstants() : invokeStatic(enumOrKeyed, "values");
	}

	/**
	 * 获取调用本方法者的调用者堆栈方法，有助于
	 * 调试
	 *
	 * @param skipMethods
	 * @param count
	 * @return
	 */
	public static String getCallerMethods(final int skipMethods, final int count) {
		final StackTraceElement[] elements = Thread.currentThread().getStackTrace();

		final StringBuilder methods = new StringBuilder();
		int counted = 0;

		for (int i = 2 + skipMethods; i < elements.length && counted < count; i++) {
			final StackTraceElement el = elements[i];

			if (!el.getMethodName().equals("getCallerMethods") && el.getClassName().indexOf("java.lang.Thread") != 0) {
				final String[] clazz = el.getClassName().split("\\.");

				methods.append(clazz[clazz.length == 0 ? 0 : clazz.length - 1]).append("#").append(el.getLineNumber()).append("-").append(el.getMethodName()).append("()").append(i + 1 == elements.length ? "" : ".");
				counted++;
			}
		}

		return methods.toString();
	}

	// ------------------------------------------------------------------------------------------
	// JavaPlugin related methods
	// ------------------------------------------------------------------------------------------

	/**
	 * 返回插件中继承给定类的有序类集合
	 *
	 * @param plugin
	 * @return
	 */
	public static List<Class<?>> getClasses(final Plugin plugin) {
		final List<Class<?>> found = new ArrayList<>();

		found.addAll(getClasses(plugin, null));

		return found;
	}

	/**
	 * 获取 Java 插件中的所有类
	 *
	 * @param <T>
	 * @param plugin
	 * @param extendingClass
	 * @return
	 */
	@SneakyThrows
	public static <T> TreeSet<Class<T>> getClasses(@NonNull Plugin plugin, Class<T> extendingClass) {
		Valid.checkNotNull(plugin, "Plugin is null!");
		Valid.checkBoolean(JavaPlugin.class.isAssignableFrom(plugin.getClass()), "Plugin must be a JavaPlugin");

		// Get the plugin .jar
		final Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
		getFileMethod.setAccessible(true);

		final File pluginFile = (File) getFileMethod.invoke(plugin);

		final TreeSet<Class<T>> classes = new TreeSet<>(Comparator.comparing(Class::toString));

		try (final JarFile jarFile = new JarFile(pluginFile)) {
			final Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();

				if (name.endsWith(".class")) {
					name = name.replaceFirst("\\.class", "").replace("/", ".");

					Class<?> clazz = null;

					try {
						clazz = Class.forName(name, false, SimplePlugin.class.getClassLoader());

						if (extendingClass == null || (extendingClass.isAssignableFrom(clazz) && clazz != extendingClass))
							classes.add((Class<T>) clazz);

					} catch (final Throwable throwable) {

						if (extendingClass != null && (clazz != null && extendingClass.isAssignableFrom(clazz)) && clazz != extendingClass)
							Common.log("Unable to load class '" + name + "' due to error: " + throwable);

						continue;
					}
				}
			}
		}

		return classes;
	}

	// ------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------

	/**
	 * <p>将指定包装类转换为对应的原生
	 * 类。</p>
	 *
	 * <p>本方法是 <code>primitiveToWrapper()</code> 的对应方法。
	 * 若传入的类是原生类型的包装类，则返回
	 * 该原生类型（例如 <code>Integer.class</code> 对应 <code>Integer.TYPE</code>）。
	 * 对于其他类，或参数为 <b>null</b> 时，返回
	 * <b>null</b>。</p>
	 *
	 * @param cls 要转换的类，可为 <b>null</b>
	 * @return 若 <code>cls</code> 是包装类则返回对应的原生类型，
	 * 否则返回 <b>null</b>
	 *
	 * @author Apache Commons ClassUtils
	 */
	public static Class<?> wrapperToPrimitive(Class<?> cls) {
		return wrapperPrimitiveMap.get(cls);
	}

	static {

		final Map<Class<? extends Enum<?>>, Map<String, V>> legacyEnums = new HashMap<>();

		final Map<String, V> entities = new HashMap<>();
		entities.put("TIPPED_ARROW", V.v1_9);
		entities.put("SPECTRAL_ARROW", V.v1_9);
		entities.put("SHULKER_BULLET", V.v1_9);
		entities.put("DRAGON_FIREBALL", V.v1_9);
		entities.put("SHULKER", V.v1_9);
		entities.put("AREA_EFFECT_CLOUD", V.v1_9);
		entities.put("LINGERING_POTION", V.v1_9);
		entities.put("POLAR_BEAR", V.v1_10);
		entities.put("HUSK", V.v1_10);
		entities.put("ELDER_GUARDIAN", V.v1_11);
		entities.put("WITHER_SKELETON", V.v1_11);
		entities.put("STRAY", V.v1_11);
		entities.put("DONKEY", V.v1_11);
		entities.put("MULE", V.v1_11);
		entities.put("EVOKER_FANGS", V.v1_11);
		entities.put("EVOKER", V.v1_11);
		entities.put("VEX", V.v1_11);
		entities.put("VINDICATOR", V.v1_11);
		entities.put("ILLUSIONER", V.v1_12);
		entities.put("PARROT", V.v1_12);
		entities.put("TURTLE", V.v1_13);
		entities.put("PHANTOM", V.v1_13);
		entities.put("TRIDENT", V.v1_13);
		entities.put("COD", V.v1_13);
		entities.put("SALMON", V.v1_13);
		entities.put("PUFFERFISH", V.v1_13);
		entities.put("TROPICAL_FISH", V.v1_13);
		entities.put("DROWNED", V.v1_13);
		entities.put("DOLPHIN", V.v1_13);
		entities.put("CAT", V.v1_14);
		entities.put("PANDA", V.v1_14);
		entities.put("PILLAGER", V.v1_14);
		entities.put("RAVAGER", V.v1_14);
		entities.put("TRADER_LLAMA", V.v1_14);
		entities.put("WANDERING_TRADER", V.v1_14);
		entities.put("FOX", V.v1_14);
		entities.put("BEE", V.v1_15);
		entities.put("HOGLIN", V.v1_16);
		entities.put("PIGLIN", V.v1_16);
		entities.put("STRIDER", V.v1_16);
		entities.put("ZOGLIN", V.v1_16);
		entities.put("PIGLIN_BRUTE", V.v1_16);
		entities.put("AXOLOTL", V.v1_17);
		entities.put("GLOW_ITEM_FRAME", V.v1_17);
		entities.put("GLOW_SQUID", V.v1_17);
		entities.put("GOAT", V.v1_17);
		entities.put("MARKER", V.v1_17);
		legacyEnums.put(EntityType.class, entities);

		final Map<String, V> spawnReasons = new HashMap<>();
		spawnReasons.put("DROWNED", V.v1_13);
		legacyEnums.put(SpawnReason.class, spawnReasons);

		legacyEnumTypes = legacyEnums;

		// Load wrappers

		primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
		primitiveWrapperMap.put(Byte.TYPE, Byte.class);
		primitiveWrapperMap.put(Character.TYPE, Character.class);
		primitiveWrapperMap.put(Short.TYPE, Short.class);
		primitiveWrapperMap.put(Integer.TYPE, Integer.class);
		primitiveWrapperMap.put(Long.TYPE, Long.class);
		primitiveWrapperMap.put(Double.TYPE, Double.class);
		primitiveWrapperMap.put(Float.TYPE, Float.class);
		primitiveWrapperMap.put(Void.TYPE, Void.TYPE);

		for (final Class<?> primitiveClass : primitiveWrapperMap.keySet()) {
			final Class<?> wrapperClass = primitiveWrapperMap.get(primitiveClass);

			if (!primitiveClass.equals(wrapperClass))
				wrapperPrimitiveMap.put(wrapperClass, primitiveClass);
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	private static final class ReflectionData<T> {
		private final Class<T> clazz;

		ReflectionData(final Class<T> clazz) {
			this.clazz = clazz;
		}

		//private final Map<String, Collection<Method>> methodCache = new ConcurrentHashMap<>();
		private final Map<Integer, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
		private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
		private final Collection<String> fieldGuard = ConcurrentHashMap.newKeySet();
		private final Collection<Integer> constructorGuard = ConcurrentHashMap.newKeySet();

		public void cacheConstructor(final Constructor<T> constructor) {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Class<?> param : constructor.getParameterTypes()) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + this.clazz);

				classes.add(param);
			}

			this.constructorCache.put(Arrays.hashCode(classes.toArray(new Class<?>[0])), constructor);
		}

		public Constructor<T> getDeclaredConstructor(final Class<?>... paramTypes) throws NoSuchMethodException {
			final Integer hashCode = Arrays.hashCode(paramTypes);

			if (this.constructorCache.containsKey(hashCode))
				return (Constructor<T>) this.constructorCache.get(hashCode);

			if (this.constructorGuard.contains(hashCode)) {
				while (this.constructorGuard.contains(hashCode)) {

				} // Wait for other thread;
				return this.getDeclaredConstructor(paramTypes);
			}

			this.constructorGuard.add(hashCode);

			try {
				final Constructor<T> constructor = this.clazz.getDeclaredConstructor(paramTypes);

				this.cacheConstructor(constructor);

				return constructor;

			} finally {
				this.constructorGuard.remove(hashCode);
			}
		}

		public Constructor<T> getConstructor(final Class<?>... paramTypes) throws NoSuchMethodException {
			final Integer hashCode = Arrays.hashCode(paramTypes);

			if (this.constructorCache.containsKey(hashCode))
				return (Constructor<T>) this.constructorCache.get(hashCode);

			if (this.constructorGuard.contains(hashCode)) {
				while (this.constructorGuard.contains(hashCode)) {
					// Wait for other thread;
				}

				return this.getConstructor(paramTypes);
			}

			this.constructorGuard.add(hashCode);

			try {
				final Constructor<T> constructor = this.clazz.getConstructor(paramTypes);

				this.cacheConstructor(constructor);

				return constructor;

			} finally {
				this.constructorGuard.remove(hashCode);
			}
		}

		/*public void cacheMethod(final Method method) {
			methodCache.computeIfAbsent(method.getName(), unused -> ConcurrentHashMap.newKeySet()).add(method);
		}*/

		/*public Method getDeclaredMethod(final String name, final Class<?>... paramTypes) throws NoSuchMethodException {
			if (methodCache.containsKey(name)) {
				final Collection<Method> methods = methodCache.get(name);
		
				for (final Method method : methods)
					if (Arrays.equals(paramTypes, method.getParameterTypes()))
						return method;
			}
		
			final Method method = clazz.getDeclaredMethod(name, paramTypes);
		
			cacheMethod(method);
		
			return method;
		}*/

		public void cacheField(final Field field) {
			this.fieldCache.put(field.getName(), field);
		}

		public Field getDeclaredField(final String name) throws NoSuchFieldException {

			if (this.fieldCache.containsKey(name))
				return this.fieldCache.get(name);

			if (this.fieldGuard.contains(name)) {
				while (this.fieldGuard.contains(name)) {
				}

				return this.getDeclaredField(name);
			}

			this.fieldGuard.add(name);

			try {
				final Field field = this.clazz.getDeclaredField(name);

				this.cacheField(field);

				return field;

			} finally {
				this.fieldGuard.remove(name);
			}
		}
	}

	/**
	 * 代表反射操作期间的异常
	 */
	public static final class ReflectionException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ReflectionException(final String message) {
			super(message);
		}

		public ReflectionException(final Throwable ex, final String message) {
			super(message, ex);
		}
	}

	/**
	 * 代表从 {@link #lookupEnum(Class, String)} 和
	 * {@link #lookupEnum(Class, String, String)} 方法获取枚举失败
	 */
	public static final class MissingEnumException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private final String enumName;

		public MissingEnumException(final String enumName, final String msg) {
			super(msg);

			this.enumName = enumName;
		}

		public MissingEnumException(final String enumName, final String msg, final Exception ex) {
			super(msg, ex);

			this.enumName = enumName;
		}

		public String getEnumName() {
			return this.enumName;
		}
	}
}
