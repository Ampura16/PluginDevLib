/**
 * (c) 2013 - 2019 - 保留所有权利。
 * <p>
 * 除非获得 MineAcademy.org 的书面许可，否则不得分享、复制、复现或出售
 * 本库的任何部分。
 * 所有侵权行为都将被追究。
 * <p>
 * 如果你是 MineAcademy.org 最终用户许可证的个人持有者，
 * 则可以在自己的插件中自用，但不得用于任何其他目的。
 */
package top.brmc.devlib.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import top.brmc.devlib.BungeeUtil;
import top.brmc.devlib.Common;
import top.brmc.devlib.FileUtil;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.annotation.AutoRegister;
import top.brmc.devlib.bungee.BungeeListener;
import top.brmc.devlib.command.RegionCommand;
import top.brmc.devlib.command.SimpleCommand;
import top.brmc.devlib.command.SimpleCommandGroup;
import top.brmc.devlib.command.SimpleSubCommand;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.event.SimpleListener;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.menu.Menu;
import top.brmc.devlib.menu.MenuListener;
import top.brmc.devlib.menu.tool.Tool;
import top.brmc.devlib.menu.tool.ToolsListener;
import top.brmc.devlib.metrics.Metrics;
import top.brmc.devlib.model.DiscordListener;
import top.brmc.devlib.model.FolderWatcher;
import top.brmc.devlib.model.HookManager;
import top.brmc.devlib.model.SimpleHologram;
import top.brmc.devlib.model.SimpleScoreboard;
import top.brmc.devlib.model.SpigotUpdater;
import top.brmc.devlib.region.DiskRegion;
import top.brmc.devlib.remain.CompMetadata;
import top.brmc.devlib.remain.Remain;
import top.brmc.devlib.settings.FileConfig;
import top.brmc.devlib.settings.Lang;
import top.brmc.devlib.settings.SimpleLocalization;
import top.brmc.devlib.settings.SimpleSettings;
import top.brmc.devlib.visual.BlockVisualizer;

import lombok.Getter;
import lombok.NonNull;

/**
 * 表示一个使用增强库功能的基础 Java 插件，
 * 同时实现了监听器以便于使用
 */
public abstract class SimplePlugin extends JavaPlugin implements Listener {

	// ----------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------

	/**
	 * 此插件的实例
	 */
	private static SimplePlugin instance;

	/**
	 * getDescription().getVersion() 的快捷方式
	 */
	@Getter
	private static String version;

	/**
	 * getName() 的快捷方式
	 */
	@Getter
	private static String named;

	/**
	 * getFile() 的快捷方式
	 */
	@Getter
	private static File source;

	/**
	 * getDataFolder() 的快捷方式
	 */
	@Getter
	private static File data;

	/**
	 * 表示插件正在重载的内部标记。
	 */
	@Getter
	private static boolean reloading = false;

	/**
	 * 返回 {@link SimplePlugin} 的实例。
	 * <p>
	 * 建议在你自己的 {@link SimplePlugin} 实现中覆盖此方法，
	 * 这样就能直接获得该实现的实例。
	 *
	 * @return 此实例
	 */
	public static SimplePlugin getInstance() {
		if (instance == null) {
			try {
				instance = JavaPlugin.getPlugin(SimplePlugin.class);

			} catch (final IllegalStateException ex) {
				if (Bukkit.getPluginManager().getPlugin("PlugMan") != null)
					Bukkit.getLogger().severe("Failed to get instance of the plugin, if you reloaded using PlugMan you need to do a clean restart instead.");

				throw ex;
			}

			Objects.requireNonNull(instance, "Cannot get a new instance! Have you reloaded?");
		}

		return instance;
	}

	/**
	 * 获取整个库使用的实例是否已设置。通常
	 * 总是已设置，测试时除外。
	 *
	 * @return 实例是否已设置。
	 */
	public static final boolean hasInstance() {
		return instance != null;
	}

	// ----------------------------------------------------------------------------------------
	// Instance specific
	// ----------------------------------------------------------------------------------------

	/**
	 * 为方便起见，可以在此设置事件监听器和定时任务，
	 * 以便在重载时自动停止/注销它们
	 */
	private final Reloadables reloadables = new Reloadables();

	/**
	 * 内部标记，表示我们是否正在调用 {@link #onReloadablesStart()}
	 * 代码块。在此代码块期间，我们使用 {@link #reloadables} 进行注册
	 */
	private boolean startingReloadables = false;

	/**
	 * 内部布尔值，表示是否可以继续加载插件。
	 */
	private final boolean canLoad = true;

	/**
	 * 临时主命令，由我们自动设置到
	 * {@link #setMainCommand(SimpleCommandGroup)} 中。
	 */
	private SimpleCommandGroup mainCommand;

	/**
	 * 临时 bungee 监听器，参见 {@link #setBungeeCord(BungeeListener)}，
	 * 由我们自动设置。
	 */
	private BungeeListener bungeeListener;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	@Override
	public final void onLoad() {
		MinecraftVersion.parseAndSet(Bukkit.getBukkitVersion());

		// Set the instance
		try {
			getInstance();

		} catch (final Throwable ex) {
			if (MinecraftVersion.olderThan(V.v1_7))
				instance = this; // Workaround
			else
				throw ex;
		}

		// Cache results for best performance
		version = instance.getDescription().getVersion();
		named = instance.getDataFolder().getName();
		source = instance.getFile();
		data = instance.getDataFolder();

		this.loadLibraries();

		// Load libraries where Spigot does not do this automatically
		if (!ReflectionUtil.isClassAvailable("net.md_5.bungee.api.ChatColor") || !ReflectionUtil.isClassAvailable("com.google.gson.Gson")) {
			this.getLogger().severe("Fatal: The required Gson and BungeeCord chat libraries are missing.");
			this.getLogger().severe("Please download BungeeChatAPI and install it as a plugin from:");
			this.getLogger().severe("https://bitbucket.org/kangarko/bungeechatapi/downloads/");
			this.getLogger().severe("");
			this.getLogger().severe("The plugin is now disabled.");

			this.getServer().getPluginManager().disablePlugin(this);
			throw new FoException("Missing libraries, see above for instructions.");
		}

		try {
			Player.class.getMethod("spigot");

		} catch (NoSuchMethodException | SecurityException ex) {
			throw new FoException("CraftBukkt is unsupported! Please use Spigot or Paper to run " + this.getName());
		}

		// Call parent
		this.onPluginLoad();
	}

	/*
	 * Loads libraries from plugin.yml or from getLibraries()
	 */
	private void loadLibraries() {
		final int javaVersion = getJavaVersion();
		final List<Library> libraries = new ArrayList<>();

		// Force add md_5 bungee chat since it's needed
		if (!ReflectionUtil.isClassAvailable("net.md_5.bungee.api.ChatColor"))
			libraries.add(Library.fromMavenRepo("net.md-5", "bungeecord-chat", "1.16-R0.4"));

		if (MinecraftVersion.olderThan(V.v1_16)) {
			final YamlConfiguration pluginFile = new YamlConfiguration();

			// We have to load it using the legacy way for ancient MC versions
			try {
				pluginFile.loadFromString(String.join("\n", FileUtil.getInternalFileContent("plugin.yml")));

			} catch (final Throwable t) {
				throw new RuntimeException(t);
			}

			for (final String libraryPath : pluginFile.getStringList("legacy-libraries")) {
				if (javaVersion < 15 && libraryPath.contains("org.openjdk.nashorn:nashorn-core"))
					continue;

				final Library library = Library.fromMavenRepo(libraryPath);

				libraries.add(library);
			}

			// Load normally
			if (!libraries.isEmpty() && javaVersion >= 9) {
				// Unsupported > upstream should shade libraries manually

			} else
				for (final Library library : libraries)
					library.load();
		}

		// Always load user-defined libraries
		final List<Library> manualLibraries = this.getLibraries();

		// But only on Java 8 (for now)
		if (!manualLibraries.isEmpty() && javaVersion > 8)
			Common.warning("The getLibraries() feature only supports Java 8 for now and does not work on Java " + javaVersion + ". To load the following libraries, "
					+ "install Java 8 or upgrade to Minecraft 16 where you use the 'libraries' feature of plugin.yml to load. Skipping loading: " + manualLibraries);

		else
			methodLibraryLoader:
			for (final Library library : manualLibraries) {

				// Detect conflicts
				for (final Library otherLibrary : libraries)
					if (library.getArtifactId().equals(otherLibrary.getArtifactId()) && library.getGroupId().equals(otherLibrary.getGroupId())) {
						Common.warning("Detected library conflict: '" + library.getGroupId() + "." + library.getArtifactId() + "' is defined both in getLibraries() and plugin.yml! "
								+ "We'll prefer the version from plugin.yml, if you want to use the one from getLibraries() then remove it from your plugin.yml file.");

						continue methodLibraryLoader;
					}

				library.load();
			}
	}

	/**
	 * 要自动下载并加载的库列表。
	 *
	 * **目前需要 JAVA 8**
	 *
	 * @deprecated 需要 Java 8，因此仅适用于安装了该 Java 版本的 Minecraft 1.16 或更低版本
	 * @return
	 */
	@Deprecated
	protected List<Library> getLibraries() {
		return new ArrayList<>();
	}

	@Override
	public final void onEnable() {

		// Disabled upstream
		if (!this.canLoad) {
			this.getLogger().severe("Not loading, the plugin is disabled (look for console errors above)");

			return;
		}

		// Solve reloading issues with PlugMan
		for (final StackTraceElement element : new Throwable().getStackTrace())
			if (element.toString().contains("com.rylinaux.plugman.util.PluginUtil.load")) {
				Common.warning("Detected PlugMan reload, which is poorly designed. "
						+ "It causes Bukkit not able to get our plugin from a static initializer."
						+ " It may or may not run. Use our own reload command or do a clean restart!");

				break;
			}

		// Check if Foundation is correctly moved
		this.checkShading();

		if (!this.isEnabled())
			return;

		// Before all, check if necessary libraries and the minimum required MC version
		if (!this.checkServerVersions0()) {
			this.setEnabled(false);

			return;
		}

		// Load debug mode early
		Debugger.detectDebugMode();

		// Print startup logo early before onPluginPreStart
		// Disable logging prefix if logo is set
		if (this.getStartupLogo() != null) {
			final String oldLogPrefix = Common.getLogPrefix();

			Common.setLogPrefix("");
			Common.log(this.getStartupLogo());
			Common.setLogPrefix(oldLogPrefix);
		}

		// Load our dependency system
		try {
			HookManager.loadDependencies();

		} catch (final Throwable throwable) {
			Common.throwError(throwable, "Error while loading " + this.getDataFolder().getName() + " dependencies!");
		}

		// Return if plugin pre start indicated a fatal problem
		if (!this.isEnabled())
			return;

		try {

			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------

			this.registerInitBungee(BungeeListener.DEFAULT_CHANNEL);

			// Hide plugin name before console messages
			final String oldLogPrefix = Common.getLogPrefix();
			Common.setLogPrefix("");

			this.startingReloadables = true;

			try {
				AutoRegisterScanner.scanAndRegister();

			} catch (final Throwable t) {
				Remain.sneaky(t);

				return;
			}

			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				this.registerEvents(CompMetadata.MetadataFile.getInstance());

			if (this.areRegionsEnabled())
				DiskRegion.loadRegions();

			this.onReloadablesStart();

			this.startingReloadables = false;

			this.onPluginStart();
			// --------------------------------------------

			if (Remain.isEnchantRegistryUnfrozen())
				Remain.freezeEnchantRegistry();

			// Return if plugin start indicated a fatal problem
			if (!this.isEnabled())
				return;

			// Start update check
			if (this.getUpdateCheck() != null)
				this.getUpdateCheck().run();

			// Register our listeners
			this.registerEvents(this);
			this.registerEvents(new FoundationListener());

			if (this.areMenusEnabled())
				this.registerEvents(new MenuListener());

			if (this.areToolsEnabled())
				this.registerEvents(new ToolsListener());

			// Register DiscordSRV listener
			if (HookManager.isDiscordSRVLoaded()) {
				final DiscordListener.DiscordListenerImpl discord = DiscordListener.DiscordListenerImpl.getInstance();

				discord.resubscribe();
				discord.registerHook();

				this.reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			// Finish off by starting metrics (currently bStats)
			if (this.getMetricsPluginId() != -1)
				new Metrics(this.getMetricsPluginId());

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			// Finally, place plugin name before console messages after plugin has (re)loaded
			Common.runLater(() -> Common.setLogPrefix(oldLogPrefix));

		} catch (final Throwable t) {
			this.displayError0(t);
		}
	}

	/**
	 * 返回对应的 Java 主版本号，例如 Java 1.8 返回 8，Java 11 返回 11。
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		String version = System.getProperty("java.version");

		if (version.startsWith("1."))
			version = version.substring(2, 3);

		else {
			final int dot = version.indexOf(".");

			if (dot != -1)
				version = version.substring(0, dot);
		}

		if (version.contains("-"))
			version = version.split("\\-")[0];

		return Integer.parseInt(version);
	}

	/**
	 * 将一个简单 bungee 类注册为自定义 bungeecord 监听器。
	 *
	 * 如果那里只有一个带 getter 的字段，请不要使用此方法，我们已经会自动注册它；
	 * 此方法用于存在多个字段、需要注册多个频道的情况。
	 * 此时只需在 onReloadablesStart 方法中调用此方法并传入该字段即可。
	 */
	protected final void registerBungeeCord(@NonNull BungeeListener bungee) {
		/*final String channelName = bungee.getChannel();
		final Messenger messenger = this.getServer().getMessenger();

		if (!messenger.isIncomingChannelRegistered(this, channelName))
			messenger.registerIncomingPluginChannel(this, channelName, BungeeListener.BungeeListenerImpl.getInstance());

		if (!messenger.isOutgoingChannelRegistered(this, channelName))
			messenger.registerOutgoingPluginChannel(this, channelName);*/

		this.reloadables.registerEvents(bungee);

	}

	/**
	 * 检查 Foundation 是否被正确 shade 的一种取巧方式
	 */
	private void checkShading() {
		try {
			throw new ShadingException();
		} catch (final Throwable t) {
		}
	}

	/**
	 * 用于检查 {@link SimplePlugin} 的实例是否因某种原因与此类的实例不匹配的异常，
	 * 这很可能是由于错误的重新打包或根本没有重新打包造成的
	 * （两个都使用 Foundation 的插件，必须为各自的 Foundation 版本
	 * 使用不同的包名）。
	 * <p>
	 * 也可能是 PlugMan 导致的，对此我们毫不留情。
	 */
	private class ShadingException extends Throwable {
		private static final long serialVersionUID = 1L;

		public ShadingException() {
			if (!SimplePlugin.getNamed().equals(SimplePlugin.this.getDescription().getName())) {
				Bukkit.getLogger().severe("We have a class path problem in the Foundation library");
				Bukkit.getLogger().severe("preventing " + SimplePlugin.this.getDescription().getName() + " from loading correctly!");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("This is likely caused by two plugins having the");
				Bukkit.getLogger().severe("same Foundation library paths - make sure you");
				Bukkit.getLogger().severe("relocale the package! If you are testing using");
				Bukkit.getLogger().severe("Ant, only test one plugin at the time.");
				Bukkit.getLogger().severe("");
				Bukkit.getLogger().severe("Possible cause: " + SimplePlugin.getNamed());
				Bukkit.getLogger().severe("Foundation package: " + SimplePlugin.class.getPackage().getName());

				throw new FoException("Shading exception, see above for details.");
			}
		}
	}

	/**
	 * 检查是否安装了所需的最低 MC 版本
	 *
	 * @return
	 */
	private boolean checkServerVersions0() {

		// Call the static block to test compatibility early
		MinecraftVersion.getCurrent();

		// Check min version
		final V minimumVersion = this.getMinimumVersion();

		if (minimumVersion != null && MinecraftVersion.olderThan(minimumVersion)) {
			Common.logFramed(false,
					this.getDataFolder().getName() + " requires Minecraft " + minimumVersion + " or newer to run.",
					"Please upgrade your server.");

			return false;
		}

		// Check max version
		final V maximumVersion = this.getMaximumVersion();

		if (maximumVersion != null && MinecraftVersion.newerThan(maximumVersion)) {
			Common.logFramed(false,
					this.getDataFolder().getName() + " requires Minecraft " + maximumVersion + " or older to run.",
					"Please downgrade your server or",
					"wait for the new version.");

			return false;
		}

		return true;
	}

	/**
	 * 处理各种启动问题
	 *
	 * @param throwable
	 */
	protected final void displayError0(Throwable throwable) {
		Debugger.printStackTrace(throwable);

		Common.log(
				"&4    ___                  _ ",
				"&4   / _ \\  ___  _ __  ___| |",
				"&4  | | | |/ _ \\| '_ \\/ __| |",
				"&4  | |_| | (_) | |_) \\__ \\_|",
				"&4   \\___/ \\___/| .__/|___(_)",
				"&4             |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + this.getDescription().getName() + " v" + this.getDescription().getVersion() + ", plugin is disabled!",
				" &cRunning on " + Bukkit.getBukkitVersion() + " & Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		if (throwable instanceof InvalidConfigurationException) {
			Common.log(" &cSeems like your config is not a valid YAML.");
			Common.log(" &cUse online services like");
			Common.log(" &chttp://yaml-online-parser.appspot.com/");
			Common.log(" &cto check for syntax errors!");

		} else if (throwable instanceof UnsupportedOperationException || throwable.getCause() != null && throwable.getCause() instanceof UnsupportedOperationException)
			if (this.getServer().getBukkitVersion().startsWith("1.2.5"))
				Common.log(" &cSorry but Minecraft 1.2.5 is no longer supported!");
			else {
				Common.log(" &cUnable to setup reflection!");
				Common.log(" &cYour server is either too old or");
				Common.log(" &cthe plugin broke on the new version :(");
			}

		{
			while (throwable.getCause() != null)
				throwable = throwable.getCause();

			String error = "Unable to get the error message, search above.";
			if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
				error = throwable.getMessage();

			Common.log(" &cError: " + error);
		}
		Common.log("&4!-----------------------------------------------------!");

		this.getPluginLoader().disablePlugin(this);
	}

	// ----------------------------------------------------------------------------------------
	// Shutdown
	// ----------------------------------------------------------------------------------------

	@Override
	public final void onDisable() {

		try {
			this.onPluginStop();
		} catch (final Throwable t) {
			Common.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
			CompMetadata.MetadataFile.getInstance().save();

		this.unregisterReloadables();

		try {
			for (final Player online : Remain.getOnlinePlayers())
				SimpleScoreboard.clearBoardsFor(online);

		} catch (final Throwable t) {
			Common.log("Error clearing scoreboards for players..");

			t.printStackTrace();
		}

		try {
			for (final Player online : Remain.getOnlinePlayers()) {
				final Menu menu = Menu.getMenu(online);

				if (menu != null)
					online.closeInventory();
			}
		} catch (final Throwable t) {
			Common.log("Error closing menu inventories for players..");

			t.printStackTrace();
		}

		if (this.areRegionsEnabled())
			for (final DiskRegion region : DiskRegion.getRegions())
				try {
					region.save();
				} catch (final Throwable t) {
					Common.log("Error saving region " + region.getName() + "...");

					t.printStackTrace();
				}

		Objects.requireNonNull(instance, "Instance of " + this.getDataFolder().getName() + " already nulled!");
		instance = null;
	}

	// ----------------------------------------------------------------------------------------
	// Delegate methods
	// ----------------------------------------------------------------------------------------

	/**
	 * 在插件启动之前调用，参见 {@link JavaPlugin#onLoad()}
	 */
	protected void onPluginLoad() {
	}

	/**
	 * 主加载方法，在准备好加载时调用
	 */
	protected abstract void onPluginStart();

	/**
	 * 即将关闭时调用的主方法
	 */
	protected void onPluginStop() {
	}

	/**
	 * 在设置重载之前调用。
	 */
	protected void onPluginPreReload() {
	}

	/**
	 * 在设置重载之后调用。
	 */
	protected void onPluginReload() {
	}

	/**
	 * 在此注册你的命令、事件、任务和文件。
	 * <p>
	 * 在启动插件、执行 /reload 或调用 {@link #reload()}
	 * 方法时都会调用此方法。
	 */
	protected void onReloadablesStart() {
	}

	// ----------------------------------------------------------------------------------------
	// Reload
	// ----------------------------------------------------------------------------------------

	/**
	 * 尝试重载插件
	 */
	public final void reload() {
		final String oldLogPrefix = Common.getLogPrefix();
		Common.setLogPrefix("");

		reloading = true;

		try {
			Debugger.detectDebugMode();

			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				CompMetadata.MetadataFile.getInstance().save();

			this.unregisterReloadables();
			this.registerInitBungee(BungeeListener.DEFAULT_CHANNEL);

			// Load our dependency system
			try {
				HookManager.loadDependencies();

			} catch (final Throwable throwable) {
				Common.throwError(throwable, "Error while loading " + this.getDataFolder().getName() + " dependencies!");
			}

			this.onPluginPreReload();
			this.reloadables.reload();

			SimpleHologram.onReload();

			this.startingReloadables = true;

			// Register classes
			AutoRegisterScanner.scanAndRegister();

			this.onPluginReload();

			// Something went wrong in the reload pipeline
			if (!this.isEnabled()) {
				this.startingReloadables = false;

				return;
			}

			// Register prefix after
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			Lang.reloadLang();
			Lang.loadPrefixes();

			if (this.areRegionsEnabled())
				DiskRegion.loadRegions();

			this.onReloadablesStart();

			this.startingReloadables = false;

			if (HookManager.isDiscordSRVLoaded()) {
				DiscordListener.DiscordListenerImpl.getInstance().resubscribe();

				this.reloadables.registerEvents(DiscordListener.DiscordListenerImpl.getInstance());
			}

			if (CompMetadata.isLegacy() && CompMetadata.ENABLE_LEGACY_FILE_STORAGE)
				this.registerEvents(CompMetadata.MetadataFile.getInstance());

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + this.getDataFolder().getName() + " " + getVersion());

		} finally {
			Common.setLogPrefix(oldLogPrefix);

			reloading = false;
		}
	}

	private void registerInitBungee(String channelName) {
		final Messenger messenger = this.getServer().getMessenger();

		// Always make the main channel available
		if (!messenger.isIncomingChannelRegistered(this, channelName))
			messenger.registerIncomingPluginChannel(this, channelName, BungeeListener.BungeeListenerImpl.getInstance());

		if (!messenger.isOutgoingChannelRegistered(this, channelName))
			messenger.registerOutgoingPluginChannel(this, channelName);
	}

	private void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		BlockVisualizer.stopAll();
		FolderWatcher.stopThreads();

		FileConfig.clearLoadedSections();

		try {
			if (HookManager.isDiscordSRVLoaded())
				DiscordListener.clearRegisteredListeners();
		} catch (final NoClassDefFoundError ex) {
		}

		try {
			HookManager.unloadDependencies(this);
		} catch (final NoClassDefFoundError ex) {
		}

		this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
		this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);

		Common.cancelTasks();

		this.mainCommand = null;
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * 便捷方法，快速为插件中所有继承给定类的类
	 * 注册事件。
	 *
	 * 注意：类必须有无参构造器，否则不会被注册
	 *
	 * 提示：将 settings.yml 中的 Debug 键设为 ["auto-register"] 可查看注册了哪些内容。
	 *
	 * @param extendingClass
	 */
	protected final <T extends Listener> void registerAllEvents(final Class<T> extendingClass) {

		Valid.checkBoolean(!extendingClass.equals(Listener.class), "registerAllEvents does not support Listener.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleListener.class), "registerAllEvents does not support SimpleListener.class due to conflicts, create your own middle class instead");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			for (final Constructor<?> con : pluginClass.getConstructors())
				if (con.getParameterCount() == 0) {
					final T instance = (T) ReflectionUtil.instantiate(con);

					this.registerEvents(instance);

					continue classLookup;
				}
		}
	}

	/**
	 * 便捷方法，快速为此插件注册事件
	 *
	 * @param listener
	 */
	protected final void registerEvents(final Listener listener) {
		if (this.startingReloadables)
			this.reloadables.registerEvents(listener);
		else
			this.getServer().getPluginManager().registerEvents(listener, this);

		if (listener instanceof DiscordListener)
			((DiscordListener) listener).register();
	}

	/**
	 * 便捷方法，快速注册单个事件
	 *
	 * @param listener
	 */
	protected final void registerEvents(final SimpleListener<? extends Event> listener) {
		if (this.startingReloadables)
			this.reloadables.registerEvents(listener);

		else
			listener.register();
	}

	/**
	 * 便捷方法，快速注册插件中所有继承给定类的
	 * 命令类。
	 *
	 * 注意：类必须有无参构造器，否则不会被注册
	 *
	 * 提示：将 settings.yml 中的 Debug 键设为 ["auto-register"] 可查看注册了哪些内容。
	 *
	 * @param extendingClass
	 */
	protected final <T extends Command> void registerAllCommands(final Class<T> extendingClass) {
		Valid.checkBoolean(!extendingClass.equals(Command.class), "registerAllCommands does not support Command.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleCommand.class), "registerAllCommands does not support SimpleCommand.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleSubCommand.class), "registerAllCommands does not support SubCommand.class");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			if (SimpleSubCommand.class.isAssignableFrom(pluginClass))
				continue;

			try {
				for (final Constructor<?> con : pluginClass.getConstructors())
					if (con.getParameterCount() == 0) {
						final T instance = (T) ReflectionUtil.instantiate(con);

						if (instance instanceof SimpleCommand)
							this.registerCommand(instance);

						else
							this.registerCommand(instance);

						continue classLookup;
					}

			} catch (final LinkageError ex) {
				Common.log("Unable to register commands in '" + pluginClass.getSimpleName() + "' due to error: " + ex);
			}
		}
	}

	/**
	 * 注册 bukkit 命令的便捷方法
	 *
	 * @param command
	 */
	protected final void registerCommand(final Command command) {
		if (command instanceof SimpleCommand)
			((SimpleCommand) command).register();

		else
			Remain.registerCommand(command);
	}

	/**
	 * 调用 {@link SimpleCommandGroup#register()} 的快捷方式
	 *
	 * @param labelAndAliases
	 * @param group
	 */
	protected final void registerCommands(final SimpleCommandGroup group) {
		if (this.startingReloadables)
			this.reloadables.registerCommands(group);

		else
			group.register();
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * 启动时显示的炫酷 logo
	 *
	 * @return 默认为 null
	 */
	protected String[] getStartupLogo() {
		return null;
	}

	/**
	 * 运行所需的最低 MC 版本
	 * <p>
	 * 如果服务器版本低于给定版本，我们会自动
	 * 阻止加载
	 *
	 * @return
	 */
	public MinecraftVersion.V getMinimumVersion() {
		return null;
	}

	/**
	 * 此插件可加载的最高 MC 版本
	 * <p>
	 * 如果服务器版本高于给定版本，我们会自动
	 * 阻止加载
	 *
	 * @return
	 */
	public MinecraftVersion.V getMaximumVersion() {
		return null;
	}

	/**
	 * 如果你在带无参构造器的命令组上使用 \@AutoRegister，
	 * 我们会使用 {@link SimpleSettings#MAIN_COMMAND_ALIASES} 中的标签和别名，
	 * 并在此记录关联。
	 *
	 * @return
	 */
	@Nullable
	public SimpleCommandGroup getMainCommand() {
		return this.mainCommand;
	}

	/**
	 * @deprecated 请勿使用，仅供内部使用
	 * @param group
	 */
	@Deprecated
	public final void setMainCommand(SimpleCommandGroup group) {
		Valid.checkBoolean(this.mainCommand == null, "Main command has already been set to " + this.mainCommand);

		this.mainCommand = group;
	}

	/**
	 * 获取在 {@link SimpleCommandGroup} 帮助中显示的创立年份
	 *
	 * @return 默认为 -1，或创立年份
	 */
	public int getFoundedYear() {
		return -1;
	}

	/**
	 * 获取自动更新检查
	 *
	 * @return
	 */
	public SpigotUpdater getUpdateCheck() {
		return null;
	}

	/**
	 * 如果你想使用 bStats.org 统计系统，
	 * 只需在此返回插件 ID（https://bstats.org/what-is-my-plugin-id），
	 * 我们就会自动开始统计。
	 * <p>
	 * 默认为 -1，表示禁用
	 *
	 * @return
	 */
	public int getMetricsPluginId() {
		return -1;
	}

	/**
	 * Foundation 可以自动为你过滤控制台消息，包括
	 * 来自其他插件或服务器本身的消息，避免不必要的控制台刷屏。
	 *
	 * 你可以返回一个消息列表，这些消息会以“startsWith 或 contains”方式匹配
	 * 并被过滤。
	 *
	 * @return
	 */
	public Set<String> getConsoleFilter() {
		return new HashSet<>();
	}

	/**
	 * 处理正则表达式时，将执行时间限制在指定时长内。
	 * 这可以防止因格式错误的正则（循环）导致服务器卡死/崩溃。
	 *
	 * @return 处理正则表达式的时间限制（毫秒）
	 */
	public int getRegexTimeout() {
		throw new FoException("Must override getRegexTimeout()");
	}

	/**
	 * 用正则检查消息时，是否先去除消息中的颜色？
	 *
	 * @return
	 */
	public boolean regexStripColors() {
		return true;
	}

	/**
	 * 在 {@link Common#compilePattern(String)} 中编译正则表达式时，是否应用 Pattern.CASE_INSENSITIVE？
	 * <p>
	 * 可能会带来轻微的性能损失，但能匹配到更多内容。
	 *
	 * @return
	 */
	public boolean regexCaseInsensitive() {
		return true;
	}

	/**
	 * 在 {@link Common#compilePattern(String)} 中编译正则表达式时，是否应用 Pattern.UNICODE_CASE？
	 * <p>
	 * 可能会带来轻微的性能损失，但对非英语服务器很有用。
	 *
	 * @return
	 */
	public boolean regexUnicode() {
		return true;
	}

	/**
	 * 匹配正则之前是否移除变音符号？
	 * 默认为 true
	 *
	 * @return
	 */
	public boolean regexStripAccents() {
		return true;
	}

	/**
	 * 在 ChatUtil 中检查两个字符串的相似度时，
	 * 是否将带重音的字符替换为不带重音的对应字符？
	 *
	 * @return 默认为 true
	 */
	public boolean similarityStripAccents() {
		return true;
	}

	/**
	 * 返回你使用的默认或“主” bungee 监听器。{@link BungeeUtil#sendPluginMessage(top.brmc.devlib.bungee.BungeeMessageType, Object...)} 会检查它，
	 * 这样你就不必每次都传入频道名称，我们会改用此监听器的频道名称。
	 *
	 * @deprecated 只返回找到的第一个 bungee 监听器；如果你有多个，请勿使用，顺序无法保证
	 * @return
	 */
	@Deprecated
	public final BungeeListener getBungeeCord() {
		return this.bungeeListener;
	}

	/**
	 * 设置第一个有效的 bungee 监听器
	 *
	 * @deprecated 仅供内部使用，请勿使用！只能设置一个 bungee 监听器；如果你有多个，顺序无法保证
	 * @param bungeeListener
	 */
	@Deprecated
	public final void setBungeeCord(BungeeListener bungeeListener) {
		this.bungeeListener = bungeeListener;
	}

	/**
	 * 是否监听 {@link Menu} 类的点击？
	 *
	 * 默认为 true。在此返回 false 会使整个 Foundation 菜单
	 * 系统失效，适用于你想使用自己的菜单系统的情况。
	 *
	 * @return
	 */
	public boolean areMenusEnabled() {
		return true;
	}

	/**
	 * 是否在此插件中监听 {@link Tool} 并
	 * 自动处理点击事件？如果你不使用我们的工具系统，
	 * 禁用它可以提升性能。默认启用。
	 *
	 * @return
	 */
	public boolean areToolsEnabled() {
		return true;
	}

	/**
	 * 是否启用区域系统？会加载 {@link DiskRegion#loadRegions()}
	 * 你仍需手动注册子命令 {@link RegionCommand}。
	 *
	 * @return
	 */
	public boolean areRegionsEnabled() {
		return false;
	}

	/**
	 * 移除控制台聊天中误导性的 [Not Secure] 消息。
	 *
	 * @return
	 */
	public boolean filterInsecureChat() {
		return true;
	}

	/**
	 * 获取插件的 jar 文件
	 */
	@Override
	protected final File getFile() {
		return super.getFile();
	}

	/**
	 * 返回 plugin.yml 中指定的命令
	 *
	 * @deprecated 仍然可用，但 Foundation 为你的命令提供了 SimpleCommand，
	 *                你可以使用 \@AutoRegister 自动注册命令，
	 *                无需使用 plugin.yml。
	 */
	@Deprecated
	@Override
	public final PluginCommand getCommand(final String name) {
		return super.getCommand(name);
	}
}
