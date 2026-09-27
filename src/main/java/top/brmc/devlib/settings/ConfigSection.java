package top.brmc.devlib.settings;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;

import lombok.NonNull;

/**
 * 表示配置节所持有的内部数据映射。
 * 功劳归于最初的 Bukkit/Spigot 团队，由 MineAcademy 增强。
 */
public class ConfigSection {

	/**
	 * 存放此配置节键和值的数据映射。
	 * 值也可以是另一个配置节。
	 */
	final Map<String, Object> map = new LinkedHashMap<>();

	/**
	 * 此配置的根
	 */
	private final ConfigSection root;

	/**
	 * 此配置的父级（如果有）。
	 */
	private final ConfigSection parent;

	/**
	 * 此配置的当前路径。
	 */
	private final String path;

	/**
	 * 此配置的完整路径。
	 */
	private final String fullPath;

	ConfigSection() {
		this.path = "";
		this.fullPath = "";
		this.parent = null;
		this.root = this;
	}

	ConfigSection(@NonNull ConfigSection parent, @NonNull String path) {
		this.path = path;
		this.parent = parent;
		this.root = parent.root;
		this.fullPath = createPath(parent, path);
	}

	// ------------------------------------------------------------------------------------
	// Getting values
	// ------------------------------------------------------------------------------------

	/**
	 * 获取包含此配置节中所有键的集合。
	 *
	 * 如果 deep 为 true，则会包含所有子配置节（及其子级等）中的全部键，
	 * 并以可直接使用的有效路径表示法给出。
	 *
	 * 如果 deep 为 false，则只包含直接子级的键，不包含子级自身的子级。
	 *
	 * @param deep
	 * @return
	 */
	@NonNull
	public final Set<String> getKeys(boolean deep) {
		final Set<String> result = new LinkedHashSet<>();
		this.mapChildrenKeys(result, this, deep);

		return result;
	}

	/**
	 * 获取包含此配置节所有键及其值的 Map。
	 *
	 * 如果 deep 为 true，则会包含所有子配置节（及其子级等）中的全部键和值，
	 * 这些键以可直接使用的有效路径表示法给出。
	 *
	 * 如果 deep 为 false，则只包含直接子级的键和值，不包含子级自身的子级。
	 *
	 * @param deep
	 * @return
	 */
	@NonNull
	public final Map<String, Object> getValues(boolean deep) {
		final Map<String, Object> result = new LinkedHashMap<>();

		this.mapChildrenValues(result, this, deep);

		return result;
	}

	/**
	 * 清除此配置节中的所有键
	 */
	public final void clear() {
		this.map.clear();
	}

	/**
	 * 若给定路径包含有效值则返回 true
	 *
	 * @param path
	 * @return
	 */
	public final boolean isStored(@NonNull String path) {

		if (this.root == null)
			return false;

		return this.retrieve(path) != null;
	}

	/**
	 * 用新值覆盖给定路径；将值设为 null 表示移除
	 *
	 * @param path
	 * @param value
	 */
	public final void store(@NonNull String path, Object value) {

		if (path.isEmpty())
			throw new IllegalArgumentException("Cannot set to an empty path");

		if (this.root == null)
			throw new IllegalStateException("Cannot use section without a root");

		int leadingIndex = -1, trailingIndex;
		ConfigSection section = this;
		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String node = path.substring(trailingIndex, leadingIndex);
			final ConfigSection subSection = section.retrieveConfigurationSection(node);
			if (subSection == null) {
				if (value == null)
					// no need to create missing sub-sections if we want to remove the value:
					return;
				section = section.createSection(node);
			} else
				section = subSection;
		}

		final String key = path.substring(trailingIndex);
		if (section == this) {
			if (value == null)
				this.map.remove(key);
			else
				this.map.put(key, value);
		} else
			section.store(key, value);
	}

	/**
	 * 获取给定路径上的键（未设置时为 null）
	 *
	 * @param path
	 * @return
	 */
	public final Object retrieve(@NonNull String path) {

		if (path.length() == 0)
			return this;

		if (this.root == null)
			throw new IllegalStateException("Cannot access section without a root");

		int leadingIndex = -1, trailingIndex;
		ConfigSection section = this;
		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String currentPath = path.substring(trailingIndex, leadingIndex);

			if (section.retrieve(currentPath) == null)
				return null;

			section = section.retrieveConfigurationSection(currentPath);

			if (section == null)
				return null;
		}

		final String key = path.substring(trailingIndex);

		if (section == this)
			return this.map.get(key);

		return section.retrieve(key);
	}

	/**
	 * 返回给定路径上的配置节，未设置时返回 null
	 *
	 * @param path
	 * @return
	 */
	public final ConfigSection retrieveConfigurationSection(@NonNull String path) {
		final Object val = this.retrieve(path);

		if (val != null)
			return (val instanceof ConfigSection) ? (ConfigSection) val : null;

		return (val instanceof ConfigSection) ? this.createSection(path) : null;
	}

	/*
	 * Helper to create a new config section at the given path
	 */
	@NonNull
	final ConfigSection createSection(@NonNull String path) {
		if (path.isEmpty())
			throw new IllegalArgumentException("Cannot create section at empty path");

		if (this.root == null)
			throw new IllegalStateException("Cannot create section without a root");

		int leadingIndex = -1, trailingIndex;
		ConfigSection section = this;
		while ((leadingIndex = path.indexOf('.', trailingIndex = leadingIndex + 1)) != -1) {
			final String node = path.substring(trailingIndex, leadingIndex);
			final ConfigSection subSection = section.retrieveConfigurationSection(node);
			if (subSection == null)
				section = section.createSection(node);
			else
				section = subSection;
		}

		final String key = path.substring(trailingIndex);
		if (section == this) {
			final ConfigSection result = new ConfigSection(this, key);
			this.map.put(key, result);
			return result;
		}
		return section.createSection(key);
	}

	/*
	 * Helper to map children keys to the given output
	 */
	private void mapChildrenKeys(@NonNull Set<String> output, @NonNull ConfigSection section, boolean deep) {
		if (section instanceof ConfigSection) {
			final ConfigSection sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				output.add(createPath(section, entry.getKey(), this));

				if ((deep) && (entry.getValue() instanceof ConfigSection)) {
					final ConfigSection subsection = (ConfigSection) entry.getValue();
					this.mapChildrenKeys(output, subsection, deep);
				}
			}
		} else {
			final Set<String> keys = section.getKeys(deep);

			for (final String key : keys)
				output.add(createPath(section, key, this));
		}
	}

	/*
	 * Helper to map children keys to the given output
	 */
	private void mapChildrenValues(@NonNull Map<String, Object> output, @NonNull ConfigSection section, boolean deep) {
		if (section instanceof ConfigSection) {
			final ConfigSection sec = section;

			for (final Map.Entry<String, Object> entry : sec.map.entrySet()) {
				final String childPath = createPath(section, entry.getKey(), this);
				output.remove(childPath);
				output.put(childPath, entry.getValue());

				if (entry.getValue() instanceof ConfigSection)
					if (deep)
						this.mapChildrenValues(output, (ConfigSection) entry.getValue(), deep);
			}
		} else {
			final Map<String, Object> values = section.getValues(deep);

			for (final Map.Entry<String, Object> entry : values.entrySet())
				output.put(createPath(section, entry.getKey(), this), entry.getValue());
		}
	}

	/*
	 * Helper to create a new config section
	 */
	@NonNull
	private static String createPath(@NonNull ConfigSection section, String key) {
		return createPath(section, key, (section == null) ? null : section.root);
	}

	/*
	 * Helper to create a new config section
	 */
	@NonNull
	private static String createPath(@NonNull ConfigSection section, String key, ConfigSection relativeTo) {
		final ConfigSection root = section.root;

		if (root == null)
			throw new IllegalStateException("Cannot create path without a root");

		final StringBuilder builder = new StringBuilder();
		if (section != null)
			for (ConfigSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.parent) {
				if (builder.length() > 0)
					builder.insert(0, '.');

				builder.insert(0, parent.path);
			}

		if ((key != null) && (key.length() > 0)) {
			if (builder.length() > 0)
				builder.append('.');

			builder.append(key);
		}

		return builder.toString();
	}

	// ------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------

	/**
	 * 将此配置节中的所有值转换为可保存的映射
	 *
	 * @return
	 */
	public final SerializedMap serialize() {
		return SerializedMap.of(this.getValues(true));
	}

	/**
	 * 若此配置节中没有任何键则返回 true
	 *
	 * @return
	 */
	public final boolean isEmpty() {
		return Valid.isNullOrEmptyValues(this.map);
	}

	@Override
	public String toString() {
		final ConfigSection root = this.root;
		return new StringBuilder()
				.append(this.getClass().getSimpleName())
				.append("[path='")
				.append(this.fullPath)
				.append("', root='")
				.append(root == null ? null : root.getClass().getSimpleName())
				.append("', keys=" + this.map + "]")
				.toString();
	}
}
