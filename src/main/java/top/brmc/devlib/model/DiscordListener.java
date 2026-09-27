package top.brmc.devlib.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import top.brmc.devlib.Common;
import top.brmc.devlib.collection.StrictSet;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.Remain;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.HierarchyException;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.WebhookUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class DiscordListener implements Listener {

	/**
	 * 保存已注册的 Discord 监听器
	 */
	private static final StrictSet<DiscordListener> registeredListeners = new StrictSet<>();

	/**
	 * 清除所有已注册的监听器
	 */
	public static final void clearRegisteredListeners() {
		registeredListeners.clear();
	}

	/**
	 * 临时存储最近收到的消息
	 */
	private Message message;

	/**
	 * 编辑消息时使用。由于编辑消息后其 ID 会改变，
	 * 此映射将旧消息 ID 对应到新 ID
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private final Map<Long, Long> editedMessages = new HashMap<>();

	/**
	 * 为 DiscordSRV 插件创建新的 Discord 监听器
	 *
	 * @param channel
	 */
	protected DiscordListener() {
		registeredListeners.add(this);
	}

	/**
	 * 注册以监听事件（仅当尚未注册时）
	 */
	public void register() {
		if (!registeredListeners.contains(this))
			registeredListeners.add(this);
	}

	/*
	 * Called automatically when someone writes a message in a Discord channel
	 */
	private final void handleMessageReceived(DiscordGuildMessagePreProcessEvent event) {
		this.message = event.getMessage();

		this.onMessageReceived(event);
	}

	/*
	 * Called automatically when someone writes a message in a Discord channel
	 */
	private final void handleMessageReceivedLate(DiscordGuildMessagePostProcessEvent event) {
		this.message = event.getMessage();

		this.onMessageReceivedLate(event);
	}

	/**
	 * 覆盖此方法，以便在有人于 Discord 频道中发消息时运行代码
	 *
	 * @param event
	 */
	protected abstract void onMessageReceived(DiscordGuildMessagePreProcessEvent event);

	/**
	 * 覆盖此方法，以便在有人于 Discord 频道中发消息、
	 * 且消息已被 DiscordSRV 处理（替换变量等）之后运行代码。
	 *
	 * @param event
	 */
	protected void onMessageReceivedLate(DiscordGuildMessagePostProcessEvent event) {

	}

	/**
	 * 当有人在 Minecraft 中发送消息且 DiscordSRV 自动处理它时
	 * 自动调用——如果你自己处理消息，建议在此
	 * 取消该事件，以避免重复发送
	 *
	 * @param event
	 */
	protected void onMessageSent(GameChatMessagePreProcessEvent event) {
	}

	/**
	 * 查找玩家的便捷方法，若玩家不在线
	 * 则会中止你的代码，参见 {@link #checkBoolean(boolean, String)}
	 *
	 * @param playerName
	 * @param offlineMessage
	 * @return
	 */
	protected final Player findPlayer(String playerName, String offlineMessage) {
		final Player player = Bukkit.getPlayer(playerName);

		this.checkBoolean(player != null, offlineMessage);
		return player;
	}

	/**
	 * 根据给定 ID 获取 {@link TextChannel}
	 *
	 * @param channelId
	 * @return
	 */
	protected final TextChannel findChannel(long channelId) {
		final JDA jda = DiscordUtil.getJda();

		// JDA can be null when server is starting or connecting
		if (jda != null)
			return jda.getTextChannelById(channelId);

		return null;
	}

	/**
	 * 获取与给定名称匹配的 {@link TextChannel} 列表
	 *
	 * @param channelId
	 * @return
	 */
	protected final List<TextChannel> findChannels(String channelName) {
		final JDA jda = DiscordUtil.getJda();

		// JDA can be null when server is starting or connecting
		if (jda != null) {
			final List<TextChannel> channels = new ArrayList<>();

			for (final TextChannel channel : jda.getTextChannels())
				if (channel.getName().equalsIgnoreCase(channelName))
					channels.add(channel);

			return channels;
		}

		return new ArrayList<>();
	}

	/**
	 * 返回所有已关联频道名称的列表
	 *
	 * @return
	 */
	protected final Set<String> getChannelsNames() {
		return HookManager.getDiscordChannels();
	}

	/**
	 * 检查给定值是否为 true，若否，则向 Discord 发送警告
	 * 消息并删除收到的消息
	 * <p>
	 * 若该布尔值为 false，你后面的代码将停止执行
	 *
	 * @param value
	 * @param warningMessage
	 * @throws RemovedMessageException
	 */
	protected final void checkBoolean(boolean value, String warningMessage) throws RemovedMessageException {
		if (!value)
			this.removeAndWarn(warningMessage);
	}

	/**
	 * 删除收到的消息，并由机器人发送一条警告消息，
	 * 显示 2 秒后删除。
	 *
	 * @param warningMessage
	 */
	protected final void removeAndWarn(String warningMessage) {
		this.removeAndWarn(this.message, warningMessage);
	}

	/**
	 * 删除给定消息，并由机器人发送一条警告消息，
	 * 显示 2 秒后删除。
	 *
	 * @param message
	 * @param warningMessage
	 */
	protected final void removeAndWarn(Message message, String warningMessage) {
		this.removeAndWarn(message, warningMessage, 2);
	}

	/**
	 * 删除给定消息，并由机器人发送一条警告消息，
	 * 显示给定秒数后删除。
	 *
	 * @param message
	 * @param warningMessage
	 * @param warningDurationSeconds 警告消息的显示时长
	 */
	protected final void removeAndWarn(Message message, String warningMessage, int warningDurationSeconds) {
		message.delete().complete();

		final MessageChannel channel = message.getChannel();
		final Message channelWarningMessage = channel.sendMessage(warningMessage).complete();

		channel.deleteMessageById(channelWarningMessage.getIdLong()).completeAfter(warningDurationSeconds, TimeUnit.SECONDS);

		throw new RemovedMessageException();
	}

	/**
	 * 向聊天发送一条 2 秒后消失的消息，并中止你的代码
	 *
	 * @param message
	 */
	protected final void returnHandled(String message) {
		final Message notifyMessage = this.message.getChannel().sendMessage(message).complete();
		notifyMessage.delete().completeAfter(2, TimeUnit.SECONDS);

		throw new RemovedMessageException();
	}

	/**
	 * 返回给定成员是否拥有给定名称的角色，
	 * 不区分大小写
	 *
	 * @param member
	 * @param roleName
	 * @return
	 */
	public final boolean hasRole(Member member, String roleName) {
		for (final Role role : member.getRoles())
			if (role.getName().equalsIgnoreCase(roleName))
				return true;

		return false;
	}

	/**
	 * 以发送者身份向 Discord 频道发送消息的便捷方法
	 * <p>
	 * 对玩家提供增强功能
	 *
	 * @param sender
	 * @param channelName
	 * @param message
	 */
	public final void sendMessage(Player sender, String channelName, String message) {
		HookManager.sendDiscordMessage(sender, channelName, message);
	}

	/**
	 * 向 Discord 频道发送消息的便捷方法
	 *
	 * @param channelName
	 * @param message
	 */
	public final void sendMessage(String channelName, String message) {
		HookManager.sendDiscordMessage(channelName, message);
	}

	/**
	 * 若给定发送者是有效的 Player，则以其身份发送 webhook 消息
	 *
	 * @param sender
	 * @param channelName
	 * @param message
	 */
	public final void sendWebhookMessage(@Nullable CommandSender sender, String channelName, String message) {
		final List<TextChannel> channels = this.findChannels(channelName);
		final TextChannel channel = channels.isEmpty() ? null : channels.get(0);

		if (channel == null)
			return;

		// Send the message
		Common.runAsync(() -> {
			try {
				// You can remove this if you don't want to use webhooks
				if (sender instanceof Player)
					WebhookUtil.deliverMessage(channel, (Player) sender, message);

				else
					HookManager.sendDiscordMessage(sender, channelName, message);

			} catch (final ErrorResponseException ex) {
				Debugger.debug("discord", "Unable to send message to Discord channel " + channelName + ", message: " + message);
			}
		});
	}

	/**
	 * 向给定频道发送一条显示四秒的消息
	 *
	 * @param channel
	 * @param message
	 */
	public final void flashMessage(TextChannel channel, String message) {
		final String finalMessage = Common.stripColors(message);

		Common.runAsync(() -> {
			final Message sentMessage = channel.sendMessage(finalMessage).complete();

			Common.runLaterAsync(4 * 20, () -> {
				try {
					channel.deleteMessageById(sentMessage.getIdLong()).complete();

				} catch (final github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException ex) {

					// Silence if deleted already
					if (!ex.getMessage().contains("Unknown Message"))
						ex.printStackTrace();
				}
			});
		});
	}

	/**
	 * 按 ID 删除给定消息
	 *
	 * @param channel
	 * @param messageId
	 */
	public final void deleteMessageById(TextChannel channel, long messageId) {
		Common.runAsync(() -> {

			// Try updating the message ID in case it has been edited
			final long latestMessageId = this.editedMessages.getOrDefault(messageId, messageId);

			try {
				channel.deleteMessageById(latestMessageId).complete();

			} catch (final Throwable t) {

				// ignore already deleted
				if (!(t instanceof github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException))
					t.printStackTrace();

				else
					Debugger.debug("discord", "Could not remove Discord message in channel '" + channel.getName() + "' id " + latestMessageId
							+ ", it was probably deleted otherwise or this is a bug.");
			}
		});
	}

	/**
	 * 按 ID 编辑给定消息
	 *
	 * @param channel
	 * @param messageId
	 * @param format
	 */
	public final void editMessageById(TextChannel channel, long messageId, String format) {
		Common.runAsync(() -> {
			try {
				final Message message = channel.retrieveMessageById(messageId).complete();

				if (message != null) {

					// Remove old message
					channel.deleteMessageById(messageId).complete();

					// Send a new one
					final Message newSentMessage = channel
							.sendMessage(format.replace("{player}", message.getAuthor().getName()))
							.complete();

					this.editedMessages.put(messageId, newSentMessage.getIdLong());
				}

			} catch (final Throwable t) {
				if (!t.toString().contains("Unknown Message"))
					t.printStackTrace();
			}
		});
	}

	/**
	 * 若发送者已关联账号，尝试将其名称解析为 Minecraft 名称
	 *
	 * @param member
	 * @param author
	 * @return
	 */
	protected final String findPlayerName(Member member, User author) {
		final String discordName = Common.getOrDefaultStrict(member.getNickname(), author.getName());
		final UUID linkedId = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(author.getId());

		final Player player;

		if (linkedId != null)

			// You could potentially look this in offline players too
			// using an async callback to prevent lag if there's tons
			// of players saved or in case of a HTTP request
			player = Remain.getPlayerByUUID(linkedId);

		else
			player = Bukkit.getPlayer(discordName);

		return player != null && player.isOnline() ? player.getName() : discordName;
	}

	/**
	 * 尝试将该玩家名称踢出频道
	 *
	 * @param discordSender
	 * @param reason
	 */
	public final void kickMember(DiscordSender discordSender, String reason) {
		Common.runAsync(() -> {
			try {
				final Member member = DiscordUtil.getMemberById(discordSender.getUser().getId());

				if (member != null)
					member.kick(reason).complete();

			} catch (final HierarchyException ex) {
				Common.log("Unable to kick " + discordSender.getName() + " because he appears to be Discord administrator");
			}
		});
	}

	/**
	 * 用于阻止后续代码执行的内部异常
	 */
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class RemovedMessageException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * 将收到的 Discord 消息分发给所有 {@link DiscordListener} 类
	 *
	 * @deprecated 仅供内部使用
	 */
	@Deprecated
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class DiscordListenerImpl implements Listener {

		@Getter
		private static final DiscordListenerImpl instance = new DiscordListenerImpl();

		/**
		 * 重新加载监听器
		 */
		public void resubscribe() {
			DiscordSRV.api.unsubscribe(this);
			DiscordSRV.api.subscribe(this);
		}

		/**
		 * 注册插件钩子
		 *
		 * https://github.com/kangarko/ChatControl-Red/issues/703
		 */
		public void registerHook() {
			try {
				DiscordSRV.getPlugin().getPluginHooks().add(SimplePlugin::getInstance);

			} catch (final Error err) {
				// Support previous Discord versions
			}
		}

		/**
		 * 将此消息平均分发给所有监听器
		 *
		 * @param event
		 */
		@Subscribe(priority = ListenerPriority.HIGH)
		public void onMessageReceived(DiscordGuildMessagePreProcessEvent event) {
			synchronized (SimplePlugin.getInstance()) {
				for (final DiscordListener listener : registeredListeners)
					try {
						listener.handleMessageReceived(event);

					} catch (final RemovedMessageException ex) {
						// Fail through since we handled that

					} catch (final Throwable t) {
						Common.error(t,
								"Failed to handle DiscordSRV->Minecraft message (pre process)!",
								"Sender: " + event.getAuthor().getName(),
								"Channel: " + event.getChannel().getName(),
								"Message: " + event.getMessage().getContentDisplay());
					}
			}
		}

		/**
		 * 将此消息平均分发给所有监听器
		 *
		 * @param event
		 */
		@Subscribe(priority = ListenerPriority.HIGH)
		public void onMessageReceivedLate(DiscordGuildMessagePostProcessEvent event) {
			synchronized (SimplePlugin.getInstance()) {
				for (final DiscordListener listener : registeredListeners)
					try {
						listener.handleMessageReceivedLate(event);

					} catch (final RemovedMessageException ex) {
						// Fail through since we handled that

					} catch (final Throwable t) {
						Common.error(t,
								"Failed to handle DiscordSRV->Minecraft message (post process)!",
								"Sender: " + event.getAuthor().getName(),
								"Channel: " + event.getChannel().getName(),
								"Message: " + event.getMessage().getContentDisplay());
					}
			}
		}

		/**
		 * 当 DiscordSRV 处理 Minecraft 消息并准备将其
		 * 发送到 Discord 时通知
		 *
		 * @param event
		 */
		@Subscribe(priority = ListenerPriority.HIGH)
		public void onMessageSend(GameChatMessagePreProcessEvent event) {
			synchronized (SimplePlugin.getInstance()) {
				for (final DiscordListener listener : registeredListeners)
					try {
						listener.onMessageSent(event);

					} catch (final RemovedMessageException ex) {
						// Fail through since we handled that

					} catch (final Throwable t) {
						Common.error(t,
								"Failed to handle Minecraft->DiscordSRV message!",
								"Sender: " + event.getPlayer().getName(),
								"Channel: " + event.getChannel(),
								"Message: " + event.getMessage());
					}
			}
		}
	}
}
