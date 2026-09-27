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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * JsonArray 是一种常用的、非线程安全的数据集合格式。JsonArray 的内容只在
 * 序列化时才会被校验为 JSON 值。也就是说，添加到 JsonArray 的所有值都必须能被 Jsoner 识别，
 * 它才是真正的 'JsonArray'；因此它实际上是一个 JsonableArrayList，只有当其所有内容
 * 都是有效 JSON 时，才会序列化为 JsonArray。
 * @author https://cliftonlabs.github.io/json-simple/
 * @since 2.0.0
 */
public class JSONArray extends ArrayList<Object> implements Jsonable {
	/**
	 * 此类兼容的序列化版本。当且仅当所做的修改只是更新注释、更新 javadoc、
	 * 向类中添加新字段、将字段从 static 改为非 static，或将字段从 transient 改为
	 * 非 transient 时，此值才不需要递增。其他所有修改
	 * 都需要递增此值。
	 */
	private static final long serialVersionUID = 1L;

	/** 实例化一个空的 JsonArray。 */
	public JSONArray() {
	}

	/**
	 * 使用 ArrayList 同类型的构造器实例化新的 JsonArray。
	 * @param collection 用于生成 JsonArray 的元素。
	 */
	public JSONArray(final Collection<?> collection) {
		super(collection);
	}

	/**
	 * 对给定元素集合调用 add，但返回 JsonArray 以便链式调用。
	 * @param collection 要追加到 JsonArray 的元素。
	 * @return 该 JsonArray，以便链式调用。
	 * @see ArrayList#addAll(Collection)
	 * @since 3.1.0 用于内联实例化。
	 */
	public JSONArray addAllChain(final Collection<?> collection) {
		this.addAll(collection);
		return this;
	}

	/**
	 * 对给定索引和集合调用 add，但返回 JsonArray 以便链式调用。
	 * @param index 元素在 JsonArray 中添加到的索引位置。
	 * @param collection 要追加到 JsonArray 的元素。
	 * @return 该 JsonArray，以便链式调用。
	 * @see ArrayList#addAll(int, Collection)
	 * @since 3.1.0 用于内联实例化。
	 */
	public JSONArray addAllChain(final int index, final Collection<?> collection) {
		this.addAll(index, collection);
		return this;
	}

	/**
	 * 对给定元素调用 add，但返回 JsonArray 以便链式调用。
	 * @param index 元素在 JsonArray 中添加到的索引位置。
	 * @param element 要追加到 JsonArray 的元素。
	 * @return 该 JsonArray，以便链式调用。
	 * @see ArrayList#add(int, Object)
	 * @since 3.1.0 用于内联实例化。
	 */
	public JSONArray addChain(final int index, final Object element) {
		this.add(index, element);
		return this;
	}

	/**
	 * 对给定元素调用 add，但返回 JsonArray 以便链式调用。
	 * @param element 要追加到 JsonArray 的元素。
	 * @return 该 JsonArray，以便链式调用。
	 * @see ArrayList#add(Object)
	 * @since 3.1.0 用于内联实例化。
	 */
	public JSONArray addChain(final Object element) {
		this.add(element);
		return this;
	}

	/**
	 * 便捷方法，假定 JsonArray 的每个元素都可以转换为 T，然后将其添加到
	 * T 的集合中。
	 * @param <T> JsonArray 的所有元素应转换成的类型，也是
	 *        集合所包含的类型。
	 * @param destination JsonArray 的所有元素在转换为所提供的泛型类型后
	 *        添加到的
	 *        目标位置。
	 * @throws ClassCastException 若将元素未经检查地转换为 T 失败。
	 */

	public <T> void asCollection(final Collection<T> destination) {
		for (final Object o : this)
			destination.add((T) o);
	}

	/**
	 * 如果该值已经是 {@linkplain JSONObject}，则转换后返回。
	 * 如果该值是 {@linkplain Map}，则会将其包装在 {@linkplain JSONObject} 中，并返回包装后的 {@linkplain Map}。
	 * 其他情况下此方法返回 {@code null}。
	 *
	 * @param index 值的键
	 * @return {@linkplain JSONObject} 或 {@code null}
	 */
	public JSONObject getObject(final int index) {
		final Object value = this.get(index);

		if (value != null)
			if (value instanceof JSONObject)
				return (JSONObject) value;

			else if (value instanceof Map)
				return new JSONObject((Map<String, ?>) value);

		return null;
	}

	/**
	 * 便捷方法，假定给定索引处是 BigDecimal、Number 或 String。如果是 Number 或
	 * String，会用它构造新的 BigDecimal。
	 * @param index 值预期所在的位置。
	 * @return 该键处存储的值；若该键不存在则为所提供的默认值。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示。
	 * @see BigDecimal
	 * @see Number#doubleValue()
	 */
	public BigDecimal getBigDecimal(final int index) {
		Object returnable = this.get(index);
		if (returnable instanceof BigDecimal) {
			/* Success there was a BigDecimal. */
		} else if (returnable instanceof Number)
			/* A number can be used to construct a BigDecimal. */
			returnable = new BigDecimal(returnable.toString());
		else if (returnable instanceof String)
			/* A number can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return (BigDecimal) returnable;
	}

	/**
	 * 便捷方法，假定给定索引处是 Boolean 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 boolean。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 */
	public Boolean getBoolean(final int index) {
		Object returnable = this.get(index);
		if (returnable instanceof String)
			returnable = Boolean.valueOf((String) returnable);
		return (Boolean) returnable;
	}

	/**
	 * 便捷方法，假定给定索引处是 Number 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 byte。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @see Number
	 */
	public Byte getByte(final int index) {
		Object returnable = this.get(index);
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
	 * @param index 值应对应的位置。
	 * @return 表示与该键对应值的 Collection。
	 * @throws ClassCastException 若值与假定的返回类型不匹配。
	 *
	 * @since 2.3.0 以使用 JsonKey
	 */

	public JSONArray getArray(final int index) {
		final Object collection = this.get(index);

		if (collection instanceof JSONArray)
			return (JSONArray) collection;

		else if (collection instanceof Collection)
			return new JSONArray((Collection<?>) collection);

		return null;
	}

	/**
	 * 便捷方法，假定给定索引处是 Number 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 double。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @see Number
	 */
	public Double getDouble(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).doubleValue();
	}

	/**
	 * 便捷方法，假定给定索引处是 Number 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 float。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @see Number
	 */
	public Float getFloat(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).floatValue();
	}

	/**
	 * 便捷方法，假定给定索引处是 Number 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 int。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @see Number
	 */
	public Integer getInteger(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).intValue();
	}

	/**
	 * 便捷方法，假定给定索引处是 Number 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 long。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @see Number
	 */
	public Long getLong(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).longValue();
	}

	/**
	 * 便捷方法，假定给定索引处是 Map 值。
	 * @param <T> 该索引处预期的映射类型。注意，除非手动添加，否则 Map 值都是 JsonObject。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 Map。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @see Map
	 */

	public <T extends Map<?, ?>> T getMap(final int index) {
		/* The unchecked warning is suppressed because there is no way of guaranteeing at compile time the cast will
		 * work. */
		return (T) this.get(index);
	}

	/**
	 * 便捷方法，假定给定索引处是 Number 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 short。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws NumberFormatException 若 String 不是 BigDecimal 的有效表示，或 Number
	 *         表示 double 或 float 的 Infinity 或 NaN。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 * @see Number
	 */
	public Short getShort(final int index) {
		Object returnable = this.get(index);
		if (returnable == null)
			return null;
		if (returnable instanceof String)
			/* A String can be used to construct a BigDecimal. */
			returnable = new BigDecimal((String) returnable);
		return ((Number) returnable).shortValue();
	}

	/**
	 * 便捷方法，假定给定索引处是 Boolean、Number 或 String 值。
	 * @param index 值预期所在的位置。
	 * @return 所提供索引处的值，转换为 String。
	 * @throws ClassCastException 若存在值但与假定的返回类型不匹配。
	 * @throws IndexOutOfBoundsException 若索引超出 JsonArray 元素索引的范围。
	 */
	public String getString(final int index) {
		Object returnable = this.get(index);
		if (returnable instanceof Boolean)
			returnable = returnable.toString();
		else if (returnable instanceof Number)
			returnable = returnable.toString();
		return (String) returnable;
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
		boolean isFirstElement = true;
		final Iterator<Object> elements = this.iterator();
		writable.write('[');
		while (elements.hasNext()) {
			if (isFirstElement)
				isFirstElement = false;
			else
				writable.write(',');
			JSONParser.serialize(elements.next(), writable);
		}
		writable.write(']');
	}

	/**
	 * 使用 {@link #getString(int)} 方法将此 {@linkplain JSONArray} 转换为 {@linkplain String} 数组。
	 *
	 * @return {@linkplain String} 数组
	 * @since 1.0.0
	 */
	public String[] toStringArray() {
		final String[] array = new String[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getString(index);

		return array;
	}

	/**
	 * 使用 {@link #getObject(int)} 方法将此 {@linkplain JSONArray} 转换为 {@linkplain JSONObject} 数组。
	 *
	 * @return {@linkplain JSONObject} 数组
	 * @since 1.0.0
	 */
	public JSONObject[] toObjectArray() {

		final JSONObject[] array = new JSONObject[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getObject(index);

		return array;
	}

	/**
	 * 使用 {@link #getArray(int)} 方法将此 {@linkplain JSONArray} 转换为 {@linkplain JSONArray} 数组。
	 *
	 * @return {@linkplain JSONArray} 数组。
	 * @since 1.0.0
	 */
	public JSONArray[] toArrayArray() {

		final JSONArray[] array = new JSONArray[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getArray(index);

		return array;
	}

	/**
	 * 将此 {@linkplain JSONArray} 转换为基本类型 {@code byte} 数组。
	 *
	 * @return 基本类型 {@code byte} 数组
	 * @since 2.0.0
	 */
	public byte[] toPrimitiveByteArray() {

		final byte[] array = new byte[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getByte(index);

		return array;
	}

	/**
	 * 将此 {@linkplain JSONArray} 转换为基本类型 {@code short} 数组。
	 *
	 * @return 基本类型 {@code short} 数组
	 * @since 2.0.0
	 */
	public short[] toPrimitiveShortArray() {

		final short[] array = new short[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getShort(index);

		return array;
	}

	/**
	 * 将此 {@linkplain JSONArray} 转换为基本类型 {@code int} 数组。
	 *
	 * @return 基本类型 {@code int} 数组
	 * @since 2.0.0
	 */
	public int[] toPrimitiveIntArray() {

		final int[] array = new int[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getInteger(index);

		return array;
	}

	/**
	 * 将此 {@linkplain JSONArray} 转换为基本类型 {@code long} 数组。
	 *
	 * @return 基本类型 {@code long} 数组
	 * @since 2.0.0
	 */
	public long[] toPrimitiveLongArray() {

		final long[] array = new long[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getLong(index);

		return array;
	}

	/**
	 * 将此 {@linkplain JSONArray} 转换为基本类型 {@code float} 数组。
	 *
	 * @return 基本类型 {@code float} 数组
	 * @since 2.0.0
	 */
	public float[] toPrimitiveFloatArray() {

		final float[] array = new float[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getFloat(index);

		return array;
	}

	/**
	 * 将此 {@linkplain JSONArray} 转换为基本类型 {@code double} 数组。
	 *
	 * @return 基本类型 {@code double} 数组
	 * @since 2.0.0
	 */
	public double[] toPrimitiveDoubleArray() {

		final double[] array = new double[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getDouble(index);

		return array;
	}

	/**
	 * 将此 {@linkplain JSONArray} 转换为基本类型 {@code boolean} 数组。
	 *
	 * @return 基本类型 {@code boolean} 数组
	 * @since 2.0.0
	 */
	public boolean[] toPrimitiveBooleanArray() {

		final boolean[] array = new boolean[this.size()];

		for (int index = 0; index < array.length; index++)
			array[index] = this.getBoolean(index);

		return array;
	}
}
