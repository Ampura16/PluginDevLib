package org.mineacademy.fo.slider;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 一个遍历物品并高亮其中一个的示例滑块。
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemFrameBounceSlider implements Slider<Map<Integer, ItemStack>> {

	/**
	 * 填充物品
	 */
	private final ItemStack fillerItem;

	/**
	 * 选中的物品
	 */
	private final ItemStack highlightItem;

	/**
	 * 位于边框每条边上的物品
	 */
	private final ItemStack sideItem;

	/**
	 * 位于边框每个角上的物品
	 */
	private final ItemStack cornerItem;

	/**
	 * 动画边框的尺寸（允许的值：18、27、36、45、54），请确保菜单尺寸足够大
	 */
	private int frameSize = 27;

	/*
	 * The first head in the slider.
	 */
	private int topPointer = 0;

	/*
	 * The second head in the slider.
	 */
	private int bottomPointer = 44;

	/*
	 * Check if the top pointer is decreasing
	 */
	boolean topDecreasing = false;

	/*
	 * Check if the bottom pointer is decreasing
	 */
	boolean bottomDecreasing = true;

	/**
	 * 设置边框尺寸
	 *
	 * @param frameSize
	 * @return
	 */
	public ItemFrameBounceSlider frameSize(final int frameSize) {
		this.frameSize = frameSize;

		return this;
	}

	/**
	 * @see org.mineacademy.fo.slider.Slider#next()
	 */
	@Override
	public Map<Integer, ItemStack> next() {
		final Map<Integer, ItemStack> items = new HashMap<>();
		final int rowCount = this.frameSize / 9;

		for (int index = 0; index < this.frameSize; index++) {
			final int row = index / 9;
			final int column = (index % 9) + 1;

			if (row == 0 || row == rowCount - 1)
				items.put(index, this.fillerItem);

			if (column == 1 || column == 9)
				items.put(index, this.sideItem);
		}

		if (this.topDecreasing)
			this.topPointer--;

		else
			this.topPointer++;

		if (this.topPointer == 0)
			this.topDecreasing = false;

		else if (this.topPointer == 8)
			this.topDecreasing = true;

		if (this.bottomDecreasing)
			this.bottomPointer--;

		else
			this.bottomPointer++;

		if (this.bottomPointer == this.frameSize - 9)
			this.bottomDecreasing = false;

		else if (this.bottomPointer == this.frameSize - 1)
			this.bottomDecreasing = true;

		items.replace(this.topPointer, this.highlightItem);
		items.replace(this.bottomPointer, this.highlightItem);

		if (this.topPointer == 0 || this.topPointer == 8)
			items.replace(this.topPointer, this.cornerItem);

		if (this.bottomPointer == this.frameSize - 9 || this.bottomPointer == this.frameSize - 1)
			items.replace(this.bottomPointer, this.cornerItem);

		return items;
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @param side
	 * @param corner
	 * @return
	 */
	public static ItemFrameBounceSlider from(final CompMaterial filler, final CompMaterial highlighted, final CompMaterial side, final CompMaterial corner) {
		return from(ItemCreator.of(filler), ItemCreator.of(highlighted), ItemCreator.of(side), ItemCreator.of(corner));
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @param side
	 * @param corner
	 * @return
	 */
	public static ItemFrameBounceSlider from(final ItemCreator filler, final ItemCreator highlighted, final ItemCreator side, final ItemCreator corner) {
		return from(filler.make(), highlighted.make(), side.make(), corner.make());
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @param side
	 * @param corner
	 * @return
	 */
	public static ItemFrameBounceSlider from(final ItemStack filler, final ItemStack highlighted, final ItemStack side, final ItemStack corner) {
		return new ItemFrameBounceSlider(filler, highlighted, side, corner);
	}
}