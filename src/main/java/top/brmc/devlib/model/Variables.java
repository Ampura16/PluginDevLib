package top.brmc.devlib.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import top.brmc.devlib.Common;
import top.brmc.devlib.GeoAPI;
import top.brmc.devlib.GeoAPI.GeoResponse;
import top.brmc.devlib.Messenger;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.PlayerUtil;
import top.brmc.devlib.TimeUtil;
import top.brmc.devlib.collection.StrictList;
import top.brmc.devlib.collection.StrictMap;
import top.brmc.devlib.collection.expiringmap.ExpiringMap;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.Remain;
import top.brmc.devlib.settings.SimpleLocalization;

/**
 * 替换消息中变量的简单引擎。
 */
public final class Variables {

	/**
	 * 用于查找单个 [syntax_name] 变量的模式。
	 */
	public static final Pattern MESSAGE_VARIABLE_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

	/**
	 * 用于查找简单 %syntax% 占位符的模式。
	 */
	public static final Pattern VARIABLE_PATTERN = Pattern.compile("[%]([^%]+)[%]");

	/**
	 * 用于查找简单 {syntax} 占位符的模式。
	 */
	public static final Pattern BRACKET_VARIABLE_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	/**
	 * 用于查找以 %rel_% 开头的简单 %syntax% 占位符的模式（用于 PlaceholderAPI）
	 */
	public static final Pattern REL_VARIABLE_PATTERN = Pattern.compile("[%](rel_)([^%]+)[%]");

	/**
	 * 用于查找以 {rel_} 开头的简单 {syntax} 占位符的模式（用于 PlaceholderAPI）
	 */
	public static final Pattern BRACKET_REL_VARIABLE_PATTERN = Pattern.compile("[({)](rel_)([^}]+)[(})]");

	/**
	 * 玩家 - [原始消息 - 翻译后的消息]
	 */
	private static final Map<String, Map<String, String>> cache = ExpiringMap.builder().expiration(500, TimeUnit.MILLISECONDS).build();

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 由你或其他插件添加到 Foundation 的变量
	 *
	 * 传入一个命令发送者（可能是也可能不是玩家），输出替换后的字符串。
	 * 变量名（键）会自动用 {} 括号包围
	 */
	private static final StrictMap<String, Function<CommandSender, String>> customVariables = new StrictMap<>();

	/**
	 * 由你或其他插件添加到 Foundation 的变量
	 *
	 * 用于根据内容动态替换变量，类似
	 * PlaceholderAPI。
	 *
	 * 我们也接入了 PlaceholderAPI，不过从那里调用时，
	 * 你需要在所有变量前加上插件的前缀。
	 */
	private static final StrictList<SimpleExpansion> customExpansions = new StrictList<>();

	/**
	 * 返回给定键对应的变量，它是一个针对玩家替换自身的函数。
	 * 如果不存在该键对应的变量则返回 null。
	 * @param key
	 *
	 * @return
	 */
	public static Function<CommandSender, String> getVariable(String key) {
		return customVariables.get(key);
	}

	/**
	 * 注册新变量。变量会在 {} 块中查找，因此如果你给出的变量名
	 * 是 player_health，它就是 {player_health}。该函数接收一个命令发送者（可以是玩家），
	 * 并输出变量值。
	 * <p>
	 * 请注意，我们会在 PlaceholderAPI 和 Javascript 变量之后再替换你的变量
	 *
	 * @param variable
	 * @param replacer
	 */
	public static void addVariable(String variable, Function<CommandSender, String> replacer) {
		customVariables.override(variable, replacer);
	}

	/**
	 * 移除已有变量，这里只填名称、不带括号，例如 player_name 而不是 {player_name}
	 * 变量不存在时会失败
	 *
	 * @param variable
	 */
	public static void removeVariable(String variable) {
		customVariables.remove(variable);
	}

	/**
	 * 检查给定变量是否存在。警告：这里只填名称、不带括号，
	 * 例如 player_name 而不是 {player_name}
	 *
	 * @param variable
	 * @return
	 */
	public static boolean hasVariable(String variable) {
		return customVariables.containsKey(variable);
	}

	/**
	 * 返回当前已加载的所有扩展的不可变列表
	 *
	 * @return
	 */
	public static List<SimpleExpansion> getExpansions() {
		return Collections.unmodifiableList(customExpansions.getSource());
	}

	/**
	 * 注册新扩展（若尚未注册）
	 *
	 * @param expansion
	 */
	public static void addExpansion(SimpleExpansion expansion) {
		customExpansions.addIfNotExist(expansion);
	}

	/**
	 * 注销扩展（若已注册）
	 *
	 * @param expansion
	 */
	public static void removeExpansion(SimpleExpansion expansion) {
		customExpansions.remove(expansion);
	}

	/**
	 * 若扩展已注册则返回 true
	 *
	 * @param expansion
	 * @return
	 */
	public static boolean hasExpansion(SimpleExpansion expansion) {
		return customExpansions.contains(expansion);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 替换消息中的变量，使用消息发送者作为对象来替换
	 * 与玩家相关的占位符。
	 *
	 * 我们也支持 PlaceholderAPI 和 MVdWPlaceholderAPI（仅当发送者是 Player 时）。
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, CommandSender sender) {
		return replace(message, sender, null);
	}

	/**
	 * 替换消息中的变量，使用消息发送者作为对象来替换
	 * 与玩家相关的占位符。
	 *
	 * 我们也支持 PlaceholderAPI 和 MVdWPlaceholderAPI（仅当发送者是 Player 时）。
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public static String replace(String message, CommandSender sender, Map<String, Object> replacements) {
		return replace(message, sender, replacements, true);
	}

	/**
	 * 替换消息中的变量，使用消息发送者作为对象来替换
	 * 与玩家相关的占位符。
	 *
	 * 我们也支持 PlaceholderAPI 和 MVdWPlaceholderAPI（仅当发送者是 Player 时）。
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @param colorize
	 * @return
	 */
	public static String replace(String message, CommandSender sender, Map<String, Object> replacements, boolean colorize) {
		return replace(message, sender, replacements, colorize, true);
	}

	/**
	 * 替换消息中的变量，使用消息发送者作为对象来替换
	 * 与玩家相关的占位符。
	 *
	 * 我们也支持 PlaceholderAPI 和 MVdWPlaceholderAPI（仅当发送者是 Player 时）。
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @param colorize
	 * @param replaceScript
	 * @return
	 */
	public static String replace(String message, CommandSender sender, Map<String, Object> replacements, boolean colorize, boolean replaceScript) {
		if (message == null || message.isEmpty() || message.equals("none"))
			return "";

		final String original = message;
		final boolean senderIsPlayer = sender instanceof Player;

		if (senderIsPlayer) {

			// Already cached ? Return.
			final Map<String, String> cached = cache.get(sender.getName());
			final String cachedVar = cached != null ? cached.get(message) : null;

			if (cachedVar != null && !cachedVar.contains("flpm_") && !cachedVar.contains("flps_"))
				return cachedVar;
		}

		// PlaceholderAPI and MVdWPlaceholderAPI
		if (senderIsPlayer)
			message = HookManager.replacePlaceholders((Player) sender, message);

		else if (sender instanceof DiscordSender)
			message = HookManager.replacePlaceholders(((DiscordSender) sender).getOfflinePlayer(), message);

		// Replace hard variables
		message = replaceHardVariables0(sender, message, Variables.VARIABLE_PATTERN.matcher(message));
		message = replaceHardVariables0(sender, message, Variables.BRACKET_VARIABLE_PATTERN.matcher(message));
		message = Messenger.replacePrefixes(message);

		// Custom placeholders
		if (replaceScript)
			message = replaceJavascriptVariables0(message, sender, replacements);

		// Replace custom variables last to avoid replacing variables in them for security
		if (replacements != null && !replacements.isEmpty())
			message = Replacer.replaceArray(message, replacements);

		if (!message.startsWith("[JSON]") && colorize)
			message = Common.colorize(message);

		if (senderIsPlayer) {
			final Map<String, String> map = cache.get(sender.getName());

			if (map != null)
				map.put(original, message);
			else
				cache.put(sender.getName(), Common.newHashMap(original, message));
		}

		return message;
	}

	/*
	 * Replaces JavaScript variables in the message
	 */
	private static String replaceJavascriptVariables0(String message, CommandSender sender, Map<String, Object> replacements) {

		message = replaceJavascriptVariables0(message, sender, replacements, VARIABLE_PATTERN.matcher(message));
		message = replaceJavascriptVariables0(message, sender, replacements, BRACKET_VARIABLE_PATTERN.matcher(message));

		return message;
	}

	private static String replaceJavascriptVariables0(String message, CommandSender sender, Map<String, Object> replacements, Matcher matcher) {
		while (matcher.find()) {
			final String variableKey = matcher.group();

			// Find the variable key without []
			final Variable variable = Variable.findVariable(variableKey.substring(1, variableKey.length() - 1));

			if (variable != null && variable.getType() == Variable.Type.FORMAT) {
				String plain = variable.buildPlain(sender, replacements);

				// And we remove the white prefix that is by default added in every component
				if (plain.startsWith(ChatColor.COLOR_CHAR + "f" + ChatColor.COLOR_CHAR + "f"))
					plain = plain.substring(4);

				message = message.replace(variableKey, plain);
			}
		}

		return message;
	}

	private static String replaceHardVariables0(CommandSender sender, String message, Matcher matcher) {
		final Player player = sender instanceof Player ? (Player) sender : null;

		while (matcher.find()) {
			String variable = matcher.group(1);
			boolean frontSpace = false;
			boolean backSpace = false;

			if (variable.startsWith("+")) {
				variable = variable.substring(1);

				frontSpace = true;
			}

			if (variable.endsWith("+")) {
				variable = variable.substring(0, variable.length() - 1);

				backSpace = true;
			}

			String value = lookupVariable0(player, sender, variable);

			if (value != null) {
				final boolean emptyColorless = Common.stripColors(value).isEmpty();
				value = value.isEmpty() ? "" : (frontSpace && !emptyColorless ? " " : "") + Common.colorize(value) + (backSpace && !emptyColorless ? " " : "");

				message = message.replace(matcher.group(), value);
			}
		}

		return message;
	}

	/*
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 */
	private static String lookupVariable0(Player player, CommandSender console, String variable) {
		GeoResponse geoResponse = null;

		if (player != null && Arrays.asList("country_code", "country_name", "region_name", "isp").contains(variable))
			geoResponse = GeoAPI.getCountry(player.getAddress());

		if (console != null) {

			// Replace custom expansions
			for (final SimpleExpansion expansion : customExpansions) {
				final String value = expansion.replacePlaceholders(console, variable);

				if (value != null)
					return value;
			}

			// Replace custom variables
			final Function<CommandSender, String> customReplacer = customVariables.get(variable);

			if (customReplacer != null)
				return customReplacer.apply(console);
		}

		switch (variable) {
			case "server_name":
				return Remain.getServerName();
			case "server_version":
				return MinecraftVersion.getFullVersion();
			case "nms_version":
				return Remain.getNmsVersion();
			case "timestamp":
			case "date":
				return TimeUtil.getFormattedDate();
			case "date_short":
				return TimeUtil.getFormattedDateShort();
			case "date_month":
				return TimeUtil.getFormattedDateMonth();
			case "chat_line":
				return Common.chatLine();
			case "chat_line_smooth":
				return Common.chatLineSmooth();
			case "town":
				return player == null ? "" : HookManager.getTownName(player);
			case "nation":
				return player == null ? "" : HookManager.getNation(player);
			case "faction":
				return player == null ? "" : HookManager.getFaction(player);

			case "world":
				return player == null ? "" : HookManager.getWorldAlias(player.getWorld());
			case "health":
				return player == null ? "" : formatHealth0(player) + ChatColor.RESET;
			case "location":
				return player == null ? "" : Common.shortLocation(player.getLocation());
			case "x":
				return player == null ? "" : String.valueOf(player.getLocation().getBlockX());
			case "y":
				return player == null ? "" : String.valueOf(player.getLocation().getBlockY());
			case "z":
				return player == null ? "" : String.valueOf(player.getLocation().getBlockZ());

			case "player":
			case "player_name": {
				if (console == null)
					return null;

				return player == null ? Common.resolveSenderName(console) : player.getName();
			}

			case "tab_name":
				return player == null ? Common.resolveSenderName(console) : player.getPlayerListName();
			case "display_name":
				return player == null ? Common.resolveSenderName(console) : player.getDisplayName();
			case "player_nick":
			case "nick":
				return player == null ? Common.resolveSenderName(console) : HookManager.getNickColored(player);

			case "player_prefix":
			case "pl_prefix":
				return player == null ? "" : HookManager.getPlayerPrefix(player);
			case "player_suffix":
			case "pl_suffix":
				return player == null ? "" : HookManager.getPlayerSuffix(player);
			case "player_group":
			case "pl_group":
				return player == null ? "" : HookManager.getPlayerPermissionGroup(player);
			case "player_primary_group":
			case "pl_primary_group":
				return player == null ? "" : HookManager.getPlayerPrimaryGroup(player);
			case "ip_address":
			case "pl_address":
				return player == null ? "" : formatIp0(player);

			case "player_vanished":
				return player == null ? "false" : String.valueOf(PlayerUtil.isVanished(player));

			case "country_code":
				return player == null ? "" : geoResponse.getCountryCode();
			case "country_name":
				return player == null ? "" : geoResponse.getCountryName();
			case "region_name":
				return player == null ? "" : geoResponse.getRegionName();
			case "isp":
				return player == null ? "" : geoResponse.getIsp();

			case "label":
				return SimplePlugin.getInstance().getMainCommand() != null ? SimplePlugin.getInstance().getMainCommand().getLabel() : SimpleLocalization.NONE;
			case "sender_is_player":
				return player != null ? "true" : "false";
			case "sender_is_discord":
				return console instanceof DiscordSender ? "true" : "false";
			case "sender_is_console":
				return console instanceof ConsoleCommandSender ? "true" : "false";
		}

		return null;
	}

	/*
	 * Formats the {health} variable
	 */
	private static String formatHealth0(Player player) {
		final int hp = Remain.getHealth(player);

		return (hp > 10 ? ChatColor.DARK_GREEN : hp > 5 ? ChatColor.GOLD : ChatColor.RED) + "" + hp;
	}

	/*
	 * Formats the IP address variable for the player
	 */
	private static String formatIp0(Player player) {
		try {
			return player.getAddress().toString().split("\\:")[0];
		} catch (final Throwable t) {
			return player.getAddress() != null ? player.getAddress().toString() : "";
		}
	}
}
