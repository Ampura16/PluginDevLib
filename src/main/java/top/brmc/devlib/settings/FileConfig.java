package top.brmc.devlib.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.Common;
import top.brmc.devlib.SerializeUtil;
import top.brmc.devlib.SerializeUtil.Mode;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.collection.StrictList;
import top.brmc.devlib.command.SimpleCommand;
import top.brmc.devlib.command.SimpleCommandGroup;
import top.brmc.devlib.exception.EventHandledException;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.model.BoxedMessage;
import top.brmc.devlib.model.ConfigSerializable;
import top.brmc.devlib.model.IsInList;
import top.brmc.devlib.model.SimpleSound;
import top.brmc.devlib.model.SimpleTime;
import top.brmc.devlib.model.Tuple;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.remain.Remain;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;

/**
 * 表示任何可以存储在文件中的配置
 */
public abstract class FileConfig {

	/**
	 * 用于同步加载/保存，并强制使用同一文件的多个实例
	 * 使用相同的内容进行设置/保存。
	 */
	private static final Map<String, ConfigSection> loadedSections = new HashMap<>();

	/**
	 * 表示 "null"，在加载没有内部来源路径的配置时
	 * 可作为便捷的简写使用。
	 */
	public static final String NO_DEFAULT = null;

	/**
	 * 切换设置文件内容的存储方式。默认为 YAML。
	 *
	 * TODO 实现 JSON 设置存储
	 */
	final SerializeUtil.Mode mode = Mode.YAML;

	/*
	 * The file that is being used
	 */
	@Nullable
	@Setter(value = AccessLevel.PROTECTED)
	File file;

	/*
	 * The main config section, overridden in load(File)
	 */
	ConfigSection section = new ConfigSection();

	/*
	 * Optional defaults section to copy values from
	 */
	@Nullable
	ConfigSection defaults;

	/*
	 * Defaults path in your JAR file, if any
	 */
	@Nullable
	String defaultsPath;

	/**
	 * 可选的配置文件头
	 */
	@Nullable
	private String[] header;

	/**
	 * 调用任何 getX 方法时自动添加的路径前缀，
	 * 可节省你的时间。
	 */
	private String pathPrefix = null;

	/**
	 * 调用 {@link #load(File)} 时，即使文件之前已加载过，是否也总是重新加载？
	 *
	 * 默认为 true
	 */
	@Setter(value = AccessLevel.PROTECTED)
	private boolean alwaysLoad = true;

	/*
	 * Internal flag to only save once during loading and save automatically
	 * after loading if any changes were made.
	 */
	boolean shouldSave = false;

	/*
	 * Internal flag to avoid duplicate save calls during loading
	 */
	private boolean loading = false;

	/*
	 * Internal flag to avoid race condition when calling save() in onSave().
	 */
	private boolean saving = false;

	protected FileConfig() {
	}

	// ------------------------------------------------------------------------------------
	// Getting fields
	// ------------------------------------------------------------------------------------

	/**
	 * 返回当前节中的所有键。
	 *
	 * @param deep
	 * @return
	 */
	@NonNull
	public final Set<String> getKeys(boolean deep) {
		return this.section.getKeys(deep);
	}

	/**
	 * 返回当前节中的所有值。值可能是另一个 {@link ConfigSection}
	 *
	 * @param deep
	 * @return
	 *
	 * @deprecated 建议改用 getMap("")，或遍历 getKeys(deep) 并对每个 key 调用 getMap(key)，
	 * 			   然后将结果输出到控制台以理解二者的区别
	 */
	@Deprecated
	public final Map<String, Object> getValues(boolean deep) {
		return this.section.getValues(deep);
	}

	/**
	 * 返回给定路径处的值。路径前缀会自动添加，参见 setPathPrefix(String)。
	 * 如果你的 JAR 中存在默认配置，且该值不存在，我们会复制该值并保存文件。
	 * 指定要自动转换成的类型。如果你获取的值是自定义类，
	 * 且其 deserialize 方法带有自定义参数，请在此传入。例如：你的自定义类
	 * 有 deserialize(SerializedMap, Player) 方法，则在 deserializeParams 中传入玩家实例
	 *
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param deserializeParams
	 * @return
	 */
	public final <T> T get(final String path, final Class<T> type, Object... deserializeParams) {
		return this.get(path, type, null, deserializeParams);
	}

	/**
	 * 返回给定路径处的值。路径前缀会自动添加，参见 setPathPrefix(String)。
	 * 如果你的 JAR 中存在默认配置，且该值不存在，我们会复制该值并保存文件。
	 * 指定要自动转换成的类型。如果你获取的值是自定义类，
	 * 且其 deserialize 方法带有自定义参数，请在此传入。例如：你的自定义类
	 * 有 deserialize(SerializedMap, Player) 方法，则在 deserializeParams 中传入玩家实例
	 *
	 * 若键不存在，你在此指定的 def 值不会被复制/保存，我们会改为尝试从你 JAR 中的
	 * 默认文件复制。只有当 JAR 文件和该键都不存在时才返回它。
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param def
	 * @param deserializeParams
	 * @return
	 */
	public final <T> T get(@NonNull String path, Class<T> type, T def, Object... deserializeParams) {

		path = this.buildPathPrefix(path);

		// Copy defaults if not set and log about this change
		this.copyDefault(path, type);

		Object raw = this.section.retrieve(path);

		if (this.defaults != null && def == null)
			Valid.checkNotNull(raw, "Failed to set '" + path + "' to " + type.getSimpleName() + " from default config's value: " + this.defaults.retrieve(path));

		if (raw != null) {

			// Workaround for empty lists
			if (raw.equals("[]") && type == List.class)
				raw = new ArrayList<>();

			// Retype manually
			if (type == Long.class && raw instanceof Integer)
				raw = ((Integer) raw).longValue();

			raw = SerializeUtil.deserialize(this.mode, type, raw, deserializeParams);
			this.checkAssignable(path, raw, type);

			return (T) raw;
		}

		return def;
	}

	/*
	 * Attempts to copy a key at the given path from inbuilt JAR to the disk.
	 */
	private void copyDefault(final String path, final Class<?> type) {
		if (this.defaults != null && !this.section.isStored(path)) {
			final Object object = this.defaults.retrieve(path);
			Valid.checkNotNull(object, "Inbuilt config " + this.getFileName() + " lacks " + (object == null ? "key" : object.getClass().getSimpleName()) + " at \"" + path + "\". Is it outdated?");

			Common.log("&7Updating " + this.getFileName() + " at &b\'&f" + path + "&b\' &7-> " + (object == null ? "&ckey removed" : "&b\'&f" + object.toString().replace("\n", ", ") + "&b\'") + "&r");
			this.section.store(path, object);
			this.shouldSave = true;
		}
	}

	/*
	 * Attempts to force a certain class type for the given object, used to prevent mistakes
	 * such as putting "Enabled: truee" (which is a String) instead of "Enabled: true" (which is a Boolean)
	 */
	private void checkAssignable(final String path, final Object object, final Class<?> type) {
		if (!type.isAssignableFrom(object.getClass()) && !type.getSimpleName().equals(object.getClass().getSimpleName())) {

			// Exceptions
			if (ConfigSerializable.class.isAssignableFrom(type) && object instanceof ConfigSection)
				return;

			throw new FoException("Malformed configuration! Key '" + path + "' in " + this.getFileName() + " must be " + type.getSimpleName() + " but got " + object.getClass().getSimpleName() + ": '" + object + "'");
		}
	}

	// ------------------------------------------------------------------------------------
	// Getting values helpers
	// ------------------------------------------------------------------------------------

	/**
	 * 返回给定路径处键的 String 值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 即使该键是只有一个值的列表，或是数字或布尔值，也同样有效。
	 *
	 * @param path
	 * @return
	 */
	public final String getString(final String path) {
		return this.getString(path, null);
	}

	/**
	 * 返回给定路径处键的 String 值，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 即使该键是只有一个值的列表，或是数字或布尔值，也同样有效。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final String getString(final String path, final String def) {
		final Object object = this.getObject(path, def);

		if (object == null)
			return null;

		else if (object instanceof List)
			return Common.join((List<?>) object, "\n");

		else if (object instanceof String[])
			return Common.join(Arrays.asList((String[]) object), "\n");

		else if (object.getClass().isArray())
			return Common.join((Object[]) object);

		else if (object instanceof Boolean
				|| object instanceof Integer
				|| object instanceof Long
				|| object instanceof Double
				|| object instanceof Float)
			return Objects.toString(object);

		else if (object instanceof Number)
			return object.toString();

		else if (object instanceof String)
			return (String) object;

		throw new FoException("Excepted string at '" + path + "' in " + this.getFileName() + ", got (" + object.getClass() + "): " + object);
	}

	/**
	 * 返回给定路径处键的 Boolean 值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final Boolean getBoolean(final String path) {
		return this.getBoolean(path, null);
	}

	/**
	 * 返回给定路径处键的 Boolean 值，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Boolean getBoolean(final String path, final Boolean def) {
		return this.get(path, Boolean.class, def);
	}

	/**
	 * 返回给定路径处键的 Integer 值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final Integer getInteger(final String path) {
		return this.getInteger(path, null);
	}

	/**
	 * 返回给定路径处键的 Integer 值，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Integer getInteger(final String path, final Integer def) {
		return this.get(path, Integer.class, def);
	}

	/**
	 * 返回给定路径处键的 Long 值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final Long getLong(final String path) {
		return this.getLong(path, null);
	}

	/**
	 * 返回给定路径处键的 Long 值，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Long getLong(final String path, final Long def) {
		return this.get(path, Long.class, def);
	}

	/**
	 * 返回给定路径处键的 Double 值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final Double getDouble(final String path) {
		return this.getDouble(path, null);
	}

	/**
	 * 返回给定路径处键的 Double 值，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Double getDouble(final String path, final Double def) {
		final Object raw = this.getObject(path, def);

		if (raw != null)
			Valid.checkBoolean(raw instanceof Number, "Expected a number at '" + path + "', got " + raw.getClass().getSimpleName() + ": " + raw);

		return raw != null ? ((Number) raw).doubleValue() : null;
	}

	/**
	 * 返回给定路径处键的 Location
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 我们使用自定义方法将位置存储在一行中；这与 Bukkit 的 getLocation
	 * 不兼容，因为 Bukkit 使用多个键进行存储。
	 *
	 * @param path
	 * @return
	 */
	public final Location getLocation(final String path) {
		return this.getLocation(path, null);
	}

	/**
	 * 返回给定路径处键的 Location，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 我们使用自定义方法将位置存储在一行中；这与 Bukkit 的 getLocation
	 * 不兼容，因为 Bukkit 使用多个键进行存储。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Location getLocation(final String path, final Location def) {
		return this.get(path, Location.class, def);
	}

	/**
	 * 返回给定路径处键的 OfflinePlayer
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final OfflinePlayer getOfflinePlayer(final String path) {
		return this.getOfflinePlayer(path, null);
	}

	/**
	 * 返回给定路径处键的 OfflinePlayer，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final OfflinePlayer getOfflinePlayer(final String path, final OfflinePlayer def) {
		return this.get(path, OfflinePlayer.class, def);
	}

	/**
	 * 返回给定路径处键的音效
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final SimpleSound getSound(final String path) {
		return this.getSound(path, null);
	}

	/**
	 * 返回给定路径处键的音效，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final SimpleSound getSound(final String path, final SimpleSound def) {
		return this.get(path, SimpleSound.class, def);
	}

	/**
	 * 返回给定路径处键的 "case"（词格）
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 其含义请参见 {@link AccusativeHelper} 的类头文档。
	 *
	 * @param path
	 * @return
	 */
	public final AccusativeHelper getAccusativePeriod(final String path) {
		return this.getAccusativePeriod(path, null);
	}

	/**
	 * 返回给定路径处键的 "case"（词格），或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 其含义请参见 {@link AccusativeHelper} 的类头文档。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final AccusativeHelper getAccusativePeriod(final String path, final String def) {
		final String rawLine = this.getString(path, def);

		return rawLine != null ? new AccusativeHelper(rawLine) : null;
	}

	/**
	 * 返回给定路径处键的标题
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final TitleHelper getTitle(final String path) {
		return this.getTitle(path, null, null);
	}

	/**
	 * 返回给定路径处键的标题，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param defTitle
	 * @param defSubtitle
	 * @return
	 */
	public final TitleHelper getTitle(final String path, final String defTitle, final String defSubtitle) {
		final String title = this.getString(path + ".Title", defTitle);
		final String subtitle = this.getString(path + ".Subtitle", defSubtitle);

		return title != null ? new TitleHelper(title, subtitle) : null;
	}

	/**
	 * 返回给定路径处键的时间
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final SimpleTime getTime(final String path) {
		return this.getTime(path, null);
	}

	/**
	 * 返回给定路径处键的时间，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final SimpleTime getTime(final String path, final SimpleTime def) {
		return this.get(path, SimpleTime.class, def);
	}

	/**
	 * 返回给定路径处键的 double 百分比
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 它以字符串形式存储，例如 85%
	 *
	 * @param path
	 * @return
	 */
	public final Double getPercentage(String path) {
		return this.getPercentage(path, null);
	}

	/**
	 * 返回给定路径处键的 double 百分比，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 它以字符串形式存储，例如 85%
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Double getPercentage(String path, Double def) {

		final Object object = this.getObject(path, def);

		if (object != null) {
			final String raw = object.toString();
			Valid.checkBoolean(raw.endsWith("%"), "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must end with %! Got: " + raw);

			final String rawNumber = raw.substring(0, raw.length() - 1);
			Valid.checkInteger(rawNumber, "Your " + path + " key in " + this.getPathPrefix() + "." + path + " must be a whole number! Got: " + raw);

			return Integer.parseInt(rawNumber) / 100D;
		}

		return null;
	}

	/**
	 * 返回给定路径处键的、可良好格式化的消息
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final BoxedMessage getBoxedMessage(final String path) {
		return this.getBoxedMessage(path, null);
	}

	/**
	 * 返回给定路径处键的、可良好格式化的消息，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final BoxedMessage getBoxedMessage(final String path, final BoxedMessage def) {
		return this.get(path, BoxedMessage.class, def);
	}

	/**
	 * 返回给定路径处键的材质
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final CompMaterial getMaterial(final String path) {
		return this.getMaterial(path, null);
	}

	/**
	 * 返回给定路径处键的材质，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final CompMaterial getMaterial(final String path, CompMaterial def) {
		return this.get(path, CompMaterial.class, def);
	}

	/**
	 * 返回给定路径处键的物品
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final ItemStack getItemStack(@NonNull String path) {
		return this.getItemStack(path, null);
	}

	/**
	 * 返回给定路径处键的物品，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final ItemStack getItemStack(@NonNull String path, ItemStack def) {
		return this.get(path, ItemStack.class, def);
	}

	/**
	 * 返回给定路径处键的元组
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 它以包含两个子键的映射形式存储，一个存第一个值，另一个存第二个值
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public final <K, V> Tuple<K, V> getTuple(final String key, Class<K> keyType, Class<V> valueType) {
		return this.getTuple(key, null, keyType, valueType);
	}

	/**
	 * 返回给定路径处键的元组，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 它以包含两个子键的映射形式存储，一个存第一个值，另一个存第二个值
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param def
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public final <K, V> Tuple<K, V> getTuple(final String key, final Tuple<K, V> def, Class<K> keyType, Class<V> valueType) {
		return this.get(key, Tuple.class, def, keyType, valueType);
	}

	/**
	 * 返回给定路径处键的未指定类型对象
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final Object getObject(final String path) {
		return this.getObject(path, null);
	}

	/**
	 * 返回给定路径处键的未指定类型对象，或提供默认值
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @param def
	 * @return
	 */
	public final Object getObject(final String path, final Object def) {
		return this.get(path, Object.class, def);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回给定路径处键的特殊位置列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 * （参见 {@link LocationList}）
	 *
	 * @param path
	 * @return
	 */
	public final LocationList getLocationList(final String path) {
		return new LocationList(this, this.getList(path, Location.class));
	}

	/**
	 * 返回给定路径处键的 maps\<string, object\> 列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final List<SerializedMap> getMapList(final String path) {
		return this.getList(path, SerializedMap.class);
	}

	/**
	 * 返回给定路径处键的特殊 {@link IsInList} 列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 它是用于检查某个值是否在其中的列表，可以包含 ["*"] 以匹配全部。
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	public final <T> IsInList<T> getIsInList(String path, Class<T> type) {
		final List<String> stringList = this.getStringList(path);

		if (stringList.size() == 1 && "*".equals(stringList.get(0)))
			return IsInList.fromStar();

		return IsInList.fromList(this.getList(path, type));
	}

	/**
	 * 返回给定路径处键的字符串列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final List<String> getStringList(final String path) {
		final Object raw = this.getObject(path);

		if (raw == null)
			return new ArrayList<>();

		if (raw instanceof String) {
			final String output = (String) raw;

			return "'[]'".equals(output) || "[]".equals(output) ? new ArrayList<>() : this.fixYamlBooleansInList((Object[]) output.split("\n"));
		}

		if (raw instanceof List)
			return this.fixYamlBooleansInList(((List<Object>) raw).toArray());

		throw new FoException("Excepted a list at '" + path + "' in " + this.getFileName() + ", got (" + raw.getClass() + "): " + raw);
	}

	/*
	 * Attempts to convert objects into strings, since SnakeYAML parser interprets
	 * "true" and "yes" as boolean types
	 */
	private List<String> fixYamlBooleansInList(@NonNull final Object... list) {
		final List<String> newList = new ArrayList<>();

		for (final Object obj : list)
			if (obj != null)
				newList.add(obj.toString());

		return newList;
	}

	/**
	 * 返回给定路径处键的、用作命令的字符串列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 这是一个基本的字符串列表，但我们强制至少有一个值（第一个 ->
	 * 主命令标签），并移除每一项开头的 /。
	 *
	 * 可用于 {@link SimpleCommandGroup} 或 {@link SimpleCommand}
	 *
	 * @param path
	 * @return
	 */
	@Nullable
	public final StrictList<String> getCommandList(final String path) {
		final List<String> list = this.getStringList(path);
		Valid.checkBoolean(!list.isEmpty(), "Please set at least one command alias in '" + path + "' (" + this.getFileName() + ") for this will be used as your main command!");

		for (int i = 0; i < list.size(); i++) {
			String command = list.get(i);

			command = command.startsWith("/") ? command.substring(1) : command;
			list.set(i, command);
		}

		return new StrictList<>(list);
	}

	/**
	 * 返回给定路径处键的材质列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final List<CompMaterial> getMaterialList(final String path) {
		return this.getList(path, CompMaterial.class);
	}

	/**
	 * 返回给定路径处键的给定类型集合
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @param deserializeParameters
	 * @return
	 */
	public final <T> Set<T> getSet(final String key, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = this.getList(key, type);

		return list == null ? new HashSet<>() : new HashSet<>(list);
	}

	/**
	 * 返回给定键值类型的元组列表
	 *
	 * @param <K>
	 * @param <V>
	 * @param path
	 * @param tupleKey
	 * @param tupleValue
	 * @return
	 */
	public final <K, V> List<Tuple<K, V>> getTupleList(final String path, final Class<K> tupleKey, final Class<V> tupleValue) {
		final List<Tuple<K, V>> list = new ArrayList<>();

		for (final Object object : this.getList(path))
			if (object == null)
				list.add(null);
			else {
				final Tuple<K, V> tuple = Tuple.deserialize(SerializedMap.of(object), tupleKey, tupleValue);

				list.add(tuple);
			}

		return list;
	}

	/**
	 * 返回给定路径处键的给定类型列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @param deserializeParameters
	 * @return
	 */
	public final <T> List<T> getList(final String path, final Class<T> type, final Object... deserializeParameters) {
		final List<T> list = new ArrayList<>();
		final List<Object> objects = this.getList(path);

		if (type == Map.class && deserializeParameters != null & deserializeParameters.length > 0 && deserializeParameters[0] != String.class)
			throw new FoException("getList('" + path + "') that returns Map must have String.class as key, not " + deserializeParameters[0]);

		if (objects != null)
			for (Object object : objects) {
				object = object != null ? SerializeUtil.deserialize(this.mode, type, object, deserializeParameters) : null;

				if (object != null)
					list.add((T) object);

				else if (!type.isPrimitive() && type != String.class)
					list.add(null);
			}

		return list;
	}

	/**
	 * 返回给定路径处键的列表（未设置则为空）
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * 为方便起见，我们允许使用单个值代替列表，例如
	 * "Apply_On: timed" 代替 "Apply_On: [timed]"
	 *
	 * @param path
	 * @return
	 */
	public final List<Object> getList(final String path) {
		final Object obj = this.getObject(path);

		if (obj != null && obj.toString().equals("[]"))
			return new ArrayList<>();

		return obj instanceof Collection<?> ? new ArrayList<>((Collection<Object>) obj) : obj != null ? Arrays.asList(obj) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting maps
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回给定路径处键的 map\<string, object\>
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param path
	 * @return
	 */
	public final SerializedMap getMap(final String path) {
		final LinkedHashMap<?, ?> map = this.getMap(path, Object.class, Object.class);

		return SerializedMap.of(map);
	}

	/**
	 * 返回给定路径处键的、具有给定键和值类型的映射
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param valueType
	 * @param valueDeserializeParams
	 * @return
	 */
	public final <Key, Value> LinkedHashMap<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType, Object... valueDeserializeParams) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, Value> map = new LinkedHashMap<>();
		final boolean exists = this.isSet(path);

		// Add path prefix right away
		path = this.buildPathPrefix(path);

		// Add defaults
		if (this.defaults != null && !exists) {
			Valid.checkBoolean(this.defaults.isStored(path), "Default '" + this.getFileName() + "' lacks a map at " + path);

			for (final String key : this.defaults.retrieveConfigurationSection(path).getKeys(false))
				this.copyDefault(path + "." + key, valueType);
		}

		// Load key-value pairs from config to our map
		final Object savedKeys = this.section.retrieve(path);

		if (savedKeys != null)
			for (final Map.Entry<String, Object> entry : SerializedMap.of(this.section.retrieve(path))) {
				final Key key = SerializeUtil.deserialize(this.mode, keyType, entry.getKey());
				final Value value;

				if (LocationList.class.isAssignableFrom(valueType)) {
					final List<?> list = SerializeUtil.deserialize(this.mode, List.class, entry.getValue());
					final List<Location> copy = new ArrayList<>();

					list.forEach(locationRaw -> copy.add(SerializeUtil.deserializeLocation(locationRaw)));

					value = (Value) new LocationList(this, copy);

				} else
					value = SerializeUtil.deserialize(this.mode, valueType, entry.getValue(), valueDeserializeParams);

				// Ensure the pair values are valid for the given paramenters
				this.checkAssignable(path, key, keyType);
				this.checkAssignable(path, value, valueType);

				map.put(key, value);
			}

		return map;
	}

	/**
	 * 返回给定路径处键的、具有给定类型的映射列表
	 * （参见 {@link #get(String, Class, Object, Object...)}）。
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param setType
	 * @param setDeserializeParameters
	 * @return
	 */
	public final <Key, Value> LinkedHashMap<Key, List<Value>> getMapList(@NonNull String path, final Class<Key> keyType, final Class<Value> setType, Object... setDeserializeParameters) {

		// The map we are creating, preserve order
		final LinkedHashMap<Key, List<Value>> map = new LinkedHashMap<>();
		final boolean exists = this.isSet(path);

		// Add path prefix right away
		path = this.buildPathPrefix(path);

		// Add defaults
		if (this.defaults != null && !exists) {
			Valid.checkBoolean(this.defaults.isStored(path), "Default '" + this.getFileName() + "' lacks a map at " + path);

			for (final String key : this.defaults.retrieveConfigurationSection(path).getKeys(false))
				this.copyDefault(path + "." + key, setType);
		}

		// Load key-value pairs from config to our map
		if (exists)
			for (final Map.Entry<String, Object> entry : SerializedMap.of(this.section.retrieve(path)).entrySet()) {
				final Key key = SerializeUtil.deserialize(this.mode, keyType, entry.getKey());
				final List<Value> value = SerializeUtil.deserialize(this.mode, List.class, entry.getValue(), setDeserializeParameters);

				// Ensure the pair values are valid for the given parameters
				this.checkAssignable(path, key, keyType);

				if (!value.isEmpty())
					for (final Value item : value)
						this.checkAssignable(path, item, setType);

				map.put(key, value);
			}

		return map;
	}

	// ------------------------------------------------------------------------------------
	// Setting values
	// ------------------------------------------------------------------------------------

	/**
	 * 将给定值设置到给定路径（将值设为 null 可移除它），
	 * 然后立即保存文件。
	 *
	 * 路径前缀会自动添加，参见 getPathPrefix()
	 * 该值使用 {@link SerializeUtil} 进行序列化
	 *
	 * @param path
	 * @param value
	 */
	public final void save(String path, Object value) {
		this.set(path, value);

		this.save();
	}

	/**
	 * 将给定值设置到给定路径（将值设为 null 可移除它）。
	 *
	 * 路径前缀会自动添加，参见 getPathPrefix()
	 * 该值使用 {@link SerializeUtil} 进行序列化
	 *
	 * @param path
	 * @param value
	 */
	public final void set(String path, Object value) {
		path = this.buildPathPrefix(path);
		value = SerializeUtil.serialize(this.mode, value);

		this.section.store(path, value);
		this.shouldSave = true;
	}

	/**
	 * 若给定路径包含非 null 值则返回 true
	 *
	 * 路径前缀会自动添加，参见 getPathPrefix()
	 *
	 * @param path
	 * @return
	 */
	public final boolean isSet(String path) {
		path = this.buildPathPrefix(path);

		return this.section.isStored(path);
	}

	/**
	 * 若已设置默认值且在给定路径处包含非 null 值
	 * 则返回 true
	 *
	 * 路径前缀会自动添加，参见 getPathPrefix()
	 *
	 * @param path
	 * @return
	 */
	public final boolean isSetDefault(String path) {
		path = this.buildPathPrefix(path);

		return this.defaults != null && this.defaults.isStored(path);
	}

	/**
	 * 尝试将给定键从相对路径（会添加路径前缀）
	 * 移动到绝对的新路径（不添加路径前缀）
	 *
	 * @param fromPathRel
	 * @param toPathAbs
	 */
	public final void move(String fromPathRel, final String toPathAbs) {
		final Object oldObject = this.getObject(fromPathRel);

		// Remove the old object
		this.set(fromPathRel, null);

		// Set it as absolute, do not add path prefix
		this.section.store(toPathAbs, oldObject);

		Common.log("&7Update " + this.getFileName() + ". Move &b\'&f" + this.buildPathPrefix(fromPathRel) + "&b\' &7(was \'" + oldObject + "&7\') to " + "&b\'&f" + toPathAbs + "&b\'" + "&r");
	}

	// ------------------------------------------------------------------------------------
	// File manipulation
	// ------------------------------------------------------------------------------------

	/**
	 * 尝试加载文件配置，不保存自上次加载以来所做的任何修改。
	 */
	public final void reload() {
		if (this.file == null && this.skipSaveIfNoFile())
			return;

		Valid.checkNotNull(this.file, "Cannot call reload() before loading a file!");

		this.load(this.file);
	}

	/*
	 * Helper to load configuration from a file
	 */
	public final void load(@NonNull File file) {
		try {
			Valid.checkBoolean(!this.loading, "Called load(" + file + ") on already being loaded configuration!");
			this.loading = true;

			final FileInputStream stream = new FileInputStream(file);
			final String path = file.getAbsolutePath();
			boolean loadedBefore = false;
			ConfigSection section = loadedSections.get(path);

			if (section == null) {
				section = new ConfigSection();

				loadedSections.put(path, section);
			}

			else
				loadedBefore = true;

			this.section = section;
			this.file = file;

			if (loadedBefore && !this.alwaysLoad) {
				// Do not load
			} else
				this.load(new InputStreamReader(stream, StandardCharsets.UTF_8));

			try {
				this.onLoad();
				this.onLoadFinish();

			} catch (final EventHandledException ex) {
				// Handled successfully in the polymorphism pipeline
			}

			if (this.shouldSave || this.alwaysSaveOnLoad()) {
				this.loading = false;
				this.save();

				this.shouldSave = false;
			}

		} catch (final Exception ex) {
			Common.throwError(ex, "Error loading " + file + ": " + ex);

		} finally {
			this.loading = false;
		}
	}

	/*
	 * Helper to load configuration from a reader
	 */
	private final void load(@NonNull Reader reader) {
		try {
			final BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
			final StringBuilder builder = new StringBuilder();

			try {
				String line;

				while ((line = input.readLine()) != null) {
					builder.append(line);
					builder.append('\n');
				}

			} finally {
				input.close();
			}

			this.loadFromString(builder.toString());

		} catch (final Exception ex) {
			Remain.sneaky(ex);
		}
	}

	/**
	 * 由具体配置类型实现，用于从给定的字符串内容加载配置。
	 *
	 * @param contents
	 */
	abstract void loadFromString(@NonNull String contents);

	/**
	 * 配置加载完成后自动调用，用于在此加载你类中的
	 * 字段。
	 *
	 * 你可以在此抛出 {@link EventHandledException}，以指示子类中断加载
	 */
	protected void onLoad() {
	}

	/**
	 * @see #onLoad()
	 *
	 * @deprecated 已重命名为 {@link #onLoad()}，请改用它。
	 */
	@Deprecated
	protected void onLoadFinish() {
	}

	/**
	 * 立即将配置保存到文件（你需要先调用 loadConfiguration(File)）
	 */
	public final void save() {
		if (this.file == null && this.skipSaveIfNoFile())
			return;

		Valid.checkNotNull(this.file, "Cannot call save() for " + this + " when no file was set! Call load first!");

		this.save(this.file);
	}

	/**
	 * 将配置保存到给定文件，并更新此配置中存储的文件。
	 *
	 * @param file
	 */
	public final void save(@NonNull File file) {
		if (this.saving)
			return;

		try {
			if (this.loading) {
				this.shouldSave = true;

				return;
			}

			this.onPreSave();

			if (this.canSaveFile()) {

				try {
					this.saving = true;
					this.onSave();

				} catch (final EventHandledException ex) {
					// Ignore, indicated that we exited polymorphism inheritance prematurely by intention

				} finally {
					this.saving = false;
				}

				final File parent = file.getCanonicalFile().getParentFile();

				if (parent != null)
					parent.mkdirs();

				final String data = this.saveToString();

				if (data != null)
					try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
						writer.write(data);

					} catch (final Exception ex) {
						Remain.sneaky(ex);
					}

				// Update file
				this.file = file;
			}

		} catch (final Exception ex) {
			Remain.sneaky(ex);
		}
	}

	/**
	 * 若加载后总是应保存文件则返回 true。
	 *
	 * @return
	 */
	protected boolean alwaysSaveOnLoad() {
		return false;
	}

	/**
	 * 在 {@link #canSaveFile()} 之前自动调用
	 */
	protected void onPreSave() {
	}

	/**
	 * 保存配置时自动调用，你可以在此调用 "set(path, value)" 方法
	 * 来保存类字段。如果 {@link #saveToMap()} 不为 null，我们会自动保存其中的内容。
	 *
	 * 在 {@link #canSaveFile()} 之后调用
	 */
	protected void onSave() {
		final SerializedMap map = this.saveToMap();
		final SerializedMap legacy = this.serialize();

		if (legacy != null)
			map.put(legacy);

		if (map != null)
			for (final Map.Entry<String, Object> entry : map.entrySet())
				this.set(entry.getKey(), entry.getValue());
	}

	/**
	 * 返回调用 {@link #save()} 时文件是否可以保存
	 *
	 * @return
	 */
	protected boolean canSaveFile() {
		return true;
	}

	/**
	 * false（默认）= 未设置文件却尝试调用 save() 时会抛出异常
	 * true = 在上述情况下安静地失败
	 *
	 * @return
	 */
	protected boolean skipSaveIfNoFile() {
		return false;
	}

	/**
	 * 由具体配置实现，用于生成要保存的文件内容。
	 *
	 * @return
	 */
	@NonNull
	public abstract String saveToString();

	/**
	 * 重写以实现自定义保存机制，会在 onSave() 中自动使用；
	 * 你可以在此只返回真正想保存的数据。
	 *
	 * 默认返回 null！
	 *
	 * @return
	 */
	public SerializedMap saveToMap() {
		return null;
	}

	/**
	 * @see #saveToMap()
	 * @deprecated 已重命名，请改为重写 {@link #saveToMap()}
	 *
	 * @return
	 */
	@Deprecated
	protected SerializedMap serialize() {
		return null;
	}

	/**
	 * 从磁盘删除已加载的文件配置。
	 */
	public final void deleteFile() {
		Valid.checkNotNull(this.file, "Cannot unregister null file before settings were loaded!");

		if (this.file.exists())
			this.file.delete();

		loadedSections.remove(this.file.getAbsolutePath());
	}

	// ------------------------------------------------------------------------------------
	// Path prefix
	// ------------------------------------------------------------------------------------

	/**
	 * 返回当前路径前缀。说明请参见 {@link #setPathPrefix(String)}。
	 *
	 * @return
	 */
	protected final String getPathPrefix() {
		return this.pathPrefix;
	}

	/**
	 * 设置给定的路径前缀，设为 null 可移除。
	 *
	 * 路径前缀是一种便捷手段，可避免重复的
	 * 节调用，例如：
	 *
	 * get("Player.Name")
	 * get("Player.Health")
	 *
	 * 你可以将路径前缀设为 "Player"，然后直接调用 get("Name") 和 get("Health")，
	 * 我们会自动在每次路径调用前加上 "Player."。
	 *
	 * @param pathPrefix
	 */
	protected final void setPathPrefix(final String pathPrefix) {
		if (pathPrefix != null) {
			Valid.checkBoolean(!pathPrefix.endsWith("."), "Path prefix must not end with a dot: " + pathPrefix);
			Valid.checkBoolean(!pathPrefix.endsWith(".yml"), "Path prefix must not end with .yml!");
		}

		this.pathPrefix = pathPrefix != null && !pathPrefix.isEmpty() ? pathPrefix : null;
	}

	/*
	 * Helper method to add path prefix
	 */
	private final String buildPathPrefix(@NonNull final String path) {
		final String prefixed = this.pathPrefix != null ? this.pathPrefix + (!path.isEmpty() ? "." + path : "") : path;
		final String newPath = prefixed.endsWith(".") ? prefixed.substring(0, prefixed.length() - 1) : prefixed;

		// Check for a case where there is multiple dots at the end... #somePeople
		Valid.checkBoolean(!newPath.endsWith("."), "Path '" + path + "' must not end with '.' after path prefix '" + this.pathPrefix + "': " + newPath);
		return newPath;
	}

	// ------------------------------------------------------------------------------------
	// Final getters
	// ------------------------------------------------------------------------------------

	/**
	 * 返回文件的注释头，可能为 null
	 *
	 * @return
	 */
	public final String[] getHeader() {
		return this.header;
	}

	/**
	 * 设置文件的注释头，设为 null 可移除。
	 * 仅在未设置默认值，或对支持的配置禁用了注释保存时有效
	 *
	 * @param values
	 */
	public final void setHeader(String... values) {
		if (values == null)
			this.header = null;

		else {
			final String mainCommandLabel = Common.getOrEmpty(SimpleSettings.MAIN_COMMAND_ALIASES.first());
			final List<String> header = new ArrayList<>();

			for (int i = 0; i < values.length; i++) {
				final String line = Common.getOrEmpty(values[i]);

				for (final String subline : line.replace("{plugin}", SimplePlugin.getNamed()).replace("{label}", mainCommandLabel).split("\n"))
					header.add(subline);
			}

			this.header = Common.toArray(header);
		}
	}

	/**
	 * 移除整个配置节中的所有键
	 */
	public final void clear() {
		this.section.clear();
	}

	/**
	 * 返回文件名（如果有），不含扩展名
	 *
	 * @return
	 */
	public String getName() {
		final String fileName = this.getFileName();

		if (fileName != null) {
			final int lastDot = fileName.lastIndexOf(".");

			if (lastDot != -1)
				return fileName.substring(0, lastDot);
		}

		return null;
	}

	/**
	 * 返回文件名（如果已设置）
	 *
	 * @return
	 */
	public final String getFileName() {
		return this.file == null ? "no file" : this.file.getName();
	}

	/**
	 * 返回此配置中是否设置了任何键
	 *
	 * @return
	 */
	public final boolean isEmpty() {
		return this.section.isEmpty();
	}

	// ------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------

	@Deprecated // internal use only
	public static final void clearLoadedSections() {
		loadedSections.clear();
	}

	// ------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------

	/**
	 * 特定于语言的辅助类，用于处理计数等场景中的不同词格：
	 *
	 * "Please wait 1 second before your next message."
	 *
	 * 在斯洛伐克语等屈折变化丰富的语言中，词格会变化三次：
	 * 0 或 5+ 秒 = 5 sekúnd
	 * 1 = 1 sekundu
	 * 2-4 = 2 sekundy
	 *
	 * 此辅助类用于自动判断并获取正确的词格。我们
	 * 将这三个值保存在同一行中，以逗号分隔。
	 */
	public static final class AccusativeHelper {

		private final String accusativeSingural; // 1 second (Slovak case - sekundu)
		private final String accusativePlural; // 2-4 seconds (Slovak case - sekundy, not in English)
		private final String genitivePlural; // 0 or 5+ seconds (Slovak case - sekund)

		private AccusativeHelper(final String raw) {
			final String[] values = raw.split(", ");

			if (values.length == 2) {
				this.accusativeSingural = values[0];
				this.accusativePlural = values[1];
				this.genitivePlural = this.accusativePlural;

				return;
			}

			if (values.length != 3)
				throw new FoException("Malformed type, use format: 'second, seconds' OR 'sekundu, sekundy, sekund' (if your language has it)");

			this.accusativeSingural = values[0];
			this.accusativePlural = values[1];
			this.genitivePlural = values[2];
		}

		public String getPlural() {
			return this.genitivePlural;
		}

		public String formatWithCount(final long count) {
			return count + " " + this.formatWithoutCount(count);
		}

		public String formatWithoutCount(final long count) {
			if (count == 1)
				return this.accusativeSingural;

			if (count > 1 && count < 5)
				return this.accusativePlural;

			return this.genitivePlural;
		}

		public static AccusativeHelper of(String singular, String plural) {
			return new AccusativeHelper(singular + ", " + plural);
		}
	}

	/**
	 * 自动向玩家发送标题和副标题的辅助类。
	 */
	public static final class TitleHelper {

		private final String title, subtitle;

		private TitleHelper(final String title, final String subtitle) {
			this.title = Common.colorize(title);
			this.subtitle = Common.colorize(subtitle);
		}

		public void playLong(final Player player) {
			this.playLong(player, null);
		}

		public void playLong(final Player player, final Function<String, String> replacer) {
			this.play(player, 5, 4 * 20, 15, replacer);
		}

		public void playShort(final Player player) {
			this.playShort(player, null);
		}

		public void playShort(final Player player, final Function<String, String> replacer) {
			this.play(player, 3, 2 * 20, 5, replacer);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut) {
			this.play(player, fadeIn, stay, fadeOut, null);
		}

		public void play(final Player player, final int fadeIn, final int stay, final int fadeOut, Function<String, String> replacer) {
			Remain.sendTitle(player, fadeIn, stay, fadeOut, replacer != null ? replacer.apply(this.title) : this.title, replacer != null ? replacer.apply(this.subtitle) : this.subtitle);
		}
	}

	/**
	 * 用于存储位置点列表的辅助类，可一键移除/添加位置点，
	 * 并自动保存你的配置。
	 */
	public static final class LocationList implements Iterable<Location> {

		private final FileConfig settings;
		private final List<Location> points;

		public LocationList(final FileConfig settings) {
			this(settings, new ArrayList<>());
		}

		private LocationList(final FileConfig settings, final List<Location> points) {
			this.settings = settings;
			this.points = points;
		}

		public boolean toggle(final Location location) {
			for (final Location point : this.points)
				if (Valid.locationEquals(point, location)) {
					this.points.remove(point);

					this.settings.save();
					return false;
				}

			this.points.add(location);
			this.settings.save();

			return true;
		}

		public void add(final Location location) {
			Valid.checkBoolean(!this.hasLocation(location), "Location at " + location + " already exists!");

			this.points.add(location);
			this.settings.save();
		}

		public void remove(final Location location) {
			final Location point = this.find(location);
			Valid.checkNotNull(point, "Location at " + location + " does not exist!");

			this.points.remove(point);
			this.settings.save();
		}

		public boolean hasLocation(final Location location) {
			return this.find(location) != null;
		}

		public Location find(final Location location) {
			for (final Location entrance : this.points)
				if (Valid.locationEquals(entrance, location))
					return entrance;

			return null;
		}

		public List<Location> getLocations() {
			return Collections.unmodifiableList(this.points);
		}

		@Override
		public Iterator<Location> iterator() {
			return this.points.iterator();
		}

		public int size() {
			return this.points.size();
		}
	}
}
