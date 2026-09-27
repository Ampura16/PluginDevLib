package top.brmc.devlib.menu.button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.brmc.devlib.Valid;
import top.brmc.devlib.conversation.SimplePrompt;
import top.brmc.devlib.conversation.SimpleStringPrompt;
import top.brmc.devlib.menu.Menu;
import top.brmc.devlib.menu.model.ItemCreator;
import top.brmc.devlib.model.RangedValue;
import top.brmc.devlib.model.Replacer;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 表示菜单中可点击的按钮
 */
public abstract class Button {

	/**
	 * 信息按钮的材质，参见 {@link #makeInfo(String...)}
	 */
	@Setter
	private static CompMaterial infoButtonMaterial = CompMaterial.NETHER_STAR;

	/**
	 * 信息按钮的标题，参见 {@link #makeInfo(String...)}
	 * <p>
	 * 会自动着色。
	 */
	@Setter
	private static String infoButtonTitle = SimpleLocalization.Menu.TOOLTIP_INFO;

	/**
	 * 此按钮在菜单中的槽位
	 */
	@Getter
	private int slot = -1;

	/**
	 * 使用给定槽位创建新按钮
	 *
	 * @param slot
	 */
	public Button(int slot) {
		this.slot = slot;
	}

	/**
	 * 创建一个没有槽位的新按钮。
	 */
	public Button() {
	}

	// ----------------------------------------------------------------
	// Button functions
	// ----------------------------------------------------------------

	/**
	 * 按钮被点击时自动调用
	 *
	 * @param player
	 * @param menu
	 * @param click
	 */
	public abstract void onClickedInMenu(Player player, Menu menu, ClickType click);

	/**
	 * 表示此按钮的物品。提示：使用 {@link ItemCreator} 创建。
	 *
	 * @return 此按钮的物品
	 */
	public abstract ItemStack getItem();

	// ----------------------------------------------------------------
	// Static methods
	// ----------------------------------------------------------------

	/**
	 * 创建一个新的下界之星按钮，点击时没有任何动作，
	 * 纯粹用于显示说明文字。
	 * <p>
	 * 每行描述默认以灰色开头，并会替换颜色代码。
	 * <p>
	 * 使用 {@link #setInfoButtonMaterial(CompMaterial)} 和 {@link #setInfoButtonTitle(String)} 进行自定义。
	 *
	 * @param description 按钮的描述
	 * @return 该按钮
	 */
	public static final DummyButton makeInfo(final String... description) {
		final List<String> lores = new ArrayList<>();
		lores.add(" ");

		for (final String line : description)
			lores.add(line);

		return makeDummy(ItemCreator.of(infoButtonMaterial).name(infoButtonTitle).hideTags(true).lore(lores));
	}

	/**
	 * 创建一个新的空按钮（空气）
	 *
	 * @return 新的空气占位按钮
	 */
	public static final DummyButton makeEmpty() {
		return makeDummy(ItemCreator.of(CompMaterial.AIR));
	}

	/**
	 * 创建一个点击时不执行任何操作的占位按钮
	 *
	 * @param material
	 * @param title
	 * @param lore
	 * @return
	 */
	public static final DummyButton makeDummy(final CompMaterial material, String title, String... lore) {
		return makeDummy(ItemCreator.of(material).name(title).lore(lore));
	}

	/**
	 * 创建一个点击时不执行任何操作的占位按钮
	 *
	 * @param creator 图标创建器
	 * @return 该按钮
	 */
	public static final DummyButton makeDummy(final ItemCreator creator) {
		return makeDummy(creator.makeMenuTool());
	}

	/**
	 * 创建一个点击时不执行任何操作的占位按钮
	 *
	 * @param item 物品
	 * @return 该按钮
	 */
	public static final DummyButton makeDummy(final ItemStack item) {
		return new DummyButton(item);
	}

	/**
	 * 创建一个简易按钮，带有给定的图标、标题、标签（描述第二行）以及
	 * 接收点击玩家的点击函数
	 *
	 * 重要：调用 {@link Menu#restartMenu()} 时更改图标不会生效，你必须
	 * 创建匿名 {@link Button} 类才能实现。
	 *
	 * @param icon
	 * @param title
	 * @param label
	 * @param onClickFunction
	 * @return
	 */
	public static final Button makeSimple(final CompMaterial icon, final String title, final String label, final Consumer<Player> onClickFunction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return ItemCreator.of(icon).name(title).lore("").lore(label.split("\n")).makeMenuTool();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				onClickFunction.accept(player);
			}
		};
	}

	/**
	 * 使用给定构建器和点击动作创建简易按钮
	 *
	 * 重要：调用 {@link Menu#restartMenu()} 时更改图标不会生效，你必须
	 * 创建匿名 {@link Button} 类才能实现。
	 *
	 * @param builder
	 * @param onClickFunction
	 * @return
	 */
	public static final Button makeSimple(ItemCreator builder, final Consumer<Player> onClickFunction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return builder.makeMenuTool();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				onClickFunction.accept(player);
			}
		};
	}

	/**
	 * 创建一个简易按钮，带有给定的图标、标题、标签（描述第二行）以及
	 * 接收玩家和点击类型的点击函数
	 *
	 * 重要：调用 {@link Menu#restartMenu()} 时更改图标不会生效，你必须
	 * 创建匿名 {@link Button} 类才能实现。
	 *
	 * @param icon
	 * @param title
	 * @param label
	 * @param onClickFunction
	 * @return
	 */
	public static final Button makeSimple(final CompMaterial icon, final String title, final String label, final BiConsumer<Player, ClickType> onClickFunction) {
		return new Button() {

			@Override
			public ItemStack getItem() {
				return ItemCreator.of(icon, title, "", label).makeMenuTool();
			}

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				onClickFunction.accept(player, click);
			}
		};
	}

	/**
	 * 创建一个可切换开/关状态的功能按钮，通常
	 * 用于切换文件中的设置，例如 Boss 是否掉落物品等。
	 *
	 * @param creator
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static final Button makeBoolean(ItemCreator creator, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		final String menuTitle = creator.getName().toLowerCase();

		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				final boolean has = getter.get();

				setter.accept(!has);

				final Menu newMenu = menu.newInstance();

				newMenu.displayTo(player);
				newMenu.restartMenu((has ? "&4Disabled" : "&2Enabled") + " " + menuTitle + "!");
			}

			@Override
			public ItemStack getItem() {
				final boolean has = getter.get();
				final ItemStack item = creator.glow(has).make();
				final ItemMeta meta = item.getItemMeta();

				meta.setLore(Replacer.replaceArray(meta.getLore(), "status", has ? "&aEnabled" : "&cDisabled"));
				item.setItemMeta(meta);

				return item;
			}
		};
	}

	/**
	 * 创建整数输入提示的便捷方法
	 *
	 * @param item
	 * @param question
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeIntegerPrompt(ItemCreator item, String question, RangedValue minMaxRange, Supplier<Object> getter, Consumer<Integer> setter) {
		return makeIntegerPrompt(item, question, null, minMaxRange, getter, setter);
	}

	/**
	 * 创建整数输入提示的便捷方法
	 *
	 * @param item
	 * @param question
	 * @param menuTitle
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeIntegerPrompt(ItemCreator item, String question, String menuTitle, RangedValue minMaxRange, Supplier<Object> getter, Consumer<Integer> setter) {
		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				new SimplePrompt() {

					@Override
					protected String getPrompt(ConversationContext ctx) {
						return question.replace("{current}", getter.get().toString());
					}

					@Override
					protected boolean isInputValid(ConversationContext context, String input) {
						return Valid.isInteger(input) && Valid.isInRange(Integer.parseInt(input), minMaxRange.getMinLong(), minMaxRange.getMaxLong());
					}

					@Override
					protected String getFailedValidationText(ConversationContext context, String invalidInput) {
						return "Invalid input '" + invalidInput + "'! Enter a whole number from " + minMaxRange.getMinLong() + " to " + minMaxRange.getMaxLong() + ".";
					}

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle != null ? "&9" + menuTitle.substring(0, 1).toUpperCase() + menuTitle.substring(1) + " set to " + getter.get() + "!" : null;
					}

					@Override
					protected Prompt acceptValidatedInput(ConversationContext context, String input) {
						setter.accept(Integer.parseInt(input));

						return END_OF_CONVERSATION;
					}

				}.show(player);
			}

			@Override
			public ItemStack getItem() {
				return item.make();
			}
		};
	}

	/**
	 * 创建小数输入提示的便捷方法
	 *
	 * @param item
	 * @param question
	 * @param minMaxRange
	 * @param setter
	 * @return
	 */
	public static Button makeDecimalPrompt(ItemCreator item, String question, RangedValue minMaxRange, Consumer<Double> setter) {
		return makeDecimalPrompt(item, question, minMaxRange, null, setter);
	}

	/**
	 * 创建小数输入提示的便捷方法
	 *
	 * @param item
	 * @param question
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeDecimalPrompt(ItemCreator item, String question, RangedValue minMaxRange, Supplier<Object> getter, Consumer<Double> setter) {
		return makeDecimalPrompt(item, question, null, minMaxRange, getter, setter);
	}

	/**
	 * 创建小数输入提示的便捷方法
	 *
	 * @param item
	 * @param question
	 * @param menuTitle
	 * @param minMaxRange
	 * @param getter
	 * @param setter
	 * @return
	 */
	public static Button makeDecimalPrompt(ItemCreator item, String question, String menuTitle, RangedValue minMaxRange, @Nullable Supplier<Object> getter, Consumer<Double> setter) {
		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				new SimplePrompt() {

					@Override
					protected String getPrompt(ConversationContext ctx) {
						return question.replace("{current}", getter != null ? getter.get().toString() : "");
					}

					@Override
					protected boolean isInputValid(ConversationContext context, String input) {
						return Valid.isDecimal(input) && Valid.isInRange(Double.parseDouble(input), minMaxRange.getMinDouble(), minMaxRange.getMaxDouble());
					}

					@Override
					protected String getFailedValidationText(ConversationContext context, String invalidInput) {
						return "Invalid input '" + invalidInput + "'! Enter a whole number from " + minMaxRange.getMinDouble() + " to " + minMaxRange.getMaxDouble() + ".";
					}

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle != null ? "&9" + menuTitle.substring(0, 1).toUpperCase() + menuTitle.substring(1) + " set to " + getter.get() + "!" : null;
					}

					@Override
					protected Prompt acceptValidatedInput(ConversationContext context, String input) {
						setter.accept(Double.parseDouble(input));

						return END_OF_CONVERSATION;
					}

				}.show(player);
			}

			@Override
			public ItemStack getItem() {
				final ItemStack itemstack = item.make();
				final ItemMeta meta = itemstack.getItemMeta();

				meta.setLore(Replacer.replaceArray(meta.getLore(), "current", getter != null ? getter.get().toString() : ""));
				itemstack.setItemMeta(meta);

				return itemstack;
			}
		};
	}

	/**
	 * 创建字符串输入提示的便捷方法
	 *
	 * @param creator
	 * @param question
	 * @param onPromptFinish
	 * @return
	 */
	public static Button makeStringPrompt(ItemCreator creator, String question, Consumer<String> onPromptFinish) {
		return makeStringPrompt(creator, question, null, onPromptFinish);
	}

	/**
	 * 创建字符串输入提示的便捷方法
	 *
	 * @param creator
	 * @param question
	 * @param menuTitle
	 * @param onPromptFinish
	 * @return
	 */
	public static Button makeStringPrompt(ItemCreator creator, String question, @Nullable String menuTitle, Consumer<String> onPromptFinish) {
		return new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				new SimpleStringPrompt(question) {

					@Override
					protected String getMenuAnimatedTitle() {
						return menuTitle;
					}

					@Override
					protected void onValidatedInput(ConversationContext context, String input) {
						onPromptFinish.accept(input);
					}

				}.show(player);
			}

			@Override
			public ItemStack getItem() {
				return creator.make();
			}
		};
	}

	@Override
	public final String toString() {
		final ItemStack item = this.getItem();

		return this.getClass().getSimpleName() + "{" + (item != null ? item.getType() : "null") + "}";
	}

	// ----------------------------------------------------------------
	// Helper classes methods
	// ----------------------------------------------------------------

	/**
	 * 点击时不执行任何操作的按钮。
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class DummyButton extends Button {

		/**
		 * 此按钮的图标
		 */
		@Getter
		private final ItemStack item;

		/**
		 * 点击时不执行任何操作
		 */
		@Override
		public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
		}
	}
}
