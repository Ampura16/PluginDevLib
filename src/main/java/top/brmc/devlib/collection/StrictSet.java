package top.brmc.devlib.collection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import top.brmc.devlib.Common;
import top.brmc.devlib.SerializeUtil;
import top.brmc.devlib.Valid;

/**
 * 严格集合，只允许移除包含的元素，或添加不包含的元素。
 * <p>
 * 否则会出错，可带可选错误消息。
 * @param <E>
 */
public final class StrictSet<E> extends StrictCollection implements Iterable<E> {

	/**
	 * 内部集合
	 */
	private final Set<E> set = new LinkedHashSet<>();

	/**
	 * 从给定元素创建新集合
	 *
	 * @param elements
	 */
	@SafeVarargs
	public StrictSet(E... elements) {
		this();

		this.addAll(Arrays.asList(elements));
	}

	/**
	 * 从给定元素创建新集合
	 *
	 * @param oldList
	 */
	public StrictSet(Iterable<E> oldList) {
		this();

		this.addAll(oldList);
	}

	/**
	 * 创建新的严格集合
	 */
	public StrictSet() {
		super("Cannot remove '%s' as it is not in the set!", "Value '%s' is already in the set!");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods below trigger strict checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 从集合移除给定元素，
	 * 若为 null 或不在集合中则失败
	 *
	 * @param value
	 */
	public void remove(E value) {
		Valid.checkNotNull(value, "Cannot remove null values");
		final boolean removed = this.set.remove(value);

		Valid.checkBoolean(removed, String.format(this.getCannotRemoveMessage(), value));
	}

	/**
	 * 从集合移除给定元素
	 *
	 * @param value
	 */
	public void removeWeak(E value) {
		this.set.remove(value);
	}

	/**
	 * 向集合添加所有元素
	 *
	 * @param collection
	 */
	public void addAll(Iterable<E> collection) {
		for (final E val : collection)
			this.add(val);
	}

	/**
	 * 向集合添加给定元素，
	 * 若为 null 或已在集合中则失败
	 *
	 * @param key
	 */
	public void add(E key) {
		Valid.checkNotNull(key, "Cannot add null values");
		Valid.checkBoolean(!this.set.contains(key), String.format(this.getCannotAddMessage(), key));

		this.set.add(key);
	}

	/**
	 * 向集合添加给定元素
	 *
	 * @param key
	 */
	public void override(E key) {
		this.set.add(key);
	}

	/**
	 * 返回第一个值，若列表为空则为 null
	 *
	 * @return
	 */
	@Nullable
	public E first() {
		return this.set.isEmpty() ? null : this.set.iterator().next();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods without throwing errors below
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若集合包含给定元素则返回 true
	 *
	 * @param key
	 * @return
	 */
	public boolean contains(E key) {
		return this.set.contains(key);
	}

	/**
	 * 清空集合
	 */
	public void clear() {
		this.set.clear();
	}

	/**
	 * 若集合为空则返回 true
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.set.isEmpty();
	}

	/**
	 * 返回集合大小
	 *
	 * @return
	 */
	public int size() {
		return this.set.size();
	}

	/**
	 * 返回集合的原始 Java 实现
	 *
	 * @return
	 */
	public Set<E> getSource() {
		return this.set;
	}

	/**
	 * 返回用给定分隔符连接的所有集合值
	 *
	 * @param separator
	 * @return
	 */
	public String join(String separator) {
		return Common.join(this.set, separator);
	}

	/**
	 * 将本集合作为数组返回
	 *
	 * @param e
	 * @return
	 */
	public E[] toArray(E[] e) {
		return this.set.toArray(e);
	}

	/**
	 * 返回集合迭代器
	 */
	@Override
	public Iterator<E> iterator() {
		return this.set.iterator();
	}

	/**
	 * 将本集合作为序列化对象列表返回
	 */
	@Override
	public Object serialize() {
		return SerializeUtil.serialize(this.getMode(), this.set);
	}

	@Override
	public String toString() {
		return "StrictSet{\n\t" + Common.join(this.set, "\n\t") + "\n}";
	}
}