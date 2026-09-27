package top.brmc.devlib.menu.tool;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.Messenger;
import top.brmc.devlib.conversation.CreateRegionPrompt;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.region.DiskRegion;
import top.brmc.devlib.region.Region;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.settings.SimpleLocalization;
import top.brmc.devlib.visual.VisualTool;
import top.brmc.devlib.visual.VisualizedRegion;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 表示用于为任意竞技场创建竞技场区域的工具
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegionTool extends VisualTool {

	/**
	 * 唯一的工具实例
	 */
	@Getter
	private static final Tool instance = new RegionTool();

	/**
	 * 手持工具时，显示在主/次方块上方的区域点
	 */
	@Getter
	@Setter
	private static String blockName = "&f[&aRegion point&f]";

	/**
	 * 可自定义的方块遮罩
	 */
	@Getter
	@Setter
	private static CompMaterial blockMask = CompMaterial.EMERALD_BLOCK;

	/**
	 * 可自定义的物品材质
	 */
	@Getter
	@Setter
	private static CompMaterial itemMaterial = CompMaterial.EMERALD;

	/**
	 * 可自定义的物品名称
	 */
	@Getter
	@Setter
	private static String itemName = "Region Tool";

	@Getter
	@Setter
	private static String[] lore = {
			"",
			"Use this tool to create",
			"and edit regions.",
			"",
			"&4&l< &7Left click &7– &7Primary",
			"&4&l> &7Right click &7– &7Secondary"
	};

	/**
	 * 实际的物品
	 */
	private ItemStack item;

	/**
	 * @see top.brmc.devlib.visual.VisualTool#getBlockName(org.bukkit.block.Block, org.bukkit.entity.Player)
	 */
	@Override
	protected String getBlockName(final Block block, final Player player) {
		return blockName;
	}

	/**
	 * @see top.brmc.devlib.visual.VisualTool#getBlockMask(org.bukkit.block.Block, org.bukkit.entity.Player)
	 */
	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player) {
		return blockMask;
	}

	/**
	 * @see top.brmc.devlib.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		if (this.item == null)
			this.item = ItemCreator.of(itemMaterial).name(itemName).lore(lore).make();

		return this.item;
	}

	/**
	 * @see top.brmc.devlib.visual.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final ClickType click, final Block block) {
		final Location location = block.getLocation();
		final boolean primary = click == ClickType.LEFT;
		final Region region = DiskRegion.getCreatedRegion(player);

		if (primary)
			region.setPrimary(location);
		else
			region.setSecondary(location);

		final boolean whole = region.isWhole();

		if (whole && !player.isConversing())
			CreateRegionPrompt.showToOrHint(player);
		else
			Messenger.success(player, primary ? SimpleLocalization.Commands.REGION_SET_PRIMARY : SimpleLocalization.Commands.REGION_SET_SECONDARY);
	}

	/**
	 * @see top.brmc.devlib.visual.VisualTool#getVisualizedPoints(org.bukkit.entity.Player)
	 */
	@Override
	protected List<Location> getVisualizedPoints(Player player) {
		final List<Location> blocks = new ArrayList<>();
		final Region region = DiskRegion.getCreatedRegion(player);

		if (region.getPrimary() != null)
			blocks.add(region.getPrimary());

		if (region.getSecondary() != null)
			blocks.add(region.getSecondary());

		return blocks;
	}

	/**
	 * @see top.brmc.devlib.visual.VisualTool#getVisualizedRegion(org.bukkit.entity.Player)
	 */
	@Override
	protected VisualizedRegion getVisualizedRegion(Player player) {
		final VisualizedRegion region = DiskRegion.getCreatedRegion(player);

		return region.isWhole() ? region : null;
	}

	/**
	 * 取消事件，以免选择方块时把方块破坏掉
	 *
	 * @see top.brmc.devlib.menu.tool.Tool#autoCancel()
	 */
	@Override
	protected boolean autoCancel() {
		return true;
	}

	/**
	 * @see top.brmc.devlib.menu.tool.BlockTool#ignoreCancelled()
	 */
	@Override
	protected boolean ignoreCancelled() {
		return true;
	}
}
