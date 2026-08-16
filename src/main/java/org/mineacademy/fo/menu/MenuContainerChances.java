package org.mineacademy.fo.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.menu.model.MenuQuantity;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 允许玩家向容器中放入物品并保存的菜单。
 *
 * 它还提供了为每个物品设置“几率”的方式。例如你可以
 * 将其用作自定义实体掉落表中的掉落几率。
 *
 * 建议你参照视频教程来实现它。我们在 mineacademy.org 的课程中
 * 提供完整的 GUI 培训。
 */
public abstract class MenuContainerChances extends Menu implements MenuQuantitable {

	/**
	 * 出于安全考虑，用来填充底栏的填充物品。
	 */
	@Getter
	@Setter
	private ItemStack bottomBarFillerItem = ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, " ").make();

	/**
	 * 在此临时存储已编辑的掉落几率
	 */
	private final StrictMap<Integer, Double> editedDropChances = new StrictMap<>();

	/**
	 * 用于切换菜单模式的按钮。
	 */
	private final Button changeModeButton;

	/**
	 * 此菜单当前所处的模式。
	 */
	@Getter
	@Setter
	private MenuQuantity quantity = MenuQuantity.ONE;

	/*
	 * The current menu mode stored here.
	 */
	@Getter(AccessLevel.PROTECTED)
	private EditMode mode = EditMode.ITEM;

	/**
	 * 创建一个可以编辑放入物品几率的新菜单。
	 *
	 * @param parent
	 */
	protected MenuContainerChances(Menu parent) {
		this(parent, false);
	}

	/**
	 * 创建一个可以编辑放入物品几率的新菜单。
	 *
	 * @param parent
	 * @param startMode
	 * @param returnMakesNewInstance
	 */
	protected MenuContainerChances(Menu parent, boolean returnMakesNewInstance) {
		super(parent, returnMakesNewInstance);

		// Default the size to 3 rows (+ 1 bottom row is added automatically)
		this.setSize(9 * 3);

		this.changeModeButton = new Button() {

			/**
			 * 切换菜单模式并刷新其内容。
			 */
			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final MenuContainerChances instance = MenuContainerChances.this;

				// Call event to properly save data early
				instance.onMenuClose(player, Remain.getTopInventoryFromOpenInventory(player));

				// Simulate mode chance in the menu
				instance.mode = MenuContainerChances.this.mode.next();
				instance.setTitle("&0Editing " + instance.mode.getKey());

				instance.restartMenu(null, false);
			}

			/**
			 * 编译编辑模式按钮。
			 */
			@Override
			public ItemStack getItem() {
				final boolean chances = MenuContainerChances.this.mode == EditMode.CHANCE;

				return ItemCreator.of(
						chances ? CompMaterial.GOLD_NUGGET : CompMaterial.CHEST,
						"Editing " + MenuContainerChances.this.mode.getKey(),
						"",
						"&7Click to edit " + MenuContainerChances.this.mode.next().getKey().toLowerCase() + ".")
						.glow(chances)
						.make();
			}
		};
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#getQuantityButtonPosition()
	 */
	@Override
	public int getQuantityButtonPosition() {
		return this.mode == EditMode.ITEM ? -1 : MenuQuantitable.super.getQuantityButtonPosition();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getting items
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#allowDecimalQuantities()
	 */
	@Override
	public boolean allowDecimalQuantities() {
		return false;
	}

	/*
	 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
	 */
	@Override
	public final ItemStack getItemAt(int slot) {
		if (slot == this.getChangeModeButtonPosition())
			return this.changeModeButton.getItem();

		final ItemStack customDrop = this.getDropAt(slot);

		if (customDrop != null) {

			if (this.mode == EditMode.ITEM || !this.canEditItem(slot))
				return customDrop;

			final double dropChance = this.mode == EditMode.ITEM ? this.getDropChance(slot) : this.editedDropChances.getOrDefault(slot, this.getDropChance(slot));
			final String level = MathUtil.formatTwoDigits(100 * dropChance) + "%";

			return this.addLevelToItem(customDrop, level);
		}

		if (slot > this.getSize() - 9)
			return this.bottomBarFillerItem;

		return NO_ITEM;
	}

	/**
	 * 返回 {@link #changeModeButton} 的位置，默认为 (getSize() - 4)
	 *
	 * @return
	 */
	protected int getChangeModeButtonPosition() {
		return this.getSize() - 2;
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#getLevelLoreLabel()
	 */
	@Override
	public String getLevelLoreLabel() {
		return "Drop chance";
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuQuantitable#quantitiesArePercents()
	 */
	@Override
	public final boolean quantitiesArePercents() {
		return true;
	}

	/**
	 * 返回应出现在给定槽位上的物品，
	 * 你应在此处从数据文件或缓存加载物品。
	 *
	 * @param slot
	 * @return
	 */
	protected abstract ItemStack getDropAt(int slot);

	/**
	 * 在此返回从磁盘或缓存加载的物品掉落几率。
	 *
	 * @param slot
	 * @return
	 */
	protected abstract double getDropChance(int slot);

	// ------------------------------------------------------------------------------------------------------------
	// Allowing clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#isActionAllowed(org.mineacademy.fo.menu.model.MenuClickLocation, int, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack)
	 */
	@Override
	public final boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, InventoryAction action) {
		if (this.mode == EditMode.CHANCE)
			return false;

		if (location != MenuClickLocation.MENU && action != InventoryAction.MOVE_TO_OTHER_INVENTORY)
			return true;

		if (!this.canEditItem(location, slot, clicked, cursor, action))
			return false;

		return slot < this.getSize() - 9;
	}

	/**
	 * 对希望玩家可以编辑的槽位返回 true。
	 * 默认允许玩家编辑底栏以上的任何内容。
	 *
	 * 此方法由 {@link #isActionAllowed(MenuClickLocation, int, ItemStack, ItemStack)} 调用，
	 * 默认会把调用转发给 {@link #canEditItem(int)}
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
	 * 如果你希望用户可以编辑除底栏以外所有物品的几率，
	 * 这里直接始终返回 true 即可。
	 *
	 * @param slot
	 * @return
	 */
	protected boolean canEditItem(int slot) {
		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Handling clicking
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.event.inventory.InventoryAction, org.bukkit.event.inventory.ClickType, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack, boolean)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {

		if (this.mode == EditMode.CHANCE && this.canEditItem(slot) && slot < this.getSize() - 9) {

			// Prevent exploiting chances menu holding an item
			if (clicked == null)
				return;

			final double chance = this.editedDropChances.getOrDefault(slot, this.getDropChance(slot));
			final double next = this.getNextQuantityDouble(click);
			final double newChance = MathUtil.range(chance + next, 0.D, 1.D);

			// Save drop chance
			this.editedDropChances.override(slot, newChance);

			// Update item
			this.setItem(slot, this.getItemAt(slot));
		}
	}

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.inventory.ItemStack)
	 */
	@Override
	protected final void onMenuClick(Player player, int slot, ItemStack clicked) {
		throw new FoException("unsupported call");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Handling saving
	// ------------------------------------------------------------------------------------------------------------

	/*
	 * @see org.mineacademy.fo.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
	 */
	@Override
	protected final void onMenuClose(Player player, Inventory inventory) {
		final StrictMap<Integer, Tuple<ItemStack, Double>> items = new StrictMap<>();

		for (int slot = 0; slot < this.getSize() - 9; slot++) {
			boolean placed = false;

			if (this.canEditItem(slot)) {
				final ItemStack item = this.mode == EditMode.ITEM ? inventory.getItem(slot) : this.getDropAt(slot);
				final Double dropChance = this.editedDropChances.getOrDefault(slot, this.getDropChance(slot));

				if (item != null && !CompMaterial.isAir(item)) {
					Valid.checkNotNull(dropChance, "Drop chances cannot be null on slot " + slot + " for " + item);

					items.put(slot, new Tuple<>(item, dropChance));
					placed = true;
				}
			}

			if (!placed)
				items.put(slot, null);
		}

		this.onMenuClose(items);
	}

	/**
	 * 需要保存映射中所有可编辑槽位时自动调用，
	 * 映射按槽位存储其物品（可为 null）及新的掉落几率
	 *
	 * @param items
	 */
	protected abstract void onMenuClose(StrictMap<Integer, Tuple<ItemStack, Double>> items);

	// ------------------------------------------------------------------------------------------------------------
	// Decoration
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		if (this.mode == EditMode.ITEM)
			return new String[] {
					"This menu allows you to drop",
					"items to this container.",
					"",
					"Simply &2drag and drop &7items",
					"from your inventory here."
			};

		else
			return new String[] {
					"This menu allows you to edit drop",
					"chances for items in this container.",
					"",
					"&2Right or left click &7on items",
					"to adjust their drop chance."
			};
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 菜单编辑模式
	 */
	@RequiredArgsConstructor
	public enum EditMode {

		/**
		 * 允许玩家向菜单容器中放入物品，
		 * 例如 Boss 死亡时应掉落的物品。
		 */
		ITEM("Items"),

		/**
		 * 允许玩家编辑其放入容器的每个物品的
		 * 掉落几率，例如 Boss 死亡时
		 * 掉落每个物品的可能性。
		 */
		CHANCE("Drop Chances");

		/**
		 * 本地化键。
		 */
		@Getter
		private final String key;

		/**
		 * 获取用于刷新菜单的下一个模式。
		 *
		 * @return
		 */
		private EditMode next() {
			return Common.getNext(this, EditMode.values(), true);
		}
	}
}
