package top.brmc.devlib.slider;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemFrameClockwiseSlider implements Slider<Map<Integer, ItemStack>> {

	/**
	 * 填充物品
	 */
	private final ItemStack fillerItem;

	/**
	 * 选中的物品
	 */
	private final ItemStack highlightItem;

	/**
	 * 动画边框的尺寸（允许的值：18、27、36、45、54），请确保菜单尺寸足够大
	 */
	private int frameSize = 27;

	/*
	 * The current head in the slider.
	 */
	private int currentPointer = 0;

	/**
	 * 设置边框尺寸
	 *
	 * @param frameSize
	 * @return
	 */
	public ItemFrameClockwiseSlider frameSize(final int frameSize) {
		this.frameSize = frameSize;

		return this;
	}

	/**
	 * @see top.brmc.devlib.slider.Slider#next()
	 */
	@Override
	public Map<Integer, ItemStack> next() {

		final Map<Integer, ItemStack> items = new HashMap<>();
		final int rowCount = this.frameSize / 9;

		for (int index = 0; index < this.frameSize; index++) {
			final int row = index / 9;
			final int column = (index % 9) + 1;

			if (row == 0 || row == rowCount - 1 || column == 1 || column == 9)
				items.put(index, this.fillerItem);
		}

		if (this.currentPointer < 8)
			this.currentPointer++;

		else if (this.currentPointer == 8)
			this.currentPointer = 17;

		else if (this.currentPointer <= this.frameSize - 1 && this.currentPointer > this.frameSize - 9)
			this.currentPointer--;

		else if (this.currentPointer == 9)
			this.currentPointer = 0;

		else if (rowCount >= 3)
			if (this.currentPointer == 17)
				this.currentPointer = 26;

			else if (this.currentPointer == 18)
				this.currentPointer = 9;

			else if (rowCount >= 4)
				if (this.currentPointer == 26)
					this.currentPointer = 35;

				else if (this.currentPointer == 27)
					this.currentPointer = 18;

				else if (rowCount >= 5)
					if (this.currentPointer == 35)
						this.currentPointer = 44;

					else if (this.currentPointer == 36)
						this.currentPointer = 27;

					else if (rowCount == 6)
						if (this.currentPointer == 44)
							this.currentPointer = 53;

						else if (this.currentPointer == 45)
							this.currentPointer = 36;

		items.replace(this.currentPointer, this.highlightItem);

		return items;
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemFrameClockwiseSlider from(final CompMaterial filler, final CompMaterial highlighted) {
		return from(ItemCreator.of(filler), ItemCreator.of(highlighted));
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemFrameClockwiseSlider from(final ItemCreator filler, final ItemCreator highlighted) {
		return from(filler.make(), highlighted.make());
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemFrameClockwiseSlider from(final ItemStack filler, final ItemStack highlighted) {
		return new ItemFrameClockwiseSlider(filler, highlighted);
	}
}
