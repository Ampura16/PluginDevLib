package org.mineacademy.fo.menu.button;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 表示返回父菜单的标准按钮
 */
@RequiredArgsConstructor
@AllArgsConstructor
public final class ButtonReturnBack extends Button {

	/**
	 * 此按钮的材质，默认为门
	 */
	@Getter
	@Setter
	private static CompMaterial material = CompMaterial.OAK_DOOR;

	/**
	 * 此按钮的标题
	 */
	@Getter
	@Setter
	private static String title = SimpleLocalization.Menu.BUTTON_RETURN_TITLE;

	/**
	 * 此按钮的描述（lore）
	 */
	@Getter
	@Setter
	private static List<String> lore = Arrays.asList(SimpleLocalization.Menu.BUTTON_RETURN_LORE);

	/**
	 * 你可以覆盖此按钮整个物品堆的外观。
	 */
	@Getter
	@Setter
	private static ItemStack itemStack = null;

	/**
	 * 父菜单
	 */
	@NonNull
	private final Menu parentMenu;

	/**
	 * 是否应创建父菜单的新实例？
	 * <p>
	 * 默认为 false。
	 */
	private boolean makeNewInstance = false;

	/**
	 * 此按钮的图标
	 */
	@Override
	public ItemStack getItem() {

		if (itemStack == null)
			return ItemCreator.of(material).name(title).lore(lore).make();

		return itemStack;
	}

	/**
	 * 点击时打开父菜单
	 */
	@Override
	public void onClickedInMenu(Player player, Menu menu, ClickType click) {

		if (this.makeNewInstance) {

			// Flush data so that the parent menu can call the saved data in the current menu
			//
			// Example: In the Boss plugin, players can create new submenus and returning back to the main
			// menu they were not able to see the new submenus in the list before this change.
			final Inventory currentChestInventory = Remain.getTopInventoryFromOpenInventory(player);

			if (currentChestInventory != null)
				menu.handleClose(currentChestInventory);

			this.parentMenu.newInstance().displayTo(player);

		} else
			this.parentMenu.displayTo(player);
	}
}