package top.brmc.devlib.menu.model;

import java.util.ArrayList;
import java.util.List;

import top.brmc.devlib.Common;
import top.brmc.devlib.menu.MenuQuantitable;

import lombok.RequiredArgsConstructor;

/**
 * 表示在菜单中点击物品时应将
 * ItemStack 的数量改变多少。
 * <p>
 * 用法示例参见 {@link MenuQuantitable}
 */
@RequiredArgsConstructor
public enum MenuQuantity {

	/**
	 * 将掉落概率改变 0.1%
	 */
	ONE_TENTH(0.1),

	/**
	 * 将掉落概率改变 0.5%
	 */
	HALF(0.5),

	/**
	 * 将掉落概率改变 1%
	 */
	ONE(1),

	/**
	 * 将掉落概率改变 2%
	 */
	TWO(2),

	/**
	 * 将掉落概率改变 5%
	 */
	FIVE(5),

	/**
	 * 将掉落概率改变 10%
	 */
	TEN(10),

	/**
	 * 将掉落概率改变 20%
	 */
	TWENTY(20);

	/**
	 * 要改变的数量
	 */
	private final double amountPercent;

	/**
	 * @deprecated 会对数量取整。你需要决定是否支持
	 * 小数数量：支持则使用 {@link #getAmountDouble()}，否则使用 {@link #getAmountPercent()}
	 *
	 * @return
	 */
	@Deprecated
	public int getAmount() {
		return (int) Math.round(this.getAmountPercent());
	}

	/**
	 * 获取数量，范围 0.00 到 1.00
	 *
	 * @return
	 */
	public double getAmountDouble() {
		return this.amountPercent / 100.D;
	}

	/**
	 * 获取数量，范围 0.00% 到 100.0%
	 *
	 * @return
	 */
	public double getAmountPercent() {
		return this.amountPercent;
	}

	/**
	 * 向后轮换枚举
	 * @param allowDecimals
	 *
	 * @return 上一个枚举序号；越界时返回最后一个
	 */
	public final MenuQuantity previous(boolean allowDecimals) {
		return Common.getNext(this, this.compileQuantities(allowDecimals), false);
	}

	/**
	 * 向前轮换枚举
	 * @param allowDecimals
	 *
	 * @return 下一个枚举序号；越界时返回第一个
	 */
	public final MenuQuantity next(boolean allowDecimals) {
		return Common.getNext(this, this.compileQuantities(allowDecimals), true);
	}

	/*
	 * Helper to compile quantities including below 1%
	 */
	private List<MenuQuantity> compileQuantities(boolean includeDecimals) {
		final List<MenuQuantity> available = new ArrayList<>();

		for (final MenuQuantity quantity : values())
			if (includeDecimals || quantity.getAmountPercent() >= 1.00)
				available.add(quantity);

		return available;
	}
}