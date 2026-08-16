package org.mineacademy.fo.settings;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.model.SpigotUpdater;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * 典型插件主设置的简单实现，
 * 每个键都可以在任何地方以静态方式访问。
 * <p>
 * 我们通常将此类用于 settings.yml 插件主配置。
 */
// Use for settings.yml
@SuppressWarnings("unused")
public class SimpleSettings extends YamlStaticConfig {

	/**
	 * 表示此类已被加载的标记
	 * <p>
	 * 你可以把此类放到 {@link org.mineacademy.fo.plugin.SimplePlugin#getSettings()} ()} 中，
	 * 让它自动加载
	 */
	private static boolean settingsClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	@Override
	protected final void onLoad() throws Exception {
		this.loadConfiguration(this.getSettingsFileName());
	}

	/**
	 * 获取这些设置的文件名，默认为 settings.yml
	 *
	 * @return
	 */
	protected String getSettingsFileName() {
		return FoConstants.File.SETTINGS;
	}

	/**
	 * 始终保持 settings.yml 文件为最新
	 */
	@Override
	protected final boolean alwaysSaveOnLoad() {
		return true;
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
	protected void preLoad() {
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

	// --------------------------------------------------------------------
	// Settings we offer by default for your main config file
	// Specify those you need to modify
	// --------------------------------------------------------------------

	/**
	 * {date}、{date_short} 和 {date_month} 的格式。
	 */
	public static DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static DateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	public static DateFormat DATE_FORMAT_MONTH = new SimpleDateFormat("dd.MM HH:mm");

	/**
	 * {location} 的格式。
	 */
	public static String LOCATION_FORMAT = "{world} [{x}, {y}, {z}]";

	/**
	 * 应在 {@link Debugger} 中启用哪些调试分区？调用 {@link Debugger#debug(String, String...)} 时，
	 * 在此设置中指定的分区会记录到控制台，否则不显示任何消息。
	 * <p>
	 * 通常留空：Debug: []
	 */
	public static StrictList<String> DEBUG_SECTIONS = new StrictList<>();

	/**
	 * 聊天/控制台消息前的插件前缀。
	 * <p>
	 * ChatControl 的典型值：
	 * <p>
	 * Prefix: "&8[&3ChatControl&8]&7 "
	 */
	public static String PLUGIN_PREFIX = "&7" + SimplePlugin.getNamed() + " //";

	/**
	 * {@link LagCatcher} 使用的卡顿阈值（毫秒）。设为 -1 表示禁用。
	 * <p>
	 * ChatControl 的典型值：
	 * <p>
	 * Log_Lag_Over_Milis: 100
	 */
	public static Integer LAG_THRESHOLD_MILLIS = 100;

	/**
	 * 处理正则表达式时，将执行时间限制在指定时长内。
	 * 这可以防止因格式错误的正则（循环）导致服务器卡死/崩溃。
	 * <p>
	 * Regex_Timeout_Milis: 100
	 */
	public static Integer REGEX_TIMEOUT = 100;

	/**
	 * 哪些命令应触发你的插件主命令（以逗号 , 分隔）？参见 {@link SimplePlugin#getMainCommand()}
	 * <p>
	 * ChatControl 的典型值：
	 * <p>
	 * Command_Aliases: [chatcontrol, chc, cc]
	 * <p>
	 * // 仅当你覆盖 {@link SimplePlugin#getMainCommand()} 时才必填 //
	 */
	public static StrictList<String> MAIN_COMMAND_ALIASES = new StrictList<>();

	/**
	 * 语言前缀，前提是你使用 {@link SimpleLocalization} 类加载和管理
	 * 语言文件。使用下面的前缀时，文件路径通常为：localization/messages_PREFIX.yml。
	 * <p>
	 * 典型值：Locale: en
	 * <p>
	 * // 仅当你使用 SIMPLELOCALIZATION 时才必填 //
	 */
	public static String LOCALE_PREFIX = "en";

	/**
	 * 是否应从 SpigotMC 检查更新，并通知控制台和有权限的用户？
	 * <p>
	 * 参见 {@link SimplePlugin#getUpdateCheck()}，你可以让它返回带有你的 Spigot 插件 ID 的 {@link SpigotUpdater}。
	 * <p>
	 * ChatControl 的典型值：
	 * <p>
	 * Notify_Updates: true
	 * <p>
	 * // 仅当你覆盖 {@link SimplePlugin#getUpdateCheck()} 时才必填 //
	 */
	public static Boolean NOTIFY_UPDATES = true;

	/**
	 * 加载各值——此方法由 {@link YamlStaticConfig} 类通过反射自动调用！
	 */
	private static void init() {
		Valid.checkBoolean(!settingsClassCalled, "Settings class already loaded!");

		setPathPrefix(null);
		upgradeOldSettings();

		if (isSetDefault("Date_Format"))
			try {
				DATE_FORMAT = new SimpleDateFormat(getString("Date_Format"));

			} catch (final IllegalArgumentException ex) {
				Common.throwError(ex, "Wrong 'Date_Format '" + getString("Date_Format") + "', see https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for examples'");
			}

		if (isSetDefault("Date_Format_Short"))
			try {
				DATE_FORMAT_SHORT = new SimpleDateFormat(getString("Date_Format_Short"));

			} catch (final IllegalArgumentException ex) {
				Common.throwError(ex, "Wrong 'Date_Format_Short '" + getString("Date_Format_Short") + "', see https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for examples'");
			}

		if (isSetDefault("Date_Format_Month"))
			try {
				DATE_FORMAT_MONTH = new SimpleDateFormat(getString("Date_Format_Month"));

			} catch (final IllegalArgumentException ex) {
				Common.throwError(ex, "Wrong 'Date_Format_Month '" + getString("Date_Format_Month") + "', see https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for examples'");
			}

		if (isSetDefault("Location_Format"))
			LOCATION_FORMAT = getString("Location_Format");

		if (isSetDefault("Prefix"))
			PLUGIN_PREFIX = getString("Prefix");

		if (isSetDefault("Log_Lag_Over_Milis")) {
			LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
			Valid.checkBoolean(LAG_THRESHOLD_MILLIS == -1 || LAG_THRESHOLD_MILLIS >= 0, "Log_Lag_Over_Milis must be either -1 to disable, 0 to log all or greater!");

			if (LAG_THRESHOLD_MILLIS == 0)
				Common.log("&eLog_Lag_Over_Milis is 0, all performance is logged. Set to -1 to disable.");
		}

		if (isSetDefault("Debug"))
			DEBUG_SECTIONS = new StrictList<>(getStringList("Debug"));

		if (isSetDefault("Regex_Timeout_Milis"))
			REGEX_TIMEOUT = getInteger("Regex_Timeout_Milis");

		// -------------------------------------------------------------------
		// Load maybe-mandatory values
		// -------------------------------------------------------------------

		{ // Load localization
			final boolean keySet = isSetDefault("Locale");

			LOCALE_PREFIX = keySet ? getString("Locale") : LOCALE_PREFIX;
		}

		{ // Load main command alias
			final boolean keySet = isSetDefault("Command_Aliases");

			MAIN_COMMAND_ALIASES = keySet ? getCommandList("Command_Aliases") : MAIN_COMMAND_ALIASES;
		}

		{ // Load updates notifier
			final boolean keySet = isSetDefault("Notify_Updates");

			NOTIFY_UPDATES = keySet ? getBoolean("Notify_Updates") : NOTIFY_UPDATES;
		}

		settingsClassCalled = true;
	}

	/**
	 * 升级我们付费插件中一些旧的、古老的设置。
	 */
	private static void upgradeOldSettings() {

		{ // Debug
			if (isSet("Debugger"))
				move("Debugger", "Debug");

			if (isSet("Serialization_Number"))
				move("Serialization_Number", "Serialization");

			// ChatControl
			if (isSet("Debugger.Keys")) {
				move("Debugger.Keys", "Serialization");
				move("Debugger.Sections", "Debug");
			}

			// Archaic
			if (isSet("Debug") && !(getObject("Debug") instanceof List))
				set("Debug", null);
		}

		{ // Prefix
			if (isSet("Plugin_Prefix"))
				move("Plugin_Prefix", "Prefix");

			if (isSet("Check_Updates"))
				move("Check_Updates", "Notify_Updates");
		}
	}

	/**
	 * 此类是否已被加载？
	 *
	 * @return
	 */
	public static final Boolean isSettingsCalled() {
		return settingsClassCalled;
	}

	/**
	 * 重置表示此类已被加载的标记，
	 * 在重载时使用。
	 */
	public static final void resetSettingsCall() {
		settingsClassCalled = false;
	}
}
