package org.mineacademy.fo.settings;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;

import lombok.NonNull;

/**
 * 可以存放已加载 {@link YamlConfig} 文件的特殊类
 * <p>
 * 它不会替你调用 {@link YamlConfig#loadConfiguration(String, String)}，
 * 你必须像平常一样自己调用！
 *
 * @param <T>
 */
public final class ConfigItems<T extends YamlConfig> {

	/**
	 * 所有已加载条目的列表
	 */
	private final StrictMap<String, T> loadedItemsMap = new StrictMap<>();

	/**
	 * 此类存放的条目类型，例如 "variable"、"format" 或 "arena class"
	 */
	private final String type;

	/**
	 * 存放条目的文件夹名称，JAR 文件中和插件文件夹中
	 * 必须一致，例如 JAR 文件中的
	 * "classes/" 与插件文件夹中的 "classes/"
	 */
	private final String folder;

	/**
	 * 如何根据文件实例化单个类？
	 *
	 * 此功能仅供高级用法。默认情况下每个配置条目都是同一个类，
	 * 例如在 Boss 插件中，bosses/ 文件夹中的每个 boss 都会生成一个 Boss 类。
	 *
	 * 适用场景示例：如果你有一个小游戏插件，想把不同的小游戏
	 * （例如 MobArena 和 BedWars）都存放在 games/ 文件夹中，
	 * 那么你可以在函数中以配置形式打开所提供的文件名，读取每个竞技场文件中的 "Type" 键，
	 * 并根据该键返回对应的竞技场类。
	 */
	private final Function<String, Class<T>> prototypeCreator;

	/**
	 * 是否所有条目都存放在同一个文件中？
	 */
	private final boolean singleFile;

	/**
	 * 创建一个新的配置条目实例
	 *
	 * @param type
	 * @param folder
	 * @param prototypeCreator
	 * @param singleFile
	 */
	private ConfigItems(String type, String folder, Function<String, Class<T>> prototypeCreator, boolean singleFile) {
		this.type = type;
		this.folder = folder;
		this.prototypeCreator = prototypeCreator;
		this.singleFile = singleFile;
	}

	/**
	 * 从给定文件夹加载条目
	 *
	 * @param <P>
	 * @param folder
	 * @param prototypeClass
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFolder(String folder, Class<P> prototypeClass) {
		return fromFolder(folder, fileName -> prototypeClass);
	}

	/**
	 * 从给定文件夹加载条目
	 *
	 * @param <P>
	 * @param folder
	 * @param prototypeCreator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFolder(String folder, Function<String, Class<P>> prototypeCreator) {
		return new ConfigItems<>(folder.substring(0, folder.length() - (folder.endsWith("es") && !folder.contains("variable") ? 2 : folder.endsWith("s") ? 1 : 0)), folder, prototypeCreator, false);
	}

	/**
	 * 从给定的 YAML 文件路径加载条目
	 *
	 * @param <P>
	 * @param path
	 * @param file
	 * @param prototypeClass
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFile(String path, String file, Class<P> prototypeClass) {
		return fromFile(path, file, fileName -> prototypeClass);
	}

	/**
	 * 从给定的 YAML 文件路径加载条目
	 *
	 * @param <P>
	 * @param path
	 * @param file
	 * @param prototypeCreator
	 * @return
	 */
	public static <P extends YamlConfig> ConfigItems<P> fromFile(String path, String file, Function<String, Class<P>> prototypeCreator) {
		return new ConfigItems<>(path, file, prototypeCreator, true);
	}

	/**
	 * 加载所有条目类：为它们创建新实例，并将其文件夹从 JAR 复制到磁盘
	 */
	public void loadItems() {
		this.loadItems(null);
	}

	/**
	 * 加载所有条目类：为它们创建新实例，并将其文件夹从 JAR 复制到磁盘
	 *
	 * @param loader 用于高级加载机制，大多数人不会用到
	 */
	public void loadItems(@Nullable Function<File, T> loader) {

		// Clear old items
		this.loadedItemsMap.clear();

		if (this.singleFile) {
			final File file = FileUtil.extract(this.folder);
			final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

			if (config.isSet(this.type))
				for (final String name : config.getConfigurationSection(this.type).getKeys(false))
					this.loadOrCreateItem(name);
		} else {
			// Try copy items from our JAR
			if (!FileUtil.getFile(this.folder).exists())
				FileUtil.extractFolderFromJar(this.folder + "/", this.folder);

			// Load items on our disk
			final File[] files = FileUtil.getFiles(this.folder, "yml");

			for (final File file : files)
				if (loader != null)
					loader.apply(file);

				else {
					final String name = FileUtil.getFileName(file);

					this.loadOrCreateItem(name);
				}
		}
	}

	/**
	 * 根据给定名称创建（新建实例）该类，
	 * 该类必须有一个接收 String（名称）参数或无参数的私有构造器
	 *
	 * @param name
	 * @return
	 */
	public T loadOrCreateItem(@NonNull final String name) {
		return this.loadOrCreateItem(name, null);
	}

	/**
	 * 根据给定名称创建（新建实例）该类，
	 * 该类必须有一个接收 String（名称）参数或无参数的私有构造器
	 *
	 * @param name
	 * @param instantiator 默认情况下我们通过调用构造器来创建条目的新实例，
	 *                     构造器可以是无参的，也可以只接收一个参数（名称）。如果这不能
	 *                     满足需求，你可以在这里提供自定义的实例化器。
	 * @return
	 */
	public T loadOrCreateItem(@NonNull final String name, @Nullable Supplier<T> instantiator) {
		Valid.checkBoolean(!this.isItemLoaded(name), "Item " + (this.type == null ? "" : this.type + " ") + "named " + name + " already exists! Available: " + this.getItemNames());

		// Create a new instance of our item
		T item = null;

		try {

			if (instantiator != null)
				item = instantiator.get();

			else {
				Constructor<T> constructor = null;
				boolean nameConstructor = true;

				final Class<T> prototypeClass = this.prototypeCreator.apply(name);
				Valid.checkNotNull(prototypeClass);

				try {
					constructor = prototypeClass.getDeclaredConstructor(String.class);

				} catch (final Throwable t) {
					try {
						constructor = prototypeClass.getDeclaredConstructor();

						nameConstructor = false;
					} catch (final Throwable tt) {
						// User forgot his constructor
					}
				}

				Valid.checkBoolean(constructor != null && (Modifier.isPrivate(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers())),
						"Your class " + prototypeClass + " must also have a private or a protected constructor taking a String or nothing! Found: " + constructor);

				constructor.setAccessible(true);

				try {
					if (nameConstructor)
						item = constructor.newInstance(name);
					else
						item = constructor.newInstance();

				} catch (final InstantiationException ex) {
					Common.throwError(ex, "Failed to create new" + (this.type == null ? prototypeClass.getSimpleName() : " " + this.type) + " " + name + " from " + constructor);
				}
			}

			// Register
			this.loadedItemsMap.put(name, item);

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load" + name + (this.singleFile ? "" : " from " + this.folder));
		}

		Valid.checkNotNull(item, "Failed to initiliaze " + name + " from " + this.folder);
		return item;
	}

	/**
	 * 按实例移除给定条目
	 *
	 * @param item
	 */
	public void removeItem(@NonNull final T item) {
		this.removeItemByName(item.getName());
	}

	/**
	 * 按名称移除给定条目
	 *
	 * @param name
	 */
	public void removeItemByName(@NonNull final String name) {
		final T item = this.findItem(name);
		Valid.checkNotNull(item, ChatUtil.capitalize(this.type) + " " + name + " not loaded. Available: " + this.getItemNames());

		if (this.singleFile)
			item.save("", null);
		else
			item.deleteFile();

		this.loadedItemsMap.remove(name);
	}

	/**
	 * 检查给定名称的条目是否已加载
	 *
	 * @param name
	 * @return
	 */
	public boolean isItemLoaded(final String name) {
		return this.findItem(name) != null;
	}

	/**
	 * 按名称返回条目实例，未加载时返回 null
	 *
	 * @param name
	 * @return
	 */
	public T findItem(@NonNull final String name) {
		final T item = this.loadedItemsMap.get(name);

		// Fallback to case insensitive
		if (item == null)
			for (final Map.Entry<String, T> entry : this.loadedItemsMap.entrySet())
				if (entry.getKey().equalsIgnoreCase(name))
					return entry.getValue();

		return item;
	}

	/**
	 * 返回所有已加载的条目
	 *
	 * @return
	 */
	public List<T> getItems() {
		return Collections.unmodifiableList(new ArrayList<>(this.loadedItemsMap.values()));
	}

	/**
	 * 返回所有已加载条目的名称
	 *
	 * @return
	 */
	public Set<String> getItemNames() {
		return this.loadedItemsMap.keySet();
	}
}
