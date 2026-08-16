package org.mineacademy.fo.settings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.command.DebugCommand;
import org.mineacademy.fo.command.PermsCommand;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.menu.tool.RegionTool;
import org.mineacademy.fo.model.ChatPaginator;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.FileConfig.AccusativeHelper;

/**
 * 基础本地化文件的简单实现。
 * 我们会自动创建 localization/messages_LOCALEPREFIX.yml 文件，
 * 并用插件 jar 中 localization/messages_LOCALEPREFIX.yml
 * 文件里的值填充它。
 */
@SuppressWarnings("unused")
public class SimpleLocalization extends YamlStaticConfig {

	/**
	 * 表示此类已被加载的标记
	 * <p>
	 * 你可以把此类放到 {@link SimplePlugin#getSettings()} 中，让它
	 * 自动加载
	 */
	private static boolean localizationClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	/**
	 * 创建并加载 localization/messages_LOCALEPREFIX.yml 文件。
	 * <p>
	 * 语言前缀参见 {@link SimpleSettings#LOCALE_PREFIX}。
	 * <p>
	 * 如果语言文件不存在，会从插件 jar 中解压到 localization/ 文件夹；
	 * 如果已过时，则会更新。
	 */
	@Override
	protected final void onLoad() throws Exception {
		final String localePath = "localization/messages_" + SimpleSettings.LOCALE_PREFIX + ".yml";
		final Object content = FileUtil.getInternalFileContent(localePath);

		Valid.checkNotNull(content, SimplePlugin.getNamed() + " does not support the localization: messages_" + SimpleSettings.LOCALE_PREFIX
				+ ".yml (For custom locale, set the Locale to 'en' and edit your English file instead)");

		this.loadConfiguration(localePath);
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * 配置版本号，取自文件中的 "Version" 键。
	 *
	 * 文件中未设置时默认为 1。
	 */
	public static Integer VERSION = 1;

	/**
	 * 自动设置并更新配置版本，不过 {@link #VERSION} 仍会
	 * 保存磁盘上文件所用的旧版本，方便你在
	 * init() 方法中进行比较
	 * <p>
	 * 重载此方法时，请以 super 方式调用它！
	 */
	@Override
	protected final void preLoad() {
		// Load version first so we can use it later
		setPathPrefix(null);

		if (isSetDefault("Version"))
			if ((VERSION = getInteger("Version")) != this.getConfigVersion())
				set("Version", this.getConfigVersion());
	}

	/**
	 * 返回最新的配置版本
	 * <p>
	 * 此处的任何修改也必须同步到设置文件中的 "Version" 键。
	 *
	 * @return
	 */
	protected int getConfigVersion() {
		return 1;
	}

	/**
	 * 始终保持语言文件为最新。
	 */
	@Override
	protected final boolean alwaysSaveOnLoad() {
		return true;
	}

	// --------------------------------------------------------------------
	// Shared values
	// --------------------------------------------------------------------

	// NB: Those keys are optional - you do not have to write them into your messages_X.yml files
	// but if you do, we will use your values instead of the default ones!

	/**
	 * 与插件命令相关的语言键
	 */
	public static final class Commands {

		/**
		 * "No_Console" 键中的消息，当控制台被禁止执行命令时显示。
		 */
		public static String NO_CONSOLE = "&cYou may only use this command as a player";

		/**
		 * 控制台执行命令但未指定目标玩家名称时显示的消息
		 */
		public static String CONSOLE_MISSING_PLAYER_NAME = "When running from console, specify player name.";

		/**
		 * 执行此命令发生致命错误时显示的消息
		 */
		public static String COOLDOWN_WAIT = "&cWait {duration} second(s) before using this command again.";

		/**
		 * 以下键表示无效的操作或输入
		 */
		public static String INVALID_ARGUMENT = "&cInvalid argument. Run &6/{label} ? &cfor help.";
		public static String INVALID_SUB_ARGUMENT = "&cInvalid argument. Run '/{label} {0}' for help.";
		public static String INVALID_ARGUMENT_MULTILINE = "&cInvalid argument. Usage:";
		public static String INVALID_TIME = "Expected time such as '3 hours' or '15 minutes'. Got: '{input}'";
		public static String INVALID_NUMBER = "The number must be a whole or a decimal number. Got: '{input}'";
		public static String INVALID_STRING = "Invalid string. Got: '{input}'";
		public static String INVALID_WORLD = "Invalid world '{world}'. Available: {available}";

		/**
		 * 作者标签
		 */
		public static String LABEL_AUTHORS = "Made by";

		/**
		 * 描述标签
		 */
		public static String LABEL_DESCRIPTION = "&c&lDescription:";

		/**
		 * 可选参数标签
		 */
		public static String LABEL_OPTIONAL_ARGS = "optional arguments";

		/**
		 * 必填参数标签
		 */
		public static String LABEL_REQUIRED_ARGS = "required arguments";

		/**
		 * 用法标签
		 */
		public static String LABEL_USAGE = "&c&lUsage:";

		/**
		 * “帮助：”标签
		 */
		public static String LABEL_HELP_FOR = "Help for /{label}";

		/**
		 * 构建子命令时显示的标签
		 */
		public static String LABEL_SUBCOMMAND_DESCRIPTION = " &f/{label} {sublabel} {usage+}{dash+}{description}";

		/**
		 * 以下键在 /command help 菜单中作为悬停提示显示。
		 */
		public static String HELP_TOOLTIP_DESCRIPTION = "&7Description: &f{description}";
		public static String HELP_TOOLTIP_PERMISSION = "&7Permission: &f{permission}";
		public static String HELP_TOOLTIP_USAGE = "&7Usage: &f";

		/**
		 * 以下键用于 {@link ReloadCommand}
		 */
		public static String RELOAD_DESCRIPTION = "Reload the configuration.";
		public static String RELOAD_STARTED = "Reloading plugin's data, please wait..";
		public static String RELOAD_SUCCESS = "&6{plugin_name} {plugin_version} has been reloaded.";
		public static String RELOAD_FILE_LOAD_ERROR = "&4Oups, &cthere was a problem loading files from your disk! See the console for more information. {plugin_name} has not been reloaded.";
		public static String RELOAD_FAIL = "&4Oups, &creloading failed! See the console for more information. Error: {error}";

		/**
		 * 执行此命令发生致命错误时显示的消息
		 */
		public static String ERROR = "&4&lOups! &cThe command failed :( Check the console and report the error.";

		/**
		 * 玩家没有权限查看命令组中任何子命令时显示的消息。
		 */
		public static String HEADER_NO_SUBCOMMANDS = "&cThere are no arguments for this command.";

		/**
		 * 玩家没有权限查看命令组中任何子命令时显示的消息。
		 */
		public static String HEADER_NO_SUBCOMMANDS_PERMISSION = "&cYou don't have permissions to view any subcommands.";

		/**
		 * ----- COMMAND ----- 页眉中显示的主颜色
		 */
		public static ChatColor HEADER_COLOR = ChatColor.GOLD;

		/**
		 * ----- COMMAND ----- 页眉中显示的次颜色，例如 /chc ? 中
		 */
		public static ChatColor HEADER_SECONDARY_COLOR = ChatColor.RED;

		/**
		 * 页眉格式
		 */
		public static String HEADER_FORMAT = "&r\n{theme_color}&m<center>&r{theme_color} {title} &m\n&r";

		/**
		 * 使用 \<center\> 时格式的居中字符
		 */
		public static String HEADER_CENTER_LETTER = "-";

		/**
		 * 使用 \<center\> 时页眉的填充
		 */
		public static Integer HEADER_CENTER_PADDING = 130;

		/**
		 * 插件正在重载时使用的键 {@link org.mineacademy.fo.plugin.SimplePlugin}
		 */
		public static String RELOADING = "reloading";

		/**
		 * 插件被禁用时使用的键 {@link org.mineacademy.fo.plugin.SimplePlugin}
		 */
		public static String DISABLED = "disabled";

		/**
		 * 插件正在重载或已被禁用时，玩家尝试执行命令所显示的消息
		 */
		public static String CANNOT_USE_WHILE_NULL = "&cCannot use this command while the plugin is {state}.";

		/**
		 * SimpleCommand.findWorld() 中显示的消息
		 */
		public static String CANNOT_AUTODETECT_WORLD = "Only living players can use ~ for their world!";

		/**
		 * 以下键用于 {@link DebugCommand}
		 */
		public static String DEBUG_DESCRIPTION = "ZIP your settings for reporting bugs.";
		public static String DEBUG_PREPARING = "&6Preparing debug log...";
		public static String DEBUG_SUCCESS = "&2Successfuly copied {amount} file(s) to debug.zip. Your sensitive MySQL information has been removed from yml files. Please upload it via ufile.io and send it to us for review.";
		public static String DEBUG_COPY_FAIL = "&cCopying files failed on file {file} and it was stopped. See console for more information.";
		public static String DEBUG_ZIP_FAIL = "&cCreating a ZIP of your files failed, see console for more information. Please ZIP debug/ folder and send it to us via ufile.io manually.";

		/**
		 * 以下键用于 {@link PermsCommand}
		 */
		public static String PERMS_DESCRIPTION = "List all permissions the plugin has.";
		public static String PERMS_USAGE = "[phrase]";
		public static String PERMS_HEADER = "Listing All {plugin_name} Permissions";
		public static String PERMS_MAIN = "Main";
		public static String PERMS_PERMISSIONS = "Permissions:";
		public static String PERMS_TRUE_BY_DEFAULT = "&7[true by default]";
		public static String PERMS_INFO = "&7Info: &f";
		public static String PERMS_DEFAULT = "&7Default? ";
		public static String PERMS_APPLIED = "&7Do you have it? ";
		public static String PERMS_YES = "&2yes";
		public static String PERMS_NO = "&cno";

		/**
		 * 以下键用于 {@link RegionTool}
		 */
		public static String REGION_SET_PRIMARY = "Set the primary region point.";
		public static String REGION_SET_SECONDARY = "Set the secondary region point.";

		/**
		 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
		 */
		private static void init() {
			setPathPrefix("Commands");

			if (isSetDefault("No_Console"))
				NO_CONSOLE = getString("No_Console");

			if (isSetDefault("Console_Missing_Player_Name"))
				CONSOLE_MISSING_PLAYER_NAME = getString("Console_Missing_Player_Name");

			if (isSetDefault("Cooldown_Wait"))
				COOLDOWN_WAIT = getString("Cooldown_Wait");

			if (isSetDefault("Invalid_Argument"))
				INVALID_ARGUMENT = getString("Invalid_Argument");

			if (isSetDefault("Invalid_Sub_Argument"))
				INVALID_SUB_ARGUMENT = getString("Invalid_Sub_Argument");

			if (isSetDefault("Invalid_Argument_Multiline"))
				INVALID_ARGUMENT_MULTILINE = getString("Invalid_Argument_Multiline");

			if (isSetDefault("Invalid_Time"))
				INVALID_TIME = getString("Invalid_Time");

			if (isSetDefault("Invalid_Number"))
				INVALID_NUMBER = getString("Invalid_Number");

			if (isSetDefault("Invalid_String"))
				INVALID_STRING = getString("Invalid_String");

			if (isSetDefault("Invalid_World"))
				INVALID_WORLD = getString("Invalid_World");

			if (isSetDefault("Label_Authors"))
				LABEL_AUTHORS = getString("Label_Authors");

			if (isSetDefault("Label_Description"))
				LABEL_DESCRIPTION = getString("Label_Description");

			if (isSetDefault("Label_Optional_Args"))
				LABEL_OPTIONAL_ARGS = getString("Label_Optional_Args");

			if (isSetDefault("Label_Required_Args"))
				LABEL_REQUIRED_ARGS = getString("Label_Required_Args");

			if (isSetDefault("Label_Usage"))
				LABEL_USAGE = getString("Label_Usage");

			if (isSetDefault("Label_Help_For"))
				LABEL_HELP_FOR = getString("Label_Help_For");

			if (isSetDefault("Label_Subcommand_Description"))
				LABEL_SUBCOMMAND_DESCRIPTION = getString("Label_Subcommand_Description");

			if (isSetDefault("Help_Tooltip_Description"))
				HELP_TOOLTIP_DESCRIPTION = getString("Help_Tooltip_Description");

			if (isSetDefault("Help_Tooltip_Permission"))
				HELP_TOOLTIP_PERMISSION = getString("Help_Tooltip_Permission");

			if (isSetDefault("Help_Tooltip_Usage"))
				HELP_TOOLTIP_USAGE = getString("Help_Tooltip_Usage");

			if (isSetDefault("Reload_Description"))
				RELOAD_DESCRIPTION = getString("Reload_Description");

			if (isSetDefault("Reload_Started"))
				RELOAD_STARTED = getString("Reload_Started");

			if (isSetDefault("Reload_Success"))
				RELOAD_SUCCESS = getString("Reload_Success");

			if (isSetDefault("Reload_File_Load_Error"))
				RELOAD_FILE_LOAD_ERROR = getString("Reload_File_Load_Error");

			if (isSetDefault("Reload_Fail"))
				RELOAD_FAIL = getString("Reload_Fail");

			if (isSetDefault("Error"))
				ERROR = getString("Error");

			if (isSetDefault("Header_No_Subcommands"))
				HEADER_NO_SUBCOMMANDS = getString("Header_No_Subcommands");

			if (isSetDefault("Header_No_Subcommands_Permission"))
				HEADER_NO_SUBCOMMANDS_PERMISSION = getString("Header_No_Subcommands_Permission");

			if (isSetDefault("Header_Color"))
				HEADER_COLOR = get("Header_Color", ChatColor.class);

			if (isSetDefault("Header_Secondary_Color"))
				HEADER_SECONDARY_COLOR = get("Header_Secondary_Color", ChatColor.class);

			if (isSetDefault("Header_Format"))
				HEADER_FORMAT = getString("Header_Format");

			if (isSetDefault("Header_Center_Letter")) {
				HEADER_CENTER_LETTER = getString("Header_Center_Letter");

				Valid.checkBoolean(HEADER_CENTER_LETTER.length() == 1, "Header_Center_Letter must only have 1 letter, not " + HEADER_CENTER_LETTER.length() + ":" + HEADER_CENTER_LETTER);
			}

			if (isSetDefault("Header_Center_Padding"))
				HEADER_CENTER_PADDING = getInteger("Header_Center_Padding");

			if (isSet("Reloading"))
				RELOADING = getString("Reloading");

			if (isSet("Disabled"))
				DISABLED = getString("Disabled");

			if (isSet("Use_While_Null"))
				CANNOT_USE_WHILE_NULL = getString("Use_While_Null");

			if (isSet("Cannot_Autodetect_World"))
				CANNOT_AUTODETECT_WORLD = getString("Cannot_Autodetect_World");

			if (isSetDefault("Debug_Description"))
				DEBUG_DESCRIPTION = getString("Debug_Description");

			if (isSetDefault("Debug_Preparing"))
				DEBUG_PREPARING = getString("Debug_Preparing");

			if (isSetDefault("Debug_Success"))
				DEBUG_SUCCESS = getString("Debug_Success");

			if (isSetDefault("Debug_Copy_Fail"))
				DEBUG_COPY_FAIL = getString("Debug_Copy_Fail");

			if (isSetDefault("Debug_Zip_Fail"))
				DEBUG_ZIP_FAIL = getString("Debug_Zip_Fail");

			if (isSetDefault("Perms_Description"))
				PERMS_DESCRIPTION = getString("Perms_Description");

			if (isSetDefault("Perms_Usage"))
				PERMS_USAGE = getString("Perms_Usage");

			if (isSetDefault("Perms_Header"))
				PERMS_HEADER = getString("Perms_Header");

			if (isSetDefault("Perms_Main"))
				PERMS_MAIN = getString("Perms_Main");

			if (isSetDefault("Perms_Permissions"))
				PERMS_PERMISSIONS = getString("Perms_Permissions");

			if (isSetDefault("Perms_True_By_Default"))
				PERMS_TRUE_BY_DEFAULT = getString("Perms_True_By_Default");

			if (isSetDefault("Perms_Info"))
				PERMS_INFO = getString("Perms_Info");

			if (isSetDefault("Perms_Default"))
				PERMS_DEFAULT = getString("Perms_Default");

			if (isSetDefault("Perms_Applied"))
				PERMS_APPLIED = getString("Perms_Applied");

			if (isSetDefault("Perms_Yes"))
				PERMS_YES = getString("Perms_Yes");

			if (isSetDefault("Perms_No"))
				PERMS_NO = getString("Perms_No");

			if (isSetDefault("Region_Set_Primary"))
				REGION_SET_PRIMARY = getString("Region_Set_Primary");

			if (isSetDefault("Region_Set_Secondary"))
				REGION_SET_SECONDARY = getString("Region_Set_Secondary");
		}
	}

	/**
	 * 与玩家-服务器对话（等待玩家聊天输入）相关的字符串
	 */
	public static final class Conversation {

		/**
		 * 玩家想要对话但并不处于对话中时使用的键。
		 */
		public static String CONVERSATION_NOT_CONVERSING = "&cYou must be conversing with the server!";

		/**
		 * 控制台尝试开始对话时调用
		 */
		public static String CONVERSATION_REQUIRES_PLAYER = "Only players may enter this conversation.";

		/**
		 * 在 try-catch 处理中发生错误时调用
		 */
		public static String CONVERSATION_ERROR = "&cOups! There was a problem in this conversation! Please contact the administrator to review the console for details.";

		/**
		 * 在 {@link org.mineacademy.fo.conversation.SimplePrompt#show(org.bukkit.entity.Player)} 中调用
		 */
		public static String CONVERSATION_CANCELLED = "Your pending chat answer has been canceled.";

		/**
		 * 在 {@link org.mineacademy.fo.conversation.SimplePrompt#show(org.bukkit.entity.Player)} 中调用
		 */
		public static String CONVERSATION_CANCELLED_INACTIVE = "Your pending chat answer has been canceled because you were inactive.";

		private static void init() {
			setPathPrefix("Conversation");

			if (isSetDefault("Not_Conversing"))
				CONVERSATION_NOT_CONVERSING = getString("Not_Conversing");

			if (isSetDefault("Requires_Player"))
				CONVERSATION_REQUIRES_PLAYER = getString("Requires_Player");

			if (isSetDefault("Conversation_Error"))
				CONVERSATION_ERROR = getString("Error");

			if (isSetDefault("Conversation_Cancelled"))
				CONVERSATION_CANCELLED = getString("Conversation_Cancelled");

			if (isSetDefault("Conversation_Cancelled_Inactive"))
				CONVERSATION_CANCELLED_INACTIVE = getString("Conversation_Cancelled_Inactive");
		}
	}

	/**
	 * 与玩家相关的键
	 */
	public static final class Player {

		/**
		 * 玩家不在此服务器上在线时显示的消息
		 */
		public static String NOT_ONLINE = "&cPlayer {player} &cis not online on this server.";

		/**
		 * 当 {@link Bukkit#getOfflinePlayer(String)} 返回该玩家从未进入过服务器时显示的消息
		 */
		public static String NOT_PLAYED_BEFORE = "&cPlayer {player} &chas not played before or we could not locate his disk data.";

		/**
		 * 根据给定 UUID 获取离线玩家却返回 null 时显示的消息。
		 */
		public static String INVALID_UUID = "&cCould not find a player from UUID {uuid}.";

		/**
		 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
		 */
		private static void init() {
			setPathPrefix("Player");

			if (isSetDefault("Not_Online"))
				NOT_ONLINE = getString("Not_Online");

			if (isSetDefault("Not_Played_Before"))
				NOT_PLAYED_BEFORE = getString("Not_Played_Before");

			if (isSetDefault("Invalid_UUID"))
				INVALID_UUID = getString("Invalid_UUID");
		}
	}

	/**
	 * 与 {@link ChatPaginator} 相关的键
	 */
	public static final class Pages {

		public static String NO_PAGE_NUMBER = "&cPlease specify the page number for this command.";
		public static String NO_PAGES = "There are no results to list.";
		public static String NO_PAGE = "Pages do not contain the given page number.";
		public static String INVALID_PAGE = "&cYour input '{input}' is not a valid number.";
		public static String GO_TO_PAGE = "&7Go to page {page}";
		public static String GO_TO_FIRST_PAGE = "&7Go to the first page";
		public static String GO_TO_LAST_PAGE = "&7Go to the last page";
		public static String[] TOOLTIP = {
				"&7You can also navigate using the",
				"&7hidden /#flp <page> command."
		};

		/**
		 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
		 */
		private static void init() {
			setPathPrefix("Pages");

			if (isSetDefault("No_Page_Number"))
				NO_PAGE_NUMBER = getString("No_Page_Number");

			if (isSetDefault("No_Pages"))
				NO_PAGES = getString("No_Pages");

			if (isSetDefault("No_Page"))
				NO_PAGE = getString("No_Page");

			if (isSetDefault("Invalid_Page"))
				INVALID_PAGE = getString("Invalid_Page");

			if (isSetDefault("Go_To_Page"))
				GO_TO_PAGE = getString("Go_To_Page");

			if (isSetDefault("Go_To_First_Page"))
				GO_TO_FIRST_PAGE = getString("Go_To_First_Page");

			if (isSetDefault("Go_To_Last_Page"))
				GO_TO_LAST_PAGE = getString("Go_To_Last_Page");

			if (isSetDefault("Tooltip"))
				TOOLTIP = Common.toArray(getStringList("Tooltip"));
		}
	}

	/**
	 * 与 GUI 系统相关的键
	 */
	public static final class Menu {

		/**
		 * 玩家不在此服务器上在线时显示的消息
		 */
		public static String ITEM_DELETED = "&2The {item} has been deleted.";

		/**
		 * 玩家尝试打开菜单但正处于对话中时显示的消息。
		 */
		public static String CANNOT_OPEN_DURING_CONVERSATION = "&cType 'exit' to quit your conversation before opening menu.";

		/**
		 * 出错时显示的消息
		 */
		public static String ERROR = "&cOups! There was a problem with this menu! Please contact the administrator to review the console for details.";

		/**
		 * 与菜单分页相关的键
		 */
		public static String PAGE_PREVIOUS = "&8<< &fPage {page}";
		public static String PAGE_NEXT = "Page {page} &8>>";
		public static String PAGE_FIRST = "&7First Page";
		public static String PAGE_LAST = "&7Last Page";

		/**
		 * 与菜单标题和提示相关的键
		 */
		public static String TITLE_TOOLS = "Tools Menu";
		public static String TOOLTIP_INFO = "&fMenu Information";
		public static String BUTTON_RETURN_TITLE = "&4&lReturn";
		public static String[] BUTTON_RETURN_LORE = { "", "Return back." };

		/**
		 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
		 */
		private static void init() {
			setPathPrefix("Menu");

			if (isSetDefault("Item_Deleted"))
				ITEM_DELETED = getString("Item_Deleted");

			if (isSetDefault("Cannot_Open_During_Conversation"))
				CANNOT_OPEN_DURING_CONVERSATION = getString("Cannot_Open_During_Conversation");

			if (isSetDefault("Error"))
				ERROR = getString("Error");

			if (isSetDefault("Page_Previous"))
				PAGE_PREVIOUS = getString("Page_Previous");

			if (isSetDefault("Page_Next"))
				PAGE_NEXT = getString("Page_Next");

			if (isSetDefault("Page_First"))
				PAGE_FIRST = getString("Page_First");

			if (isSetDefault("Page_Last"))
				PAGE_LAST = getString("Page_Last");

			if (isSetDefault("Title_Tools"))
				TITLE_TOOLS = getString("Title_Tools");

			if (isSetDefault("Tooltip_Info"))
				TOOLTIP_INFO = getString("Tooltip_Info");

			if (isSetDefault("Button_Return_Title"))
				BUTTON_RETURN_TITLE = getString("Button_Return_Title");

			if (isSetDefault("Button_Return_Lore"))
				BUTTON_RETURN_LORE = Common.toArray(getStringList("Button_Return_Lore"));
		}
	}

	/**
	 * 与工具相关的键
	 */
	public static final class Tool {

		/**
		 * 工具出错时显示的消息。
		 */
		public static String ERROR = "&cOups! There was a problem with this tool! Please contact the administrator to review the console for details.";

		/**
		 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
		 */
		private static void init() {
			setPathPrefix("Tool");

			if (isSetDefault("Error"))
				ERROR = getString("Error");
		}
	}

	/**
	 * 与大小写/语法格相关的键
	 */
	public static class Cases {

		public static AccusativeHelper SECOND = AccusativeHelper.of("second", "seconds");
		public static AccusativeHelper MINUTE = AccusativeHelper.of("minute", "minutes");
		public static AccusativeHelper HOUR = AccusativeHelper.of("hour", "hours");
		public static AccusativeHelper DAY = AccusativeHelper.of("day", "days");
		public static AccusativeHelper WEEK = AccusativeHelper.of("week", "weeks");
		public static AccusativeHelper MONTH = AccusativeHelper.of("month", "months");
		public static AccusativeHelper YEAR = AccusativeHelper.of("year", "years");

		private static void init() {
			setPathPrefix("Cases");

			if (isSetDefault("Second"))
				SECOND = getCasus("Second");

			if (isSetDefault("Minute"))
				MINUTE = getCasus("Minute");

			if (isSetDefault("Hour"))
				HOUR = getCasus("Hour");

			if (isSetDefault("Day"))
				DAY = getCasus("Day");

			if (isSetDefault("Week"))
				WEEK = getCasus("Week");

			if (isSetDefault("Month"))
				MONTH = getCasus("Month");

			if (isSetDefault("Year"))
				YEAR = getCasus("Year");
		}
	}

	/**
	 * 与插件更新相关的键
	 */
	public static final class Update {

		/**
		 * 发现新版本但未下载时显示的消息
		 */
		public static String AVAILABLE = "&2A new version of &3{plugin_name}&2 is available.\n"
				+ "&2Current version: &f{current}&2; New version: &f{new}\n"
				+ "&2URL: &7https://spigotmc.org/resources/{resource_id}/.";

		/**
		 * 发现新版本并已下载时显示的消息
		 */
		public static String DOWNLOADED = "&3{plugin_name}&2 has been upgraded from {current} to {new}.\n"
				+ "&2Visit &7https://spigotmc.org/resources/{resource_id} &2for more information.\n"
				+ "&2Please restart the server to load the new version.";

		/**
		 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
		 */
		private static void init() {
			setPathPrefix(null);

			// Upgrade from old path
			if (isSet("Update_Available"))
				move("Update_Available", "Update.Available");

			setPathPrefix("Update");

			if (isSetDefault("Available"))
				AVAILABLE = getString("Available");

			if (isSetDefault("Downloaded"))
				DOWNLOADED = getString("Downloaded");
		}
	}

	/**
	 * 表示“无”的消息
	 */
	public static String NONE = "None";

	/**
	 * 玩家缺少权限时显示的消息。
	 */
	public static String NO_PERMISSION = "&cInsufficient permission ({permission}).";

	/**
	 * 服务器前缀。示例：如果你从控制台向玩家发送消息，
	 * 需要手动使用它
	 */
	public static String SERVER_PREFIX = "[Server]";

	/**
	 * 控制台的本地化名称。示例：Console
	 */
	public static String CONSOLE_NAME = "Console";

	/**
	 * 数据文件（以 .db 结尾的文件）中缺少某个节时显示的消息（我们通常用
	 * 此文件存储序列化的值，例如小游戏插件中的竞技场）。
	 */
	public static String DATA_MISSING = "&c{name} lacks database information! Please only create {type} in-game! Skipping..";

	/**
	 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
	 */
	private static void init() {
		setPathPrefix(null);
		Valid.checkBoolean(!localizationClassCalled, "Localization class already loaded!");

		if (isSetDefault("No_Permission"))
			NO_PERMISSION = getString("No_Permission");

		if (isSetDefault("Server_Prefix"))
			SERVER_PREFIX = getString("Server_Prefix");

		if (isSetDefault("Console_Name"))
			CONSOLE_NAME = getString("Console_Name");

		if (isSetDefault("Data_Missing"))
			DATA_MISSING = getString("Data_Missing");

		if (isSetDefault("None"))
			NONE = getString("None");

		localizationClassCalled = true;
	}

	/**
	 * 此类是否已被加载？
	 *
	 * @return
	 */
	public static final Boolean isLocalizationCalled() {
		return localizationClassCalled;
	}

	/**
	 * 重置表示此类已被加载的标记，
	 * 在重载时使用。
	 */
	public static final void resetLocalizationCall() {
		localizationClassCalled = false;
	}
}
