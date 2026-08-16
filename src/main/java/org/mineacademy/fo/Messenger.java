package org.mineacademy.fo;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

/**
 * 简化向玩家发送主题消息的流程
 */
@UtilityClass
public class Messenger {

	/**
	 * 是否全局使用 Messenger，例如在命令和监听器中？
	 */
	public static boolean ENABLED = true;

	/**
	 * 发送 info 消息时的前缀
	 */
	@Setter
	@Getter
	private String infoPrefix = "&8&l[&9&li&8&l]&7 ";

	/**
	 * 发送 success 消息时的前缀
	 */
	@Setter
	@Getter
	private String successPrefix = "&8&l[&2&l\u2714&8&l]&7 ";

	/**
	 * 发送 warning 消息时的前缀
	 */
	@Setter
	@Getter
	private String warnPrefix = "&8&l[&6&l!&8&l]&6 ";

	/**
	 * 发送 error 消息时的前缀
	 */
	@Setter
	@Getter
	private String errorPrefix = "&8&l[&4&l\u2715&8&l]&c ";

	/**
	 * 发送 questions 消息时的前缀
	 */
	@Setter
	@Getter
	private String questionPrefix = "&8&l[&a&l?&l&8&l]&7 ";

	/**
	 * 发送 announcements 消息时的前缀
	 */
	@Setter
	@Getter
	private String announcePrefix = "&8&l[&5&l!&l&8&l]&d ";

	/**
	 * 发送以 {@link #getInfoPrefix()} 开头的消息
	 *
	 * @param message
	 */
	public void broadcastInfo(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, infoPrefix, message);
	}

	/**
	 * 发送以 {@link #getSuccessPrefix()} 开头的消息
	 *
	 * @param message
	 */
	public void broadcastSuccess(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, successPrefix, message);
	}

	/**
	 * 发送以 {@link #getWarnPrefix()} 开头的消息
	 *
	 * @param message
	 */
	public void broadcastWarn(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, warnPrefix, message);
	}

	/**
	 * 发送以 {@link #getErrorPrefix()} 开头的消息
	 *
	 * @param message
	 */
	public void broadcastError(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, errorPrefix, message);
	}

	/**
	 * 发送以 {@link #getQuestionPrefix()} 开头的消息
	 *
	 * @param message
	 */
	public void broadcastQuestion(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, questionPrefix, message);
	}

	/**
	 * 发送以 {@link #getAnnouncePrefix()} 开头的消息
	 *
	 * @param message
	 */
	public void broadcastAnnounce(final String message) {
		for (final Player online : Remain.getOnlinePlayers())
			tell(online, announcePrefix, message);
	}

	/**
	 * 发送以 {@link #getInfoPrefix()} 开头的消息
	 *
	 * @param player
	 * @param message
	 */
	public void info(final CommandSender player, final String message) {
		tell(player, infoPrefix, message);
	}

	/**
	 * 发送以 {@link #getSuccessPrefix()} 开头的消息
	 *
	 * @param player
	 * @param message
	 */
	public void success(final CommandSender player, final String message) {
		tell(player, successPrefix, message);
	}

	/**
	 * 发送以 {@link #getWarnPrefix()} 开头的消息
	 *
	 * @param player
	 * @param message
	 */
	public void warn(final CommandSender player, final String message) {
		tell(player, warnPrefix, message);
	}

	/**
	 * 发送以 {@link #getErrorPrefix()} 开头的多条消息
	 *
	 * @param player
	 * @param messages
	 */
	public void error(final CommandSender player, final String... messages) {
		for (final String message : messages)
			error(player, message);
	}

	/**
	 * 发送以 {@link #getErrorPrefix()} 开头的消息
	 *
	 * @param player
	 * @param message
	 */
	public void error(final CommandSender player, final String message) {
		tell(player, errorPrefix, message);
	}

	/**
	 * 发送以 {@link #getQuestionPrefix()} 开头的消息
	 *
	 * @param player
	 * @param message
	 */
	public void question(final CommandSender player, final String message) {
		tell(player, questionPrefix, message);
	}

	/**
	 * 发送以 {@link #getAnnouncePrefix()} 开头的消息
	 *
	 * @param player
	 * @param message
	 */
	public void announce(final CommandSender player, final String message) {
		tell(player, announcePrefix, message);
	}

	/*
	 * Internal method to perform the sending
	 */
	private void tell(final CommandSender player, final String prefix, String message) {

		// Support localization being none or empty
		if (message.isEmpty() || "none".equals(message))
			return;

		final String colorless = Common.stripColors(message);
		boolean noPrefix = ChatUtil.isInteractive(colorless);

		// Special case: Send the prefix for actionbar
		if (colorless.startsWith("<actionbar>"))
			message = message.replace("<actionbar>", "<actionbar>" + prefix);

		if (colorless.startsWith("@noprefix")) {
			message = message.replace("@noprefix", "");

			noPrefix = true;
		}

		// Only insert prefix if the message is sent through the normal chat
		Common.tellNoPrefix(player, (noPrefix ? "" : prefix) + message);
	}

	/**
	 * 将 {plugin_prefix}、{X_prefix} 和 {prefix_X} 替换为相应的 Messenger 变量，
	 * 例如将 {warn_prefix} 替换为 {@link #getWarnPrefix()} 等。
	 *
	 * @param message
	 * @return
	 */
	public static String replacePrefixes(String message) {
		return Replacer.replaceArray(message,
				"plugin_prefix", SimpleSettings.PLUGIN_PREFIX,
				"info_prefix", message.contains(infoPrefix) ? "" : infoPrefix,
				"prefix_info", message.contains(infoPrefix) ? "" : infoPrefix,
				"success_prefix", message.contains(successPrefix) ? "" : successPrefix,
				"prefix_success", message.contains(successPrefix) ? "" : successPrefix,
				"warn_prefix", message.contains(warnPrefix) ? "" : warnPrefix,
				"prefix_warn", message.contains(warnPrefix) ? "" : warnPrefix,
				"error_prefix", message.contains(errorPrefix) ? "" : errorPrefix,
				"prefix_error", message.contains(errorPrefix) ? "" : errorPrefix,
				"question_prefix", message.contains(questionPrefix) ? "" : questionPrefix,
				"prefix_question", message.contains(questionPrefix) ? "" : questionPrefix,
				"announce_prefix", message.contains(announcePrefix) ? "" : announcePrefix,
				"prefix_announce", message.contains(announcePrefix) ? "" : announcePrefix);
	}
}
