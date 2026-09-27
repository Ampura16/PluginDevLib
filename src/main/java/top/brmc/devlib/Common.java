package top.brmc.devlib;

import static org.bukkit.ChatColor.COLOR_CHAR;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.conversations.Conversable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.collection.StrictList;
import top.brmc.devlib.collection.StrictMap;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.exception.RegexTimeoutException;
import top.brmc.devlib.model.HookManager;
import top.brmc.devlib.model.Replacer;
import top.brmc.devlib.model.SimpleRunnable;
import top.brmc.devlib.model.SimpleTask;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.CompChatColor;
import top.brmc.devlib.remain.Remain;
import top.brmc.devlib.settings.ConfigSection;
import top.brmc.devlib.settings.SimpleLocalization;
import top.brmc.devlib.settings.SimpleSettings;

import com.google.gson.Gson;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * 我们的主工具类，包含大量各种便捷功能
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Common {

	// ------------------------------------------------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 默认的 GSON 实例。
	 */
	public static final Gson GSON = new Gson();

	/**
	 * 用于匹配 & 或 {@link CompChatColor#COLOR_CHAR} 颜色的正则
	 */
	private static final Pattern COLOR_AND_DECORATION_REGEX = Pattern.compile("(&|" + COLOR_CHAR + ")[0-9a-fk-orA-FK-OR]");

	/**
	 * 用于匹配 MC 1.16+ 的 #HEX 代码颜色的正则
	 *
	 * 匹配 {#CCCCCC}、&#CCCCCC 或 #CCCCCC
	 */
	public static final Pattern HEX_COLOR_REGEX = Pattern.compile("(?<!\\\\)(\\{|&|)#((?:[0-9a-fA-F]{3}){2})(\\}|)");

	/**
	 * 用于匹配 MC 1.16+ 的 #HEX 代码颜色的正则
	 */
	private static final Pattern RGB_X_COLOR_REGEX = Pattern.compile("(" + COLOR_CHAR + "x)(" + COLOR_CHAR + "[0-9a-fA-F]){6}");

	/**
	 * 高性能颜色正则匹配器，用于 {@link #stripColors(String)}
	 */
	private static final Pattern ALL_IN_ONE = Pattern.compile("((&|" + COLOR_CHAR + ")[0-9a-fk-or])|(" + COLOR_CHAR + "x(" + COLOR_CHAR + "[0-9a-fA-F]){6})|((?<!\\\\)(\\{|&|)#((?:[0-9a-fA-F]{3}){2})(\\}|))");

	/**
	 * 用于向玩家发送不重复的消息，例如当他们试图在限制区域破坏方块时，
	 * 我们不会用 "You cannot break this block here" 刷屏 120 次，
	 * 而是每 X 秒只发送一次。该缓存记录上次发送时间，
	 * 以便知道下次发送前要等多久。
	 */
	private static final Map<String, Long> TIMED_TELL_CACHE = new HashMap<>();

	/**
	 * 见 {@link #TIMED_TELL_CACHE}，但这是向控制台发送消息用的
	 */
	private static final Map<String, Long> TIMED_LOG_CACHE = new HashMap<>();

	// ------------------------------------------------------------------------------------------------------------
	// Tell prefix
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * tell() 方法使用的消息前缀，默认为空
	 */
	@Getter
	private static String tellPrefix = "";

	/**
	 * log() 方法使用的日志前缀，默认为 [PluginName]
	 */
	@Getter
	private static String logPrefix = "[" + SimplePlugin.getNamed() + "]";

	/**
	 * 设置 tell() 方法向玩家发送消息时使用的前缀
	 * <p>
	 * & 颜色代码会自动转换。
	 *
	 * @param prefix
	 */
	public static void setTellPrefix(final String prefix) {
		tellPrefix = prefix == null ? "" : colorize(prefix);
	}

	/**
	 * 设置 log() 方法向控制台发送消息时使用的前缀
	 * <p>
	 * & 颜色代码会自动转换。
	 *
	 * @param prefix
	 */
	public static void setLogPrefix(final String prefix) {
		logPrefix = prefix == null ? "" : colorize(prefix);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Broadcasting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 广播消息，将其中的 {player} 变量替换为给定的命令发送者
	 *
	 * @param message
	 * @param playerReplacement
	 */
	public static void broadcast(final String message, final CommandSender playerReplacement) {
		broadcast(message, resolveSenderName(playerReplacement));
	}

	/**
	 * 广播消息，将其中的 {player} 变量替换为给定的玩家替换名
	 *
	 * @param message
	 * @param playerReplacement
	 */
	public static void broadcast(final String message, final String playerReplacement) {
		broadcast(message.replace("{player}", playerReplacement));
	}

	/**
	 * 向所有人广播消息并记录日志
	 *
	 * @param messages
	 */
	public static void broadcast(final String... messages) {
		if (messages != null)
			for (final String message : messages) {
				for (final Player online : Remain.getOnlinePlayers())
					tellJson(online, message);

				log(message);
			}
	}

	/**
	 * 向所有接收者发送消息
	 *
	 * @param recipients
	 * @param messages
	 */
	public static void broadcastTo(final Iterable<? extends CommandSender> recipients, final String... messages) {
		for (final CommandSender recipient : recipients)
			tell(recipient, messages);
	}

	/**
	 * 向所有有权限的人广播消息
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(final String showPermission, final String message, final boolean log) {
		if (message != null) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, showPermission))
					tellJson(online, message);

			if (log)
				log(message);
		}
	}

	/**
	 * 向所有有权限的人广播消息，不带 {@link #getTellPrefix()} 和 {@link #getLogPrefix()}
	 *
	 * @param showPermission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPermNoPrefix(final String showPermission, final String message, final boolean log) {
		if (message != null) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, showPermission))
					tellNoPrefix(online, message);

			if (log)
				logNoPrefix(message);
		}
	}

	/**
	 * 向所有有权限的人广播文本组件消息
	 *
	 * @param permission
	 * @param message
	 * @param log
	 */
	public static void broadcastWithPerm(final String permission, @NonNull final TextComponent message, final boolean log) {
		final String legacy = message.toLegacyText();

		if (!legacy.equals("none")) {
			for (final Player online : Remain.getOnlinePlayers())
				if (PlayerUtil.hasPerm(online, permission))
					Remain.sendComponent(online, message);

			if (log)
				log(legacy);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 向玩家发送消息并保存发送时间。
	 * 秒数延迟指再次调用本方法时，不向玩家发送
	 * 相同消息的间隔。
	 *
	 * 消息前不会加上 {@link #getTellPrefix()}
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimedNoPrefix(final int delaySeconds, final CommandSender sender, final String message) {
		final String oldPrefix = new String(tellPrefix);
		tellPrefix = "";

		tellTimed(delaySeconds, sender, message);
		tellPrefix = oldPrefix;
	}

	/**
	 * 向玩家发送消息并保存发送时间。
	 * 秒数延迟指再次调用本方法时，不向玩家发送
	 * 相同消息的间隔。
	 *
	 * @param delaySeconds
	 * @param sender
	 * @param message
	 */
	public static void tellTimed(final int delaySeconds, final CommandSender sender, final String message) {

		// No previous message stored, just tell the player now
		if (!TIMED_TELL_CACHE.containsKey(message)) {
			tell(sender, message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_TELL_CACHE.get(message) > delaySeconds) {
			tell(sender, message);

			TIMED_TELL_CACHE.put(message, TimeUtil.currentTimeSeconds());
		}
	}

	/**
	 * 稍后向可交谈对象发送消息
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	public static void tellLaterConversing(final int delayTicks, final Conversable conversable, final String message) {
		runLater(delayTicks, () -> tellConversing(conversable, message));
	}

	/**
	 * 向可交谈玩家发送带颜色的消息
	 *
	 * @param conversable
	 * @param message
	 */
	public static void tellConversing(final Conversable conversable, final String message) {
		final String prefix = message.contains(tellPrefix) || tellPrefix.isEmpty() ? "" : tellPrefix + " ";

		conversable.sendRawMessage(colorize(prefix + message));
	}

	/**
	 * 稍后向可交谈对象发送消息
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	public static void tellLaterConversingNoPrefix(final int delayTicks, final Conversable conversable, final String message) {
		runLater(delayTicks, () -> tellConversingNoPrefix(conversable, message));
	}

	/**
	 * 向可交谈玩家发送带颜色的消息
	 *
	 * @param conversable
	 * @param message
	 */
	public static void tellConversingNoPrefix(final Conversable conversable, final String message) {
		conversable.sendRawMessage(colorize(message));
	}

	/**
	 * 延迟向发送者发送消息，支持 & 颜色代码
	 *
	 * @param sender
	 * @param delayTicks
	 * @param messages
	 */
	public static void tellLater(final int delayTicks, final CommandSender sender, final String... messages) {
		runLater(delayTicks, () -> {
			if (sender instanceof Player && !((Player) sender).isOnline())
				return;

			tell(sender, messages);
		});
	}

	/**
	 * 向发送者发送一堆消息，支持 & 颜色代码，
	 * 不带 {@link #getTellPrefix()} 前缀
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final Collection<String> messages) {
		tellNoPrefix(sender, Common.toArray(messages));
	}

	/**
	 * 向发送者发送一堆消息，支持 & 颜色代码，
	 * 不带 {@link #getTellPrefix()} 前缀
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tellNoPrefix(final CommandSender sender, final String... messages) {
		final String oldPrefix = new String(tellPrefix);

		tellPrefix = "";
		tell(sender, messages);
		tellPrefix = oldPrefix;
	}

	/**
	 * 向发送者发送一堆消息，支持 & 颜色代码
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(final CommandSender sender, final Collection<String> messages) {
		tell(sender, toArray(messages));
	}

	/**
	 * 向发送者发送一堆消息，忽略等于 "none" 或 null 的消息，
	 * 替换 & 颜色代码和 {player} 玩家变量
	 *
	 * @param sender
	 * @param messages
	 */
	public static void tell(final CommandSender sender, final String... messages) {
		for (final String message : messages)
			tellJson(sender, message);
	}

	/**
	 * 向玩家发送消息，替换消息中给定的占位符关联数组
	 *
	 * @param recipient
	 * @param message
	 * @param replacements
	 */
	public static void tellReplaced(CommandSender recipient, String message, Object... replacements) {
		tell(recipient, Replacer.replaceArray(message, replacements));
	}

	/*
	 * Tells the sender a basic message with & colors replaced and {player} with his variable replaced.
	 * <p>
	 * If the message starts with [JSON] than we remove the [JSON] prefix and handle the message
	 * as a valid JSON component.
	 * <p>
	 * Finally, a prefix to non-json messages is added, see {@link #getTellPrefix()}
	 */
	private static void tellJson(@NonNull final CommandSender sender, String message) {
		if (message == null || message.isEmpty() || "none".equals(message))
			return;

		// Has prefix already? This is replaced when colorizing
		final boolean hasPrefix = message.contains("{prefix}");
		final boolean hasJSON = message.startsWith("[JSON]");

		// Replace player
		message = message.replace("{player}", resolveSenderName(sender));

		// Replace colors
		if (!hasJSON)
			message = colorize(message);

		// Send [JSON] prefixed messages as json component
		if (hasJSON) {
			final String stripped = message.replace("[JSON]", "").trim();

			if (!stripped.isEmpty())
				Remain.sendJson(sender, stripped);

		} else if (message.startsWith("<actionbar>")) {
			final String stripped = message.replace("<actionbar>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					Remain.sendActionBar((Player) sender, stripped);
				else
					tellJson(sender, stripped);

		} else if (message.startsWith("<toast>")) {
			final String stripped = message.replace("<toast>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					Remain.sendToast((Player) sender, stripped);
				else
					tellJson(sender, stripped);

		} else if (message.startsWith("<title>")) {
			final String stripped = message.replace("<title>", "");

			if (!stripped.isEmpty()) {
				final String[] split = stripped.split("\\|");
				final String title = split[0];
				final String subtitle = split.length > 1 ? Common.joinRange(1, split) : null;

				if (sender instanceof Player)
					Remain.sendTitle((Player) sender, title, subtitle);

				else {
					tellJson(sender, title);

					if (subtitle != null)
						tellJson(sender, subtitle);
				}
			}

		} else if (message.startsWith("<bossbar>")) {
			final String stripped = message.replace("<bossbar>", "");

			if (!stripped.isEmpty())
				if (sender instanceof Player)
					// cannot provide time here so we show it for 10 seconds
					Remain.sendBossbarTimed((Player) sender, stripped, 10);
				else
					tellJson(sender, stripped);

		} else
			for (final String part : message.split("\n")) {
				final String prefix = !hasPrefix && !tellPrefix.isEmpty() ? tellPrefix + " " : "";
				final String toSend = part.startsWith("<center>") ? ChatUtil.center(prefix + part.replace("<center>", "")) : prefix + part;

				// Make player engaged in a server conversation still receive the message
				if (sender instanceof Conversable && ((Conversable) sender).isConversing())
					((Conversable) sender).sendRawMessage(toSend);

				else
					sender.sendMessage(toSend);
			}
	}

	/**
	 * 若发送者是玩家或 Discord 发送者则返回其名称，若是控制台则返回 {@link SimpleLocalization#CONSOLE_NAME}
	 *
	 * @param sender
	 * @return
	 */
	public static String resolveSenderName(final CommandSender sender) {
		return sender instanceof ConsoleCommandSender ? SimpleLocalization.CONSOLE_NAME : sender != null ? sender.getName() : "";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Colorizing messages
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 替换列表中每个字符串的 & 颜色
	 * 会创建只包含非 null 值的新列表
	 *
	 * @param list
	 * @return
	 */
	public static List<String> colorize(final List<String> list) {
		final List<String> copy = new ArrayList<>();
		copy.addAll(list);

		for (int i = 0; i < copy.size(); i++) {
			final String message = copy.get(i);

			if (message != null)
				copy.set(i, colorize(message));
		}

		return copy;
	}

	/**
	 * 将消息中的 & 字母替换为 {@link CompChatColor#COLOR_CHAR}。
	 *
	 * @param messages 要将颜色代码替换为 '&' 的消息
	 * @return 带颜色的消息
	 */
	public static String colorize(final String... messages) {
		return colorize(String.join("\n", messages));
	}

	/**
	 * 将消息中的 & 字母替换为 {@link CompChatColor#COLOR_CHAR}。
	 *
	 * @param messages 要将颜色代码替换为 '&' 的消息
	 * @return 带颜色的消息
	 */
	public static String[] colorizeArray(final String... messages) {

		for (int i = 0; i < messages.length; i++)
			messages[i] = colorize(messages[i]);

		return messages;
	}

	/**
	 * 将消息中的 & 字母替换为 {@link CompChatColor#COLOR_CHAR}。
	 * <p>
	 * 还会将 {prefix} 替换为 {@link #getTellPrefix()}，将 {server} 替换为 {@link SimpleLocalization#SERVER_PREFIX}
	 *
	 * @param message 要将颜色代码替换为 '&' 的消息
	 * @return 带颜色的消息
	 */
	public static String colorize(final String message) {
		if (message == null || message.isEmpty())
			return "";

		String result = CompChatColor.translateColorCodes(message
				.replace("{prefix}", message.startsWith(tellPrefix) ? "" : tellPrefix)
				.replace("{server}", SimpleLocalization.SERVER_PREFIX)
				.replace("{plugin_name}", SimplePlugin.getNamed())
				.replace("{plugin_version}", SimplePlugin.getVersion()));

		// Replace hex colors on 1.16+ or find the closest color for legacy versions
		final Matcher match = HEX_COLOR_REGEX.matcher(result);

		while (match.find()) {
			final String matched = match.group();
			final String colorCode = match.group(2);
			String replacement = "";

			try {
				replacement = CompChatColor.of("#" + colorCode).toString();

			} catch (final IllegalArgumentException ex) {
			}

			result = result.replaceAll(Pattern.quote(matched), replacement);
		}

		if (result.contains("\\\\#"))
			result = result.replace("\\\\#", "\\#");

		else if (result.contains("\\#"))
			result = result.replace("\\#", "#");

		return result;
	}

	/**
	 * 将 {@link ChatColor#COLOR_CHAR} 颜色替换为 & 字母
	 *
	 * @param messages
	 * @return
	 */
	public static String[] revertColorizing(final String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = revertColorizing(messages[i]);

		return messages;
	}

	/**
	 * 将 {@link ChatColor#COLOR_CHAR} 颜色替换为 & 字母
	 *
	 * @param message
	 * @return
	 */
	public static String revertColorizing(final String message) {
		return message.replaceAll("(?i)" + ChatColor.COLOR_CHAR + "([0-9a-fk-or])", "&$1");
	}

	/**
	 * 移除消息中所有的 {@link ChatColor#COLOR_CHAR} 和 & 字母颜色
	 *
	 * @param message
	 * @return
	 */
	public static String stripColors(String message) {
		if (message == null || message.isEmpty())
			return message;

		// Replace & color codes
		final Matcher matcher = ALL_IN_ONE.matcher(message);

		while (matcher.find())
			message = matcher.replaceAll("");

		// Replace hex colors, both raw and parsed
		/*if (Remain.hasHexColors()) {
			matcher = HEX_COLOR_REGEX.matcher(message);
		
			while (matcher.find())
				message = matcher.replaceAll("");
		
			matcher = RGB_X_COLOR_REGEX.matcher(message);
		
			while (matcher.find())
				message = matcher.replaceAll("");
		
			message = message.replace(ChatColor.COLOR_CHAR + "x", "");
		}*/

		return message;
	}

	/**
	 * 只移除消息中的 & 颜色
	 *
	 * @param message
	 * @return
	 */
	public static String stripColorsLetter(final String message) {
		return message == null ? "" : message.replaceAll("&([0-9a-fk-orA-F-K-OR])", "");
	}

	/**
	 * 返回消息是否包含 {@link CompChatColor#COLOR_CHAR} 或 & 字母颜色
	 *
	 * @param message
	 * @return
	 */
	public static boolean hasColors(final String message) {
		return COLOR_AND_DECORATION_REGEX.matcher(message).find();
	}

	/**
	 * 返回给定消息中的最后一个颜色，& 或 {@link ChatColor#COLOR_CHAR}
	 *
	 * @param message 消息，若没有则为空
	 * @return
	 */
	public static String lastColor(final String message) {

		// RGB colors
		if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_16)) {
			final int c = message.lastIndexOf(ChatColor.COLOR_CHAR);
			final Matcher match = RGB_X_COLOR_REGEX.matcher(message);

			String lastColor = null;

			while (match.find())
				lastColor = match.group(0);

			if (lastColor != null)
				if (c == -1 || c < message.lastIndexOf(lastColor) + lastColor.length())
					return lastColor;
		}

		final String andLetter = lastColorLetter(message);
		final String colorChat = lastColorChar(message);

		return !andLetter.isEmpty() ? andLetter : !colorChat.isEmpty() ? colorChat : "";
	}

	/**
	 * 返回消息中最后一个 & 颜色 + 颜色字母，若不存在则为空
	 *
	 * @param message
	 * @return
	 */
	public static String lastColorLetter(final String message) {
		return lastColor(message, '&');
	}

	/**
	 * 返回消息中最后一个 {@link ChatColor#COLOR_CHAR} + 颜色字母，若不存在则为空
	 *
	 * @param message
	 * @return
	 */
	public static String lastColorChar(final String message) {
		return lastColor(message, ChatColor.COLOR_CHAR);
	}

	private static String lastColor(final String msg, final char colorChar) {
		final int c = msg.lastIndexOf(colorChar);

		// Contains our character
		if (c != -1) {

			// Contains a character after color character
			if (msg.length() > c + 1)
				if (msg.substring(c + 1, c + 2).matches("([0-9a-fk-or])"))
					return msg.substring(c, c + 2).trim();

			// Search after colors before that invalid character
			return lastColor(msg.substring(0, c), colorChar);
		}

		return "";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Aesthetics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回一条长 ------ 控制台分隔线
	 *
	 * @return
	 */
	public static String consoleLine() {
		return "!-----------------------------------------------------!";
	}

	/**
	 * 返回一条长 ______ 控制台分隔线
	 *
	 * @return
	 */
	public static String consoleLineSmooth() {
		return "______________________________________________________________";
	}

	/**
	 * 返回一条长 -------- 聊天分隔线
	 *
	 * @return
	 */
	public static String chatLine() {
		return "*---------------------------------------------------*";
	}

	/**
	 * 返回一条带删除线效果的长 &m----------- 聊天分隔线
	 *
	 * @return
	 */
	public static String chatLineSmooth() {
		return "&m-----------------------------------------------------";
	}

	/**
	 * 返回一条很长的 -------- 配置分隔线
	 *
	 * @return
	 */
	public static String configLine() {
		return "-------------------------------------------------------------------------------------------";
	}

	/**
	 * 返回给定横线数量的 |------------| 记分板分隔线
	 *
	 * @param length
	 * @return
	 */
	public static String scoreboardLine(final int length) {
		String fill = "";

		for (int i = 0; i < length; i++)
			fill += "-";

		return "&m|" + fill + "|";
	}

	/**
	 * 打印数量及列表实际内容的便捷方法。
	 * 例如：
	 * "X bosses: Creeper, Zombie
	 *
	 * @param <T>
	 * @param iterable
	 * @param ofWhat
	 * @return
	 */
	public static <T> String plural(final Collection<T> iterable, final String ofWhat) {
		return plural(iterable.size(), ofWhat) + ": " + join(iterable);
	}

	/**
	 * 若数量为 0 或大于 1，给给定字符串加 "s"
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String plural(final long count, final String ofWhat) {
		final String exception = getException(count, ofWhat);

		return exception != null ? exception : count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("s") ? "s" : "");
	}

	/**
	 * 若数量为 0 或大于 1，给给定字符串加 "es"
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralEs(final long count, final String ofWhat) {
		final String exception = getException(count, ofWhat);

		return exception != null ? exception : count + " " + ofWhat + (count == 0 || count > 1 && !ofWhat.endsWith("es") ? "es" : "");
	}

	/**
	 * 若数量为 0 或大于 1，给给定字符串加 "ies"
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 */
	public static String pluralIes(final long count, final String ofWhat) {
		final String exception = getException(count, ofWhat);

		return exception != null ? exception : count + " " + (count == 0 || count > 1 && !ofWhat.endsWith("ies") ? ofWhat.substring(0, ofWhat.length() - 1) + "ies" : ofWhat);
	}

	/**
	 * 从例外列表返回复数词，若没有则返回 null
	 *
	 * @param count
	 * @param ofWhat
	 * @return
	 * @deprecated 只包含极有限的最常用英语复数不规则变化
	 */
	@Deprecated
	private static String getException(final long count, final String ofWhat) {
		final SerializedMap exceptions = SerializedMap.ofArray(
				"life", "lives",
				"class", "classes",
				"wolf", "wolves",
				"knife", "knives",
				"wife", "wives",
				"calf", "calves",
				"leaf", "leaves",
				"potato", "potatoes",
				"tomato", "tomatoes",
				"hero", "heroes",
				"torpedo", "torpedoes",
				"veto", "vetoes",
				"foot", "feet",
				"tooth", "teeth",
				"goose", "geese",
				"man", "men",
				"woman", "women",
				"mouse", "mice",
				"die", "dice",
				"ox", "oxen",
				"child", "children",
				"person", "people",
				"penny", "pence",
				"sheep", "sheep",
				"fish", "fish",
				"deer", "deer",
				"moose", "moose",
				"swine", "swine",
				"buffalo", "buffalo",
				"shrimp", "shrimp",
				"trout", "trout",
				"spacecraft", "spacecraft",
				"cactus", "cacti",
				"axis", "axes",
				"analysis", "analyses",
				"crisis", "crises",
				"thesis", "theses",
				"datum", "data",
				"index", "indices",
				"entry", "entries",
				"boss", "bosses",
				"iron", "iron",
				"Iron", "Iron",
				"gold", "gold",
				"Gold", "Gold");

		return exceptions.containsKey(ofWhat) ? count + " " + (count == 0 || count > 1 ? exceptions.getString(ofWhat) : ofWhat) : null;
	}

	/**
	 * 给给定字符串加上 "a" 或 "an"（只做简单的音节检查）
	 *
	 * @param ofWhat
	 * @return
	 * @deprecated 只是简单的音节检查，例如会返回 a hour
	 */
	@Deprecated
	public static String article(final String ofWhat) {
		Valid.checkBoolean(ofWhat.length() > 0, "String cannot be empty");
		final List<String> syllables = Arrays.asList("a", "e", "i", "o", "u", "y");

		return (syllables.contains(ofWhat.toLowerCase().trim().substring(0, 1)) ? "an" : "a") + " " + ofWhat;
	}

	/**
	 * 生成表示进度的条形。例如：
	 * <p>
	 * ##-----
	 * ###----
	 * ####---
	 *
	 * @param min            最小进度
	 * @param minChar
	 * @param max            最大进度
	 * @param maxChar
	 * @param delimiterColor
	 * @return
	 */
	public static String fancyBar(final int min, final char minChar, final int max, final char maxChar, final ChatColor delimiterColor) {
		String formatted = "";

		for (int i = 0; i < min; i++)
			formatted += minChar;

		formatted += delimiterColor;

		for (int i = 0; i < max - min; i++)
			formatted += maxChar;

		return formatted;
	}

	/**
	 * 将向量位置格式化为一位小数
	 *
	 * 请勿用于保存，仅用于调试
	 * 保存向量请使用 {@link SerializeUtil#serialize(Object)}
	 *
	 * @param vec
	 * @return
	 */
	public static String shortLocation(final Vector vec) {
		return " [" + MathUtil.formatOneDigit(vec.getX()) + ", " + MathUtil.formatOneDigit(vec.getY()) + ", " + MathUtil.formatOneDigit(vec.getZ()) + "]";
	}

	/**
	 * 将给定位置格式化为无小数的方块坐标
	 *
	 * 请勿用于保存，仅用于调试
	 * 保存位置请使用 {@link SerializeUtil#serialize(Object)}
	 *
	 * @param location
	 * @return
	 */
	public static String shortLocation(final Location location) {
		if (location == null)
			return "Location(null)";

		if (location.equals(new Location(null, 0, 0, 0)))
			return "Location(null, 0, 0, 0)";

		Valid.checkNotNull(location.getWorld(), "Cannot shorten a location with null world!");

		return Replacer.replaceArray(SimpleSettings.LOCATION_FORMAT,
				"world", location.getWorld().getName(),
				"x", location.getBlockX(),
				"y", location.getBlockY(),
				"z", location.getBlockZ());
	}

	/**
	 * 将给定文本重复给定次数的超简单辅助方法。
	 *
	 * 例如：duplicate("apple", 2) 将产生 "appleapple"
	 *
	 * @param text
	 * @param nTimes
	 * @return
	 */
	public static String duplicate(String text, int nTimes) {
		if (nTimes == 0)
			return "";

		final String toDuplicate = new String(text);

		for (int i = 1; i < nTimes; i++)
			text += toDuplicate;

		return text;
	}

	/**
	 * 将字符串限制在给定最大长度内，
	 * 截断时在末尾追加 "..."
	 *
	 * @param text
	 * @param maxLength
	 * @return
	 */
	public static String limit(String text, int maxLength) {
		final int length = text.length();

		return maxLength >= length ? text : text.substring(0, maxLength) + "...";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Plugins management
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 检查插件是否已启用。我们还会调度一个异步任务，
	 * 确保服务器启动完毕时插件已正确加载
	 * <p>
	 * 若已加载则返回 true（不代表它工作正常）
	 *
	 * @param pluginName
	 * @return
	 */
	public static boolean doesPluginExist(final String pluginName) {
		Plugin lookup = null;

		for (final Plugin otherPlugin : Bukkit.getPluginManager().getPlugins())
			if (otherPlugin.getDescription().getName().equals(pluginName)) {
				lookup = otherPlugin;

				break;
			}

		final Plugin found = lookup;

		if (found == null)
			return false;

		if (!found.isEnabled())
			runLaterAsync(0, () -> Valid.checkBoolean(found.isEnabled(), SimplePlugin.getNamed() + " could not hook into " + pluginName + " as the plugin is disabled! (DO NOT REPORT THIS TO " + SimplePlugin.getNamed() + ", look for errors above and contact support of '" + pluginName + "')"));

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Running commands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 从命令中找到插件命令。
	 *
	 * 命令可以只是标签，如 "/give" 或 "give"，
	 * 也可以是完整命令，如 "/give kangarko diamonds"，这种情况下
	 * 我们会找出标签，只匹配 "/give"
	 *
	 * @param command
	 * @return
	 */
	public static Command findCommand(final String command) {
		final String[] args = command.split(" ");

		if (args.length > 0) {
			String label = args[0].toLowerCase();

			if (label.startsWith("/"))
				label = label.substring(1);

			for (final Plugin otherPlugin : Bukkit.getPluginManager().getPlugins()) {
				final JavaPlugin plugin = (JavaPlugin) otherPlugin;

				if (plugin instanceof JavaPlugin) {
					final Command pluginCommand = plugin.getCommand(label);

					if (pluginCommand != null)
						return pluginCommand;
				}
			}

			final Command serverCommand = Remain.getCommandMap().getCommand(label);

			if (serverCommand != null)
				return serverCommand;
		}

		return null;
	}

	/**
	 * 以控制台身份运行给定命令（不带 /），将 {player} 替换为发送者
	 *
	 * 你可以在命令前加 @(announce|warn|error|info|question|success) 前缀，直接向玩家替换名发送格式化
	 * 消息。
	 *
	 * @param playerReplacement
	 * @param command
	 */
	public static void dispatchCommand(@Nullable CommandSender playerReplacement, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		if (command.startsWith("@announce ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @announce without a player in: " + command);

			Messenger.announce(playerReplacement, command.replace("@announce ", ""));
		}

		else if (command.startsWith("@warn ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @warn without a player in: " + command);

			Messenger.warn(playerReplacement, command.replace("@warn ", ""));
		}

		else if (command.startsWith("@error ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @error without a player in: " + command);

			Messenger.error(playerReplacement, command.replace("@error ", ""));
		}

		else if (command.startsWith("@info ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @info without a player in: " + command);

			Messenger.info(playerReplacement, command.replace("@info ", ""));
		}

		else if (command.startsWith("@question ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @question without a player in: " + command);

			Messenger.question(playerReplacement, command.replace("@question ", ""));
		}

		else if (command.startsWith("@success ")) {
			Valid.checkNotNull(playerReplacement, "Cannot use @success without a player in: " + command);

			Messenger.success(playerReplacement, command.replace("@success ", ""));
		}

		else {
			command = command.startsWith("/") && !command.startsWith("//") ? command.substring(1) : command;
			command = command.replace("{player}", playerReplacement == null ? "" : resolveSenderName(playerReplacement));

			// Workaround for JSON in tellraw getting HEX colors replaced
			if (!command.startsWith("tellraw"))
				command = colorize(command);

			checkBlockedCommands(playerReplacement, command);

			final String finalCommand = command;

			if (Bukkit.isPrimaryThread())
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);

			else
				runLater(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
		}
	}

	/**
	 * 让发送者亲自键入般运行给定命令（不带 /），将 {player} 替换为他的名字
	 *
	 * @param playerSender
	 * @param command
	 */
	public static void dispatchCommandAsPlayer(@NonNull final Player playerSender, @NonNull String command) {
		if (command.isEmpty() || command.equalsIgnoreCase("none"))
			return;

		// Remove trailing /
		if (command.startsWith("/") && !command.startsWith("//"))
			command = command.substring(1);

		checkBlockedCommands(playerSender, command);

		final String finalCommand = colorize(command.replace("{player}", resolveSenderName(playerSender)));

		if (Bukkit.isPrimaryThread())
			playerSender.performCommand(finalCommand);

		else if (Remain.isFolia())
			playerSender.getScheduler().run(SimplePlugin.getInstance(), task -> {
				playerSender.performCommand(finalCommand);
			}, () -> {
			});

		else
			runLater(() -> playerSender.performCommand(finalCommand));
	}

	/*
	 * A pitiful attempt at blocking a few known commands which might be used for malicious intent.
	 * We log the attempt to a file for manual review.
	 */
	private static boolean checkBlockedCommands(@Nullable CommandSender sender, String command) {
		if (command.startsWith("op ") || command.startsWith("minecraft:op ")) {
			final String errorMessage = (sender != null ? sender.getName() : "Console") + " tried to run blocked command: " + command;
			FileUtil.writeFormatted("blocked-commands.log", errorMessage);

			throw new FoException(errorMessage);
		}

		return false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Logging and error handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 记录消息并保存记录时间。若在秒数延迟内调用本方法记录
	 * 完全相同的消息，则不会记录。
	 * <p>
	 * 避免控制台刷屏。
	 *
	 * @param delaySec
	 * @param msg
	 */
	public static void logTimed(final int delaySec, final String msg) {
		if (!TIMED_LOG_CACHE.containsKey(msg)) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
			return;
		}

		if (TimeUtil.currentTimeSeconds() - TIMED_LOG_CACHE.get(msg) > delaySec) {
			log(msg);
			TIMED_LOG_CACHE.put(msg, TimeUtil.currentTimeSeconds());
		}
	}

	/**
	 * 与 {@link String#format(String, Object...)} 类似，但
	 * 所有参数都会被展开，因此玩家名会正确给出、位置会被缩短等。
	 *
	 * @param format
	 * @param args
	 */
	public static void logF(final String format, @NonNull final Object... args) {
		final String formatted = format(format, args);

		log(false, formatted);
	}

	/**
	 * 将无聊的 CraftPlayer{name=noob} 替换为正确的玩家名，
	 * 对实体、世界和位置同样有效
	 * <p>
	 * 用法示例：format("Hello %s from world %s", player, player.getWorld())
	 *
	 * @param format
	 * @param args
	 * @return
	 */
	public static String format(final String format, @NonNull final Object... args) {
		for (int i = 0; i < args.length; i++) {
			final Object arg = args[i];

			if (arg != null)
				args[i] = simplify(arg);
		}

		return String.format(format, args);
	}

	/**
	 * 简单的辅助方法，给给定消息加上 "&cWarning: &f"
	 * 并将它记录下来。
	 *
	 * @param message
	 */
	public static void warning(String message) {
		log("&cWarning: &7" + message);
	}

	/**
	 * 向控制台记录一堆消息，支持 & 颜色
	 *
	 * @param messages
	 */
	public static void log(final List<String> messages) {
		log(toArray(messages));
	}

	/**
	 * 向控制台记录一堆消息，支持 & 颜色
	 *
	 * @param messages
	 */
	public static void log(final String... messages) {
		log(true, messages);
	}

	/**
	 * 向控制台记录一堆消息，支持 & 颜色
	 * <p>
	 * 不添加 {@link #getLogPrefix()}
	 *
	 * @param messages
	 */
	public static void logNoPrefix(final String... messages) {
		log(false, messages);
	}

	/*
	 * Logs a bunch of messages to the console, & colors are supported
	 */
	private static void log(final boolean addLogPrefix, final String... messages) {
		if (messages == null)
			return;

		final CommandSender console = Bukkit.getConsoleSender();
		Valid.checkNotNull(console, "Failed to initialize Console Sender, are you running Foundation under a Bukkit/Spigot server?");

		for (String message : messages) {
			if (message == null || "none".equals(message))
				continue;

			if (message.replace(" ", "").isEmpty()) {
				console.sendMessage("  ");

				continue;
			}

			message = colorize(message);

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					log(Remain.toLegacyText(stripped, false));

			} else
				for (final String part : message.split("\n")) {
					final String log = (addLogPrefix && !logPrefix.isEmpty() ? logPrefix + " " : "") + getOrEmpty(part);

					console.sendMessage(log);
				}
		}
	}

	/**
	 * 向控制台在 {@link #consoleLine()} 边框内记录一堆消息。
	 *
	 * @param messages
	 */
	public static void logFramed(final String... messages) {
		logFramed(false, messages);
	}

	/**
	 * 向控制台在 {@link #consoleLine()} 边框内记录一堆消息。
	 * <p>
	 * 出错时使用，还可以禁用插件
	 *
	 * @param disablePlugin
	 * @param messages
	 */
	public static void logFramed(final boolean disablePlugin, final String... messages) {
		if (messages != null && !Valid.isNullOrEmpty(messages)) {
			log("&7" + consoleLine());
			for (final String msg : messages)
				log(" &c" + msg);

			if (disablePlugin)
				log(" &cPlugin is now disabled.");

			log("&7" + consoleLine());
		}

		if (disablePlugin)
			Bukkit.getPluginManager().disablePlugin(SimplePlugin.getInstance());
	}

	/**
	 * 保存错误、打印堆栈并在边框内记录。
	 * 可使用 %error 变量
	 *
	 * @param throwable
	 * @param messages
	 */
	public static void error(@NonNull Throwable throwable, String... messages) {

		if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
			throwable = throwable.getCause();

		if (!(throwable instanceof FoException))
			Debugger.saveError(throwable, messages);

		Debugger.printStackTrace(throwable);
		logFramed(replaceErrorVariable(throwable, messages));
	}

	/**
	 * 在边框内记录消息（若不为 null），
	 * 将错误保存到 errors.log 然后抛出
	 * <p>
	 * 可使用 %error 变量
	 *
	 * @param t
	 * @param messages
	 */
	public static void throwError(Throwable t, final String... messages) {

		// Delegate to only print out the relevant stuff
		if (t instanceof FoException)
			throw (FoException) t;

		if (messages != null)
			logFramed(false, replaceErrorVariable(t, messages));

		Debugger.saveError(t, messages);

		t.printStackTrace();

		Remain.sneaky(t);
	}

	/*
	 * Replace the %error variable with a smart error info, see above
	 */
	private static String[] replaceErrorVariable(Throwable throwable, final String... msgs) {
		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

		for (int i = 0; i < msgs.length; i++) {
			final String error = throwableName + throwableMessage;

			msgs[i] = msgs[i]
					.replace("%error%", error)
					.replace("%error", error);
		}

		return msgs;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Regular expressions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若给定正则匹配给定消息则返回 true
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(final String regex, final String message) {
		return regExMatch(compilePattern(regex), message);
	}

	/**
	 * 若给定模式匹配给定消息则返回 true
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static boolean regExMatch(final Pattern regex, final String message) {
		return regExMatch(compileMatcher(regex, message));
	}

	/**
	 * 若给定匹配器匹配则返回 true。我们还会评估
	 * 评估耗时，若太长则停止，
	 * 见 {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param matcher
	 * @return
	 */
	public static boolean regExMatch(final Matcher matcher) {
		Valid.checkNotNull(matcher, "Cannot call regExMatch on null matcher");

		try {
			return matcher.find();

		} catch (final RegexTimeoutException ex) {
			handleRegexTimeoutException(ex, matcher.pattern());

			return false;
		}
	}

	/**
	 * 为给定模式和消息编译匹配器。颜色会被剥离。
	 * <p>
	 * 我们还会评估耗时，若太长则停止，
	 * 见 {@link SimplePlugin#getRegexTimeout()}
	 *
	 * @param pattern
	 * @param message
	 * @return
	 */
	public static Matcher compileMatcher(@NonNull final Pattern pattern, final String message) {

		try {
			final SimplePlugin instance = SimplePlugin.getInstance();

			String strippedMessage = instance.regexStripColors() ? stripColors(message) : message;
			strippedMessage = instance.regexStripAccents() ? ChatUtil.replaceDiacritic(strippedMessage) : strippedMessage;

			return pattern.matcher(TimedCharSequence.withSettingsLimit(strippedMessage));

		} catch (final RegexTimeoutException ex) {
			handleRegexTimeoutException(ex, pattern);

			return null;
		}
	}

	/**
	 * 为给定正则和消息编译匹配器
	 *
	 * @param regex
	 * @param message
	 * @return
	 */
	public static Matcher compileMatcher(final String regex, final String message) {
		return compileMatcher(compilePattern(regex), message);
	}

	/**
	 * 从给定正则编译模式，剥离颜色并使其
	 * 不区分大小写
	 *
	 * @param regex
	 * @return
	 */
	public static Pattern compilePattern(String regex) {
		final SimplePlugin instance = SimplePlugin.getInstance();
		Pattern pattern = null;

		regex = instance.regexStripColors() ? stripColors(regex) : regex;
		regex = instance.regexStripAccents() ? ChatUtil.replaceDiacritic(regex) : regex;

		try {

			if (instance.regexCaseInsensitive())
				pattern = Pattern.compile(regex, instance.regexUnicode() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : Pattern.CASE_INSENSITIVE);

			else
				pattern = instance.regexUnicode() ? Pattern.compile(regex, Pattern.UNICODE_CASE) : Pattern.compile(regex);

		} catch (final PatternSyntaxException ex) {
			throwError(ex,
					"Your regular expression is malformed!",
					"Expression: '" + regex + "'",
					"",
					"IF YOU CREATED IT YOURSELF, we unfortunately",
					"can't provide support for custom expressions.",
					"Use online services like regex101.com to put your",
					"expression there (without '') and discover where",
					"the syntax error lays and how to fix it.");

			return null;
		}

		return pattern;
	}

	/**
	 * 处理正则超时异常的特殊调用，请勿使用
	 *
	 * @param ex
	 * @param pattern
	 */
	public static void handleRegexTimeoutException(RegexTimeoutException ex, Pattern pattern) {
		final boolean caseInsensitive = SimplePlugin.getInstance().regexCaseInsensitive();

		Common.error(ex,
				"A regular expression took too long to process, and was",
				"stopped to prevent freezing your server.",
				" ",
				"Limit " + SimpleSettings.REGEX_TIMEOUT + "ms ",
				"Expression: '" + (pattern == null ? "unknown" : pattern.pattern()) + "'",
				"Evaluated message: '" + ex.getCheckedMessage() + "'",
				" ",
				"IF YOU CREATED THAT RULE YOURSELF, we unfortunately",
				"can't provide support for custom expressions.",
				" ",
				"Sometimes, all you need doing is increasing timeout",
				"limit in your settings.yml",
				" ",
				"Use services like regex101.com to test and fix it.",
				"Put the expression without '' and the message there.",
				"Ensure to turn flags 'insensitive' and 'unicode' " + (caseInsensitive ? "on" : "off"),
				"on there when testing: https://i.imgur.com/PRR5Rfn.png");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Joining strings and lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 将数组列表连接成一个大数组
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	@SafeVarargs
	public static <T> Object[] joinArrays(final T[]... arrays) {
		final List<T> all = new ArrayList<>();

		for (final T[] array : arrays)
			for (final T element : array)
				all.add(element);

		return all.toArray();
	}

	/**
	 * 将数组列表连接成一个大列表
	 *
	 * @param <T>
	 * @param arrays
	 * @return
	 */
	@SafeVarargs
	public static <T> List<T> joinLists(final Iterable<T>... arrays) {
		final List<T> all = new ArrayList<>();

		for (final Iterable<T> array : arrays)
			for (final T element : array)
				all.add(element);

		return all;
	}

	/**
	 * 将命令发送者数组转换为其名称数组的便捷方法，
	 * 给定玩家除外
	 *
	 * @param <T>
	 * @param array
	 * @param nameToIgnore
	 * @return
	 */
	public static <T extends CommandSender> String joinPlayersExcept(final Iterable<T> array, final String nameToIgnore) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			if (!next.getName().equals(nameToIgnore))
				message += next.getName() + (it.hasNext() ? ", " : "");
		}

		return message.endsWith(", ") ? message.substring(0, message.length() - 2) : message;
	}

	/**
	 * 返回给定枚举所有键名的特殊方法。该枚举的
	 * 每个常量必须有 "getKey()" 方法。
	 *
	 * 例如返回："apple, banana, carrot" 等。
	 *
	 * @param <T>
	 * @param enumeration
	 * @return
	 */
	public static <T extends Enum<?>> String keys(Class<T> enumeration) {
		return Common.join(enumeration.getEnumConstants(), (Stringer<T>) object -> ReflectionUtil.invoke("getKey", object));
	}

	/**
	 * 从给定起始下标开始，用空格连接数组
	 *
	 * @param startIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(final int startIndex, final String[] array) {
		return joinRange(startIndex, array.length, array);
	}

	/**
	 * 用给定范围，用空格连接数组
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(final int startIndex, final int stopIndex, final String[] array) {
		return joinRange(startIndex, stopIndex, array, " ");
	}

	/**
	 * 用给定分隔符连接数组
	 *
	 * @param start
	 * @param stop
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static String joinRange(final int start, final int stop, final String[] array, final String delimiter) {
		String joined = "";

		for (int i = start; i < MathUtil.range(stop, 0, array.length); i++)
			joined += (joined.isEmpty() ? "" : delimiter) + array[i];

		return joined;
	}

	/**
	 * 将对象数组转换为字符串数组的便捷方法
	 * 每个非 null 对象调用 "toString"，为 null 则返回 ""
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String join(final T[] array) {
		return array == null ? "null" : join(Arrays.asList(array));
	}

	/**
	 * 将对象列表转换为字符串数组的便捷方法
	 * 每个非 null 对象调用 "toString"，为 null 则返回 ""
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String join(final Iterable<T> array) {
		return array == null ? "null" : join(array, ", ");
	}

	/**
	 * 将对象列表转换为字符串数组的便捷方法
	 * 每个非 null 对象调用 "toString"，为 null 则返回 ""
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static <T> String join(final T[] array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
	}

	/**
	 * 将对象列表转换为字符串数组的便捷方法
	 * 每个非 null 对象调用 "toString"，为 null 则返回 ""
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static <T> String join(final Iterable<T> array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
	}

	/**
	 * 使用 ", " 分隔符和辅助接口连接给定类型的数组，
	 * 将数组中每个元素转为字符串
	 *
	 * @param <T>
	 * @param array
	 * @param stringer
	 * @return
	 */
	public static <T> String join(final T[] array, final Stringer<T> stringer) {
		return join(array, ", ", stringer);
	}

	/**
	 * 使用给定分隔符和辅助接口连接给定类型的数组，
	 * 将数组中每个元素转为字符串
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static <T> String join(final T[] array, final String delimiter, final Stringer<T> stringer) {
		Valid.checkNotNull(array, "Cannot join null array!");

		return join(Arrays.asList(array), delimiter, stringer);
	}

	/**
	 * 使用逗号分隔符和辅助接口连接给定类型的列表，
	 * 将数组中每个元素转为字符串
	 *
	 * @param <T>
	 * @param array
	 * @param stringer
	 * @return
	 */
	public static <T> String join(final Iterable<T> array, final Stringer<T> stringer) {
		return join(array, ", ", stringer);
	}

	/**
	 * 使用给定分隔符和辅助接口连接给定类型的列表，
	 * 将数组中每个元素转为字符串
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static <T> String join(final Iterable<T> array, final String delimiter, final Stringer<T> stringer) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			if (next != null)
				message += stringer.toString(next) + (it.hasNext() ? delimiter : "");
		}

		return message;
	}

	/**
	 * 自动将一些常见类（如实体）替换为名称
	 *
	 * @param arg
	 * @return
	 */
	public static String simplify(Object arg) {
		if (arg instanceof Entity)
			return Remain.getName((Entity) arg);

		else if (arg instanceof CommandSender)
			return ((CommandSender) arg).getName();

		else if (arg instanceof World)
			return ((World) arg).getName();

		else if (arg instanceof Location)
			return Common.shortLocation((Location) arg);

		else if (arg.getClass() == double.class || arg.getClass() == float.class)
			return MathUtil.formatTwoDigits((double) arg);

		else if (arg instanceof Collection)
			return Common.join((Collection<?>) arg, ", ", Common::simplify);

		else if (arg instanceof ChatColor)
			return ((Enum<?>) arg).name().toLowerCase();

		else if (arg instanceof CompChatColor)
			return ((CompChatColor) arg).getName();

		else if (arg instanceof Enum)
			return ((Enum<?>) arg).toString().toLowerCase();

		try {
			if (arg instanceof net.md_5.bungee.api.ChatColor)
				return ((net.md_5.bungee.api.ChatColor) arg).getName();
		} catch (final Exception e) {
			// No MC compatible
		}

		return arg.toString();
	}

	/**
	 * 动态填充页面，用于命令或菜单中的分页
	 *
	 * @param <T>
	 * @param cellSize
	 * @param items
	 * @return
	 */
	public static <T> Map<Integer, List<T>> fillPages(int cellSize, Iterable<T> items) {
		final List<T> allItems = Common.toList(items);

		final Map<Integer, List<T>> pages = new HashMap<>();
		final int pageCount = allItems.size() == cellSize ? 0 : allItems.size() / cellSize;

		for (int i = 0; i <= pageCount; i++) {
			final List<T> pageItems = new ArrayList<>();

			final int down = cellSize * i;
			final int up = down + cellSize;

			for (int valueIndex = down; valueIndex < up; valueIndex++)
				if (valueIndex < allItems.size()) {
					final T page = allItems.get(valueIndex);

					pageItems.add(page);
				} else
					break;

			// If the menu is completely empty, at least allow the first page
			if (i == 0 || !pageItems.isEmpty())
				pages.put(i, pageItems);
		}

		return pages;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting and retyping
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回列表中的最后一个键，若列表为 null 或空则返回 null
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> T last(List<T> list) {
		return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
	}

	/**
	 * 返回数组中的最后一个键，若数组为 null 或空则返回 null
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> T last(T[] array) {
		return array == null || array.length == 0 ? null : array[array.length - 1];
	}

	/**
	 * 获取世界名称列表的便捷方法
	 *
	 * @return
	 */
	public static List<String> getWorldNames() {
		final List<String> worlds = new ArrayList<>();

		for (final World world : Bukkit.getWorlds())
			worlds.add(world.getName());

		return worlds;
	}

	/**
	 * 获取玩家名称列表的便捷方法
	 *
	 * @return
	 */
	public static List<String> getPlayerNames() {
		return getPlayerNames(true, null);
	}

	/**
	 * 获取玩家名称列表的便捷方法，
	 * 可选择是否包含隐身玩家
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNames(final boolean includeVanished) {
		return getPlayerNames(includeVanished, null);
	}

	/**
	 * 获取玩家名称列表的便捷方法，
	 * 可选择仅包含另一玩家可见的玩家
	 *
	 * @param includeVanished
	 * @param otherPlayer
	 *
	 * @return
	 */
	public static List<String> getPlayerNames(final boolean includeVanished, Player otherPlayer) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers()) {
			if (PlayerUtil.isVanished(online, otherPlayer) && !includeVanished)
				continue;

			found.add(online.getName());
		}

		return found;
	}

	/**
	 * 返回在线玩家的昵称
	 *
	 * @param includeVanished
	 * @return
	 */
	public static List<String> getPlayerNicknames(final boolean includeVanished) {
		return getPlayerNicknames(includeVanished, null);
	}

	/**
	 * 返回在线玩家的昵称
	 *
	 * @param includeVanished
	 * @param otherPlayer
	 * @return
	 */
	public static List<String> getPlayerNicknames(final boolean includeVanished, Player otherPlayer) {
		final List<String> found = new ArrayList<>();

		for (final Player online : Remain.getOnlinePlayers()) {
			if (PlayerUtil.isVanished(online, otherPlayer) && !includeVanished)
				continue;

			found.add(HookManager.getNickColorless(online));
		}

		return found;
	}

	/**
	 * 将一种类型对象的列表转换为另一种
	 *
	 * @param <OLD>
	 * @param <NEW>
	 * @param list      旧列表
	 * @param converter 转换器；
	 * @return 新列表
	 */
	public static <OLD, NEW> List<NEW> convert(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final List<NEW> copy = new ArrayList<>();

		for (final OLD old : list) {
			final NEW result = converter.convert(old);
			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * 将一种类型对象的集合转换为另一种
	 *
	 * @param <OLD>
	 * @param <NEW>
	 * @param list      旧列表
	 * @param converter 转换器；
	 * @return 新列表
	 */
	public static <OLD, NEW> Set<NEW> convertSet(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final Set<NEW> copy = new HashSet<>();

		for (final OLD old : list) {
			final NEW result = converter.convert(old);
			if (result != null)
				copy.add(converter.convert(old));
		}

		return copy;
	}

	/**
	 * 将一种类型对象的列表转换为另一种
	 * 
	 * @param <OLD>
	 * @param <NEW>
	 * @param list      旧列表
	 * @param converter 转换器
	 * @return 新列表
	 */
	public static <OLD, NEW> StrictList<NEW> convertStrict(final Iterable<OLD> list, final TypeConverter<OLD, NEW> converter) {
		final StrictList<NEW> copy = new StrictList<>();

		for (final OLD old : list)
			copy.add(converter.convert(old));

		return copy;
	}

	/**
	 * 尝试将给定映射转换为另一个映射
	 *
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param <NEW_KEY>
	 * @param <NEW_VALUE>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> Map<NEW_KEY, NEW_VALUE> convert(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final Map<NEW_KEY, NEW_VALUE> newMap = new HashMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * 尝试将给定映射转换为另一个映射
	 *
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param <NEW_KEY>
	 * @param <NEW_VALUE>
	 * @param oldMap
	 * @param converter
	 * @return
	 */
	public static <OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> StrictMap<NEW_KEY, NEW_VALUE> convertStrict(final Map<OLD_KEY, OLD_VALUE> oldMap, final MapToMapConverter<OLD_KEY, OLD_VALUE, NEW_KEY, NEW_VALUE> converter) {
		final StrictMap<NEW_KEY, NEW_VALUE> newMap = new StrictMap<>();
		oldMap.entrySet().forEach(e -> newMap.put(converter.convertKey(e.getKey()), converter.convertValue(e.getValue())));

		return newMap;
	}

	/**
	 * 尝试将给定映射转换为列表
	 *
	 * @param <LIST_KEY>
	 * @param <OLD_KEY>
	 * @param <OLD_VALUE>
	 * @param map
	 * @param converter
	 * @return
	 */
	public static <LIST_KEY, OLD_KEY, OLD_VALUE> StrictList<LIST_KEY> convertToList(final Map<OLD_KEY, OLD_VALUE> map, final MapToListConverter<LIST_KEY, OLD_KEY, OLD_VALUE> converter) {
		final StrictList<LIST_KEY> list = new StrictList<>();

		for (final Entry<OLD_KEY, OLD_VALUE> e : map.entrySet())
			list.add(converter.convert(e.getKey(), e.getValue()));

		return list;
	}

	/**
	 * 尝试将数组转换为不同类型
	 *
	 * @param <OLD_TYPE>
	 * @param <NEW_TYPE>
	 * @param oldArray
	 * @param converter
	 * @return
	 */
	public static <OLD_TYPE, NEW_TYPE> List<NEW_TYPE> convert(final OLD_TYPE[] oldArray, final TypeConverter<OLD_TYPE, NEW_TYPE> converter) {
		final List<NEW_TYPE> newList = new ArrayList<>();

		for (final OLD_TYPE old : oldArray)
			newList.add(converter.convert(old));

		return newList;
	}

	/**
	 * 将给定字符串按给定最大行长度拆分为数组
	 *
	 * @param input
	 * @param maxLineLength
	 * @return
	 */
	public static String[] split(String input, int maxLineLength) {
		final StringTokenizer tok = new StringTokenizer(input, " ");
		final StringBuilder output = new StringBuilder(input.length());

		int lineLen = 0;

		while (tok.hasMoreTokens()) {
			final String word = tok.nextToken();

			if (lineLen + word.length() > maxLineLength) {
				output.append("\n");

				lineLen = 0;
			}

			output.append(word + " ");
			lineLen += word.length() + 1;
		}

		return output.toString().split("\n");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc message handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 创建只包含非 null 且非空字符串元素的新列表
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> List<T> removeNullAndEmpty(final T[] array) {
		return array != null ? removeNullAndEmpty(Arrays.asList(array)) : new ArrayList<>();
	}

	/**
	 * 创建只包含非 null 且非空字符串元素的新列表
	 *
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> List<T> removeNullAndEmpty(final List<T> list) {
		final List<T> copy = new ArrayList<>();

		for (final T key : list)
			if (key != null)
				if (key instanceof String) {
					if (!((String) key).isEmpty())
						copy.add(key);
				} else
					copy.add(key);

		return copy;
	}

	/**
	 * 将所有 null 替换为空字符串
	 *
	 * @param list
	 * @return
	 */
	public static String[] replaceNullWithEmpty(final String[] list) {
		for (int i = 0; i < list.length; i++)
			if (list[i] == null)
				list[i] = "";

		return list;
	}

	/**
	 * 返回给定下标的值，若下标在数组中不存在则返回默认值
	 *
	 * @param <T>
	 * @param array
	 * @param index
	 * @param def
	 * @return
	 */
	public static <T> T getOrDefault(final T[] array, final int index, final T def) {
		return index < array.length ? array[index] : def;
	}

	/**
	 * 若字符串为 null 或等于 none 则返回空字符串。
	 *
	 * @param input
	 * @return
	 */
	public static String getOrEmpty(final String input) {
		return input == null || "none".equalsIgnoreCase(input) ? "" : input;
	}

	/**
	 * 若字符串等于 none 或为空则返回 null
	 *
	 * @param input
	 * @return
	 */
	public static String getOrNull(final String input) {
		return input == null || "none".equalsIgnoreCase(input) || input.isEmpty() ? null : input;
	}

	/**
	 * 返回值，若为 null 则返回其默认值
	 *
	 * 提示：若值为字符串，当值为空或等于 "none" 时我们返回默认值
	 *
	 * @param <T>
	 * @param value 主要值
	 * @param def   默认值
	 * @return 该值，若值为 null 则返回默认值
	 */
	public static <T> T getOrDefault(final T value, final T def) {
		if (value instanceof String && ("none".equalsIgnoreCase((String) value) || "".equals(value)))
			return def;

		return getOrDefaultStrict(value, def);
	}

	/**
	 * 返回值，若为 null 则返回其默认值
	 *
	 * @param <T>
	 * @param value
	 * @param def
	 * @return
	 */
	public static <T> T getOrDefaultStrict(final T value, final T def) {
		return value != null ? value : def;
	}

	/**
	 * 获取列表中的下一个元素，若 forward 为 true 则下标加 1，
	 * 若为 false 则下标减 1
	 *
	 * @param <T>
	 * @param given
	 * @param list
	 * @param forward
	 * @return
	 */
	public static <T> T getNext(final T given, final List<T> list, final boolean forward) {
		if (given == null && list.isEmpty())
			return null;

		final T[] array = (T[]) Array.newInstance((given != null ? given : list.get(0)).getClass(), list.size());

		for (int i = 0; i < list.size(); i++)
			Array.set(array, i, list.get(i));

		return getNext(given, array, forward);
	}

	/**
	 * 获取列表中的下一个元素，若 forward 为 true 则下标加 1，
	 * 若为 false 则下标减 1
	 *
	 * @param <T>
	 * @param given
	 * @param array
	 * @param forward
	 * @return
	 */
	public static <T> T getNext(final T given, final T[] array, final boolean forward) {
		if (array.length == 0)
			return null;

		int index = 0;

		for (int i = 0; i < array.length; i++) {
			final T element = array[i];

			if (element.equals(given)) {
				index = i;

				break;
			}
		}

		if (index != -1) {
			final int nextIndex = index + (forward ? 1 : -1);

			// Return the first slot if reached the end, or the last if vice versa
			return nextIndex >= array.length ? array[0] : nextIndex < 0 ? array[array.length - 1] : array[nextIndex];
		}

		return null;
	}

	/**
	 * 将字符串列表转换为字符串数组
	 *
	 * @param array
	 * @return
	 */
	public static String[] toArray(final Collection<String> array) {
		return array == null ? new String[0] : array.toArray(new String[array.size()]);
	}

	/**
	 * 从数组创建新的可修改 ArrayList
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> ArrayList<T> toList(final T... array) {
		return array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array));
	}

	/**
	 * 将 {@link Iterable} 转换为 {@link List}
	 *
	 * @param <T>
	 * @param it 可迭代对象
	 * @return 新列表
	 */
	public static <T> List<T> toList(final Iterable<T> it) {
		final List<T> list = new ArrayList<>();

		if (it != null)
			it.forEach(el -> {
				if (el != null)
					list.add(el);
			});

		return list;
	}

	/**
	 * 反转数组中的元素
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> T[] reverse(final T[] array) {
		if (array == null)
			return null;

		int i = 0;
		int j = array.length - 1;

		while (j > i) {
			final T tmp = array[j];

			array[j] = array[i];
			array[i] = tmp;

			j--;
			i++;
		}

		return array;
	}

	/**
	 * 返回包含给定首个键值对的新 HashMap
	 *
	 * @param <A>
	 * @param <B>
	 * @param firstKey
	 * @param firstValue
	 * @return
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);

		return map;
	}

	/**
	 * 创建包含单个键值对的新 {@link HashMap}。
	 *
	 * @param <A>        键的类型。
	 * @param <B>        值的类型。
	 * @param firstKey   第一个条目的键。
	 * @param firstValue 第一个条目的值。
	 * @param secondKey
	 * @param secondValue
	 * @return 包含指定键值对的新 {@link HashMap}。
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue, final A secondKey, final B secondValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);
		map.put(secondKey, secondValue);

		return map;
	}

	/**
	 * 创建包含单个键值对的新 {@link HashMap}。
	 *
	 * @param <A>        键的类型。
	 * @param <B>        值的类型。
	 * @param firstKey   第一个条目的键。
	 * @param firstValue 第一个条目的值。
	 * @param secondKey
	 * @param secondValue
	 * @param thirdKey
	 * @param thirdValue
	 * @return 包含指定键值对的新 {@link HashMap}。
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue, final A secondKey, final B secondValue, final A thirdKey, final B thirdValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);
		map.put(secondKey, secondValue);
		map.put(thirdKey, thirdValue);

		return map;
	}

	/**
	 * 创建包含单个键值对的新 {@link HashMap}。
	 *
	 * @param <A>        键的类型。
	 * @param <B>        值的类型。
	 * @param firstKey   第一个条目的键。
	 * @param firstValue 第一个条目的值。
	 * @param secondKey
	 * @param secondValue
	 * @param thirdKey
	 * @param thirdValue
	 * @param forthKey
	 * @param forthValue
	 * @return 包含指定键值对的新 {@link HashMap}。
	 */
	public static <A, B> Map<A, B> newHashMap(final A firstKey, final B firstValue, final A secondKey, final B secondValue, final A thirdKey, final B thirdValue, final A forthKey, final B forthValue) {
		final Map<A, B> map = new HashMap<>();
		map.put(firstKey, firstValue);
		map.put(secondKey, secondValue);
		map.put(thirdKey, thirdValue);
		map.put(forthKey, forthValue);

		return map;
	}

	/**
	 * 创建新的 HashSet
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static <T> Set<T> newSet(final T... keys) {
		return new HashSet<>(Arrays.asList(keys));
	}

	/**
	 * 创建新的可变 ArrayList（若调用 Arrays.asList 则不可修改）
	 *
	 * @param <T>
	 * @param keys
	 * @return
	 */
	public static <T> List<T> newList(final T... keys) {
		final List<T> list = new ArrayList<>();

		Collections.addAll(list, keys);

		return list;
	}

	/**
	 * 返回按值排序的映射（数字即从小到大）
	 *
	 * @param map
	 * @return
	 */
	public static Map<String, Integer> sortByValue(Map<String, Integer> map) {
		final List<Map.Entry<String, Integer>> list = new LinkedList<>(map.entrySet());
		list.sort(Map.Entry.comparingByValue());

		final Map<String, Integer> sortedMap = new LinkedHashMap<>();

		for (final Map.Entry<String, Integer> entry : list)
			sortedMap.put(entry.getKey(), entry.getValue());

		return sortedMap;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Scheduling
	// ------------------------------------------------------------------------------------------------------------

	private static Object foliaScheduler;
	private static Method runAtFixedRate;
	private static Method runDelayed;
	private static Method execute;
	private static Method cancel;
	private static Method cancelTasks;

	static {
		if (Remain.isFolia()) {
			foliaScheduler = ReflectionUtil.invoke("getGlobalRegionScheduler", org.bukkit.Bukkit.getServer());
			runAtFixedRate = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
			execute = ReflectionUtil.getMethod(foliaScheduler.getClass(), "run", Plugin.class, Consumer.class);
			runDelayed = ReflectionUtil.getMethod(foliaScheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class);
			cancelTasks = ReflectionUtil.getMethod(foliaScheduler.getClass(), "cancelTasks", Plugin.class);
			cancel = ReflectionUtil.getMethod(ReflectionUtil.lookupClass("io.papermc.paper.threadedregions.scheduler.ScheduledTask"), "cancel");
		}
	}

	/**
	 * 尝试取消所有任务
	 */
	public static void cancelTasks() {
		if (Remain.isFolia())
			ReflectionUtil.invoke(cancelTasks, foliaScheduler, SimplePlugin.getInstance());
		else
			Bukkit.getScheduler().cancelTasks(SimplePlugin.getInstance());
	}

	/**
	 * 若插件已正确启用则运行任务
	 *
	 * @param task 该任务
	 * @return 该任务，若失败则为 null
	 */
	public static SimpleTask runLater(final Runnable task) {
		return runLater(1, task);
	}

	/**
	 * 即使插件因某种原因被禁用也运行任务。
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return 该任务，若失败则为 null
	 */
	public static SimpleTask runLater(final int delayTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle;

			if (delayTicks == 0)
				taskHandle = ReflectionUtil.invoke(execute, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run());
			else
				taskHandle = ReflectionUtil.invoke(runDelayed, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), delayTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskLater(SimplePlugin.getInstance(), delayTicks);

			else
				task = Bukkit.getScheduler().runTaskLater(SimplePlugin.getInstance(), runnable, delayTicks);

			final SimpleTask simpleTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleSyncDelayedTask(SimplePlugin.getInstance(), runnable, delayTicks), false);
		}
	}

	/**
	 * 即使插件因某种原因被禁用也异步运行任务。
	 * <p>
	 * 安排在下一个 tick 运行。
	 *
	 * @param task
	 * @return
	 */
	public static SimpleTask runAsync(final Runnable task) {
		return runLaterAsync(0, task);
	}

	/**
	 * 即使插件因某种原因被禁用也异步运行任务。
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return 该任务，若失败则为 null
	 */
	public static SimpleTask runLaterAsync(final int delayTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle;

			if (delayTicks == 0)
				taskHandle = ReflectionUtil.invoke(execute, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run());
			else
				taskHandle = ReflectionUtil.invoke(runDelayed, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), delayTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskLaterAsynchronously(SimplePlugin.getInstance(), delayTicks);

			else
				task = Bukkit.getScheduler().runTaskLaterAsynchronously(SimplePlugin.getInstance(), runnable, delayTicks);

			final SimpleTask simpleTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleAsyncDelayedTask(SimplePlugin.getInstance(), runnable, delayTicks), true);
		}
	}

	/**
	 * 即使插件被禁用也运行任务计时器。
	 *
	 * @param repeatTicks 每次执行之间的延迟
	 * @param task        该任务
	 * @return Bukkit 任务，若失败则为 null
	 */
	public static SimpleTask runTimer(final int repeatTicks, final Runnable task) {
		return runTimer(0, repeatTicks, task);
	}

	/**
	 * 即使插件被禁用也运行任务计时器。
	 *
	 * @param delayTicks  首次运行前的延迟
	 * @param repeatTicks 每次运行之间的延迟
	 * @param runnable        该任务
	 * @return Bukkit 任务，若出错则为 null
	 */
	public static SimpleTask runTimer(final int delayTicks, final int repeatTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle = ReflectionUtil.invoke(runAtFixedRate, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), Math.max(1, delayTicks), repeatTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskTimer(SimplePlugin.getInstance(), delayTicks, repeatTicks);

			else
				task = Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks);

			final SimpleTask simpleTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simpleTask);

			return simpleTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleSyncRepeatingTask(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks), false);
		}
	}

	/**
	 * 即使插件被禁用也异步运行任务计时器。
	 *
	 * @param repeatTicks
	 * @param task
	 * @return
	 */
	public static SimpleTask runTimerAsync(final int repeatTicks, final Runnable task) {
		return runTimerAsync(0, repeatTicks, task);
	}

	/**
	 * 即使插件被禁用也异步运行任务计时器。
	 *
	 * @param delayTicks
	 * @param repeatTicks
	 * @param runnable
	 * @return
	 */
	public static SimpleTask runTimerAsync(final int delayTicks, final int repeatTicks, Runnable runnable) {
		if (runIfDisabled(runnable))
			return null;

		if (Remain.isFolia()) {
			final Object taskHandle = ReflectionUtil.invoke(runAtFixedRate, foliaScheduler, SimplePlugin.getInstance(), (Consumer<Object>) t -> runnable.run(), Math.max(1, delayTicks), repeatTicks);

			return SimpleTask.fromFolia(cancel, taskHandle);
		}

		try {
			BukkitTask task;

			if (runnable instanceof BukkitRunnable)
				task = ((BukkitRunnable) runnable).runTaskTimerAsynchronously(SimplePlugin.getInstance(), delayTicks, repeatTicks);

			else
				task = Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks);

			final SimpleTask simplTask = SimpleTask.fromBukkit(task);

			if (runnable instanceof SimpleRunnable)
				((SimpleRunnable) runnable).setupTask(simplTask);

			return simplTask;

		} catch (final NoSuchMethodError err) {
			return SimpleTask.fromBukkit(Bukkit.getScheduler().scheduleAsyncRepeatingTask(SimplePlugin.getInstance(), runnable, delayTicks, repeatTicks), true);
		}
	}

	// Check our plugin instance if it's enabled
	// In case it is disabled, just runs the task and returns true
	// Otherwise we return false and the task will be run correctly in Bukkit scheduler
	// This is fail-safe to critical save-on-exit operations in case our plugin is improperly reloaded (PlugMan) or malfunctions
	private static boolean runIfDisabled(final Runnable run) {
		if (!SimplePlugin.getInstance().isEnabled()) {
			run.run();

			return true;
		}

		return false;
	}

	/**
	 * 在 Bukkit 中调用事件，并返回它是否成功经过管道触发
	 * （未被取消）
	 *
	 * @param event 该事件
	 * @return 若事件未被取消则返回 true
	 */
	public static boolean callEvent(final Event event) {
		Bukkit.getPluginManager().callEvent(event);

		return event instanceof Cancellable ? !((Cancellable) event).isCancelled() : true;
	}

	/**
	 * 以我们实例身份注册事件的便捷方法
	 *
	 * @param listener
	 */
	public static void registerEvents(final Listener listener) {
		Bukkit.getPluginManager().registerEvents(listener, SimplePlugin.getInstance());
	}

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 解析 Bukkit {@link MemorySection} 中的内部 Map
	 *
	 * @param mapOrSection
	 * @return
	 */
	public static Map<String, Object> getMapFromSection(@NonNull Object mapOrSection) {
		mapOrSection = Remain.getRootOfSectionPathData(mapOrSection);

		final Map<String, Object> map = mapOrSection instanceof ConfigSection ? ((ConfigSection) mapOrSection).getValues(false)
				: mapOrSection instanceof Map ? (Map<String, Object>) mapOrSection
						: mapOrSection instanceof MemorySection ? ReflectionUtil.getFieldContent(mapOrSection, "map") : null;

		Valid.checkNotNull(map, "Unexpected " + mapOrSection.getClass().getSimpleName() + " '" + mapOrSection + "'. Must be Map or MemorySection! (Do not just send config name here, but the actual section with get('section'))");

		final Map<String, Object> copy = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : map.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			copy.put(key, Remain.getRootOfSectionPathData(value));
		}

		return copy;
	}

	/**
	 * 若域名可达则返回 true。本方法会阻塞。
	 *
	 * @param url
	 * @param timeout
	 * @return
	 */
	public static boolean isDomainReachable(String url, final int timeout) {
		url = url.replaceFirst("^https", "http");

		try {
			final HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();

			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);
			c.setRequestMethod("HEAD");

			final int responseCode = c.getResponseCode();
			return 200 <= responseCode && responseCode <= 399;

		} catch (final IOException exception) {
			return false;
		}
	}

	/**
	 * 受检的睡眠方法，来自 {@link Thread#sleep(long)} 但无需 try-catch
	 *
	 * @param millis
	 */
	public static void sleep(final int millis) {
		try {
			Thread.sleep(millis);

		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 将对象转换为字符串的简单接口
	 *
	 * @param <T>
	 */
	public interface Stringer<T> {

		/**
		 * 将给定对象转换为字符串
		 *
		 * @param object
		 * @return
		 */
		String toString(T object);
	}

	/**
	 * 在类型之间转换的简单接口
	 *
	 * @param <Old> 要转换的初始类型
	 * @param <New> 要转换到的最终类型
	 */
	public interface TypeConverter<Old, New> {

		/**
		 * 将给定类型从 A 转换为 B
		 *
		 * @param value 旧值类型
		 * @return 新值类型
		 */
		New convert(Old value);
	}

	/**
	 * 将映射转换为列表的便捷类
	 *
	 * @param <O>
	 * @param <K>
	 * @param <V>
	 */
	public interface MapToListConverter<O, K, V> {

		/**
		 * 将给定映射键值对转换为存于列表中的新类型
		 *
		 * @param key
		 * @param value
		 * @return
		 */
		O convert(K key, V value);
	}

	/**
	 * 在映射之间转换的便捷类
	 *
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 */
	public interface MapToMapConverter<A, B, C, D> {

		/**
		 * 将旧键类型转换为新类型
		 *
		 * @param key
		 * @return
		 */
		C convertKey(A key);

		/**
		 * 将旧值转换为新值类型
		 *
		 * @param value
		 * @return
		 */
		D convertValue(B value);
	}

	/**
	 * 代表计时聊天序列，用于检查正则表达式时
	 * 统计耗时，
	 * 若耗时过长则停止执行
	 */
	public final static class TimedCharSequence implements CharSequence {

		/**
		 * 计时的消息
		 */
		private final CharSequence message;

		/**
		 * 超时限制（毫秒）
		 */
		private final long futureTimestampLimit;

		/*
		 * Create a new timed message for the given message with a timeout in millis
		 */
		private TimedCharSequence(@NonNull final CharSequence message, long futureTimestampLimit) {
			this.message = message;
			this.futureTimestampLimit = futureTimestampLimit;
		}

		/**
		 * 获取给定下标的字符，
		 * 若在构造之后调用太晚则抛错。
		 */
		@Override
		public char charAt(final int index) {

			// Temporarily disabled due to a rare condition upstream when we take this message
			// and run it in a runnable, then this is still being evaluated past limit and it fails
			//
			//if (System.currentTimeMillis() > futureTimestampLimit)
			//	throw new RegexTimeoutException(message, futureTimestampLimit);

			try {
				return this.message.charAt(index);
			} catch (final StringIndexOutOfBoundsException ex) {

				// Odd case: Java 8 seems to overflow for too-long unicode characters, security feature
				return ' ';
			}
		}

		@Override
		public int length() {
			return this.message.length();
		}

		@Override
		public CharSequence subSequence(final int start, final int end) {
			return new TimedCharSequence(this.message.subSequence(start, end), this.futureTimestampLimit);
		}

		@Override
		public String toString() {
			return this.message.toString();
		}

		/**
		 * 用 settings.yml 中的限制编译新的字符序列
		 *
		 * @param message
		 * @return
		 */
		public static TimedCharSequence withSettingsLimit(CharSequence message) {
			return new TimedCharSequence(message, System.currentTimeMillis() + SimpleSettings.REGEX_TIMEOUT);
		}
	}
}