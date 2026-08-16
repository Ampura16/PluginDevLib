package org.mineacademy.fo.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * 自动支持分页的高级物品列表菜单
 *
 * @param <T> 每页所包含的条目类型
 */
public abstract class MenuPagged<T> extends Menu {

	/**
	 * 激活状态翻页按钮的材质，用于可以点击的上一页/下一页按钮
	 * （例如可以前往下一页/上一页时）
	 *
	 * 默认为黄绿色染料
	 */
	@Getter
	@Setter
	private static CompMaterial activePageButton = CompMaterial.LIME_DYE;

	/**
	 * 未激活状态翻页按钮的材质，用于无法点击的上一页/下一页
	 * 按钮（即在第一页/最后一页时）
	 *
	 * 默认为灰色染料
	 */
	@Getter
	@Setter
	private static CompMaterial inactivePageButton = CompMaterial.GRAY_DYE;

	/**
	 * 当前页物品所在的槽位
	 */
	@Getter
	private final List<Integer> slots;

	/**
	 * 被遍历的原始条目
	 */
	private final Iterable<T> items;

	/**
	 * 页面大小，会覆盖根据物品数量自动调整菜单大小的
	 * 自动分页系统
	 */
	private final Integer manualPageSize;

	/**
	 * 按页码存放的页面，每页包含一个条目列表
	 */
	@Getter
	private final Map<Integer, List<T>> pages = new HashMap<>();

	/**
	 * 当前页
	 */
	@Getter
	private int currentPage = 1;

	/**
	 * 自动生成的下一页按钮
	 */
	private Button nextButton;

	/**
	 * 自动生成的“前往上一页”按钮
	 */
	private Button prevButton;

	/**
	 * 创建自动确定页面大小的新分页菜单
	 *
	 * @param items
	 */
	protected MenuPagged(@NonNull final T... items) {
		this(null, null, null, Arrays.asList(items), false);
	}

	/**
	 * 创建自动确定页面大小的新分页菜单
	 *
	 * @param items 页面条目
	 */
	protected MenuPagged(final Iterable<T> items) {
		this(null, null, null, items, false);
	}

	/**
	 * 创建自动确定页面大小的新分页菜单
	 *
	 * @param parent 父菜单
	 * @param items  页面条目
	 */
	protected MenuPagged(final Menu parent, @NonNull final T... items) {
		this(null, parent, null, Arrays.asList(items), false);
	}

	/**
	 * 创建自动确定页面大小的新分页菜单
	 *
	 * @param parent 父菜单
	 * @param items  页面条目
	 */
	protected MenuPagged(final Menu parent, final Iterable<T> items) {
		this(null, parent, null, items, false);
	}

	/**
	 * 创建自动确定页面大小的新分页菜单
	 *
	 * @param slots  每页放置条目的槽位
	 * @param items  页面条目
	 */
	protected MenuPagged(final List<Integer> slots, final Iterable<T> items) {
		this(null, null, slots, items, false);
	}

	/**
	 * 创建自动确定页面大小的新分页菜单
	 *
	 * @param parent 父菜单
	 * @param slots  每页放置条目的槽位
	 * @param items  页面条目
	 */
	protected MenuPagged(final Menu parent, final List<Integer> slots, final Iterable<T> items) {
		this(null, parent, slots, items, false);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param parent 父菜单
	 * @param items  页面条目
	 * @param returnMakesNewInstance
	 */
	protected MenuPagged(final Menu parent, final Iterable<T> items, final boolean returnMakesNewInstance) {
		this(null, parent, null, items, returnMakesNewInstance);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param parent 父菜单
	 * @param slots  每页放置条目的槽位
	 * @param items  页面条目
	 * @param returnMakesNewInstance
	 */
	protected MenuPagged(final Menu parent, final List<Integer> slots, final Iterable<T> items, final boolean returnMakesNewInstance) {
		this(null, parent, slots, items, returnMakesNewInstance);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param pageSize 菜单大小，须为 9 的倍数（注意我们已经额外
	 *                 添加了 1 行）
	 * @param items    页面条目
	 */
	protected MenuPagged(final int pageSize, @NonNull final T... items) {
		this(pageSize, null, null, Arrays.asList(items), false);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param pageSize 菜单大小，须为 9 的倍数（注意我们已经额外
	 *                 添加了 1 行）
	 * @param items    页面条目
	 */
	protected MenuPagged(final int pageSize, final Iterable<T> items) {
		this(pageSize, null, null, items, false);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param pageSize 菜单大小，须为 9 的倍数（注意我们已经额外
	 *                 添加了 1 行）
	 * @param items    页面条目
	 */
	protected MenuPagged(final int pageSize, final List<Integer> slots, final Iterable<T> items) {
		this(pageSize, null, slots, items, false);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param pageSize 菜单大小，须为 9 的倍数（注意我们已经额外
	 *                 添加了 1 行）
	 * @param parent   父菜单
	 * @param items    页面条目
	 */
	protected MenuPagged(final int pageSize, final Menu parent, @NonNull T... items) {
		this(pageSize, parent, null, Arrays.asList(items), false);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param pageSize 菜单大小，须为 9 的倍数（注意我们已经额外
	 *                 添加了 1 行）
	 * @param parent   父菜单
	 * @param items    页面条目
	 */
	protected MenuPagged(final int pageSize, final Menu parent, final Iterable<T> items) {
		this(pageSize, parent, null, items, false);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param pageSize
	 * @param parent
	 * @param items
	 * @param returnMakesNewInstance
	 */
	protected MenuPagged(final int pageSize, final Menu parent, final Iterable<T> items, final boolean returnMakesNewInstance) {
		this(pageSize, parent, null, items, returnMakesNewInstance);
	}

	/**
	 * 创建新的分页菜单
	 *
	 * @param pageSize               菜单大小，须为 9 的倍数（注意我们已经额外
	 *                               添加了 1 行）
	 * @param slots                  每页放置条目的槽位
	 * @param parent                 父菜单
	 * @param items                  页面条目
	 * @param returnMakesNewInstance 返回父菜单时是否应重新实例化父菜单？
	 */
	private MenuPagged(final Integer pageSize, final Menu parent, final List<Integer> slots, final Iterable<T> items, final boolean returnMakesNewInstance) {
		super(parent, returnMakesNewInstance);

		this.slots = slots != null ? slots : new ArrayList<>();
		this.items = items;
		this.manualPageSize = pageSize;

		this.calculatePages();
		this.setButtons();
	}

	/*
	 * Recalculate pages
	 */
	private void calculatePages() {
		final int items = this.getItemAmount(this.items);
		final int autoPageSize;

		if (this.slots.isEmpty()) {
			autoPageSize = this.manualPageSize != null ? this.manualPageSize : items <= 9 ? 9 * 1 : items <= 9 * 2 ? 9 * 2 : items <= 9 * 3 ? 9 * 3 : items <= 9 * 4 ? 9 * 4 : 9 * 5;

			for (int i = 0; i < autoPageSize; i++)
				this.slots.add(i);

			this.setSize(9 + autoPageSize);

		} else
			autoPageSize = this.slots.size();

		this.pages.clear();
		this.pages.putAll(Common.fillPages(autoPageSize, this.items));
	}

	@SuppressWarnings("unused")
	private int getItemAmount(final Iterable<T> pages) {
		int amount = 0;

		for (final T t : pages)
			amount++;

		return amount;
	}

	// Render the next/prev buttons
	private void setButtons() {

		// Set previous button
		this.prevButton = this.canShowPreviousButton() ? this.formPreviousButton() : Button.makeEmpty();

		// Set next page button
		this.nextButton = this.canShowNextButton() ? this.formNextButton() : Button.makeEmpty();
	}

	/**
	 * 默认情况下，至少有 2 页时返回 true，
	 * 可覆盖以实现自定义功能。
	 *
	 * @return
	 */
	protected boolean canShowPreviousButton() {
		return this.pages.size() > 1;
	}

	/**
	 * 默认情况下，至少有 2 页时返回 true，
	 * 可覆盖以实现自定义功能。
	 *
	 * @return
	 */
	protected boolean canShowNextButton() {
		return this.pages.size() > 1;
	}

	/**
	 * 返回用于显示上一页的按钮，
	 * 可覆盖以自定义。
	 *
	 * @return
	 */
	public Button formPreviousButton() {
		return new Button() {
			final boolean canGo = getCurrentPage() > 1;

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				if (this.canGo)
					setCurrentPage(MathUtil.range(getCurrentPage() - 1, 1, getPages().size()));
			}

			@Override
			public ItemStack getItem() {
				final int previousPage = getCurrentPage() - 1;

				return ItemCreator
						.of(this.canGo ? MenuPagged.getActivePageButton() : MenuPagged.getInactivePageButton())
						.name(previousPage == 0 ? SimpleLocalization.Menu.PAGE_FIRST : SimpleLocalization.Menu.PAGE_PREVIOUS.replace("{page}", String.valueOf(previousPage)))
						.make();
			}
		};
	}

	/**
	 * 返回用于显示下一页的按钮，
	 * 可覆盖以自定义。
	 *
	 * @return
	 */
	public Button formNextButton() {
		return new Button() {
			final boolean canGo = getCurrentPage() < getPages().size();

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				if (this.canGo)
					setCurrentPage(MathUtil.range(getCurrentPage() + 1, 1, getPages().size()));
			}

			@Override
			public ItemStack getItem() {
				final boolean lastPage = getCurrentPage() == getPages().size();

				return ItemCreator
						.of(this.canGo ? MenuPagged.getActivePageButton() : MenuPagged.getInactivePageButton())
						.name(lastPage ? SimpleLocalization.Menu.PAGE_LAST : SimpleLocalization.Menu.PAGE_NEXT.replace("{page}", String.valueOf(getCurrentPage() + 1)))
						.make();
			}
		};
	}

	/**
	 * 显示当前页的物品
	 *
	 * @param currentPage
	 */
	protected void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;

		this.updatePage();
	}

	// Reinits the menu and plays the anvil sound
	private void updatePage() {
		this.setButtons();
		this.restartMenu();

		Menu.getSound().play(this.getViewer());
		PlayerUtil.updateInventoryTitle(this.getViewer(), this.getTitleWithPageNumbers());
	}

	/**
	 * 获取标题和页码
	 *
	 * @return
	 */
	public final String getTitleWithPageNumbers() {
		final boolean canAddNumbers = this.addPageNumbers() && this.pages.size() > 1;

		return "&0" + this.getTitle() + (canAddNumbers ? " &8" + this.currentPage + "/" + this.pages.size() : "");
	}

	/**
	 * 自动在标题前加上页码
	 * <p>
	 * 可覆盖以实现自定义的最终处理，但
	 * 请务必调用 super 方法，否则不会在
	 * {@link InventoryDrawer} 中设置标题
	 *
	 * @param
	 */
	@Override
	protected final void onPreDisplay(final InventoryDrawer drawer) {
		drawer.setTitle(this.getTitleWithPageNumbers());

		this.onPostDisplay(drawer);
	}

	/**
	 * 菜单重启时重新加载页面
	 */
	@Override
	final void onRestartInternal() {
		this.calculatePages();
	}

	/**
	 * 在菜单显示之前调用
	 *
	 * @param drawer
	 */
	protected void onPostDisplay(InventoryDrawer drawer) {
	}

	/**
	 * 返回某页上某个条目的 {@link ItemStack} 表示
	 * <p>
	 * 使用 {@link ItemCreator} 可以轻松创建。
	 *
	 * @param item 给定对象，例如 Arena
	 * @return 物品堆，例如带有竞技场名称的钻石剑
	 */
	protected abstract ItemStack convertToItemStack(T item);

	/**
	 * 条目被点击时自动调用
	 *
	 * @param player 点击的玩家
	 * @param item   被点击的条目
	 * @param click  点击类型
	 */
	protected abstract void onPageClick(Player player, T item, ClickType click);

	/**
	 * 若希望系统在标题后添加“页码/总页数”后缀则返回 true，
	 * 默认为 true
	 *
	 * @return
	 */
	protected boolean addPageNumbers() {
		return true;
	}

	/**
	 * 返回是否完全没有条目
	 *
	 * @return
	 */
	protected boolean isEmpty() {
		return this.pages.isEmpty() || this.pages.get(0).isEmpty();
	}

	/**
	 * 自动从实际页面获取正确的物品，包括
	 * 上一页/下一页按钮
	 *
	 * 覆盖此方法时，务必调用 super.getItemAt，否则
	 * 此菜单将不会生成页面和导航按钮
	 *
	 * @param slot 槽位
	 * @return 物品，或 null
	 */
	@Override
	public ItemStack getItemAt(final int slot) {
		if (this.slots.contains(slot) && this.slots.indexOf(slot) < this.getCurrentPageItems().size()) {
			final T object = this.getCurrentPageItems().get(this.slots.indexOf(slot));

			if (object != null)
				return this.convertToItemStack(object);
		}

		if (slot == this.getPreviousButtonPosition())
			return this.prevButton.getItem();

		if (slot == this.getNextButtonPosition())
			return this.nextButton.getItem();

		return null;
	}

	/**
	 * 覆盖以修改上一页按钮的位置，
	 * 默认为“菜单大小 - 6”
	 *
	 * @return
	 */
	protected int getPreviousButtonPosition() {
		return this.getSize() - 6;
	}

	/**
	 * 覆盖以修改下一页按钮的位置，
	 * 默认为“菜单大小 - 4”
	 *
	 * @return
	 */
	protected int getNextButtonPosition() {
		return this.getSize() - 4;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onMenuClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {
		if (this.slots.contains(slot) && this.slots.indexOf(slot) < this.getCurrentPageItems().size()) {
			final T obj = this.getCurrentPageItems().get(this.slots.indexOf(slot));

			if (obj != null) {
				final InventoryType prevType = Remain.invokeOpenInventoryMethod(player, "getType");
				this.onPageClick(player, obj, click);

				if (prevType == Remain.invokeOpenInventoryMethod(player, "getType")) {
					final Inventory topInventory = Remain.getTopInventoryFromOpenInventory(player);

					topInventory.setItem(slot, this.getItemAt(slot));
				}
			}
		}
	}

	/*
	 * Call our title method instead of getTitle
	 */
	@Override
	public final void animateTitle(final String title) {
		if (Menu.isTitleAnimationEnabled())
			PlayerUtil.updateInventoryTitle(this, this.getViewer(), title, this.getTitleWithPageNumbers(), Menu.getTitleAnimationDurationTicks());
	}

	// Do not allow override
	@Override
	public final void onButtonClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final Button button) {
		super.onButtonClick(player, slot, action, click, button);
	}

	// Do not allow override
	@Override
	public final void onMenuClick(final Player player, final int slot, final ItemStack clicked) {
		throw new FoException("Simplest click unsupported");
	}

	// Get all items in a page
	private List<T> getCurrentPageItems() {
		Valid.checkBoolean(this.pages.containsKey(this.currentPage - 1), "The menu has only " + this.pages.size() + " pages, not " + this.currentPage + "!");

		return this.pages.get(this.currentPage - 1);
	}
}
