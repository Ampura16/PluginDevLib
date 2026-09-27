package top.brmc.devlib.menu.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.Common;
import top.brmc.devlib.remain.CompMaterial;

import lombok.Getter;

/**
 * 表示使用 Bukkit/Spigot 原生方法
 * 向玩家渲染背包界面的方式。
 * <p>
 * 如果你只是想显示某个背包界面，
 * 而不想创建完整的菜单，这也很方便。
 */
public final class InventoryDrawer {

	/**
	 * 背包界面的大小。
	 */
	@Getter
	private final int size;

	/**
	 * 背包界面的标题
	 */
	private String title;

	/**
	 * 此背包界面中的物品
	 */
	private final ItemStack[] content;

	/**
	 * 创建一个新的背包绘制器，参见 {@link #of(int, String)}
	 *
	 * @param size  大小
	 * @param title 标题
	 */
	private InventoryDrawer(int size, String title) {
		this.size = size;
		this.title = title;

		this.content = new ItemStack[size];
	}

	/**
	 * 从 0 号槽位开始，将物品添加到第一个空槽位
	 * <p>
	 * 如果背包已满，则添加到最后一个槽位并替换现有物品
	 *
	 * @param item 物品
	 */
	public void pushItem(ItemStack item) {
		boolean added = false;

		for (int i = 0; i < this.content.length; i++) {
			final ItemStack currentItem = this.content[i];

			if (currentItem == null) {
				this.content[i] = item;
				added = true;

				break;
			}
		}

		if (!added)
			this.content[this.size - 1] = item;
	}

	/**
	 * 当前槽位是否被非 null 的 {@link ItemStack} 占用？
	 *
	 * @param slot 槽位
	 * @return 槽位被占用时返回 true
	 */
	public boolean isSet(int slot) {
		return this.getItem(slot) != null;
	}

	/**
	 * 获取槽位上的物品；槽位越界或未设置物品时返回 null
	 *
	 * @param slot
	 * @return
	 */
	public ItemStack getItem(int slot) {
		return slot < this.content.length ? this.content[slot] : null;
	}

	/**
	 * 在指定槽位设置物品
	 *
	 * @param slot
	 * @param item
	 */
	public void setItem(int slot, ItemStack item) {
		this.content[slot] = item;
	}

	/**
	 * 设置此背包界面的全部内容
	 * <p>
	 * 如果给定内容较短，所有多出的槽位都会被替换为空气
	 *
	 * @param newContent 新内容
	 */
	public void setContent(ItemStack[] newContent) {
		for (int i = 0; i < this.content.length; i++)
			this.content[i] = i < newContent.length ? newContent[i] : new ItemStack(CompMaterial.AIR.getMaterial());
	}

	/**
	 * 设置此背包绘制器的标题；若背包界面正被查看，不会更新它
	 *
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * 向玩家显示此背包界面；若已打开旧的背包界面则先关闭
	 *
	 * @param player
	 */
	public void display(Player player) {
		final Inventory inv = this.build(player);

		player.openInventory(inv);
	}

	/**
	 * 构建背包界面
	 *
	 * @return
	 */
	public Inventory build() {
		return this.build(null);
	}

	/**
	 * 为给定持有者构建背包界面
	 *
	 * @param holder
	 * @return
	 */
	public Inventory build(InventoryHolder holder) {

		// Automatically append the black color in the menu, can be overriden by colors
		final Inventory inv = Bukkit.createInventory(holder, this.size, Common.colorize("&0" + (this.title.length() > 30 ? this.title.substring(0, 30) : this.title)));

		inv.setContents(this.content);

		return inv;
	}

	/**
	 * 创建一个新的背包绘制器
	 *
	 * @param size  大小
	 * @param title 标题，颜色代码会被替换
	 * @return 背包绘制器
	 */
	public static InventoryDrawer of(int size, String title) {
		return new InventoryDrawer(size, title);
	}
}
