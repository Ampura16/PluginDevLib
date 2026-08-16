package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;

/**
 * 一个相当专用的类，用于为玩家随机挑选条目，
 * 在竞技场插件中为玩家分配随机职业时使用，
 * 每个被挑中的职业都会检查玩家的权限
 *
 * @param <T>
 */
public abstract class RandomNoRepeatPicker<T> {

	/**
	 * 存放所有条目的列表
	 */
	private final List<T> list = new ArrayList<>();

	/**
	 * 设置我们要从中挑选的条目
	 *
	 * @param list
	 */
	public void setItems(final Iterable<T> list) {
		Valid.checkBoolean(list != null && list.iterator().hasNext(), "Cannot set items to an empty list!");

		this.list.clear();
		this.list.addAll(Common.toList(list));
	}

	/**
	 * 返回是否已没有可挑选的条目
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	/**
	 * 从给定列表中随机挑选 1 个条目
	 * 注意：这也会装载该列表
	 *
	 * @param items
	 * @return
	 */
	public T pickFrom(final Iterable<T> items) {
		return this.pickFromFor(items, null);
	}

	/**
	 * 使用 canObtain 方法为玩家随机挑选 1 个条目
	 * 注意：这也会装载该列表
	 *
	 * @param items
	 * @param player
	 * @return
	 */
	public T pickFromFor(final Iterable<T> items, final Player player) {
		for (final T item : items)
			this.list.add(item);

		return this.pickRandom(player);
	}

	/**
	 * 从列表中随机挑选并移除 1 个条目，直到
	 * 条目用完
	 *
	 * @return
	 */
	public T pickRandom() {
		return this.pickRandom(null);
	}

	/**
	 * 随机挑选 1 个条目并用 canObtain 方法检验，
	 * 直到条目用完或找到一个玩家可以获得的条目
	 *
	 * @param player
	 * @return
	 */
	public T pickRandom(final Player player) {
		if (this.list.isEmpty())
			return null;

		while (!this.list.isEmpty()) {
			final T picked = this.list.remove(RandomUtil.nextInt(this.list.size()));

			if (picked != null && this.canObtain(player, picked))
				return picked;
		}

		return null;
	}

	/**
	 * 返回剩余的元素数量
	 *
	 * @return
	 */
	public int remaining() {
		return this.list.size();
	}

	/**
	 * 若玩家可以获得给定条目，应返回 true
	 *
	 * @param player
	 * @param picked
	 * @return
	 */
	protected abstract boolean canObtain(Player player, T picked);

	/**
	 * 为给定类类型创建一个新的随机不重复挑选器
	 * 所有玩家总是可以获得
	 *
	 * @param <T>
	 * @param pickedType
	 * @return
	 */
	public static final <T> RandomNoRepeatPicker<T> newPicker(final Class<T> pickedType) {
		return newPicker((player, type) -> true);
	}

	/**
	 * 创建一个新的随机不重复挑选器，并使用函数
	 * 检查玩家能否获得该职业
	 *
	 * @param <T>
	 * @param canObtain
	 * @return
	 */
	public static final <T> RandomNoRepeatPicker<T> newPicker(final BiFunction<Player, T, Boolean> canObtain) {
		return new RandomNoRepeatPicker<T>() {

			@Override
			protected boolean canObtain(final Player player, final T picked) {
				return canObtain.apply(player, picked);
			}
		};
	}
}