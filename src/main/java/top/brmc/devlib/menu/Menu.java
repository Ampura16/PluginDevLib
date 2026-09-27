package top.brmc.devlib.menu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import top.brmc.devlib.Common;
import top.brmc.devlib.ItemUtil;
import top.brmc.devlib.Messenger;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.PlayerUtil;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.constants.FoConstants;
import top.brmc.devlib.event.MenuCloseEvent;
import top.brmc.devlib.event.MenuOpenEvent;
import top.brmc.devlib.exception.EventHandledException;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.menu.button.Button;
import top.brmc.devlib.menu.button.Button.DummyButton;
import top.brmc.devlib.menu.button.ButtonReturnBack;
import top.brmc.devlib.menu.button.StartPosition;
import top.brmc.devlib.menu.button.annotation.Position;
import top.brmc.devlib.menu.model.InventoryDrawer;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.menu.model.MenuClickLocation;
import top.brmc.devlib.model.SimpleRunnable;
import top.brmc.devlib.model.SimpleSound;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.remain.CompSound;
import top.brmc.devlib.remain.Remain;
import top.brmc.devlib.settings.SimpleLocalization;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Menu 的核心类，表示一个简单菜单。
 *
 * <p>
 * 这是所有带菜单的插件推荐使用的菜单类。它支持
 * 父菜单、返回按钮，以及向用户说明菜单用途的
 * 信息按钮。
 *
 * <p>
 * 入门方法：在你的菜单类中放置 final 的 {@link Button} 字段，并
 * 在构造器中实例化它们。它们会被自动注册为可点击按钮。
 * 要渲染它们，请重写 {@link #getItemAt(int)}，让它们
 * 在你期望的位置返回。
 */
public abstract class Menu {

	// --------------------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------------------

	/**
	 * 在菜单之间切换时的默认音效。设为 null 可禁用
	 */
	@Getter
	@Setter
	@Nullable
	private static SimpleSound sound = new SimpleSound(CompSound.BLOCK_NOTE_BLOCK_HAT.getSound(), .4F);

	/**
	 * 是否为菜单标题添加动画？
	 */
	@Getter
	@Setter
	private static boolean titleAnimationEnabled = true;

	/**
	 * 新的动画标题在恢复为旧标题之前
	 * 的默认持续时间
	 * <p>
	 * 在 {@link #updateInventoryTitle(Menu, Player, String, String)} 中使用
	 */
	@Getter
	@Setter
	private static int titleAnimationDurationTicks = 20;

	/**
	 * 占位符，表示不应显示/返回任何物品
	 */
	protected static final ItemStack NO_ITEM = null;

	// --------------------------------------------------------------------------------
	// Actual class
	// --------------------------------------------------------------------------------

	/**
	 * 此菜单中自动注册的按钮（通过反射）
	 */
	private final Map<Button, Position> registeredButtons = new HashMap<>();
	private final Map<Integer, Button> registeredButtonPositions = new HashMap<>();

	/**
	 * 手动注册的按钮列表，适用于你不想将它们存储为字段的情况。
	 */
	private final List<Button> buttons = new ArrayList<>();

	/**
	 * 负责扫描类并使按钮生效的
	 * 注册器
	 */
	private boolean buttonsRegistered = false;

	/**
	 * 父菜单
	 */
	private final Menu parent;

	/**
	 * 返回上一菜单的按钮，没有则为 null
	 */
	private final Button returnButton;

	// --------------------------------------------------------------------------------
	// Other constructors
	// --------------------------------------------------------------------------------

	/**
	 * 菜单的背包标题，支持 & 颜色代码
	 */
	private String title = "&0Menu";

	/**
	 * 菜单的大小
	 */
	private Integer size = 9 * 3;

	/**
	 * 此菜单的查看者，在调用 {@link #displayTo(Player)} 之前为 null
	 */
	private Player viewer;

	/**
	 * 调试选项：将空位渲染为可见槽位 id 的玻璃板
	 */
	private boolean slotNumbersVisible;

	/**
	 * 单向布尔值，表示此菜单至少已打开过一次
	 */
	private boolean opened = false;

	/**
	 * 是否允许在菜单物品上 Shift 点击。
	 * 默认返回 false。
	 */
	@Getter
	@Setter
	private boolean allowShift = false;
	/**
	 * 特殊按钮，仅当此菜单为 {@link MenuQuantitable} 时注册
	 */
	@Nullable
	private final Button quantityButton;

	/**
	 * 创建一个没有父菜单、大小为 9*3 的新菜单
	 *
	 * <p>
	 * 建议你在构造器中通过调用 {@link #setTitle(String)} 和
	 * {@link #setSize(Integer)} 修改此菜单的大小和标题
	 *
	 * <p>
	 * 注意：此菜单的 {@link #getViewer()} 此时仍为 null！
	 */
	protected Menu() {
		this(null);
	}

	/**
	 * 创建一个带父菜单、大小为 9*3 的新菜单
	 *
	 * <p>
	 * 建议你在构造器中通过调用 {@link #setTitle(String)} 和
	 * {@link #setSize(Integer)} 修改此菜单的大小和标题
	 *
	 * <p>
	 * 注意：此菜单的 {@link #getViewer()} 此时仍为 null！
	 *
	 * @param parent 父菜单
	 */
	protected Menu(final Menu parent) {
		this(parent, false);
	}

	/**
	 * 创建一个带父菜单、大小为 9*3 的新菜单
	 *
	 * <p>
	 * 建议你在构造器中通过调用 {@link #setTitle(String)} 和
	 * {@link #setSize(Integer)} 修改此菜单的大小和标题
	 *
	 * <p>
	 * 注意：此菜单的 {@link #getViewer()} 此时仍为 null！
	 *
	 * @param parent                 父菜单
	 * @param returnMakesNewInstance 返回父菜单时是否重新实例化
	 *                               父菜单？
	 */
	protected Menu(final Menu parent, final boolean returnMakesNewInstance) {
		this.parent = parent;
		this.returnButton = parent != null ? new ButtonReturnBack(parent, returnMakesNewInstance) : Button.makeEmpty();
		this.quantityButton = this instanceof MenuQuantitable ? ((MenuQuantitable) this).getQuantityButton(this) : Button.makeEmpty();
	}

	/**
	 * 返回玩家当前的菜单
	 *
	 * @param player 玩家
	 * @return 菜单，没有则为 null
	 */
	public static final Menu getMenu(final Player player) {
		return getMenu0(player, FoConstants.NBT.TAG_MENU_CURRENT);
	}

	/**
	 * 返回玩家的上一个菜单
	 *
	 * @param player 玩家
	 * @return 菜单，或无
	 */
	public static final Menu getPreviousMenu(final Player player) {
		return getMenu0(player, FoConstants.NBT.TAG_MENU_PREVIOUS);
	}

	/**
	 * 返回最后关闭的菜单，不存在则为 null。
	 *
	 * @param player
	 * @return
	 */
	@Nullable
	public static final Menu getLastClosedMenu(final Player player) {
		if (player.hasMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED)) {
			final Menu menu = (Menu) player.getMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED).get(0).value();

			return menu;
		}

		return null;
	}

	// Returns the menu associated with the players metadata, or null
	private static Menu getMenu0(final Player player, final String tag) {
		if (player.hasMetadata(tag)) {
			final Menu menu = (Menu) player.getMetadata(tag).get(0).value();
			Valid.checkNotNull(menu, "Menu missing from " + player.getName() + "'s metadata '" + tag + "' tag!");

			return menu;
		}

		return null;
	}

	// --------------------------------------------------------------------------------
	// Reflection to make life easier
	// --------------------------------------------------------------------------------

	/**
	 * 手动向此菜单注册按钮，按钮无需是字段。
	 *
	 * 不要用于作为字段的按钮，字段按钮会自动注册
	 *
	 * @param button
	 */
	protected final void registerButton(final Button button) {
		Valid.checkBoolean(button.getSlot() != -1, "When calling registerButton, you must set the slot of the button either in the constructor or by overriding Button#getSlot()!");

		this.buttons.add(button);
	}

	/**
	 * 扫描此菜单所继承的菜单类并注册按钮
	 */
	final void registerButtons() {
		this.registeredButtons.clear();

		// Register buttons explicitly given
		{
			final List<Button> buttons = this.getButtonsToAutoRegister();

			if (buttons != null) {
				final Map<Button, Position> buttonsRemapped = new HashMap<>();

				for (final Button button : buttons)
					buttonsRemapped.put(button, null);

				this.registeredButtons.putAll(buttonsRemapped);
			}
		}

		// Register buttons from the list
		{
			for (final Button button : this.buttons)
				this.registeredButtons.put(button, null);
		}

		// Register buttons declared as fields
		{
			Class<?> lookup = this.getClass();

			do
				for (final Field f : lookup.getDeclaredFields())
					this.registerButton0(f);
			while (Menu.class.isAssignableFrom(lookup = lookup.getSuperclass()));
		}
	}

	// Scans the class and register fields that extend Button class
	private void registerButton0(final Field field) {
		field.setAccessible(true);

		final Class<?> type = field.getType();

		if (Button.class.isAssignableFrom(type)) {
			final Button button = (Button) ReflectionUtil.getFieldContent(field, this);

			Valid.checkNotNull(button, "Null button field named " + field.getName() + " in " + this);
			final Position position = field.getAnnotation(Position.class);

			this.registeredButtons.put(button, position);

		} else if (Button[].class.isAssignableFrom(type))
			throw new FoException("Button[] is no longer supported in menu for " + this.getClass());
	}

	/*
	 * Utility method to register buttons if they yet have not been registered
	 *
	 * This method will only register them once until the server is reset
	 */
	private final void registerButtonsIfHasnt() {
		if (!this.buttonsRegistered) {
			this.registerButtons();

			this.buttonsRegistered = true;
		}
	}

	/**
	 * 返回应手动注册的按钮列表。
	 *
	 * 注意：你类中的按钮字段会自动注册，不要将它们
	 * 添加到这里
	 *
	 * @return 按钮列表，默认为 null
	 */
	protected List<Button> getButtonsToAutoRegister() {
		return null;
	}

	/**
	 * 尝试查找与给定物品堆图标相同的按钮。
	 *
	 * @param fromItem 要比较的物品堆
	 * @return 按钮，未找到则为 null
	 *
	 * @deprecated 请改用 Position 注解或 Button#getSlot，因为按物品比较时，若按钮与某个生存模式物品
	 *             具有相同的物品和元数据，可能会把该物品当作按钮返回
	 */
	@Deprecated
	@Nullable
	protected final Button getButton(final ItemStack fromItem) {
		this.registerButtonsIfHasnt();

		for (final Map.Entry<Button, Position> entry : this.registeredButtons.entrySet()) {
			final Button button = entry.getKey();
			final Position position = entry.getValue();

			Valid.checkNotNull(button, "Menu button is null at " + this.getClass().getSimpleName());

			if (position == null && button.getSlot() == -1 && ItemUtil.isSimilar(fromItem, button.getItem()))
				return button;
		}

		return null;
	}

	/**
	 * 根据 {@link Position} 注解或 {@link Button#getSlot()} 返回某个槽位上的按钮
	 *
	 * @param slot
	 * @return
	 */
	@Nullable
	protected final Button getButton(final int slot) {
		this.registerButtonsIfHasnt();

		// Cannot put Button#getSlot into registeredButtonPositions because it can be dynamically set each time the menu is opened
		for (final Button button : this.registeredButtons.keySet()) {
			Valid.checkNotNull(button, "Menu button is null at " + this.getClass().getSimpleName());

			if (button.getSlot() != -1 && button.getSlot() == slot)
				return button;
		}

		return this.registeredButtonPositions.get(slot);
	}

	/**
	 * 返回此菜单的新实例
	 *
	 * <p>
	 * 某些情况下你必须重写此方法
	 *
	 * @return 新实例，或 null
	 * @throws FoException 若无法创建新实例，例如菜单的构造器
	 *            需要参数时
	 */
	public Menu newInstance() {
		try {
			return ReflectionUtil.instantiate(this.getClass());
		} catch (final Throwable t) {
			try {
				final Object parent = this.getClass().getMethod("getParent").invoke(this.getClass());

				if (parent != null)
					return ReflectionUtil.instantiate(this.getClass(), parent);
			} catch (final Throwable tt) {
			}

			t.printStackTrace();
		}

		throw new FoException(this.getClass().getSimpleName() + " lacks newInstance() method! Store your constructor parameters as fields, "
				+ "override the method and return a new instance using fields as paramteres here. Example: https://i.imgur.com/5mqJ2nD.png");
	}

	// --------------------------------------------------------------------------------
	// Rendering the menu
	// --------------------------------------------------------------------------------

	/**
	 * 向玩家显示此菜单
	 *
	 * @param player 玩家
	 */
	public final void displayTo(final Player player) {
		Valid.checkNotNull(this.size, "Size not set in " + this + " (call setSize in your constructor)");
		Valid.checkNotNull(this.title, "Title not set in " + this + " (call setTitle in your constructor)");

		if (MinecraftVersion.olderThan(V.v1_5)) {
			final String error = "Displaying menus require Minecraft 1.5.2 or greater.";

			if (Messenger.ENABLED)
				Messenger.error(player, error);
			else
				Common.tell(player, error);

			return;
		}

		this.viewer = player;
		this.registerButtonsIfHasnt();

		// Draw the menu
		final InventoryDrawer drawer = InventoryDrawer.of(this.size, this.title);

		// Allocate items
		this.compileItems().forEach((slot, item) -> drawer.setItem(slot, item));

		// Allow last minute modifications
		this.onPreDisplay(drawer);

		// Render empty slots as slot numbers if enabled
		this.debugSlotNumbers(drawer);

		// Call event after items have been set to allow to get them
		if (!Common.callEvent(new MenuOpenEvent(this, drawer, player)))
			return;

		// Prevent menu in conversation
		if (player.isConversing()) {
			player.sendRawMessage(Common.colorize(SimpleLocalization.Menu.CANNOT_OPEN_DURING_CONVERSATION));

			return;
		}

		// Play the pop sound
		if (sound != null)
			sound.play(player);

		// Register previous menu if exists
		{
			final Menu previous = getMenu(player);

			if (previous != null)
				player.setMetadata(FoConstants.NBT.TAG_MENU_PREVIOUS, new FixedMetadataValue(SimplePlugin.getInstance(), previous));
		}

		// Register current menu
		Common.runLater(1, () -> {
			try {
				this.onDisplay(drawer, player);

			} catch (final Throwable t) {
				Common.error(t, "Error opening menu " + Menu.this);

				return;
			}

			player.setMetadata(FoConstants.NBT.TAG_MENU_CURRENT, new FixedMetadataValue(SimplePlugin.getInstance(), Menu.this));

			this.opened = true;
			this.onPostDisplay(player);
		});
	}

	/**
	 * 将所有空槽位设为浅灰色玻璃板；若 {@link #slotNumbersVisible} 为 true，
	 * 则在已有物品的描述中添加槽位编号
	 *
	 * @param drawer
	 */
	private void debugSlotNumbers(final InventoryDrawer drawer) {
		if (this.slotNumbersVisible)
			for (int slot = 0; slot < drawer.getSize(); slot++) {
				final ItemStack item = drawer.getItem(slot);

				if (item == null)
					drawer.setItem(slot, ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, "Slot " + slot).make());
			}
	}

	/**
	 * 在菜单显示之前、所有物品都已绘制之后
	 * 自动调用
	 *
	 * <p>
	 * 可重写以进行最后时刻的自定义修改
	 *
	 * @param drawer 绘制器
	 */
	protected void onPreDisplay(final InventoryDrawer drawer) {
	}

	/**
	 * 在菜单向玩家显示时调用，默认从背包绘制器
	 * 显示菜单
	 *
	 * @param drawer
	 * @param player
	 */
	protected void onDisplay(final InventoryDrawer drawer, final Player player) {
		drawer.display(player);
	}

	/**
	 * 在菜单向查看者显示后自动调用
	 *
	 * @param viewer
	 */
	protected void onPostDisplay(final Player viewer) {
	}

	/**
	 * 重新绘制并刷新所有按钮
	 */
	public final void restartMenu() {
		this.restartMenu(null);
	}

	/**
	 * 重新绘制并重新注册所有按钮，同时向玩家发送
	 * 标题动画
	 *
	 * @param animatedTitle 动画标题
	 */
	public final void restartMenu(final String animatedTitle) {
		this.restartMenu(animatedTitle, true);
	}

	final void restartMenu(final String animatedTitle, final boolean callOnMenuClose) {

		final Player player = this.getViewer();
		Valid.checkNotNull(player, "Cannot restartMenu if it was not yet shown to a player! Menu: " + this);

		final Inventory inventory = Remain.getTopInventoryFromOpenInventory(player);
		Valid.checkBoolean(inventory.getType() == InventoryType.CHEST, player.getName() + "'s inventory closed in the meanwhile (now == " + inventory.getType() + ").");

		// Most plugins save items here
		if (callOnMenuClose)
			this.onMenuClose(player, inventory);

		this.registerButtons();

		// Call before calling getItemAt
		this.onRestartInternal();
		this.onRestart();

		final ItemStack[] content = inventory.getContents();
		final Map<Integer, ItemStack> newContent = this.compileItems();

		for (int i = 0; i < content.length; i++)
			content[i] = newContent.get(i);

		inventory.setContents(content);

		if (animatedTitle != null)
			this.animateTitle(animatedTitle);
	}

	/**
	 * 重新绘制通过 {@link Position} 注解注册或设置了 {@link Button#getSlot()} 的按钮
	 */
	public void redrawButtons() {

		// Redraw positions
		for (final Map.Entry<Integer, Button> entry : this.registeredButtonPositions.entrySet()) {
			final int slot = entry.getKey();
			final Button button = entry.getValue();

			this.setItem(slot, button.getItem());
		}

		// Redraw slots
		for (final Button button : this.registeredButtons.keySet())
			if (button.getSlot() != -1)
				this.setItem(button.getSlot(), button.getItem());
	}

	/*
	 * Internal hook before calling getItemAt
	 */
	void onRestartInternal() {
	}

	/**
	 * 菜单重启时自动调用。在 getItemAt() 之前、registerButtons() 之后调用
	 */
	public void onRestart() {
	}

	/**
	 * 是否允许在菜单物品上 Shift 点击。
	 * 默认返回 false。与
	 * {@link #isAllowShift()} 方法相比，此方法
	 * 可以只允许在特定槽位点击。
	 *
	 * @param slot 玩家点击的槽位。
	 * @return 若允许点击则为 true。
	 */
	public boolean isAllowShift(int slot) {
		return false;
	}

	/**
	 * 为玩家背包绘制底部栏
	 *
	 * @return
	 */
	private Map<Integer, ItemStack> compileItems() {
		this.registeredButtonPositions.clear();
		final Map<Integer, ItemStack> items = new HashMap<>();

		final boolean hasReturnButton = this.addReturnButton() && !(this.returnButton instanceof DummyButton);

		// Begin with basic items
		for (int slot = 0; slot < this.size; slot++) {
			ItemStack item = this.getItemAt(slot);

			if (item != null && CompMaterial.isAir(item))
				item = null;

			items.put(slot, item);
		}

		// Override by buttons
		for (final Map.Entry<Button, Position> entry : this.registeredButtons.entrySet()) {
			final Button button = entry.getKey();
			final Position position = entry.getValue();

			if (button.getSlot() != -1) {
				items.put(button.getSlot(), button.getItem());

			} else if (position != null) {
				int slot = position.value();
				final StartPosition startPosition = position.start();

				if (startPosition == StartPosition.CENTER)
					slot += this.getCenterSlot();

				else if (startPosition == StartPosition.BOTTOM_CENTER)
					slot += this.getSize() - 5;

				else if (startPosition == StartPosition.BOTTOM_LEFT)
					slot += this.getSize() - (hasReturnButton ? 2 : 1);

				else if (startPosition == StartPosition.TOP_LEFT)
					slot += 0;
				else
					throw new FoException("Does not know how to implement button position's Slot." + startPosition);

				this.registeredButtonPositions.put(slot, button);
				items.put(slot, button.getItem());
			}
		}

		// Add quantity edit button
		if (this instanceof MenuQuantitable) {
			final int slot = ((MenuQuantitable) this).getQuantityButtonPosition();

			if (slot != -1)
				items.put(slot, this.quantityButton.getItem());
		}

		// Override by hotbar
		{
			if (this.addInfoButton() && this.getInfo() != null)
				items.put(this.getInfoButtonPosition(), Button.makeInfo(this.getInfo()).getItem());

			if (hasReturnButton)
				items.put(this.getReturnButtonPosition(), this.returnButton.getItem());
		}

		return items;

	}

	// --------------------------------------------------------------------------------
	// Convenience messenger functions
	// --------------------------------------------------------------------------------

	/**
	 * 向查看者发送消息
	 *
	 * @param messages
	 */
	public final void tell(final String... messages) {
		Common.tell(this.viewer, messages);
	}

	/**
	 * 向查看者发送消息
	 *
	 * @param message
	 */
	public final void tellInfo(final String message) {
		Messenger.info(this.viewer, message);
	}

	/**
	 * 向查看者发送消息
	 *
	 * @param message
	 */
	public final void tellSuccess(final String message) {
		Messenger.success(this.viewer, message);
	}

	/**
	 * 向查看者发送消息
	 *
	 * @param message
	 */
	public final void tellWarn(final String message) {
		Messenger.warn(this.viewer, message);
	}

	/**
	 * 向查看者发送消息
	 *
	 * @param message
	 */
	public final void tellError(final String message) {
		Messenger.error(this.viewer, message);
	}

	/**
	 * 向查看者发送消息
	 *
	 * @param message
	 */
	public final void tellQuestion(final String message) {
		Messenger.question(this.viewer, message);
	}

	/**
	 * 向查看者发送消息
	 *
	 * @param message
	 */
	public final void tellAnnounce(final String message) {
		Messenger.announce(this.viewer, message);
	}

	// --------------------------------------------------------------------------------
	// Animations
	// --------------------------------------------------------------------------------

	/**
	 * 为此菜单的标题添加动画
	 *
	 * <p>
	 * 1 秒后自动恢复为旧标题
	 *
	 * @param title 要显示动画的标题
	 */
	public void animateTitle(final String title) {
		if (titleAnimationEnabled)
			PlayerUtil.updateInventoryTitle(this, this.getViewer(), title, this.getTitle(), titleAnimationDurationTicks);
	}

	/**
	 * 在主线程上启动一个以给定刻数为周期的重复任务，
	 * 当查看者不再看到此菜单时会自动停止。
	 *
	 * 可能带来性能开销。使用 cancel() 取消。
	 *
	 * 重要提示：
	 *
	 * 1. 要更新按钮，请通过 {@link Position} 或 {@link Button#getSlot()} 设置其槽位，然后调用 {@link #redrawButtons()}。
	 * 2. 要更高效地为物品添加动画，请在你的插件中创建一个实现 Runnable 的新类，遍历所有
	 *    玩家并对每个玩家调用 {@link Menu#getMenu(Player)}。然后检查该菜单是否为你的菜单实例，
	 *    在该菜单类中编写 onUpdate() 方法，并改为在你的 runnable 中调用它。
	 *
	 * 带动画按钮的菜单示例：https://i.imgur.com/z1VZDcw.png
	 *
	 * @param periodTicks
	 * @param task
	 */
	protected final void animate(final int periodTicks, final MenuRunnable task) {
		Valid.checkNotNull(this.viewer, "Cannot call animate() before the menu is shown, call your method in onDisplay() method instead.");

		Common.runTimer(2, periodTicks, this.wrapAnimation(task));
	}

	/**
	 * 异步启动一个以给定刻数为周期的重复任务，
	 * 当查看者不再看到此菜单时会自动停止。
	 *
	 * 使用 cancel() 取消。
	 *
	 * 重要提示：
	 *
	 * 1. 要更新按钮，请通过 {@link Position} 或 {@link Button#getSlot()} 设置其槽位，然后调用 {@link #redrawButtons()}。
	 * 2. 要更高效地为物品添加动画，请在你的插件中创建一个实现 Runnable 的新类，遍历所有
	 *    玩家并对每个玩家调用 {@link Menu#getMenu(Player)}。然后检查该菜单是否为你的菜单实例，
	 *    在该菜单类中编写 onUpdate() 方法，并改为在你的 runnable 中调用它。
	 *
	 * 带动画按钮的菜单示例：https://i.imgur.com/z1VZDcw.png
	 *
	 * @param periodTicks
	 * @param task
	 */
	protected final void animateAsync(final int periodTicks, final MenuRunnable task) {
		Valid.checkNotNull(this.viewer, "Cannot call animate() before the menu is shown, call your method in onDisplay() method instead.");

		Common.runTimerAsync(2, periodTicks, this.wrapAnimation(task));
	}

	/*
	 * Helper method to create a bukkit runnable
	 */
	private SimpleRunnable wrapAnimation(final MenuRunnable task) {
		return new SimpleRunnable() {
			boolean canceled = false;

			@Override
			public void run() {

				if (!Menu.this.opened) {
					if (!this.canceled)
						this.cancel();

					return;
				}

				try {
					task.run();

				} catch (final EventHandledException ex) {
					this.canceled = true;

					this.cancel();
				}
			}
		};
	}

	/**
	 * 用于菜单动画的特殊包装器
	 */
	@FunctionalInterface
	public interface MenuRunnable extends Runnable {

		/**
		 * 取消菜单动画
		 */
		default void cancel() {
			throw new EventHandledException();
		}
	}

	// --------------------------------------------------------------------------------
	// Menu functions
	// --------------------------------------------------------------------------------

	/**
	 * 返回某个槽位上的物品
	 *
	 * @param slot 槽位
	 * @return 物品；若给定槽位没有图标则为 null（默认）
	 */
	public ItemStack getItemAt(final int slot) {
		return NO_ITEM;
	}

	/**
	 * 获取信息按钮的位置
	 *
	 * @return 信息按钮所在的槽位
	 */
	protected int getInfoButtonPosition() {
		return this.size - 9;
	}

	/**
	 * 是否自动在左下角添加返回按钮？
	 *
	 * @return 若应添加返回按钮则为 true，默认为 true
	 */
	protected boolean addReturnButton() {
		return true;
	}

	/**
	 * 是否自动在 {@link #getInfoButtonPosition()} 处添加
	 * 信息按钮 {@link #getInfo()}？
	 *
	 * @return
	 */
	protected boolean addInfoButton() {
		return true;
	}

	/**
	 * 获取返回按钮的位置
	 *
	 * @return 返回按钮所在的槽位
	 */
	protected int getReturnButtonPosition() {
		return this.size - 1;
	}

	/**
	 * 计算此菜单的中心槽位
	 *
	 * <p>
	 * 感谢 Gober，出处：
	 * https://www.spigotmc.org/threads/get-the-center-slot-of-a-menu.379586/
	 *
	 * @return 估算的中心槽位
	 */
	protected final int getCenterSlot() {
		final int pos = this.size / 2;

		return this.size % 2 == 1 ? pos : pos - 5;
	}

	/**
	 * 返回菜单最后一行（快捷栏）的中间槽位
	 *
	 * @return
	 */
	protected final int getBottomCenterSlot() {
		return this.size - 5;
	}

	/**
	 * 是否阻止点击或拖动？
	 *
	 * @param location 点击位置
	 * @param slot     槽位
	 * @param clicked  被点击的物品
	 * @param cursor   光标
	 * @param action   背包操作
	 *
	 * @return 该操作是否在 {@link InventoryClickEvent} 中被取消，
	 * 默认为 false
	 */
	protected boolean isActionAllowed(final MenuClickLocation location, final int slot, @Nullable final ItemStack clicked, @Nullable final ItemStack cursor, final InventoryAction action) {
		return this.isActionAllowed(location, slot, clicked, cursor);
	}

	/**
	 * 是否阻止点击或拖动？
	 *
	 * @param location 点击位置
	 * @param slot     槽位
	 * @param clicked  被点击的物品
	 * @param cursor   光标
	 * @param action   背包操作
	 *
	 * @return 该操作是否在 {@link InventoryClickEvent} 中被取消，
	 * 默认为 false
	 */
	protected boolean isActionAllowed(final MenuClickLocation location, final int slot, @Nullable final ItemStack clicked, @Nullable final ItemStack cursor) {
		return false;
	}

	/**
	 * 此菜单的标题
	 *
	 * @return 菜单标题
	 */
	public final String getTitle() {
		return this.title;
	}

	/**
	 * 设置此背包的标题；若此菜单已向某玩家显示，
	 * 该修改会立即体现。
	 *
	 * @param title 新标题
	 */
	protected final void setTitle(final String title) {
		this.title = title;

		if (this.viewer != null && this.opened)
			PlayerUtil.updateInventoryTitle(this.viewer, title);
	}

	/**
	 * 返回父菜单，或 null
	 *
	 * @return
	 */
	public final Menu getParent() {
		return this.parent;
	}

	/**
	 * 获取此菜单的大小
	 *
	 * @return
	 */
	public final Integer getSize() {
		return this.size;
	}

	/**
	 * 设置此菜单的大小（不会更新玩家容器——如果你
	 * 想更新它，请调用 {@link #restartMenu()}）
	 *
	 * @param size
	 */
	protected final void setSize(final Integer size) {
		this.size = size;
	}

	/**
	 * 设置菜单的描述
	 *
	 * <p>
	 * 用于在左下角创建信息按钮，参见
	 * {@link Button#makeInfo(String...)}
	 *
	 * return info 要设置的信息
	 */
	protected String[] getInfo() {
		return null;
	}

	/**
	 * 获取与此菜单实例关联的查看者
	 *
	 * @return 此实例的查看者，或 null
	 */
	protected final Player getViewer() {
		return this.viewer;
	}

	/**
	 * 设置此菜单实例的查看者
	 *
	 * @param viewer
	 */
	protected final void setViewer(@NonNull final Player viewer) {
		this.viewer = viewer;
	}

	/**
	 * 如果查看者存在，返回其顶部打开的背包
	 *
	 * @return
	 */
	protected final Inventory getInventory() {
		Valid.checkNotNull(this.viewer, "Cannot get inventory when there is no viewer!");

		final Inventory topInventory = Remain.getTopInventoryFromOpenInventory(this.viewer);
		Valid.checkNotNull(topInventory, "Top inventory is null!");

		return topInventory;
	}

	/**
	 * 获取与数组长度匹配的已打开背包内容，并克隆物品
	 * 以防止 yaml 文件中的 ID 不匹配
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final ItemStack[] getContent(final int from, final int to) {
		final ItemStack[] content = this.getInventory().getContents();
		final ItemStack[] copy = new ItemStack[content.length];

		for (int i = from; i < copy.length; i++) {
			final ItemStack item = content[i];

			copy[i] = item != null ? item.clone() : null;
		}

		return Arrays.copyOfRange(copy, from, to);
	}

	/**
	 * 更新此菜单中的某个槽位。如果该槽位是按钮且你希望它继续生效，
	 * 请使用 {@link Position} 注解或在按钮本身中设置槽位。
	 *
	 * @param slot
	 * @param item
	 */
	protected final void setItem(final int slot, final ItemStack item) {
		final Inventory inventory = this.getInventory();

		inventory.setItem(slot, item);
	}

	/**
	 * 如果你想知道菜单中每个空槽位的编号，
	 * 请在构造器中将其设为 true
	 *
	 * <p>
	 * 仅在构造器中或调用 {@link #displayTo(Player)} 之前使用时生效，
	 * 且无法在 {@link #restartMenu()} 中更新
	 *
	 * @param visible
	 */
	protected final void setSlotNumbersVisible() {
		this.slotNumbersVisible = true;
	}

	/**
	 * 返回给定玩家是否仍在查看此菜单：我们会比较
	 * 玩家正在查看的菜单的类，二者相同则返回 true。
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(final Player player) {
		final Menu menu = Menu.getMenu(player);

		return menu != null && menu.getClass().getName().equals(this.getClass().getName());
	}

	// --------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------

	/**
	 * 菜单被点击时自动调用。
	 *
	 * <p>
	 * 默认会调用更简短的 {@link #onMenuClick(Player, int, ItemStack)}
	 * 方法。
	 *
	 * @param player    玩家
	 * @param slot      槽位
	 * @param action    操作
	 * @param click     点击
	 * @param cursor    光标
	 * @param clicked   被点击的物品
	 * @param cancelled 事件是否已取消？
	 */
	protected void onMenuClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final ItemStack cursor, final ItemStack clicked, final boolean cancelled) {
		this.onMenuClick(player, slot, clicked);
	}

	/**
	 * 菜单被点击时自动调用
	 *
	 * @param player  玩家
	 * @param slot    槽位
	 * @param clicked 被点击的物品
	 */
	protected void onMenuClick(final Player player, final int slot, final ItemStack clicked) {
	}

	/**
	 * 已注册的按钮被点击时自动调用
	 *
	 * <p>
	 * 默认情况下，此方法会将点击转交给
	 * {@link Button#onClickedInMenu(Player, Menu, ClickType)}
	 *
	 * @param player 玩家
	 * @param slot   槽位
	 * @param action 操作
	 * @param click  点击
	 * @param button 按钮
	 */
	protected void onButtonClick(final Player player, final int slot, final InventoryAction action, final ClickType click, final Button button) {
		button.onClickedInMenu(player, this, click);
	}

	/**
	 * 处理菜单关闭。这不会关闭背包，只在内部做清理，
	 * 请勿使用。
	 *
	 * @deprecated 仅供内部使用
	 * @param inventory
	 */
	@Deprecated
	public final void handleClose(final Inventory inventory) {
		this.viewer.removeMetadata(FoConstants.NBT.TAG_MENU_CURRENT, SimplePlugin.getInstance());
		this.viewer.setMetadata(FoConstants.NBT.TAG_MENU_LAST_CLOSED, new FixedMetadataValue(SimplePlugin.getInstance(), this));
		this.opened = false;

		this.onMenuClose(this.viewer, inventory);

		// End by calling API
		Common.callEvent(new MenuCloseEvent(this, inventory, this.viewer));
	}

	/**
	 * 菜单关闭时自动调用
	 *
	 * @param player    玩家
	 * @param inventory 正在关闭的菜单背包
	 */
	protected void onMenuClose(final Player player, final Inventory inventory) {
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{}";
	}
}
