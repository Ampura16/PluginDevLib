package top.brmc.devlib.slider;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 一个遍历物品并高亮其中一个的示例滑块。
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemSlider implements Slider<List<ItemStack>> {

	/**
	 * 填充物品
	 */
	private final ItemStack fillerItem;

	/**
	 * 选中的物品
	 */
	private final ItemStack highlightItem;

	/**
	 * 高亮物品两侧（左侧和右侧）环绕的物品数量。
	 */
	private int width = 1;

	/*
	 * The current head in the slider.
	 */
	private int currentPointer = 0;

	/**
	 * 设置高亮物品两侧（左侧和右侧）环绕的物品数量。
	 *
	 * @param width
	 * @return
	 */
	public ItemSlider width(int width) {
		this.width = width;

		return this;
	}

	/**
	 * @see top.brmc.devlib.slider.Slider#next()
	 */
	@Override
	public List<ItemStack> next() {

		if (this.currentPointer == this.width)
			this.currentPointer = 0;

		final List<ItemStack> items = new ArrayList<>();

		for (int i = this.width - 1; i > this.width - this.currentPointer - 1; i--)
			items.add(this.fillerItem);

		items.add(this.highlightItem);

		for (int i = 0; i < this.width - this.currentPointer - 1; i++)
			items.add(this.fillerItem);

		this.currentPointer++;

		return items;
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemSlider from(CompMaterial filler, CompMaterial highlighted) {
		return from(ItemCreator.of(filler), ItemCreator.of(highlighted));
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemSlider from(ItemCreator filler, ItemCreator highlighted) {
		return from(filler.make(), highlighted.make());
	}

	/**
	 * 为给定物品创建一个新的滑块。
	 *
	 * @param filler
	 * @param highlighted
	 * @return
	 */
	public static ItemSlider from(ItemStack filler, ItemStack highlighted) {
		return new ItemSlider(filler, highlighted);
	}
}