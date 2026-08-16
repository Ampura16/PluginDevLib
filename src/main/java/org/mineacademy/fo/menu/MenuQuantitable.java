package org.mineacademy.fo.menu;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.NonNull;

/**
 * 高级菜单概念，允许单次点击时将物品的数值改变
 * 超过 1。
 * <p>
 * 例如：你想把生成概率从 1% 改到 100%，于是
 * 把编辑数量设为 20，这样只需点击物品 5 次，
 * 而不是 99999 次。
 * <p>
 * 我们把它做成接口，这样你就可以扩展其他各种菜单
 */
public interface MenuQuantitable {

	/* ------------------------------------------------------------------------------- */
	/* Quantity */
	/* ------------------------------------------------------------------------------- */

	/**
	 * 获取当前的编辑数量
	 *
	 * @return 编辑数量
	 */
	@NonNull
	MenuQuantity getQuantity();

	/**
	 * 设置新的编辑数量
	 *
	 * @param newQuantity 新的数量
	 */
	void setQuantity(@NonNull MenuQuantity newQuantity);

	/**
	 * 工具方法，获取每次点击改变的概率数量，
	 * 并在末尾附加 % 符号进行格式化。
	 *
	 * @return
	 */
	default String getCurrentQuantityPercent() {
		final double percent = this.getQuantity().getAmountPercent();

		return (this.allowDecimalQuantities() ? MathUtil.formatTwoDigits(percent) : String.valueOf((int) percent)) + (this.quantitiesArePercents() ? "%" : "");
	}

	/**
	 * @deprecated 会对数量取整。你需要决定是否支持
	 * 小数数量：支持则使用 {@link #getNextQuantityDouble(ClickType)}，否则使用 {@link #getNextQuantityPercent(ClickType)}
	 *
	 * @param clickType
	 * @return
	 */
	@Deprecated
	default int getNextQuantity(ClickType clickType) {
		return (int) Math.round(this.getNextQuantityPercent(clickType));
	}

	/**
	 * 根据点击获取下一个编辑数量，范围 0.0 到 1.0
	 *
	 * @param clickType 点击类型
	 * @return 下一个数量（根据点击变高或变低）
	 */
	default double getNextQuantityDouble(ClickType clickType) {
		return clickType == ClickType.LEFT ? -this.getQuantity().getAmountDouble() : this.getQuantity().getAmountDouble();
	}

	/**
	 * 根据点击获取下一个编辑数量，范围 0.0 到 100.0
	 *
	 * @param clickType 点击类型
	 * @return 下一个数量（根据点击变高或变低）
	 */
	default double getNextQuantityPercent(ClickType clickType) {
		return clickType == ClickType.LEFT ? -this.getQuantity().getAmountPercent() : this.getQuantity().getAmountPercent();
	}

	/**
	 * @see #getQuantityButton(Menu)
	 * @deprecated 已重命名为 {@link #getQuantityButton(Menu)}
	 *
	 * @param menu
	 * @return
	 */
	@Deprecated
	default Button getEditQuantityButton(Menu menu) {
		return this.getQuantityButton(menu);
	}

	/**
	 * 获取负责设置编辑数量的按钮
	 * 默认已实现。
	 *
	 * @param menu 菜单
	 * @return 负责设置编辑数量的按钮
	 */
	default Button getQuantityButton(Menu menu) {
		return new Button() {

			@Override
			public final void onClickedInMenu(Player player, Menu clickedMenu, ClickType clickType) {
				final MenuQuantity nextQuantity = clickType == ClickType.LEFT ? MenuQuantitable.this.getQuantity().previous(MenuQuantitable.this.allowDecimalQuantities()) : MenuQuantitable.this.getQuantity().next(MenuQuantitable.this.allowDecimalQuantities());
				Valid.checkNotNull(nextQuantity, "Next quantity cannot be null. Current: " + MenuQuantitable.this.getQuantity() + " Click: " + clickType);

				MenuQuantitable.this.setQuantity(nextQuantity);

				menu.restartMenu("&9Editing quantity set to " + MenuQuantitable.this.getCurrentQuantityPercent());
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator
						.of(
								CompMaterial.STRING,
								"Edit Quantity: &7" + MenuQuantitable.this.getCurrentQuantityPercent(),
								"",
								"&8< &7Left click to decrease",
								"&8> &7Right click to increase")
						.make();
			}
		};
	}

	/**
	 * 返回用于编辑单次点击时向容器物品添加/移除多少数量的按钮
	 * 应放置的位置。默认为底部中间槽位。
	 *
	 * 返回 -1 表示隐藏该按钮。
	 *
	 * @return
	 */
	default int getQuantityButtonPosition() {
		return ((Menu) this).getBottomCenterSlot();
	}

	/**
	 * 是否允许编辑低于 1% 的数量，例如 0.5%？
	 * 不要用于物品堆数量。
	 *
	 * @return
	 */
	default boolean allowDecimalQuantities() {
		return false;
	}

	/**
	 * 数量是否为百分比？我们只是用它在物品描述中
	 * 为数量追加 %。
	 *
	 * @return
	 */
	default boolean quantitiesArePercents() {
		return false;
	}

	/* ------------------------------------------------------------------------------- */
	/* Level-related */
	/* ------------------------------------------------------------------------------- */

	default ItemStack addLevelToItem(ItemStack item, int level) {
		return this.addLevelToItem(item, String.valueOf(level));
	}

	default ItemStack addLevelToItem(ItemStack item, String level) {

		// Paint the item with the drop chance lore
		final List<String> dropChanceLore = Replacer.replaceArray(Arrays.asList(

				// Lore
				"",
				this.getLevelLoreLabel() + ": &6{level}",
				"",
				"   &8(Mouse click)",
				"  &7&l< &4-{quantity}    &2+{quantity} &7&l>"),

				// Variables
				"level", level,
				"quantity", this.getCurrentQuantityPercent());

		return ItemCreator.of(item.clone()).clearLore().lore(dropChanceLore).makeMenuTool();
	}

	default String getLevelLoreLabel() {
		return "Current level";
	}
}
