package org.mineacademy.fo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.PermsCommand;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用于将聊天消息分页的草案 API。
 *
 * 早期实现参见 {@link PermsCommand}。
 */
@Getter
@RequiredArgsConstructor
public final class ChatPaginator {

	/**
	 * 使用 {@link #setFoundationHeader(String)} 时，
	 * 可以填满所有聊天行（20 行）的高度。
	 *
	 * 之所以是 17，是因为我们的页眉占 3 行。
	 */
	public static final int FOUNDATION_HEIGHT = 15;

	/**
	 * 每页多少行？屏幕上最多 20 行，需减去页眉和页脚。
	 */
	private final int linesPerPage;

	/**
	 * 页眉和页脚使用的颜色
	 */
	private final ChatColor themeColor;

	/**
	 * 每页都包含的页眉。
	 */
	private final List<SimpleComponent> header = new ArrayList<>();

	/**
	 * 各页及其内容。
	 */
	private final Map<Integer, List<SimpleComponent>> pages = new HashMap<>();

	/**
	 * 每页都包含的页脚。
	 */
	private final List<SimpleComponent> footer = new ArrayList<>();

	/**
	 * 在使用 {@link #setFoundationHeader(String)} 且没有页脚的前提下，
	 * 构建占满聊天栏最大化时整个可见区域的聊天分页。
	 * 高度使用 {@link #FOUNDATION_HEIGHT}，
	 * 颜色使用 {@link org.mineacademy.fo.settings.SimpleLocalization.Commands#HEADER_COLOR}。
	 */
	public ChatPaginator() {
		this(FOUNDATION_HEIGHT, SimpleLocalization.Commands.HEADER_COLOR);
	}

	/**
	 * 在使用 {@link #setFoundationHeader(String)} 且没有页脚的前提下，
	 * 构建占满聊天栏最大化时整个可见区域的聊天分页。高度使用 {@link #FOUNDATION_HEIGHT}。
	 *
	 * @param themeColor
	 */
	public ChatPaginator(ChatColor themeColor) {
		this(FOUNDATION_HEIGHT, themeColor);
	}

	/**
	 * 按给定的每页行数创建分页器。屏幕上最多 20 行，需减去页眉和页脚。
	 * 使用 {@link org.mineacademy.fo.settings.SimpleLocalization.Commands#HEADER_COLOR} 颜色。
	 *
	 * @param linesPerPage
	 */
	public ChatPaginator(int linesPerPage) {
		this(linesPerPage, SimpleLocalization.Commands.HEADER_COLOR);
	}

	/**
	 * 设置各插件通用的标准 Foundation 页眉。
	 * ----------------
	 * \<center\>title
	 * ---------------
	 *
	 * @param title
	 * @return
	 */
	public ChatPaginator setFoundationHeader(String title) {
		final String format = SimpleLocalization.Commands.HEADER_FORMAT
				.replace("{theme_color}", this.themeColor.toString())
				.replace("{title}", title);

		for (String message : format.split("\n")) {

			// Support centering inside the message itself
			final String[] centeredParts = message.split("\\<center\\>");

			if (centeredParts.length > 1) {
				Valid.checkBoolean(centeredParts.length == 2, "Cannot use <center> more than once in: " + title);

				message = centeredParts[0] + ChatUtil.center(centeredParts[1], SimpleLocalization.Commands.HEADER_CENTER_LETTER.charAt(0), SimpleLocalization.Commands.HEADER_CENTER_PADDING);
			}

			this.header.add(SimpleComponent.of(message));
		}

		return this;
	}

	/**
	 * 设置内容
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setHeader(SimpleComponent... components) {
		Collections.addAll(this.header, components);

		return this;
	}

	/**
	 * 设置内容
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setHeader(String... messages) {
		for (final String message : messages)
			this.header.add(SimpleComponent.of(message));

		return this;
	}

	/**
	 * 设置内容
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(SimpleComponent... components) {
		this.pages.clear();
		this.pages.putAll(Common.fillPages(this.linesPerPage, Arrays.asList(components)));

		return this;
	}

	/**
	 * 设置内容
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setPages(String... messages) {
		final List<SimpleComponent> pages = new ArrayList<>();

		for (final String message : messages)
			pages.add(SimpleComponent.of(message));

		return this.setPages(pages);
	}

	/**
	 * 设置内容
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setPages(Collection<SimpleComponent> components) {
		this.pages.clear();
		this.pages.putAll(Common.fillPages(this.linesPerPage, components));

		return this;
	}

	/**
	 * 设置内容
	 *
	 * @param components
	 * @return
	 */
	public ChatPaginator setFooter(SimpleComponent... components) {
		Collections.addAll(this.footer, components);

		return this;
	}

	/**
	 * 设置内容
	 *
	 * @param messages
	 * @return
	 */
	public ChatPaginator setFooter(String... messages) {
		for (final String message : messages)
			this.footer.add(SimpleComponent.of(message));

		return this;
	}

	/**
	 * 开始向发送者显示第一页
	 *
	 * @param sender
	 */
	public void send(CommandSender sender) {
		this.send(sender, 1);
	}

	/**
	 * 向发送者显示给定页；若发送者是控制台，则不分页、完整输出
	 *
	 * @param sender
	 * @param page
	 */
	public void send(CommandSender sender, int page) {
		if (Bukkit.isPrimaryThread())
			this.send0(sender, page);
		else
			Common.runLater(() -> this.send0(sender, page));
	}

	private void send0(CommandSender sender, int page) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;

			// Remove old FoPages to prevent conflicts when two or more plugins use Foundation shaded
			if (player.hasMetadata("FoPages")) {
				final Plugin owningPlugin = player.getMetadata("FoPages").get(0).getOwningPlugin();

				player.removeMetadata("FoPages", owningPlugin);
			}

			player.setMetadata("FoPages", new FixedMetadataValue(SimplePlugin.getInstance(), SimplePlugin.getNamed()));
			player.setMetadata(getPageNbtTag(), new FixedMetadataValue(SimplePlugin.getInstance(), this));

			player.chat("/#flp " + page);
		}

		else {
			for (final SimpleComponent component : this.header)
				component.send(sender);

			int amount = 1;

			for (final List<SimpleComponent> components : this.pages.values())
				for (final SimpleComponent component : components)
					component.replace("{count}", amount++).send(sender);

			for (final SimpleComponent component : this.footer)
				component.send(sender);
		}
	}

	public static String getPageNbtTag() {
		return "FoPages_" + SimplePlugin.getNamed();
	}
}
