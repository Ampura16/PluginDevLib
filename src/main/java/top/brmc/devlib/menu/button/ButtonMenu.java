package top.brmc.devlib.menu.button;

import java.util.concurrent.Callable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.menu.Menu;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.remain.CompMaterial;

import lombok.Getter;

/**
 * 打开另一个菜单的按钮
 */
public final class ButtonMenu extends Button {

	/**
	 * 有时你需要在创建按钮时分配数据，
	 * 但在创建此按钮的新实例时这些数据还不可用
	 * <p>
	 * 使用此辅助对象，在显示按钮之前再设置它们
	 */
	private final Callable<Menu> menuLateBind;

	/**
	 * 此按钮打开的菜单
	 */
	private final Menu menuToOpen;

	/**
	 * 此按钮的图标
	 */
	@Getter
	private final ItemStack item;

	/**
	 * 显示菜单时是否使用 {@link Menu#newInstance()} 创建新实例？
	 */
	private final boolean newInstance;

	/**
	 * 创建一个打开另一个菜单的新按钮
	 *
	 * @param menuClass
	 * @param material
	 * @param name
	 * @param lore
	 */
	public ButtonMenu(final Class<? extends Menu> menuClass, final CompMaterial material, final String name, final String... lore) {
		this(null, () -> ReflectionUtil.instantiate(menuClass), ItemCreator.of(material, name, lore).hideTags(true).make(), false);
	}

	/**
	 * 创建一个打开另一个菜单的新按钮
	 *
	 * @param menuLateBind
	 * @param item
	 */
	public ButtonMenu(final Callable<Menu> menuLateBind, final ItemCreator item) {
		this(null, menuLateBind, item.hideTags(true).make(), false);
	}

	/**
	 * 创建一个打开另一个菜单的新按钮
	 *
	 * @param menuLateBind
	 * @param item
	 */
	public ButtonMenu(final Callable<Menu> menuLateBind, final ItemStack item) {
		this(null, menuLateBind, item, false);
	}

	/**
	 * 创建一个打开另一个菜单的新按钮
	 *
	 * @param menu
	 * @param material
	 * @param name
	 * @param lore
	 */
	public ButtonMenu(final Menu menu, final CompMaterial material, final String name, final String... lore) {
		this(menu, ItemCreator.of(material, name, lore));
	}

	/**
	 * 创建一个打开另一个菜单的新按钮
	 *
	 * @param menu
	 * @param item
	 */
	public ButtonMenu(final Menu menu, final ItemCreator item) {
		this(menu, null, item.hideTags(true).make(), false);
	}

	/**
	 * 创建一个打开另一个菜单的新按钮
	 *
	 * @param menu
	 * @param item
	 */
	public ButtonMenu(final Menu menu, final ItemStack item) {
		this(menu, null, item, false);
	}

	public ButtonMenu(final Menu menu, final ItemStack item, final boolean newInstance) {
		this(menu, null, item, newInstance);
	}

	// Private constructor
	private ButtonMenu(final Menu menuToOpen, final Callable<Menu> menuLateBind, final ItemStack item, final boolean newInstance) {
		this.menuToOpen = menuToOpen;
		this.menuLateBind = menuLateBind;
		this.item = item;
		this.newInstance = newInstance;
	}

	/**
	 * 点击按钮时自动显示另一个菜单
	 */
	@Override
	public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
		if (this.menuLateBind != null) {
			Menu menuToOpen = null;

			try {
				menuToOpen = this.menuLateBind.call();
			} catch (final Exception ex) {
				ex.printStackTrace();

				return;
			}

			if (this.newInstance)
				menuToOpen = menuToOpen.newInstance();

			menuToOpen.displayTo(pl);

		} else {
			Valid.checkNotNull(this.menuToOpen, "Report / ButtonTrigger requires either 'late bind menu' or normal menu to be set!");

			if (this.newInstance)
				this.menuToOpen.newInstance().displayTo(pl);
			else
				this.menuToOpen.displayTo(pl);
		}
	}
}