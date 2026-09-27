package top.brmc.devlib.menu.button;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.Common;
import top.brmc.devlib.menu.Menu;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.model.Replacer;
import top.brmc.devlib.remain.CompColor;
import top.brmc.devlib.remain.CompItemFlag;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 表示一个标准化的删除按钮，点击后打开删除确认对话框。
 * <p>
 * 通常用于删除竞技场、职业、升级等。
 */
@RequiredArgsConstructor
public class ButtonRemove extends Button {

	/**
	 * 删除按钮的物品名称
	 */
	@Getter
	@Setter
	private static String title = "&4&lRemove {name}";

	/**
	 * 删除按钮的物品描述（lore）
	 */
	@Getter
	@Setter
	private static List<String> lore = Arrays.asList(
			"&r",
			"&7The selected {type} will",
			"&7be removed permanently.");

	/**
	 * 父菜单
	 */
	private final Menu parentMenu;

	/**
	 * 要删除的对象类型，例如职业、升级、竞技场
	 */
	private final String toRemoveType;

	/**
	 * 要删除的对象名称，例如职业的 "Warrior"
	 */
	private final String toRemoveName;

	/**
	 * 对象被删除时触发的操作
	 */
	private final Runnable removeAction;

	/**
	 * 此按钮的图标
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator

				.of(CompMaterial.LAVA_BUCKET)
				.name(title.replace("{name}", this.toRemoveName))

				.lore(Replacer.replaceArray(lore,
						"name", this.toRemoveName,
						"type", this.toRemoveType))

				.flags(CompItemFlag.HIDE_ATTRIBUTES)
				.make();
	}

	/**
	 * 确认删除的图标
	 *
	 * @return
	 */
	public ItemStack getRemoveConfirmItem() {
		return ItemCreator

				.ofWool(CompColor.RED)
				.name("&6&lRemove " + this.toRemoveName)

				.lore(Arrays.asList(
						"&r",
						"&7Confirm that this " + this.toRemoveType + " will",
						"&7be removed permanently.",
						"&cCannot be undone."))

				.flags(CompItemFlag.HIDE_ATTRIBUTES)
				.make();
	}

	public String getMenuTitle() {
		return "&0Confirm removal";
	}

	/**
	 * 点击时打开确认对话框
	 */
	@Override
	public void onClickedInMenu(final Player pl, final Menu menu, final ClickType click) {
		new MenuDialogRemove(this.parentMenu, new RemoveConfirmButton()).displayTo(pl);
	}

	/**
	 * 点击后真正删除对象的按钮
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	final class RemoveConfirmButton extends Button {

		@Override
		public ItemStack getItem() {
			return ButtonRemove.this.getRemoveConfirmItem();
		}

		/**
		 * 使用 {@link ButtonRemove#removeAction} 删除对象
		 */
		@Override
		public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
			player.closeInventory();
			ButtonRemove.this.removeAction.run();

			Common.tell(player, SimpleLocalization.Menu.ITEM_DELETED.replace("{item}", (!ButtonRemove.this.toRemoveType.isEmpty() ? ButtonRemove.this.toRemoveType + " " : "") + ButtonRemove.this.toRemoveName));
		}
	}

	/**
	 * 预先准备好的菜单，用于实现带确认步骤的两步删除
	 */
	final class MenuDialogRemove extends Menu {

		/**
		 * 触发删除的确认按钮
		 */
		private final Button confirmButton;

		/**
		 * 返回按钮
		 */
		private final Button returnButton;

		/**
		 * 创建一个新的删除确认对话框
		 *
		 * @param parentMenu    父菜单
		 * @param confirmButton 删除按钮
		 */
		public MenuDialogRemove(final Menu parentMenu, final RemoveConfirmButton confirmButton) {
			super(parentMenu);

			this.confirmButton = confirmButton;
			this.returnButton = new ButtonReturnBack(parentMenu);

			this.setSize(9 * 3);
			this.setTitle(ButtonRemove.this.getMenuTitle());
		}

		/**
		 * 返回正确槽位上的对应物品
		 *
		 * @param slot 槽位
		 * @return 物品，或 null
		 */
		@Override
		public ItemStack getItemAt(final int slot) {
			if (slot == 9 + 3)
				return this.confirmButton.getItem();

			if (slot == 9 + 5)
				return this.returnButton.getItem();

			return NO_ITEM;
		}

		/**
		 * 不要重复添加返回按钮
		 *
		 * @see top.brmc.devlib.menu.Menu#addReturnButton()
		 */
		@Override
		protected boolean addReturnButton() {
			return false;
		}

		@Override
		protected String[] getInfo() {
			return null;
		}
	}
}