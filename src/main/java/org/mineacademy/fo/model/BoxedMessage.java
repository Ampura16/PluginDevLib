package org.mineacademy.fo.model;

import java.util.Arrays;
import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.ChatUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 表示上下被整行分隔线包围的聊天消息：
 * <p>
 * -----------------------------------
 * Hello this is a test!
 * -----------------------------------
 * <p>
 * 也可以在文本前加上 &lt;center&gt; 使其居中。
 */
public final class BoxedMessage {

	/**
	 * 上下分隔线本身
	 */
	public static String LINE = Common.chatLineSmooth();

	/**
	 * 上下分隔线的颜色
	 */
	public static ChatColor LINE_COLOR = ChatColor.DARK_GRAY;

	/**
	 * 所有消息接收者
	 */
	private final Iterable<? extends CommandSender> recipients;

	/**
	 * 消息的发送者
	 */
	private final Player sender;

	/**
	 * 要发送的消息
	 */
	private final String[] messages;

	/**
	 * 根据给定消息创建新的带边框消息，
	 * 但不发送给任何玩家
	 *
	 * @param messages
	 */
	public BoxedMessage(@NonNull String... messages) {
		this(null, null, messages);
	}

	/**
	 * 创建新的带边框消息
	 *
	 * @param recipients
	 * @param sender
	 * @param messages
	 */
	private BoxedMessage(Iterable<? extends CommandSender> recipients, Player sender, @NonNull String[] messages) {
		this.recipients = recipients == null ? null : Common.toList(recipients); // Make a copy to prevent changes in the list on send
		this.sender = sender;
		this.messages = messages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helper methods
	// ------------------------------------------------------------------------------------------------------------

	private void launch() {
		Common.runLater(2, () -> {
			final String oldTellPrefix = Common.getTellPrefix();
			Common.setTellPrefix("");

			this.sendFrame();

			Common.setTellPrefix(oldTellPrefix);
		});
	}

	private void sendFrame() {
		this.sendLine();
		this.sendFrameInternals0();
		this.sendLine();
	}

	private void sendFrameInternals0() {
		for (int i = 0; i < this.getTopLines(); i++)
			this.send("&r");

		for (final String message : this.messages)
			for (final String part : message.split("\n"))
				this.send(part);

		for (int i = 0; i < this.getBottomLines(); i++)
			this.send("&r");
	}

	private int getTopLines() {
		switch (this.length()) {
			case 1:
				return 2;
			case 2:
			case 3:
			case 4:
				return 1;

			default:
				return 0;
		}
	}

	private int getBottomLines() {
		switch (this.length()) {
			case 1:
			case 2:
				return 2;
			case 3:
				return 1;

			default:
				return 0;
		}
	}

	private void sendLine() {
		this.send(LINE_COLOR + LINE);
	}

	private int length() {
		int length = 0;

		for (final String message : this.messages)
			for (@SuppressWarnings("unused")
			final String part : message.split("\n"))
				length++;

		return length;
	}

	private void send(String message) {
		if (Common.stripColors(message).startsWith("<center>"))
			message = ChatUtil.center(message.replaceFirst("\\<center\\>(\\s|)", ""));

		if (this.recipients == null)
			this.broadcast0(message);

		else
			this.tell0(message);
	}

	private void broadcast0(String message) {
		if (this.sender != null)
			Common.broadcast(message, this.sender);
		else
			Common.broadcastTo(Remain.getOnlinePlayers(), message);
	}

	private void tell0(String message) {
		if (this.sender != null)
			message = message.replace("{player}", Common.resolveSenderName(this.sender));

		Common.broadcastTo(this.recipients, message);
	}

	/**
	 * 查找给定变量（无需加 {} 括号，我们会自动加上）
	 * 并用实例替换它们
	 *
	 * @param variables
	 * @return
	 */
	public Replacor find(String... variables) {
		return new Replacor(variables);
	}

	public String getMessage() {
		return this.messages.length == 0 ? "" : String.join("\n", this.messages);
	}

	@Override
	public String toString() {
		return "Boxed{" + String.join(", ", this.messages) + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 向消息中的所有人广播此消息
	 */
	public void broadcast() {
		broadcast(null, this.messages);
	}

	/**
	 * 以发送者身份向所有玩家广播此消息，
	 * 并将 {player} 替换为发送者名称
	 *
	 * @param sender
	 */
	public void broadcastAs(Player sender) {
		new BoxedMessage(null, sender, this.messages).launch();
	}

	/**
	 * 向接收者发送此消息
	 *
	 * @param recipient
	 */
	public void tell(CommandSender recipient) {
		tell(null, Arrays.asList(recipient), this.messages);
	}

	/**
	 * 向给定接收者发送此消息
	 *
	 * @param recipients
	 */
	public void tell(Iterable<? extends CommandSender> recipients) {
		tell(null, recipients, this.messages);
	}

	/**
	 * 向给定接收者发送此消息，
	 * 并将 {player} 替换为发送者名称
	 *
	 * @param receiver
	 * @param sender
	 */
	public void tellAs(CommandSender receiver, Player sender) {
		tell(sender, Arrays.asList(receiver), this.messages);
	}

	/**
	 * 向给定接收者发送此消息，
	 * 并将 {player} 替换为发送者名称
	 *
	 * @param receivers
	 * @param sender
	 */
	public void tellAs(Iterable<? extends CommandSender> receivers, Player sender) {
		new BoxedMessage(receivers, sender, this.messages).launch();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 向所有人发送此消息
	 *
	 * @param messages
	 */
	public static void broadcast(String... messages) {
		broadcast(null, messages);
	}

	/**
	 * 以发送者身份向所有玩家发送此消息
	 *
	 * @param sender
	 * @param messages
	 */
	public static void broadcast(Player sender, String... messages) {
		new BoxedMessage(null, sender, messages).launch();
	}

	/**
	 * 向接收者发送消息
	 *
	 * @param recipient
	 * @param messages
	 */
	public static void tell(CommandSender recipient, String... messages) {
		tell(null, Arrays.asList(recipient), messages);
	}

	/**
	 * 向给定接收者发送消息
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void tell(Iterable<? extends CommandSender> recipients, String... messages) {
		tell(null, recipients, messages);
	}

	/**
	 * 以发送者身份向接收者发送此消息
	 *
	 * @param sender
	 * @param receiver
	 * @param messages
	 */
	public static void tell(Player sender, CommandSender receiver, String... messages) {
		tell(sender, Arrays.asList(receiver), messages);
	}

	/**
	 * 以发送者身份向多个接收者发送此消息
	 *
	 * @param sender
	 * @param receivers
	 * @param messages
	 */
	public static void tell(Player sender, Iterable<? extends CommandSender> receivers, String... messages) {
		new BoxedMessage(receivers, sender, messages).launch();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacor
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 用于快速替换变量的工具类
	 */
	@RequiredArgsConstructor
	public class Replacor {

		/**
		 * 要替换的占位符名称
		 */
		private final String[] variables;

		/**
		 * 用给定的对象替换值替换我们存储的变量
		 *
		 * @param replacements
		 * @return
		 */
		public final BoxedMessage replace(Object... replacements) {
			String message = String.join("%delimiter%", BoxedMessage.this.messages);

			for (int i = 0; i < this.variables.length; i++) {
				String find = this.variables[i];

				{ // Auto insert brackets
					if (!find.startsWith("{"))
						find = "{" + find;

					if (!find.endsWith("}"))
						find = find + "}";
				}

				final Object rep = i < replacements.length ? replacements[i] : null;

				message = message.replace(find, rep != null ? Objects.toString(rep) : "");
			}

			final String[] copy = message.split("%delimiter%");

			return new BoxedMessage(BoxedMessage.this.recipients, BoxedMessage.this.sender, copy);
		}
	}
}