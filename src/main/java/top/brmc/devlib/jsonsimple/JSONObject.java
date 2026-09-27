/* Copyright 2016-2017 Clifton Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package top.brmc.devlib.jsonsimple;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * JsonObject 是一种常用的、非线程安全的字符串到数据的映射格式。JsonObject 的内容
 * 只在序列化时才会被校验为 JSON 值。也就是说，添加到 JsonObject 的所有值都必须能被
 * Jsoner 识别，它才是真正的 'JsonObject'；因此它实际上是一个 JsonableHashMap，只有当其所有内容
 * 都是有效 JSON 时，才会序列化为 JsonObject。
 * @author https://cliftonlabs.github.io/json-simple/
 * @since 2.0.0
 */
public class JSONObject extends HashMap<String, Object> implements Jsonable {
	/**
	 * 此类兼容的序列化版本。当且仅当所做的修改只是更新注释、更新 javadoc、
	 * 向类中添加新字段、将字段从 static 改为非 static，或将字段从 transient 改为
	 * 非 transient 时，此值才不需要递增。其他所有修改
	 * 都需要递增此值。
	 */
	private static final long serialVersionUID = 2L;

	/** 实例化一个空的 JsonObject。 */
	public JSONObject() {
	}

	/**
	 * 通过接收一个映射的条目来实例化新的 JsonObject。由于条目值未被校验为 JSON 值，
	 * 这可能导致生成的 JsonObject 出现反序列化/序列化问题。
	 * @param map 用于生成 JsonObject 的映射。
	 */
	public JSONObject(final Map<String, ?> map) {
		super(map);
	}

	/**
	 * 如果该值已经是 {@linkplain JSONObject}，则转换后返回。
	 * 如果该值是 {@linkplain Map}，则会将其包装在 {@linkplain JSONObject} 中，并返回包装后的 {@linkplain Map}。
	 * 其他情况下此方法返回 {@code null}。
	 *
	 * @param key 值的键
	 * @return {@linkplain JSONObject} 或 {@code null}
	 */
	public JSONObject getObject(String key) {
		final Object value = this.get(key);

		if (value != null)
			if (value instanceof JSONObject)
				return (JSONObject) value;

			else if (value instanceof Map)
				return new JSONObject((Map<String, ?>) value);

		return null;
	}

	/**
	 * 便捷方法，假定给定键处是 BigDecimal、Number 或 String。如果是 Number，
	 * 会使用其 Number#toString() 构造新的 BigDecimal(String)。如果是 String，则直接用它
	 * 构造新的 BigDecimal(String)。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 BigDecimal。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see BigDecimal
	 * @see Number#toString()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public BigDecimal getBigDecimal(final String key) {
		Object returnable = this.get(key);
		if (returnable instanceof BigDecimal) {
			/* Success there was a BigDecimal or it defaulted. */
		} else if (returnable instanceof Number)
			/* A number can be used to construct a BigDecimal */
			returnable = new BigDecimal(returnable.toString());
		else if (returnable instanceof String)
			/* A number can be used to construct a BigDecimal */
			returnable = new BigDecimal((String) returnable);
		return (BigDecimal) returnable;
	}

	/**
	 * 便捷方法，假定给定键处是 BigDecimal、Number 或 String。如果是 Number，
	 * 会使用其 Number#toString() 构造新的 BigDecimal(String)。如果是 String，则直接用它
	 * 构造新的 BigDecimal(String)。
	 * @param key 值应对应的键。
	 * @param def
	 * @return 表示与该键对应值的 BigDecimal；若该键不存在则为 JsonKey#getValue()。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see BigDecimal
	 * @see Number#toString()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public BigDecimal getBigDecimalOrDefault(final String key, final BigDecimal def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;

		if (returnable instanceof BigDecimal) {
			/* Success there was a BigDecimal or it defaulted. */
		} else if (returnable instanceof Number)
			/* A number can be used to construct a BigDecimal */
			returnable = new BigDecimal(returnable.toString());
		else if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal */
			returnable = new BigDecimal((String) returnable);
		return (BigDecimal) returnable;
	}

	/**
	 * 便捷方法，假定给定键处是 Boolean 或 String 值。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 Boolean。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Boolean getBoolean(final String key) {
		Object returnable = this.get(key);
		if (returnable instanceof String)
			returnable = Boolean.valueOf((String) returnable);
		return (Boolean) returnable;
	}

	/**
	 * 便捷方法，假定给定键处是 Boolean 或 String 值。
	 * @param key 值应对应的键。
	 * @param def
	 * @return 表示与该键对应值的 Boolean；若该键不存在则为 JsonKey#getValue()。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Boolean getBooleanOrDefault(final String key, final boolean def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable instanceof String)
			returnable = Boolean.valueOf((String) returnable);
		return (Boolean) returnable;
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 Byte（可能涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#byteValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Byte getByte(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).byteValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @param def
	 * @return 表示与该键对应值的 Byte；若该键不存在则为 JsonKey#getValue()（可能
	 *         涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#byteValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Byte getByteOrDefault(final String key, final byte def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).byteValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Collection。
	 *
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 Collection。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */

	public JSONArray getArray(final String key) {
		final Object collection = this.get(key);

		if (collection instanceof JSONArray)
			return (JSONArray) collection;

		else if (collection instanceof Collection)
			return new JSONArray((Collection<?>) collection);

		return null;
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 Double（可能涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#doubleValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Double getDouble(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).doubleValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @param def
	 * @return 表示与该键对应值的 Double；若该键不存在则为 JsonKey#getValue()（可能
	 *         涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#doubleValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Double getDoubleOrDefault(final String key, final double def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).doubleValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 Integer（可能涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#intValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Integer getInteger(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).intValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @param def
	 * @return 表示与该键对应值的 Integer；若该键不存在则为 JsonKey#getValue()
	 *         （可能涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#intValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Integer getIntegerOrDefault(final String key, final int def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).intValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 Long（可能涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#longValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Long getLong(final String key) {
		Object returnable = this.get(key);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).longValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Number 或 String 值。
	 * @param key 值应对应的键。
	 * @param def
	 * @return 表示与该键对应值的 Long；若该键不存在则为 JsonKey#getValue()（可能
	 *         涉及舍入或截断）。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @see Number#longValue()
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public Long getLongOrDefault(final String key, final long def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).longValue();
	}

	/**
	 * 便捷方法，假定给定键处是 Map。
	 * @param <T> 该键处预期的映射类型。注意，除非手动添加，否则 Map 值都是 JsonObject。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 Map。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */

	public <T extends Map<?, ?>> T getMap(final String key) {
		/* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
		return (T) this.get(key);
	}

	/**
	 * 便捷方法，假定给定键处是 Map。
	 * @param <T>
	 * @param key 值应对应的键。
	 * @param def 该键处预期的映射类型。注意，除非手动添加，否则 Map 值都是 JsonObject。
	 * @return 表示与该键对应值的 Map；若该键不存在则为 JsonKey#getValue()。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */

	public <T extends Map<?, ?>> T getMapOrDefault(final String key, final T def) {
		/* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		return (T) returnable;
	}

	/**
	 * 便捷方法，假定给定键处是 Boolean、Number 或 String 值。
	 * @param key 值应对应的键。
	 * @return 表示与该键对应值的 String。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public String getString(final String key) {
		Object returnable = this.get(key);
		if (returnable instanceof Boolean)
			returnable = returnable.toString();
		else if (returnable instanceof Number)
			returnable = returnable.toString();
		return (String) returnable;
	}

	/**
	 * 便捷方法，假定给定键处是 Boolean、Number 或 String 值。
	 * @param key 值应对应的键。
	 * @param def
	 * @return 表示与该键对应值的 String；若该键不存在则为 JsonKey#getValue()。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */
	public String getStringOrDefault(final String key, final String def) {
		Object returnable;
		if (this.containsKey(key))
			returnable = this.get(key);
		else
			returnable = def;
		if (returnable instanceof Boolean)
			returnable = returnable.toString();
		else if (returnable instanceof Number)
			returnable = returnable.toString();
		return (String) returnable;
	}

	/**
	 * 对给定映射调用 putAll，但返回 JsonObject 以便链式调用。
	 * @param map 要复制到 JsonObject 中的映射。
	 * @return 该 JsonObject，以便链式调用。
	 * @see Map#putAll(Map)
	 * @since 3.1.0 用于内联实例化。
	 */
	public JSONObject putAllChain(final Map<String, Object> map) {
		this.putAll(map);
		return this;
	}

	/**
	 * 对给定键和值调用 put，但返回 JsonObject 以便链式调用。
	 * @param key 该值在映射中关联的键。
	 * @param value 该键在映射中关联的值。
	 * @return 该 JsonObject，以便链式调用。
	 * @see Map#put(Object, Object)
	 * @since 3.1.0 用于内联实例化。
	 */
	public JSONObject putChain(final String key, final Object value) {
		this.put(key, value);
		return this;
	}

	/**
	 * 确保给定的键都存在。
	 * @param keys 必须存在的键。
	 * @throws NoSuchElementException 若缺少任何给定的键。
	 * @since 2.3.0 用于确保关键键存在于 JsonObject 中。
	 */
	public void requireKeys(final String... keys) {
		/* Track all of the missing keys. */
		final Set<String> missing = new HashSet<>();
		for (final String subkey : keys)
			if (!this.containsKey(subkey))
				missing.add(subkey);
		if (!missing.isEmpty()) {
			/* Report any missing keys in the exception. */
			final StringBuilder sb = new StringBuilder();
			for (final String subkey : missing)
				sb.append(subkey).append(", ");
			sb.setLength(sb.length() - 2);
			final String s = missing.size() > 1 ? "s" : "";
			throw new NoSuchElementException("A JsonObject is missing required key" + s + ": " + sb.toString());
		}
	}

	/* (non-Javadoc)
	 * @see org.json.simple.Jsonable#toJson() */
	@Override
	public String toJson() {
		final StringWriter writable = new StringWriter();
		try {
			this.toJson(writable);
		} catch (final IOException caught) {
			/* See java.io.StringWriter. */
		}
		return writable.toString();
	}

	/* (non-Javadoc)
	 * @see org.json.simple.Jsonable#toJson(java.io.Writer) */
	@Override
	public void toJson(final Writer writable) throws IOException {
		/* Writes the map in JSON object format. */
		boolean isFirstEntry = true;
		writable.write('{');
		for (final Entry<String, Object> entry : this.entrySet()) {
			if (isFirstEntry)
				isFirstEntry = false;
			else
				writable.write(',');
			JSONParser.serialize(entry.getKey(), writable);
			writable.write(':');
			JSONParser.serialize(entry.getValue(), writable);
		}
		writable.write('}');
	}

	/**
	 * @see #toJson()
	 */
	@Override
	public String toString() {
		return this.toJson();
	}
}
