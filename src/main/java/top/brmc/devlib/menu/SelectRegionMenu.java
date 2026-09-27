package top.brmc.devlib.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.Messenger;
import top.brmc.devlib.conversation.CreateRegionPrompt;
import top.brmc.devlib.menu.button.Button;
import top.brmc.devlib.menu.button.StartPosition;
import top.brmc.devlib.menu.button.annotation.Position;
import top.brmc.devlib.menu.model.InventoryDrawer;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.region.DiskRegion;
import top.brmc.devlib.remain.CompColor;
import top.brmc.devlib.remain.CompMaterial;

/**
 * 玩家可以选择或创建区域的菜单。
 */
public class SelectRegionMenu extends MenuPagged<String> {

	@Position(start = StartPosition.BOTTOM_LEFT)
	private final Button createButton;

	private int colorMask = CompColor.values().length;

	public SelectRegionMenu(Menu parent) {
		super(parent, DiskRegion.getRegionNames());

		this.setTitle("Create Or Edit Regions");

		this.createButton = Button.makeSimple(ItemCreator.of(CompMaterial.EMERALD,
				"&aCreate New",
				"",
				"Click to create",
				"a new region."), player -> {
					if (SimplePlugin.getInstance().areToolsEnabled())
						CreateRegionPrompt.showToOrHint(player);
					else {
						player.closeInventory();

						Messenger.error(player, "Enable Register_Tools in settings.yml before creating regions!");
					}
				});
	}

	/**
	 * @see top.brmc.devlib.menu.MenuPagged#onPostDisplay(top.brmc.devlib.menu.model.InventoryDrawer)
	 */
	@Override
	protected void onPostDisplay(InventoryDrawer drawer) {
		this.colorMask = 0;
	}

	@Override
	protected ItemStack convertToItemStack(String regionName) {
		return ItemCreator.of(CompMaterial.WHITE_STAINED_GLASS,
				"Region " + regionName,
				"",
				"Click to open the region",
				"menu and customize it.")
				.color(CompColor.values()[this.colorMask++ % CompColor.values().length])
				.make();
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Select a region to open its",
				"menu and customize it.",
		};
	}

	@Override
	public Menu newInstance() {
		return new SelectRegionMenu(this.getParent());
	}

	@Override
	protected void onPageClick(Player player, String regionName, ClickType click) {
		RegionMenu.showTo(player, DiskRegion.findRegion(regionName));
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	public static Menu create(Menu parent) {
		return new SelectRegionMenu(parent);
	}

	public static Menu create() {
		return new SelectRegionMenu(null);
	}
}