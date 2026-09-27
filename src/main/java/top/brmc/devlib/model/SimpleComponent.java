package top.brmc.devlib.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.ChatUtil;
import top.brmc.devlib.Common;
import top.brmc.devlib.PlayerUtil;
import top.brmc.devlib.SerializeUtil;
import top.brmc.devlib.SerializeUtil.Mode;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.event.SimpleComponentSendEvent;
import top.brmc.devlib.exception.FoScriptException;
import top.brmc.devlib.remain.CompMaterial;
import top.brmc.devlib.remain.Remain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * 发送交互式聊天消息的一种非常简单的方式
 */
public final class SimpleComponent implements ConfigSerializable {

	/**
	 * 是否通过移除交互元素，防止过大的 JSON 导致玩家被踢出？
	 */
	public static boolean STRIP_OVERSIZED_COMPONENTS = true;

	/**
	 * 是否自动让聊天中的网址可点击？
	 */
	public static boolean AUTO_CREATE_LINKS = true;

	/**
	 * 解析文本时用于匹配 URL 地址的模式
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("^(https?:\\/\\/|)(www\\.|)[\\w-:.\\d]{1,256}\\.[\\w()]{1,12}\\b([\\w-@:%.,+~#=?!&$/\\d]*)$");

	/**
	 * 之前的组件
	 */
	private final List<Part> pastComponents = new ArrayList<>();

	/**
	 * 当前正在创建的组件
	 */
	private Part currentComponent;

	/**
	 * 此组件是否每次都调用 {@link SimpleComponentSendEvent}？默认为 false
	 */
	@Getter
	private boolean firingEvent = false;

	/**
	 * 此组件是否忽略空组件？默认为 false
	 */
	private boolean ignoreEmpty = false;

	/**
	 * 创建新的交互式聊天组件
	 *
	 * @param text
	 */
	private SimpleComponent(String text) {

		// Inject the center element here already
		if (Common.stripColors(text).startsWith("<center>"))
			text = ChatUtil.center(text.replace("<center>", "").trim());

		this.currentComponent = new Part(text);
	}

	/**
	 * 反序列化时使用的私有构造器
	 */
	private SimpleComponent() {
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * 添加显示文本事件
	 *
	 * @param texts
	 * @return
	 */
	public SimpleComponent onHover(final Collection<String> texts) {
		return this.onHover(Common.toArray(texts));
	}

	/**
	 * 添加显示文本的悬停事件
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(final String... lines) {
		// I don't know why we have to wrap this inside new text component but we do this
		// to properly reset bold and other decoration colors
		final String joined = Common.colorize(String.join("\n", lines));
		this.currentComponent.hoverEvent = new SimpleHover(HoverEvent.Action.SHOW_TEXT, joined);

		return this;
	}

	/**
	 * 若物品不是空气，则在悬停时显示该物品。
	 * <p>
	 * 注意：由于 Minecraft/Spigot 的 bug，描述中的部分颜色可能会丢失。
	 *
	 * @param item
	 * @return
	 */
	public SimpleComponent onHover(@NonNull final ItemStack item) {
		if (CompMaterial.isAir(item.getType()))
			return this.onHover("Air");

		this.currentComponent.hoverEvent = new SimpleHover(HoverEvent.Action.SHOW_ITEM, Remain.toJson(item));

		return this;
	}

	/**
	 * 为最后一个组件部分设置查看权限
	 *
	 * @param viewPermission
	 * @return
	 */
	public SimpleComponent viewPermission(final String viewPermission) {
		this.currentComponent.viewPermission = viewPermission;

		return this;
	}

	/**
	 * 为最后一个组件部分设置查看条件
	 *
	 * @param viewCondition
	 * @return
	 */
	public SimpleComponent viewCondition(final String viewCondition) {
		this.currentComponent.viewCondition = viewCondition;

		return this;
	}

	/**
	 * 添加执行命令事件
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickRunCmd(final String text) {
		return this.onClick(Action.RUN_COMMAND, text);
	}

	/**
	 * 添加建议命令事件
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickSuggestCmd(final String text) {
		return this.onClick(Action.SUGGEST_COMMAND, text);
	}

	/**
	 * 打开给定 URL
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickOpenUrl(final String url) {
		return this.onClick(Action.OPEN_URL, url);
	}

	/**
	 * 添加命令事件
	 *
	 * @param action
	 * @param text
	 * @return
	 */
	public SimpleComponent onClick(final Action action, final String text) {
		this.currentComponent.clickEvent = new SimpleClick(action, text);

		return this;
	}

	/**
	 * 调用 {@link TextComponent#setInsertion(String)}
	 *
	 * @param insertion
	 * @return
	 */
	public SimpleComponent onClickInsert(final String insertion) {
		this.currentComponent.insertion = insertion;

		return this;
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * 在所有组件的开头追加新组件
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent appendFirst(final SimpleComponent component) {
		this.pastComponents.add(0, component.currentComponent);
		this.pastComponents.addAll(0, component.pastComponents);

		return this;
	}

	/**
	 * 向此简单组件追加文本
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent append(final String text) {
		return this.append(text, true);
	}

	/**
	 * 向此简单组件追加文本
	 *
	 * @param text
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(final String text, final boolean colorize) {
		return this.append(text, null, colorize);
	}

	/**
	 * 创建另一个组件。当前组件会被放入之前组件的列表中，
	 * 这样下次使用 onClick 或 onHover 时，事件会添加到此处
	 * 指定的新组件上
	 *
	 * @param text
	 * @param inheritFormatting
	 * @return
	 */
	public SimpleComponent append(final String text, final BaseComponent inheritFormatting) {
		return this.append(text, inheritFormatting, true);
	}

	/**
	 * 创建另一个组件。当前组件会被放入之前组件的列表中，
	 * 这样下次使用 onClick 或 onHover 时，事件会添加到此处
	 * 指定的新组件上
	 *
	 * @param text
	 * @param inheritFormatting
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(String text, final BaseComponent inheritFormatting, final boolean colorize) {

		// Get the last extra
		BaseComponent inherit = inheritFormatting != null ? inheritFormatting : this.currentComponent.toTextComponent(false, null);

		if (inherit != null && inherit.getExtra() != null && !inherit.getExtra().isEmpty())
			inherit = inherit.getExtra().get(inherit.getExtra().size() - 1);

		// Center text for each line separately if replacing colors
		if (colorize) {
			final List<String> formatContents = Arrays.asList(text.split("\n"));

			for (int i = 0; i < formatContents.size(); i++) {
				final String line = formatContents.get(i);

				if (Common.stripColors(line).startsWith("<center>"))
					formatContents.set(i, ChatUtil.center(line.replace("<center>", "")));
			}

			text = String.join("\n", formatContents);
			text = Common.colorize(text);
		}

		this.pastComponents.add(this.currentComponent);

		this.currentComponent = new Part(text);
		this.currentComponent.inheritFormatting = inherit;

		return this;
	}

	/**
	 * 在此组件末尾追加新组件
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent append(final SimpleComponent component) {
		this.pastComponents.add(this.currentComponent);
		this.pastComponents.addAll(component.pastComponents);

		// Get the last extra
		BaseComponent inherit = Common.getOrDefault(component.currentComponent.inheritFormatting, this.currentComponent.toTextComponent(false, null));

		if (inherit != null && inherit.getExtra() != null && !inherit.getExtra().isEmpty())
			inherit = inherit.getExtra().get(inherit.getExtra().size() - 1);

		this.currentComponent = component.currentComponent;
		this.currentComponent.inheritFormatting = inherit;

		return this;
	}

	/**
	 * 返回将所有组件合并后的纯着色消息，
	 * 不含点击/悬停事件
	 *
	 * @return
	 */
	public String getPlainMessage() {
		return this.build(null).toLegacyText();
	}

	/**
	 * 将此组件及其之前的组件构建为 {@link TextComponent}
	 *
	 * @return
	 */
	public TextComponent getTextComponent() {
		return this.build(null);
	}

	/**
	 * 将此组件及其之前的组件构建为 {@link TextComponent}
	 *
	 * @param receiver
	 * @return
	 */
	public TextComponent build(final CommandSender receiver) {
		TextComponent preparedComponent = null;

		for (final Part part : this.pastComponents) {
			final TextComponent component = part.toTextComponent(true, receiver);

			if (component != null)
				if (preparedComponent == null)
					preparedComponent = component;
				else
					this.addExtra(preparedComponent, component);
		}

		final TextComponent currentComponent = this.currentComponent == null ? null : this.currentComponent.toTextComponent(true, receiver);

		if (currentComponent != null)
			if (preparedComponent == null)
				preparedComponent = currentComponent;
			else
				this.addExtra(preparedComponent, currentComponent);

		return Common.getOrDefault(preparedComponent, new TextComponent(""));
	}

	/*
	 * Mostly resolving some ancient MC version incompatibility: "UnsupportedOperationException"
	 */
	private void addExtra(final BaseComponent component, final BaseComponent extra) {
		try {
			component.addExtra(extra);

		} catch (final Throwable t) {
			final List<BaseComponent> safeList = new ArrayList<>();

			if (component.getExtra() != null)
				safeList.addAll(component.getExtra());

			safeList.add(extra);
			component.setExtra(safeList);
		}
	}

	/**
	 * 快速替换此组件所有部分中的某个对象
	 *
	 * @param variable 实际的变量——你必须自己加上括号
	 * @param value
	 * @return
	 */
	public SimpleComponent replace(final String variable, final Object value) {
		final String serialized = SerializeUtil.serialize(Mode.YAML, value).toString();

		for (final Part part : this.pastComponents) {
			Valid.checkNotNull(part.text);

			part.replace(variable, serialized);
		}

		Valid.checkNotNull(this.currentComponent.text);
		this.currentComponent.replace(variable, serialized);

		return this;
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * 尝试将完整的 {@link SimpleComponent} 发送给给定的
	 * 命令发送者。如果是玩家，会发送交互元素。
	 * <p>
	 * 如果是控制台，则收到纯文本消息。
	 * @param receivers
	 * @param <T>
	 */
	public <T extends CommandSender> void send(final T... receivers) {
		this.send(Arrays.asList(receivers));
	}

	/**
	 * 尝试将完整的 {@link SimpleComponent} 发送给给定的
	 * 命令发送者。如果是玩家，会发送交互元素。
	 * <p>
	 * 如果是控制台，则收到纯文本消息。
	 *
	 * @param <T>
	 * @param receivers
	 */
	public <T extends CommandSender> void send(final Iterable<T> receivers) {
		this.sendAs(null, receivers);
	}

	/**
	 * 尝试将完整的 {@link SimpleComponent} 发送给给定的
	 * 命令发送者。如果是玩家，会发送交互元素。
	 * <p>
	 * 如果是控制台，则收到纯文本消息。
	 *
	 * 如果设置了发送者且其为玩家，我们还会替换关系占位符。
	 *
	 * @param <T>
	 * @param sender
	 * @param receivers
	 */
	public <T extends CommandSender> void sendAs(@Nullable final CommandSender sender, final Iterable<T> receivers) {
		for (final CommandSender receiver : receivers) {

			TextComponent component = this.build(receiver);

			if (receiver instanceof Player && sender instanceof Player)
				this.setRelationPlaceholders(component, (Player) receiver, (Player) sender);

			if (this.firingEvent) {
				final SimpleComponentSendEvent event = new SimpleComponentSendEvent(sender, receiver, component);

				if (!Common.callEvent(event))
					continue;

				component = event.getComponent();
			}

			final String legacy = Common.colorize(component.toLegacyText());

			if (Common.stripColors(legacy).trim().isEmpty() && this.ignoreEmpty) {
				Debugger.debug("component", "Message is empty, skipping.");

				continue;
			}

			// Prevent clients being kicked out, so we just send plain message instead
			if (STRIP_OVERSIZED_COMPONENTS && Remain.toJson(component).length() + 1 >= Short.MAX_VALUE) {
				if (legacy.length() + 1 >= Short.MAX_VALUE)
					Common.warning("JSON Message to " + receiver.getName() + " was too large and could not be sent: '" + legacy + "'");

				else {
					final int oversize = Remain.toJson(component).length() + 1 - Short.MAX_VALUE;
					Common.warning("JSON Message to " + receiver.getName() + " was " + oversize + " bytes oversize, removing interactive elements to avoid kick. Sending plain: '" + legacy + "'");

					receiver.sendMessage(legacy);
				}

			} else
				Remain.sendComponent(receiver, component);
		}
	}

	/*
	 * Replace relationship placeholders in the full component and all of its extras
	 */
	private void setRelationPlaceholders(final TextComponent component, final Player receiver, final Player sender) {

		// Set the main text
		component.setText(HookManager.replaceRelationPlaceholders(sender, receiver, component.getText()));

		if (component.getExtra() == null)
			return;

		for (final BaseComponent extra : component.getExtra())
			if (extra instanceof TextComponent) {

				final TextComponent text = (TextComponent) extra;
				final ClickEvent clickEvent = text.getClickEvent();
				final HoverEvent hoverEvent = text.getHoverEvent();

				// Replace for the text itself
				//text.setText(HookManager.replaceRelationPlaceholders(sender, receiver, text.getText()));

				// And for the click event
				if (clickEvent != null)
					text.setClickEvent(new ClickEvent(clickEvent.getAction(), HookManager.replaceRelationPlaceholders(sender, receiver, clickEvent.getValue())));

				// And for the hover event
				if (hoverEvent != null)
					for (final BaseComponent hoverBaseComponent : hoverEvent.getValue())
						if (hoverBaseComponent instanceof TextComponent) {
							final TextComponent hoverTextComponent = (TextComponent) hoverBaseComponent;

							hoverTextComponent.setText(HookManager.replaceRelationPlaceholders(sender, receiver, hoverTextComponent.getText()));
						}

				// Then repeat for the extra parts in the text itself
				this.setRelationPlaceholders(text, receiver, sender);
			}
	}

	/**
	 * 设置此组件是否应调用 {@link SimpleComponentSendEvent}？默认为 false 以避免过载
	 *
	 * @param firingEvent
	 * @return
	 */
	public SimpleComponent setFiringEvent(final boolean firingEvent) {
		this.firingEvent = firingEvent;

		return this;
	}

	/**
	 * 设置此组件是否应忽略空组件？默认为 false
	 *
	 * @param ignoreEmpty
	 * @return
	 */
	public SimpleComponent setIgnoreEmpty(final boolean ignoreEmpty) {
		this.ignoreEmpty = ignoreEmpty;

		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.serialize().toStringFormatted();
	}

	// --------------------------------------------------------------------
	// Serialize
	// --------------------------------------------------------------------

	/**
	 * @see top.brmc.devlib.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIf("Current_Component", this.currentComponent);
		map.put("Past_Components", this.pastComponents);

		return map;
	}

	/**
	 * 根据序列化映射创建 {@link SimpleComponent}
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(final SerializedMap map) {
		final SimpleComponent component = new SimpleComponent();

		component.currentComponent = map.get("Current_Component", Part.class);
		component.pastComponents.addAll(map.getList("Past_Components", Part.class));

		return component;
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * 将消息编译为组件，每当消息出现新的 & 颜色/格式时
	 * 都创建一个新的 {@link PermissibleComponent}，并保留
	 * 最后的颜色
	 *
	 * @param message
	 * @param inheritFormatting
	 * @param viewPermission
	 * @return
	 */
	private static TextComponent[] toComponent(@NonNull String message, final BaseComponent inheritFormatting) {
		final List<TextComponent> components = new ArrayList<>();

		// Plot the previous formatting manually before the message to retain it
		if (inheritFormatting != null) {

			if (inheritFormatting.isBold())
				message = ChatColor.BOLD + message;

			if (inheritFormatting.isItalic())
				message = ChatColor.ITALIC + message;

			if (inheritFormatting.isObfuscated())
				message = ChatColor.MAGIC + message;

			if (inheritFormatting.isStrikethrough())
				message = ChatColor.STRIKETHROUGH + message;

			if (inheritFormatting.isUnderlined())
				message = ChatColor.UNDERLINE + message;

			message = inheritFormatting.getColor() + message;
		}

		StringBuilder builder = new StringBuilder();
		TextComponent component = new TextComponent();

		for (int index = 0; index < message.length(); index++) {
			char letter = message.charAt(index);

			if (letter == ChatColor.COLOR_CHAR) {
				if (++index >= message.length())
					break;

				letter = message.charAt(index);

				if (letter >= 'A' && letter <= 'Z')
					letter += 32;

				ChatColor format;

				if (letter == 'x' && index + 12 < message.length()) {
					final StringBuilder hex = new StringBuilder("#");

					for (int j = 0; j < 6; j++)
						hex.append(message.charAt(index + 2 + j * 2));

					try {
						format = ChatColor.of(hex.toString());

					} catch (NoSuchMethodError | IllegalArgumentException ex) {
						format = null;
					}

					index += 12;

				} else
					format = ChatColor.getByChar(letter);

				if (format == null)
					continue;

				if (builder.length() > 0) {
					final TextComponent old = component;

					component = new TextComponent(old);
					old.setText(builder.toString());

					builder = new StringBuilder();
					components.add(old);
				}

				if (format == ChatColor.BOLD)
					component.setBold(true);

				else if (format == ChatColor.ITALIC)
					component.setItalic(true);

				else if (format == ChatColor.UNDERLINE)
					component.setUnderlined(true);

				else if (format == ChatColor.STRIKETHROUGH)
					component.setStrikethrough(true);

				else if (format == ChatColor.MAGIC)
					component.setObfuscated(true);

				else if (format == ChatColor.RESET) {
					format = ChatColor.WHITE;

					component = new TextComponent();
					component.setColor(format);

					/*component.setBold(false);
					component.setItalic(false);
					component.setStrikethrough(false);
					component.setUnderlined(false);*/

				} else {
					component = new TextComponent();

					component.setColor(format);
				}

				continue;
			}

			int pos = message.indexOf(' ', index);

			if (pos == -1)
				pos = message.length();

			// Web link handling
			if (URL_PATTERN.matcher(message).region(index, pos).find()) {

				if (builder.length() > 0) {
					final TextComponent old = component;
					component = new TextComponent(old);

					old.setText(builder.toString());

					builder = new StringBuilder();
					components.add(old);
				}

				final TextComponent old = component;
				component = new TextComponent(old);

				String urlString = message.substring(index, pos);
				component.setText(urlString);

				if (urlString.endsWith(",") || urlString.endsWith(".") || urlString.endsWith(":") || urlString.endsWith("?") || urlString.endsWith("!"))
					urlString = urlString.substring(0, urlString.length() - 1);

				if (AUTO_CREATE_LINKS)
					component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlString.startsWith("http") ? urlString : "http://" + urlString));

				components.add(component);

				index += pos - index - 1;
				component = old;

				continue;
			}

			builder.append(letter);
		}

		component.setText(builder.toString());
		components.add(component);

		//return components.toArray(new TextComponent[components.size()]);
		return new TextComponent[] { new TextComponent(components.toArray(new TextComponent[components.size()])) };
	}

	/**
	 * 创建新的交互式聊天组件
	 * 之后可以在文本基础上添加交互元素
	 *
	 * @return
	 */
	public static SimpleComponent empty() {
		return of(true, "");
	}

	/**
	 * 创建新的交互式聊天组件
	 * 之后可以在文本基础上添加交互元素
	 *
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(final String text) {
		return of(true, text);
	}

	/**
	 * 创建新的交互式聊天组件
	 * 之后可以在文本基础上添加交互元素
	 *
	 * @param colorize
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(final boolean colorize, final String text) {
		return new SimpleComponent(colorize ? Common.colorize(text) : text);
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	/**
	 * 正在创建的部分
	 */
	static final class Part implements ConfigSerializable {

		/**
		 * 文本
		 */
		private String text;

		/**
		 * 查看权限
		 */

		private String viewPermission;

		/**
		 * 查看条件（JS）
		 */

		private String viewCondition;

		/**
		 * 悬停事件
		 */

		private SimpleHover hoverEvent;

		/**
		 * 点击事件
		 */
		private SimpleClick clickEvent;

		/**
		 * 插入文本
		 */

		private String insertion;

		/**
		 * 从哪个组件继承颜色/装饰？
		 */

		private BaseComponent inheritFormatting;

		/*
		 * Create a new part
		 */
		private Part(final String text) {
			Valid.checkNotNull(text, "Part text cannot be null");

			this.text = text;
		}

		/**
		 * @see top.brmc.devlib.model.ConfigSerializable#serialize()
		 */
		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Text", this.text);
			map.putIf("View_Permission", this.viewPermission);
			map.putIf("View_Condition", this.viewCondition);
			map.putIf("Hover_Event", this.hoverEvent);
			map.putIf("Click_Event", this.clickEvent);
			map.putIf("Insertion", this.insertion);
			map.putIf("Inherit_Formatting", this.inheritFormatting);

			return map;
		}

		/**
		 * 根据给定的 serializedMap 创建 Part
		 *
		 * @param map
		 * @return
		 */
		public static Part deserialize(final SerializedMap map) {
			final Part part = new Part(map.getString("Text"));

			part.viewPermission = map.getString("View_Permission");
			part.viewCondition = map.getString("View_Condition");
			part.hoverEvent = map.get("Hover_Event", SimpleHover.class);
			part.clickEvent = map.get("Click_Event", SimpleClick.class);
			part.insertion = map.getString("Insertion");
			part.inheritFormatting = map.get("Inherit_Formatting", BaseComponent.class);

			return part;
		}

		/**
		 * 用给定值替换给定变量
		 */
		private void replace(final String variable, final String value) {
			this.text = this.text.replace(variable, value);

			if (this.hoverEvent != null)
				this.hoverEvent.value = this.hoverEvent.value.replace(variable, value);

			if (this.clickEvent != null)
				this.clickEvent.value = this.clickEvent.value.replace(variable, value);
		}

		/**
		 * 为给定接收者将组件的这一部分转换为
		 * {@link TextComponent}
		 *
		 * @param checkForReceiver
		 * @param receiver
		 * @return
		 */
		private TextComponent toTextComponent(final boolean checkForReceiver, final CommandSender receiver) {
			if (checkForReceiver && !this.canSendTo(receiver) || this.isEmpty())
				return null;

			final List<BaseComponent> base = toComponent(this.text, this.inheritFormatting)[0].getExtra();

			for (final BaseComponent part : base) {
				if (this.hoverEvent != null) {
					if (this.hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT)
						part.setHoverEvent(new HoverEvent(this.hoverEvent.getAction(), new BaseComponent[] { new TextComponent(TextComponent.fromLegacyText(this.hoverEvent.getValue())) }));

					else if (this.hoverEvent.getAction() == HoverEvent.Action.SHOW_ITEM)
						part.setHoverEvent(new HoverEvent(this.hoverEvent.getAction(), new BaseComponent[] { new TextComponent(this.hoverEvent.getValue()) }));

					else
						throw new IllegalStateException("Not implemented hover event action: " + this.hoverEvent.getAction() + " to show " + this.hoverEvent.getValue());
				}

				if (this.clickEvent != null)
					part.setClickEvent(new ClickEvent(this.clickEvent.getAction(), this.clickEvent.getValue()));

				if (this.insertion != null)
					try {
						part.setInsertion(this.insertion);
					} catch (final Throwable t) {
						// Unsupported MC version
					}
			}

			return new TextComponent(base.toArray(new BaseComponent[base.size()]));
		}

		/*
		 * Return if we're dealing with an empty format
		 */
		private boolean isEmpty() {
			return this.text.isEmpty() && this.hoverEvent == null && this.clickEvent == null && this.insertion == null;
		}

		/*
		 * Can this component be shown to the given sender?
		 */
		private boolean canSendTo(final CommandSender receiver) {

			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !PlayerUtil.hasPerm(receiver, this.viewPermission)))
				return false;

			if (this.viewCondition != null && !this.viewCondition.isEmpty()) {
				if (receiver == null)
					return false;

				final Object result;

				try {
					result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);

				} catch (final FoScriptException ex) {
					Common.logFramed(
							"Failed parsing View_Condition for component!",
							"",
							"The View_Condition must be a JavaScript code that returns a boolean!",
							"Component: " + this,
							"Line: " + ex.getErrorLine(),
							"Error: " + ex.getMessage());

					throw ex;
				}

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "View condition must return Boolean not " + (result == null ? "null" : result.getClass()) + " for component: " + this);

					if (!((boolean) result))
						return false;
				}

			}

			return true;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return this.serialize().toStringFormatted();
		}
	}

	@Data
	@AllArgsConstructor
	public static class SimpleHover implements ConfigSerializable {

		private HoverEvent.Action action;
		private String value;

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Action", action);
			map.put("Value", value);

			return map;
		}

		public static SimpleHover deserialize(final SerializedMap map) {
			final HoverEvent.Action action = map.get("Action", HoverEvent.Action.class);
			final String value = map.getString("Value");

			return new SimpleHover(action, value);
		}
	}

	@Data
	@AllArgsConstructor
	public static class SimpleClick implements ConfigSerializable {

		private ClickEvent.Action action;
		private String value;

		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Action", action);
			map.put("Value", value);

			return map;
		}

		public static SimpleClick deserialize(final SerializedMap map) {
			final ClickEvent.Action action = map.get("Action", ClickEvent.Action.class);
			final String value = map.getString("Value");

			return new SimpleClick(action, value);
		}
	}
}
