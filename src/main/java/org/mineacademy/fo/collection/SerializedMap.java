package org.mineacademy.fo.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.SerializeUtil.Mode;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.jsonsimple.JSONObject;
import org.mineacademy.fo.jsonsimple.JSONParser;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.ConfigSection;

import lombok.Getter;
import lombok.NonNull;

/**
 * 序列化映射让你轻松保存并保留配置中的值，
 * 例如位置、其他映射或列表等，
 * 还有更多。
 */
public final class SerializedMap extends StrictCollection implements Iterable<Map.Entry<String, Object>> {

	/**
	 * 带值的内部映射
	 */
	private final StrictMap<String, Object> map = new StrictMap<>();

	/**
	 * 此映射是否从 json 字符串创建？
	 */
	@Getter
	private SerializeUtil.Mode mode;

	/**
	 * 此映射实例 get 时是否移除条目，
	 */
	private boolean removeOnGet = false;

	/**
	 * 用给定首个键值对创建新的序列化映射
	 *
	 * @param key
	 * @param value
	 */
	private SerializedMap(final String key, final Object value) {
		this();

		this.put(key, value);
	}

	/**
	 * 创建新映射
	 */
	public SerializedMap() {
		this(Mode.YAML);
	}

	/*
	 * Create a new map
	 */
	private SerializedMap(SerializeUtil.Mode mode) {
		super("Cannot remove '%s' as it is not in the map!", "Value '%s' is already in the map!");

		this.mode = mode;
	}

	/**
	 * 将另一映射的键值对放入本映射
	 * <p>
	 * 若键已存在则忽略
	 *
	 * @param anotherMap
	 * @return
	 */
	public SerializedMap mergeFrom(final SerializedMap anotherMap) {
		for (final Map.Entry<String, Object> entry : anotherMap.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			if (key != null && value != null && !this.map.containsKey(key))
				this.map.put(key, value);
		}

		return this;
	}

	/**
	 * @see Map#containsKey(Object)
	 *
	 * @param key
	 * @return
	 */
	public boolean containsKey(final String key) {
		return this.map.containsKey(key);
	}

	/**
	 * 仅当值不为 null 时将 key:value 对放入映射
	 *
	 * @param associativeArray
	 * @return
	 */
	public SerializedMap putArray(final Object... associativeArray) {
		boolean nextIsString = true;
		String lastKey = null;

		for (final Object obj : associativeArray) {
			if (nextIsString) {
				Valid.checkBoolean(obj instanceof String, "Expected String, got " + obj.getClass().getSimpleName() + ": " + obj);

				lastKey = (String) obj;

			} else
				this.map.override(lastKey, obj);

			nextIsString = !nextIsString;
		}

		return this;
	}

	/**
	 * 将另一映射添加到本映射
	 *
	 * @param anotherMap
	 * @return this
	 */
	public SerializedMap put(@NonNull SerializedMap anotherMap) {
		this.map.putAll(anotherMap.asMap());

		return this;
	}

	/**
	 * 若值为 true 则将键值对放入映射
	 *
	 * @param key
	 * @param value
	 */
	public void putIfTrue(final String key, final boolean value) {
		if (value)
			this.put(key, value);
	}

	/**
	 * 若值不为 null 且非零则将键值对放入映射
	 *
	 * @param key
	 * @param value
	 */
	public void putIfNonZero(final String key, final Number value) {
		if (value != null && value.longValue() != 0)
			this.put(key, value);
	}

	/**
	 * 若值不为 null 则将键值对放入映射
	 *
	 * @param key
	 * @param value
	 */
	public void putIfExist(final String key, final Object value) {
		if (value != null)
			this.put(key, value);
	}

	/**
	 * 若映射不为 null 且不为空则将其放入本映射
	 *
	 * 若值为 null 则会向映射放入 NULL 值
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final Map<?, ?> value) {
		if (value != null && !value.isEmpty())
			this.put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			this.map.getSource().put(key, null);
	}

	/**
	 * 若集合不为 null 且不为空则将其放入映射
	 *
	 * 若值为 null 则会向映射放入 NULL 值
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final Collection<?> value) {
		if (value != null && !value.isEmpty())
			this.put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			this.map.getSource().put(key, null);
	}

	/**
	 * 若布尔值为 true 则将其放入映射
	 *
	 * 若值为 null 则会向映射放入 NULL 值
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final boolean value) {
		if (value)
			this.put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			this.map.getSource().put(key, null);
	}

	/**
	 * 若值不为 null 则将其放入映射
	 *
	 * 若值为 null 则会向映射放入 NULL 值
	 *
	 * @param key
	 * @param value
	 */
	public void putIf(final String key, final Object value) {
		if (value != null)
			this.put(key, value);

		// This value is undesirable to save if null, so if YamlConfig is used
		// it will remove it from the config
		else
			this.map.getSource().put(key, null);
	}

	/**
	 * 向映射放入新键值对，若值为 null
	 * 或旧键已存在则失败
	 *
	 * @param key
	 * @param value
	 */
	public void put(final String key, final Object value) {
		Valid.checkNotNull(value, "Value with key '" + key + "' is null!");

		this.map.put(key, value);
	}

	/**
	 * 向映射放入新键值对，若键为 null 则失败，
	 * 若旧键存在则替换
	 *
	 * @param key
	 * @param value
	 */
	public void override(final String key, final Object value) {
		this.map.override(key, value);
	}

	/**
	 * 覆盖所有映射值
	 *
	 * @param map
	 */
	public void overrideAll(SerializedMap map) {
		map.forEach(this::override);
	}

	/**
	 * 移除给定键，若未设置则返回 null
	 *
	 * @param key
	 * @return
	 */
	public Object removeWeak(final String key) {
		return this.map.removeWeak(key);
	}

	/**
	 * 移除给定键，若未设置则抛错
	 *
	 * @param key
	 * @return
	 */
	public Object remove(final String key) {
		return this.map.remove(key);
	}

	/**
	 * 按值移除给定键
	 *
	 * @param value
	 */
	public void removeByValue(final Object value) {
		this.map.removeByValue(value);
	}

	/**
	 * 返回映射中的字符串，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public String getString(final String key) {
		return this.getString(key, null);
	}

	/**
	 * 返回映射中的字符串，可带默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public String getString(final String key, final String def) {
		return this.get(key, String.class, def);
	}

	/**
	 * 返回映射中的 UUID，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public UUID getUUID(final String key) {
		return this.getUUID(key, null);
	}

	/**
	 * 返回映射中的 UUID，可带默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public UUID getUUID(final String key, final UUID def) {
		return this.get(key, UUID.class, def);
	}

	/**
	 * 返回映射中的位置，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public Location getLocation(final String key) {
		return this.get(key, org.bukkit.Location.class, null);
	}

	/**
	 * 返回映射中的 long，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public Long getLong(final String key) {
		return this.getLong(key, null);
	}

	/**
	 * 返回 long 值或默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Long getLong(final String key, final Long def) {
		final Number n = this.get(key, Long.class, def);

		return n != null ? n.longValue() : null;
	}

	/**
	 * 返回映射中的整数，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public Integer getInteger(final String key) {
		return this.getInteger(key, null);
	}

	/**
	 * 返回整数键或默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Integer getInteger(final String key, final Integer def) {
		return this.get(key, Integer.class, def);
	}

	/**
	 * 返回映射中的 double，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public Double getDouble(final String key) {
		return this.getDouble(key, null);
	}

	/**
	 * 返回 double 键或默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Double getDouble(final String key, final Double def) {
		return this.get(key, Double.class, def);
	}

	/**
	 * 返回映射中的 float，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public Float getFloat(final String key) {
		return this.getFloat(key, null);
	}

	/**
	 * 返回 float 键或默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Float getFloat(final String key, final Float def) {
		return this.get(key, Float.class, def);
	}

	/**
	 * 返回映射中的布尔值，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public Boolean getBoolean(final String key) {
		return this.getBoolean(key, null);
	}

	/**
	 * 返回布尔键或默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Boolean getBoolean(final String key, final Boolean def) {
		return this.get(key, Boolean.class, def);
	}

	/**
	 * 返回映射中的材质，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public CompMaterial getMaterial(final String key) {
		return this.getMaterial(key, null);
	}

	/**
	 * 返回映射中的材质或给定默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public CompMaterial getMaterial(final String key, final CompMaterial def) {
		final String raw = this.getString(key);

		return raw != null ? CompMaterial.fromString(raw) : def;
	}

	/**
	 * 返回映射中的物品堆，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public ItemStack getItemStack(final String key) {
		return this.getItemStack(key, null);
	}

	/**
	 * 返回键位置的物品堆或默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public ItemStack getItemStack(final String key, final ItemStack def) {
		final Object obj = this.get(key, Object.class, null);

		if (obj == null)
			return def;

		return SerializeUtil.deserialize(this.mode, ItemStack.class, obj);
	}

	/**
	 * 返回元组
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public <K, V> Tuple<K, V> getTuple(final String key, Class<K> keyType, Class<V> valueType) {
		return this.getTuple(key, null, keyType, valueType);
	}

	/**
	 * 返回元组或默认值
	 *
	 * @param <K>
	 * @param <V>
	 * @param key
	 * @param def
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public <K, V> Tuple<K, V> getTuple(final String key, final Tuple<K, V> def, Class<K> keyType, Class<V> valueType) {
		return this.get(key, Tuple.class, def, keyType, valueType);
	}

	/**
	 * 返回映射中的字符串列表，若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public List<String> getStringList(final String key) {
		return this.getStringList(key, null);
	}

	/**
	 * 返回字符串列表或默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public List<String> getStringList(final String key, final List<String> def) {
		final List<String> list = this.getList(key, String.class);

		return list == null ? def : list;
	}

	/**
	 * 返回序列化映射列表，若未设置则为 null
	 *
	 * @param key
	 * @return
	 */
	public List<SerializedMap> getMapList(final String key) {
		return this.getList(key, SerializedMap.class);
	}

	/**
	 * 返回映射中的集合，若映射不包含
	 * 给定键则返回空集合。
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 * @see #getList(String, Class)
	 */
	public <T> Set<T> getSet(final String key, final Class<T> type) {
		final List<T> list = this.getList(key, type);

		return new HashSet<>(list);
	}

	/**
	 * 返回 {@link IsInList} 实现，若给定键等于 ["*"] 则列表
	 * 总是返回 true
	 *
	 * @param <T>
	 * @param path
	 * @param type
	 * @return
	 */
	public <T> IsInList<T> getIsInList(String path, Class<T> type) {
		final List<String> stringList = this.getStringList(path);

		if (stringList.size() == 1 && "*".equals(stringList.get(0)))
			return IsInList.fromStar();

		return IsInList.fromList(this.getList(path, type));
	}

	/**
	 * 返回带给定键值的元组列表
	 *
	 * @param <K>
	 * @param <V>
	 * @param path
	 * @param tupleKey
	 * @param tupleValue
	 * @return
	 */
	public <K, V> List<Tuple<K, V>> getTupleList(final String path, final Class<K> tupleKey, final Class<V> tupleValue) {
		final List<Tuple<K, V>> list = new ArrayList<>();

		for (final Object object : this.getList(path, Object.class))
			if (object == null)
				list.add(null);

			else {
				final Tuple<K, V> tuple = Tuple.deserialize(of(object, this.mode), tupleKey, tupleValue);

				list.add(tuple);
			}

		return list;
	}

	/**
	 * 返回给定类型的对象列表，若映射不包含键则返回空列表。
	 * <p>
	 * 若类型是你自己的类，记得在其中放入 public static deserialize(SerializedMap)
	 * 方法，从映射返回该类对象！
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> List<T> getList(final String key, final Class<T> type) {
		return this.getList(key, type, (Object[]) null);
	}

	/**
	 * 返回给定类型的对象列表，若映射不包含键则返回空列表。
	 * <p>
	 * 若类型是你自己的类，记得在其中放入 public static deserialize(SerializedMap)
	 * 方法，从映射返回该类对象！
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @param parameters 为每个列表键创建列表时应用的反序列化参数
	 * @return
	 */
	public <T> List<T> getList(final String key, final Class<T> type, final Object... parameters) {
		final List<T> list = new ArrayList<>();

		if (!this.map.containsKey(key))
			return list;

		final Object rawList = Remain.getRootOfSectionPathData(this.removeOnGet ? this.map.removeWeak(key) : this.map.get(key));

		// Forgive if string used instead of string list
		if (type == String.class && rawList instanceof String)
			list.add((T) rawList);
		else {
			if (rawList instanceof Object[])
				for (final Object object : (Object[]) rawList)
					list.add(object == null ? null : SerializeUtil.deserialize(this.mode, type, object, parameters));
			else {
				Valid.checkBoolean(rawList instanceof Collection<?>, "Key '" + key + "' expected to have a list, got " + rawList.getClass().getSimpleName() + " instead! Try putting '' quotes around the message: " + rawList);

				for (final Object object : (Collection<Object>) rawList)
					list.add(object == null ? null : SerializeUtil.deserialize(this.mode, type, object, parameters));
			}
		}

		return list;
	}

	/**
	 * 返回映射中的序列化映射（String-Object 对），若不存在则为 null
	 *
	 * @param key
	 * @return
	 */
	public SerializedMap getMap(final String key) {
		final Object raw = this.get(key, Object.class);

		return raw != null ? of(raw, this.mode) : new SerializedMap();
	}

	/**
	 * 从给定路径加载保序映射。映射中每个键
	 * 必须匹配给定键/值类型，并会被反序列化
	 * <p>
	 * 适用时我们会添加默认值
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param valueType
	 * @return
	 */
	public <Key, Value> LinkedHashMap<Key, Value> getMap(@NonNull String path, final Class<Key> keyType, final Class<Value> valueType) {
		// The map we are creating, preserve order
		final LinkedHashMap<Key, Value> map = new LinkedHashMap<>();
		final Object raw = this.map.get(path);

		if (raw != null)
			for (final Entry<?, ?> entry : of(raw, this.mode).entrySet()) {
				final Key key = SerializeUtil.deserialize(this.mode, keyType, entry.getKey());
				final Value value = SerializeUtil.deserialize(this.mode, valueType, entry.getValue());

				// Ensure the pair values are valid for the given paramenters
				this.checkAssignable(path, key, keyType);
				this.checkAssignable(path, value, valueType);

				map.put(key, value);
			}

		return map;
	}

	/**
	 * 用给定参数加载值为 Set 的映射
	 *
	 * @param <Key>
	 * @param <Value>
	 * @param path
	 * @param keyType
	 * @param setType
	 * @return
	 */
	public <Key, Value> LinkedHashMap<Key, Set<Value>> getMapSet(@NonNull String path, final Class<Key> keyType, final Class<Value> setType) {
		// The map we are creating, preserve order
		final LinkedHashMap<Key, Set<Value>> map = new LinkedHashMap<>();
		Object raw = this.map.get(path);

		if (raw != null) {
			raw = of(raw, this.mode);

			for (final Entry<String, Object> entry : ((SerializedMap) raw).entrySet()) {
				final Key key = SerializeUtil.deserialize(this.mode, keyType, entry.getKey());
				final List<Value> value = SerializeUtil.deserialize(this.mode, List.class, entry.getValue());

				// Ensure the pair values are valid for the given paramenters
				this.checkAssignable(path, key, keyType);

				if (!value.isEmpty())
					for (final Value item : value)
						this.checkAssignable(path, item, setType);

				map.put(key, new HashSet<>(value));
			}
		}

		return map;
	}

	/*
	 * Checks if the clazz parameter can be assigned to the given value
	 */
	private void checkAssignable(final String path, final Object value, final Class<?> clazz) {
		if (!clazz.isAssignableFrom(value.getClass()) && !clazz.getSimpleName().equals(value.getClass().getSimpleName()))
			throw new FoException("Malformed map! Key '" + path + "' in the map must be " + clazz.getSimpleName() + " but got " + value.getClass().getSimpleName() + ": '" + value + "'");
	}

	/**
	 * 返回给定位置的对象
	 *
	 * @param key
	 * @return
	 */
	public Object getObject(final String key) {
		return this.get(key, Object.class);
	}

	/**
	 * 返回给定位置的对象，若不存在则返回默认值
	 *
	 * @param key
	 * @param def
	 * @return
	 */
	public Object getObject(final String key, final Object def) {
		return this.get(key, Object.class, def);
	}

	/**
	 * 返回键并尝试将其反序列化为给定类型
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @return
	 */
	public <T> T get(final String key, final Class<T> type) {
		return this.get(key, type, null);
	}

	/**
	 * 返回键并尝试将其反序列化为给定类型，可带默认值
	 *
	 * @param <T>
	 * @param key
	 * @param type
	 * @param def
	 * @param deserializeParameters
	 * @return
	 */
	public <T> T get(final String key, final Class<T> type, final T def, Object... deserializeParameters) {
		Object raw = this.removeOnGet ? this.map.removeWeak(key) : this.map.get(key);

		// Try to get the value by key with ignoring case
		if (raw == null)
			raw = this.getValueIgnoreCase(key);

		// Assume empty means default for enumerations
		if ("".equals(raw) && Enum.class.isAssignableFrom(type))
			return def;

		return raw == null ? def : SerializeUtil.deserialize(this.mode, type, raw, deserializeParameters);

	}

	/**
	 * 按字符串键查找值，忽略大小写
	 *
	 * @param key
	 * @return
	 */
	public Object getValueIgnoreCase(final String key) {
		for (final Entry<String, Object> entry : this.map.entrySet())
			if (entry.getKey().equalsIgnoreCase(key))
				return entry.getValue();

		return null;
	}

	/**
	 * @see Map#forEach(BiConsumer)
	 *
	 * @param consumer
	 */
	public void forEach(final BiConsumer<String, Object> consumer) {
		for (final Entry<String, Object> e : this.map.entrySet())
			consumer.accept(e.getKey(), e.getValue());
	}

	/**
	 * 返回第一个条目，若映射为空则为 null
	 *
	 * @return
	 */
	public Map.Entry<String, Object> firstEntry() {
		return this.isEmpty() ? null : this.map.getSource().entrySet().iterator().next();
	}

	/**
	 * @see Map#keySet()
	 *
	 * @return
	 */
	public Set<String> keySet() {
		return this.map.keySet();
	}

	/**
	 * @see Map#values()
	 *
	 * @return
	 */
	public Collection<Object> values() {
		return this.map.values();
	}

	/**
	 * @see Map#entrySet()
	 *
	 * @return
	 */
	public Set<Entry<String, Object>> entrySet() {
		return this.map.entrySet();
	}

	/**
	 * @see Map#size()
	 *
	 * @return
	 */
	public int size() {
		return this.map.size();
	}

	/**
	 * 获取 Java 映射表示
	 *
	 * @return
	 */
	public Map<String, Object> asMap() {
		return this.map.getSource();
	}

	/**
	 * 将本映射转换为序列化映射（再转一次，但会遍历每个键值对）
	 */
	@Override
	public Object serialize() {
		return this.map.serialize();
	}

	/**
	 * 将本映射转换为 JSON 字符串
	 *
	 * @return
	 */
	public String toJson() {

		try {
			final JSONObject jsonMap = new JSONObject();

			for (final Map.Entry<String, Object> entry : this.map.entrySet()) {
				final Object key = SerializeUtil.serialize(Mode.JSON, entry.getKey());
				final Object value = SerializeUtil.serialize(Mode.JSON, entry.getValue());

				if (key != null && value != null)
					jsonMap.put(key.toString(), value);
			}

			return jsonMap.toString();

		} catch (final Throwable t) {
			Common.error(t, "Failed to serialize to json, unparsed data: " + this.map);

			return "{}";
		}
	}

	/**
	 * @see Map#isEmpty()
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	/**
	 * 自动将本映射中的某个部分从一种类型转换为另一种
	 *
	 * @param <O>
	 * @param <N>
	 * @param path
	 * @param from
	 * @param to
	 * @param converter
	 */
	public <O, N> void convert(final String path, final Class<O> from, final Class<N> to, final Function<O, N> converter) {
		final Object old = this.getObject(path);

		if (old != null)
			// If the old is a collection check if the first value is old, assume the rest is old as well
			if (old instanceof Collection) {
				final Collection<?> collection = (Collection<?>) old;

				if (collection.isEmpty() || !from.isAssignableFrom(collection.iterator().next().getClass()))
					return;

				final List<N> newCollection = new ArrayList<>();

				for (final O oldItem : (Collection<O>) collection)
					newCollection.add(converter.apply(oldItem));

				this.override(path, newCollection);

				Common.logNoPrefix("[" + SimplePlugin.getNamed() + "] Converted '" + path + "' from " + from.getSimpleName() + "[] to " + to.getSimpleName() + "[]");

			} else if (from.isAssignableFrom(old.getClass())) {
				this.override(path, converter.apply((O) old));

				Common.logNoPrefix("[" + SimplePlugin.getNamed() + "] Converted '" + path + "' from '" + from.getSimpleName() + "' to '" + to.getSimpleName() + "'");
			}
	}

	/**
	 * 将键值对转换为格式化字符串，例如 {
	 * 	"key" = "value"
	 *  "another" = "value2"
	 *  ...
	 * }
	 *
	 * @return
	 */
	public String toStringFormatted() {
		final Map<?, ?> map = (Map<?, ?>) this.serialize();
		final List<String> lines = new ArrayList<>();

		lines.add("{");

		for (final Map.Entry<?, ?> entry : map.entrySet()) {
			final Object value = entry.getValue();

			if (value != null && !value.toString().equals("[]") && !value.toString().equals("{}") && !value.toString().isEmpty() && !value.toString().equals("0.0") && !value.toString().equals("false"))
				lines.add("\t'" + entry.getKey() + "' = '" + entry.getValue() + "'");
		}

		lines.add("}");

		return String.join("\n", lines);
	}

	/**
	 * @param removeOnGet 要设置的 removeOnGet
	 */
	public void setRemoveOnGet(boolean removeOnGet) {
		this.removeOnGet = removeOnGet;
	}

	@Override
	public Iterator<Entry<String, Object>> iterator() {
		return this.map.entrySet().iterator();
	}

	@Override
	public String toString() {
		return this.serialize().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SerializedMap) {
			final SerializedMap other = (SerializedMap) obj;

			if (this.size() == other.size()) {
				for (final Entry<String, Object> entry : this.map.entrySet()) {
					final String key = entry.getKey();
					final Object value = entry.getValue();

					if (!other.map.containsKey(key) || !value.equals(other.map.get(key)))
						return false;
				}

				return true;
			}
		}

		return false;
	}

	// ----------------------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 用首个键值对创建新映射
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public static SerializedMap of(final String key, final Object value) {
		return new SerializedMap(key, value);
	}

	/**
	 * 像在 PHP 中一样从键值对创建新的序列化映射：
	 * <p>
	 * array(
	 * "name" => value,
	 * "name2" => value2,
	 * )
	 * <p>
	 * 只不过现在用逗号代替 =>
	 *
	 * @param array
	 * @return
	 */
	public static SerializedMap ofArray(final Object... array) {

		// If the first argument is a map already, treat as such
		if (array != null && array.length == 1) {
			final Object firstArgument = array[0];

			if (firstArgument instanceof SerializedMap)
				return (SerializedMap) firstArgument;

			if (firstArgument instanceof Map)
				return SerializedMap.of(firstArgument);

			if (firstArgument instanceof StrictMap)
				return SerializedMap.of(((StrictMap<String, Object>) firstArgument).getSource());
		}

		final SerializedMap map = new SerializedMap();
		map.putArray(array);

		return map;
	}

	/**
	 * 将给定对象解析为序列化映射
	 *
	 * @param object
	 * @return 序列化映射，若对象无法解析则为空映射
	 */
	public static SerializedMap of(@NonNull Object object) {
		return of(object, Mode.YAML);
	}

	/*
	 * Parses the given object into Serialized map
	 */
	private static SerializedMap of(@NonNull Object object, Mode mode) {

		if (object instanceof SerializedMap) {
			((SerializedMap) object).mode = mode;

			return (SerializedMap) object;
		}

		if (object instanceof String && object.toString().equals("{}"))
			return new SerializedMap(mode);

		if (object instanceof MemorySection)
			return of(Common.getMapFromSection(object));

		if (object instanceof ConfigSection)
			return of(((ConfigSection) object).getValues(false));

		if (object instanceof Map) {
			final Map<String, Object> copyOf = new LinkedHashMap<>();

			for (final Map.Entry<?, ?> entry : ((Map<String, Object>) object).entrySet()) {
				final Object key = entry.getKey();

				if (key == null)
					copyOf.put(null, entry.getValue());

				else {
					final String stringKey = key.toString();
					final Object value = entry.getValue();

					final String[] split = stringKey.split("\\=");

					// Spigot's special way of storing maps 'key=value'
					if (split.length == 2 && value == null) {
						final String actualKey = split[0];
						final String actualValue = split[1];

						copyOf.put(actualKey, actualValue);
					}

					else
						copyOf.put(stringKey, value);
				}
			}

			final SerializedMap serialized = new SerializedMap(mode);
			serialized.map.putAll(copyOf);

			return serialized;
		}

		// Exception since some config sections are stored like this when they are empty
		if (object instanceof List && ((List<?>) object).isEmpty())
			return new SerializedMap(mode);

		throw new FoException("Cannot instantiate SerializedMap(" + mode + ") from " + object.getClass().getSimpleName() + ": " + object);
	}

	/**
	 * 尝试将给定 JSON 解析为序列化映射
	 * <p>
	 * 值不会立即反序列化，调用 get() 函数时
	 * 才会转换
	 *
	 * @param json
	 * @return
	 */
	public static SerializedMap fromJson(@NonNull final String json) {

		if (json.isEmpty() || "[]".equals(json) || "{}".equals(json))
			return new SerializedMap(Mode.JSON);

		// Fallback to simple
		try {
			final Object parsed = JSONParser.deserialize(json);

			if (parsed instanceof JSONObject)
				return of(parsed, Mode.JSON);

			throw new FoException("Expected JSONObject, got " + (parsed != null ? parsed.getClass() : "unknown class") + " from raw JSON input: " + json);

		} catch (final Throwable secondThrowable) {
			Common.throwError(secondThrowable, "Failed to parse JSON from " + json);

			return null;
		}
	}
}
