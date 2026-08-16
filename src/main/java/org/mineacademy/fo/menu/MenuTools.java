package org.mineacademy.fo.menu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * 标准化菜单，显示玩家可以切换获取到背包中的
 * 工具列表
 */
public abstract class MenuTools extends Menu {

	/**
	 * 工具列表
	 */
	private final List<ToggleableTool> tools;

	/**
	 * 创建一个新的工具菜单
	 */
	protected MenuTools() {
		this(null);
	}

	/**
	 * 创建一个带父菜单的新工具菜单
	 *
	 * @param parent
	 */
	protected MenuTools(final Menu parent) {
		super(parent);

		this.tools = this.compile0(this.compileTools());

		final int items = this.tools.size();
		final int pages = items < 9 ? 9 * 1 : items < 9 * 2 ? 9 * 2 : items < 9 * 3 ? 9 * 3 : items < 9 * 4 ? 9 * 4 : 9 * 5;

		this.setSize(pages);
		this.setTitle(SimpleLocalization.Menu.TITLE_TOOLS);
	}

	/**
	 * 尝试自动编译一组工具。接受一个包含
	 * {@link Button}、{@link ItemStack} 的数组，或填 0 表示空气。
	 *
	 * @return 此菜单中的物品数组
	 */
	protected abstract Object[] compileTools();

	/**
	 * 可在 {@link #compileTools()} 方法中直接使用的辅助方法，
	 * 它会自动扫描插件中所有继承给定类的类，
	 * 并返回其中包含以下字段的类：
	 * <p>
	 * public static Tool instance = new X()（X = 该类）
	 *
	 * @param extendingClass
	 * @return
	 */
	protected Object[] lookupTools(final Class<? extends Tool> extendingClass) {
		final List<Object> instances = new ArrayList<>();

		for (final Class<?> clazz : ReflectionUtil.getClasses(SimplePlugin.getInstance(), extendingClass))
			try {
				final Object instance = ReflectionUtil.getFieldContent(clazz, "instance", null);

				instances.add(instance);

			} catch (final Throwable ex) {
				// continue, unsupported tool. It must have an "instance" static
				// field with its instance
			}

		return instances.toArray();
	}

	// Compiles the given tools from makeTools()
	private final List<ToggleableTool> compile0(final Object... tools) {
		final List<ToggleableTool> list = new ArrayList<>();

		if (tools != null)
			for (final Object tool : tools)
				list.add(new ToggleableTool(tool));

		return list;
	}

	/**
	 * 返回 compileTools() 中每个槽位
	 * 对应位置上的工具
	 *
	 * @param slot 槽位
	 * @return 工具，或 null
	 */
	@Override
	public final ItemStack getItemAt(final int slot) {
		return slot < this.tools.size() ? this.tools.get(slot).get(this.getViewer()) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onMenuClick(final Player pl, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack item, final boolean cancelled) {
		final ItemStack it = this.getItemAt(slot);
		final ToggleableTool tool = it != null ? this.findTool(it) : null;

		if (tool != null) {
			tool.giveOrTake(pl);

			this.restartMenu();
		}
	}

	// Converts the clicked item into a toggleable tool
	private final ToggleableTool findTool(final ItemStack item) {
		for (final ToggleableTool h : this.tools)
			if (h.equals(item))
				return h;

		return null;
	}

	@Override
	protected int getInfoButtonPosition() {
		return this.getSize() - 1;
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return null;
	}

	/**
	 * 编译一个自动化工具菜单并向玩家显示。
	 *
	 * @param player
	 * @param pluginToolClasses 我们会在你的插件中扫描此类，
	 *                          所有继承它的类都会被加载到
	 *                          菜单中
	 * @param description       菜单描述
	 */
	public static final void display(final Player player, final Class<? extends Tool> pluginToolClasses, final String... description) {
		of(pluginToolClasses, description).displayTo(player);
	}

	/**
	 * 编译一个自动化工具菜单。
	 *
	 * @param pluginToolClasses 我们会在你的插件中扫描此类，
	 *                          所有继承它的类都会被加载到
	 *                          菜单中
	 * @param description       菜单描述
	 * @return
	 */
	public static final MenuTools of(final Class<? extends Tool> pluginToolClasses, final String... description) {
		return new MenuTools() {

			@Override
			protected Object[] compileTools() {
				return this.lookupTools(pluginToolClasses);
			}

			@Override
			protected String[] getInfo() {
				return description;
			}
		};
	}
}

/**
 * 表示一个可“切换”的工具，即玩家背包中最多只能有 1 个
 * 此工具，点击时会被收回或给予。
 */
final class ToggleableTool {

	/**
	 * 物品表示
	 */
	private final ItemStack item;

	/**
	 * 内部标记，表示自上次检查以来玩家是否拥有该工具
	 */
	private boolean playerHasTool = false;

	/**
	 * 创建一个新工具
	 *
	 * @param unparsed 要解析的对象，参见 {@link MenuTools#compileTools()}
	 */
	ToggleableTool(final Object unparsed) {

		if (unparsed != null) {
			if (unparsed instanceof ItemStack)
				this.item = (ItemStack) unparsed;

			else if (unparsed instanceof Tool)
				this.item = ((Tool) unparsed).getItem();

			else if (unparsed instanceof Number && ((Number) unparsed).intValue() == 0)
				this.item = new ItemStack(Material.AIR);

			else if (unparsed instanceof Class && Tool.class.isAssignableFrom((Class<?>) unparsed)) {
				final Method getInstance = ReflectionUtil.getMethod((Class<?>) unparsed, "getInstance");
				Valid.checkNotNull(getInstance, "Class " + unparsed + " must have a public static method getInstance() returning a Tool");

				this.item = ((Tool) ReflectionUtil.invokeStatic(getInstance)).getItem();

			} else
				throw new FoException("Unknown tool: " + unparsed + " (we only accept ItemStack, Tool's instance or 0 for air)");

		} else
			this.item = new ItemStack(Material.AIR);
	}

	/**
	 * 自动返回物品堆，根据玩家是否已拥有该工具
	 * 返回不同的物品
	 *
	 * @param player
	 * @return 物品
	 */
	ItemStack get(final Player player) {
		this.update(player);

		return this.playerHasTool ? this.getToolWhenHas() : this.getToolWhenHasnt();
	}

	private void update(final Player player) {
		this.playerHasTool = Remain.getBottomInventoryFromOpenInventory(player).containsAtLeast(this.item, 1);
	}

	// Return the dummy placeholder tool when the player already has it
	private ItemStack getToolWhenHas() {
		return ItemCreator
				.of(this.item)
				.glow(true)
				.lore("", "&6You already have this item.", "&6Click to take it away.")
				.makeMenuTool();
	}

	// Return the actual working tool in case player does not have it yet
	private ItemStack getToolWhenHasnt() {
		return this.item;
	}

	/**
	 * 根据 {@link #playerHasTool} 为玩家给予或收回该工具
	 *
	 * @param player 玩家
	 */
	void giveOrTake(final Player player) {
		final PlayerInventory inv = player.getInventory();

		if (this.playerHasTool = !this.playerHasTool)
			inv.addItem(this.item);

		else
			inv.removeItem(this.item);
	}

	boolean equals(final ItemStack item) {
		return this.getToolWhenHas().isSimilar(item) || this.getToolWhenHasnt().isSimilar(item);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Toggleable{" + this.item.getType() + "}";
	}
}
