package top.brmc.devlib.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import top.brmc.devlib.FileUtil;
import top.brmc.devlib.ReflectionUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import lombok.NonNull;

/**
 * 核心设置类。完全兼容 Minecraft 1.7.10 至
 * 最新版本，包括注释支持（需要默认文件，参见 saveComments()），
 * 以及在请求仅存在于默认文件中的值时自动升级配置。
 */
public class YamlConfig extends FileConfig {

	/**
	 * Yaml 实例
	 */
	private final Yaml yaml;

	/**
	 * 是否保存空节或 null 值（要求没有默认文件）
	 */
	private boolean saveEmptyValues = true;

	/**
	 * 创建新实例（不会加载，请使用 {@link #load(File)} 加载）
	 */
	protected YamlConfig() {
		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setIndent(2);
		dumperOptions.setWidth(4096); // Do not wrap long lines

		YamlRepresenter representer;

		try {
			representer = new YamlRepresenter(dumperOptions);

		} catch (final Throwable t) {
			representer = new YamlRepresenter();
		}

		representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		// Load options only if available
		if (ReflectionUtil.isClassAvailable("org.yaml.snakeyaml.LoaderOptions")) {
			final LoaderOptions loaderOptions = new LoaderOptions();

			Yaml yaml;
			YamlConstructor constructor;

			try {
				constructor = new YamlConstructor(loaderOptions);

			} catch (final Throwable t) {
				// 1.12
				constructor = new YamlConstructor();
			}

			try {
				loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
				loaderOptions.setCodePointLimit(Integer.MAX_VALUE);

			} catch (final Throwable t) {
				// Thankfully unsupported
				// https://i.imgur.com/wAgKukK.png
			}

			try {
				yaml = new Yaml(constructor, representer, dumperOptions, loaderOptions);
			} catch (final Throwable t) {
				yaml = new Yaml(constructor, representer, dumperOptions);
			}

			this.yaml = yaml;
		}

		else
			this.yaml = new Yaml(new YamlConstructor(), representer, dumperOptions);
	}

	/**
	 * 如果你有默认文件并希望保存其中的注释，则返回 true
	 *
	 * 用户编写的注释和用户填写的值都会丢失。
	 *
	 * 请参见 {@link #getUncommentedSections()}，将包含用户可创建映射的节写入其中，
	 * 以避免丢失。
	 *
	 * @return
	 */
	protected boolean saveComments() {
		return true;
	}

	/**
	 * 参见 {@link #saveComments()}
	 *
	 * @return
	 */
	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	/**
	 * （要求没有默认文件，或 saveComments() 为 false）
	 * 设置保存时是否移除空列表或空节。
	 * 默认为 true，即会保存空节。
	 *
	 * @param saveEmptyValues
	 */
	public final void setSaveEmptyValues(boolean saveEmptyValues) {
		this.saveEmptyValues = saveEmptyValues;
	}

	/**
	 * 若此配置包含任意键则返回 true。可覆盖以实现
	 * 自定义逻辑。
	 *
	 * @return
	 */
	public boolean isValid() {
		return !this.section.map.isEmpty();
	}

	// ------------------------------------------------------------------------------------
	// File manipulation
	// ------------------------------------------------------------------------------------

	/**
	 * 尝试从 JAR 中给定的内部路径加载配置。
	 * 如果插件文件夹中不存在该文件，会自动将其移过去。
	 * 支持子文件夹，例如：localization/messages_en.yml
	 *
	 * @param internalPath
	 */
	public final void loadConfiguration(String internalPath) {
		this.loadConfiguration(internalPath, internalPath);
	}

	/**
	 * 从 JAR 文件中可选的来源路径加载配置，
	 * 若插件文件夹中给定路径下不存在该文件，则将其解压过去。
	 *
	 * @param from
	 * @param to
	 */
	public final void loadConfiguration(@Nullable String from, String to) {
		File file;

		if (from != null) {

			// Copy if not exists yet
			file = FileUtil.extract(from, to);

			// Initialize file early
			this.file = file;

			// Keep a loaded copy to copy default values from
			final YamlConfig defaultConfig = new YamlConfig();
			final String defaultContent = String.join("\n", FileUtil.getInternalFileContent(from));

			defaultConfig.file = file;
			defaultConfig.loadFromString(defaultContent);

			this.defaults = defaultConfig.section;
			this.defaultsPath = from;
		}

		else {
			file = FileUtil.getFile(to);

			if (!file.exists()) {
				FileUtil.createIfNotExists(to);

				if (this.getHeader() != null)
					this.shouldSave = true;
			}
		}

		this.load(file);
	}

	/**
	 * 从内部路径加载配置，但不调用 onLoad()、
	 * 不设置默认值，也不解压文件。
	 *
	 * @param internalPath
	 */
	public final void loadInternal(String internalPath) {
		final String content = String.join("\n", FileUtil.getInternalFileContent(internalPath));

		this.loadFromString(content);
	}

	/*
	 * Dumps all values in this config into a saveable format
	 */
	@NonNull
	@Override
	public final String saveToString() {

		// Do not use comments
		if (this.defaults == null || !this.saveComments()) {
			String header = "";

			if (this.getHeader() != null) {
				for (final String line : this.getHeader())
					header += "# " + line + "\n";

				header += "\n";
			}

			final Map<String, Object> values = this.section.getValues(false);

			if (!this.saveEmptyValues)
				removeEmptyValues(values);

			String dump = this.yaml.dump(values);

			// Blank config
			if (dump.equals("{}\n"))
				dump = "";

			return header + dump;
		}

		// Special case, write using comments engine
		YamlComments.writeComments(this.defaultsPath, this.file, this.getUncommentedSections());

		return null;
	}

	/*
	 * Attempts to remove empty maps, lists or arrays from the given map
	 */
	private static void removeEmptyValues(Map<String, Object> map) {
		for (final Iterator<Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
			final Entry<String, Object> entry = it.next();
			final Object value = entry.getValue();

			if (value instanceof ConfigSection) {
				final Map<String, Object> childMap = ((ConfigSection) value).map;

				removeEmptyValues(childMap);

				if (childMap.isEmpty())
					it.remove();
			}

			if (value == null
					|| value instanceof Iterable<?> && !((Iterable<?>) value).iterator().hasNext()
					|| value.getClass().isArray() && ((Object[]) value).length == 0
					|| value instanceof Map<?, ?> && ((Map<?, ?>) value).isEmpty()) {

				it.remove();

				continue;
			}
		}
	}

	/*
	 * Loads configuration from the given string contents
	 */
	@Override
	final void loadFromString(@NonNull String contents) {

		Map<?, ?> input;

		try {
			input = (Map<?, ?>) this.yaml.load(contents);

		} catch (final YAMLException ex) {
			throw ex;

		} catch (final ClassCastException e) {
			throw new IllegalArgumentException("Top level is not a Map.");
		}

		final String header = this.parseHeader(contents);

		if (header.trim().length() > 0)
			this.setHeader(header);

		this.section.map.clear();

		if (input != null)
			this.convertMapsToSections(input, this.section);
	}

	/*
	 * Converts the given maps to sections
	 */
	private void convertMapsToSections(@NonNull Map<?, ?> input, @NonNull ConfigSection section) {
		for (final Map.Entry<?, ?> entry : input.entrySet()) {
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof Map)
				this.convertMapsToSections((Map<?, ?>) value, section.createSection(key));
			else
				section.store(key, value);
		}
	}

	/*
	 * Converts the given input to header
	 */
	@NonNull
	private String parseHeader(@NonNull String input) {
		final String commentPrefix = "# ";
		final String[] lines = input.split("\r?\n", -1);
		final StringBuilder result = new StringBuilder();

		boolean readingHeader = true;
		boolean foundHeader = false;

		for (int i = 0; i < lines.length && readingHeader; i++) {
			final String line = lines[i].trim();

			if (line.startsWith(commentPrefix) || line.equals("#")) {
				if (i > 0)
					result.append("\n");

				if (line.length() > commentPrefix.length())
					result.append(line.substring(commentPrefix.length()));

				foundHeader = true;

			} else if (foundHeader && line.length() == 0)
				result.append("\n");

			else if (foundHeader)
				readingHeader = false;
		}

		final String string = result.toString();

		return string.trim().isEmpty() ? "" : string + "\n";
	}

	@Override
	public int hashCode() {
		return this.getFileName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof YamlConfig && ((YamlConfig) obj).getFileName().equals(this.getFileName());
	}

	// -----------------------------------------------------------------------------------------------------
	// Static
	// -----------------------------------------------------------------------------------------------------

	/**
	 * 从 JAR 内部路径加载配置，必要时将其解压。
	 *
	 * @param path
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromInternalPath(@NonNull String path) {

		final YamlConfig config = new YamlConfig();

		try {
			config.loadConfiguration(path);

		} catch (final Exception ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + path, ex);
		}

		return config;
	}

	/**
	 * 从 JAR 内部路径加载配置，不设置文件、
	 * 不解压，也不使用默认值。
	 *
	 * @param path
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromInternalPathFast(@NonNull String path) {

		final YamlConfig config = new YamlConfig();

		try {
			config.loadInternal(path);

		} catch (final Exception ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + path, ex);
		}

		return config;
	}

	/**
	 * 从插件文件夹中的文件加载配置。
	 *
	 * @param file
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromFile(@NonNull File file) {

		final YamlConfig config = new YamlConfig();

		try {
			config.load(file);
		} catch (final Exception ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
		}

		return config;
	}

	/**
	 * 从插件文件夹中的文件加载配置。
	 *
	 * @param file
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromFileFast(@NonNull File file) {
		final YamlConfig config = new YamlConfig();

		try {
			final List<String> content = FileUtil.readLines(file);
			config.loadFromString(String.join("\n", content));

		} catch (final Exception ex) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
		}

		return config;
	}

	// -----------------------------------------------------------------------------------------------------
	// Classes
	// -----------------------------------------------------------------------------------------------------

	/**
	 * 辅助类，归功于原 Bukkit/Spigot 团队，由 MineAcademy 增强
	 */
	private final static class YamlConstructor extends SafeConstructor {

		public YamlConstructor(LoaderOptions options) {
			super(options);

			this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
		}

		public YamlConstructor() {
			super();

			this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
		}

		private class ConstructCustomObject extends ConstructYamlMap {

			@Override
			public Object construct(@NonNull Node node) {
				if (node.isTwoStepsConstruction())
					throw new YAMLException("Unexpected referential mapping structure. Node: " + node);

				final Map<?, ?> raw = (Map<?, ?>) super.construct(node);

				if (raw.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
					final Map<String, Object> typed = new LinkedHashMap<>(raw.size());
					for (final Map.Entry<?, ?> entry : raw.entrySet())
						typed.put(entry.getKey().toString(), entry.getValue());

					try {
						return ConfigurationSerialization.deserializeObject(typed);
					} catch (final IllegalArgumentException ex) {
						throw new YAMLException("Could not deserialize object", ex);
					}
				}

				return raw;
			}

			@Override
			public void construct2ndStep(@NonNull Node node, @NonNull Object object) {
				throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
			}
		}
	}

	/**
	 * 辅助类，归功于原 Bukkit/Spigot 团队，由 MineAcademy 增强
	 */
	private final static class YamlRepresenter extends Representer {

		public YamlRepresenter(DumperOptions options) {
			super(options);

			this.multiRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
			this.multiRepresenters.put(ConfigSection.class, new RepresentConfigurationSection());
			this.multiRepresenters.remove(Enum.class);
		}

		public YamlRepresenter() {
			super();

			this.multiRepresenters.put(ConfigurationSerializable.class, new RepresentConfigurationSerializable());
			this.multiRepresenters.put(ConfigSection.class, new RepresentConfigurationSection());
			this.multiRepresenters.remove(Enum.class);
		}

		private class RepresentConfigurationSection extends RepresentMap {

			@NonNull
			@Override
			public Node representData(@NonNull Object data) {
				return super.representData(((ConfigSection) data).getValues(false));
			}
		}

		private class RepresentConfigurationSerializable extends RepresentMap {

			@NonNull
			@Override
			public Node representData(@NonNull Object data) {
				final ConfigurationSerializable serializable = (ConfigurationSerializable) data;
				final Map<String, Object> values = new LinkedHashMap<>();
				values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
				values.putAll(serializable.serialize());

				return super.representData(values);
			}
		}
	}
}
