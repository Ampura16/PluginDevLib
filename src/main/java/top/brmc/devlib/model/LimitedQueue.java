package top.brmc.devlib.model;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

import com.google.common.collect.ForwardingQueue;

/**
 * 表示一个只存放少量条目的简单有限队列
 *
 * @param <E>
 */
public final class LimitedQueue<E> extends ForwardingQueue<E> {

	/**
	 * 被委托的队列
	 */
	private final Queue<E> delegate;

	/**
	 * 队列最大容量
	 */
	private final int capacity;

	/**
	 * 以给定容量创建一个新的有限队列
	 *
	 * @param capacity
	 */
	public LimitedQueue(final int capacity) {
		this.delegate = new ArrayDeque<>(capacity);
		this.capacity = capacity;
	}

	@Override
	protected Queue<E> delegate() {
		return this.delegate;
	}

	/**
	 * 参见 {@link Queue#add(Object)}，不过如果队列已满，会先调用 {@link Queue#poll()} 再添加
	 */
	@Override
	public boolean add(final E element) {
		if (this.size() >= this.capacity)
			this.delegate.poll();

		return this.delegate.add(element);
	}

	/**
	 * 参见 {@link Queue#addAll(Collection)}
	 */
	@Override
	public boolean addAll(final Collection<? extends E> collection) {
		return this.standardAddAll(collection);
	}

	/**
	 * 参见 {@link Queue#offer(Object)}
	 */
	@Override
	public boolean offer(final E o) {
		return this.standardOffer(o);
	}

}