package top.brmc.devlib.menu;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.collection.StrictMap;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.menu.model.MenuClickLocation;
import top.brmc.devlib.remain.CompMaterial;

import lombok.Getter;
import lombok.Setter;

/**
 * 一个允许玩家向容器中放入或取出物品的简单菜单。
 *
 * 你可以将其与文件存储系统连接，
 * 以保存或加载玩家在容器中编辑的物品。
 */
public abstract class MenuContainer extends Menu {

	/**
	 * 出于安全考虑，用来填充底栏的填充物品。
	 */
	@Getter
	@Setter
	private ItemStack bottomBarFillerItem = ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, " ").make();

	/**
	 * 创建一个可以编辑放入物品几率的新菜单。
	 */
	protected MenuContainer() {
		this(null);
	}

	/**
	 * 创建一个可以编辑放入物品几率的新菜单。
	 *
	 * @param parent
	 */
	protected MenuContainer(Menu parent) {
		this(parent, false);
	}

	/**
	 * 创建一个可以编辑放入物品几率的新菜单。
	 *
	 * @param parent
	 * @param returnMakesNewInstance 返回父菜单时是否应
	 *                               重新实例化父菜单？
	 */
	protected MenuContainer(Menu parent, boolean returnMakesNewInstance) {
		super(parent, returnMakesNewInstance);

		// Default the size to 3 rows (+ 1 bottom row is added automatically)
		this.setSize(9 * 3);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting items
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see top.brmc.devlib.menu.Menu#getItemAt(int)
	 */
	@Override
	public final ItemStack getItemAt(int slot) {

		final ItemStack customDrop = this.getDropAt(slot);

		if (customDrop != null)
			return customDrop;

		if (slot > this.getSize() - 9)
			return this.bottomBarFillerItem;

		return NO_ITEM;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Allowing clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see top.brmc.devlib.menu.Menu#isActionAllowed(top.brmc.devlib.menu.model.MenuClickLocation, int, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack)
	 */
	@Override
	public final boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, final InventoryAction action) {

		if (location != MenuClickLocation.MENU && action != InventoryAction.MOVE_TO_OTHER_INVENTORY)
			return true;

		if (!this.canEditItem(location, slot, clicked, cursor, action))
			return false;

		return true;
	}

	/**
	 * 对希望玩家可以编辑的槽位返回 true。
	 * 默认允许玩家编辑底栏以上的任何内容。
	 *
	 * 此方法由 {@link #isActionAllowed(MenuClickLocation, int, ItemStack, ItemStack)} 调用，
	 * 默认会把调用转发给 {@link #canEditItem(int)}
	 *
	 * 底行始终受保护
	 *
	 * @param location
	 * @param slot
	 * @param clicked
	 * @param cursor
	 * @param action
	 *
	 * @return
	 */
	protected boolean canEditItem(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, InventoryAction action) {
		return this.canEditItem(slot);
	}

	/**
	 * 返回你希望在菜单中允许编辑物品的
	 * 槽位编号（如果你不想允许编辑
	 * 整个容器窗口）。
	 *
	 * @param slot
	 * @return
	 */
	protected boolean canEditItem(int slot) {
		return slot < this.getSize() - 9;
	}

	/**
	 * 返回应出现在给定槽位上的物品，
	 * 你应在此处从数据文件或缓存加载物品。
	 *
	 * @param slot
	 * @return
	 */
	protected abstract ItemStack getDropAt(int slot);

	// ------------------------------------------------------------------------------------------------------------
	// Handling clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see top.brmc.devlib.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.event.inventory.InventoryAction, org.bukkit.event.inventory.ClickType, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack, boolean)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, InventoryAction action, ClickType clickType, ItemStack cursor, ItemStack clicked, boolean cancelled) {

		if (this.canEditItem(slot) && slot < this.getSize() - 9) {

			// Call our handler
			clicked = this.onItemClick(slot, clickType, clicked);

			// Update item
			this.setItem(slot, clicked);
		}
	}

	/*
	 * @see top.brmc.devlib.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.inventory.ItemStack)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, ItemStack clicked) {
		throw new FoException("unsupported call");
	}

	/**
	 * 点击给定槽位时自动调用，
	 * 你可以在此编辑被点击的物品，或直接原样传递。
	 *
	 * @param slot
	 * @param clickType
	 * @param item
	 * @return
	 */
	protected ItemStack onItemClick(int slot, ClickType clickType, @Nullable ItemStack item) {
		return item;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Handling saving
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see top.brmc.devlib.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
	 */
	@Override
	protected final void onMenuClose(Player player, Inventory inventory) {
		final StrictMap<Integer, ItemStack> items = new StrictMap<>();

		for (int slot = 0; slot < this.getSize() - 9; slot++)
			if (this.canEditItem(slot)) {
				final ItemStack item = inventory.getItem(slot);

				items.put(slot, item);
			}

		this.onMenuClose(items);
	}

	/**
	 * 需要保存映射中所有可编辑槽位时自动调用，
	 * 映射按槽位存储其物品（可为 null）。
	 *
	 * @param items
	 */
	protected abstract void onMenuClose(StrictMap<Integer, ItemStack> items);

	// ------------------------------------------------------------------------------------------------------------
	// Decoration
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see top.brmc.devlib.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				"This menu allows you to drop",
				"items to this container.",
				"",
				"Simply &2drag and drop &7items",
				"from your inventory here."
		};
	}
}
