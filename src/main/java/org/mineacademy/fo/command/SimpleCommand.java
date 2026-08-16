package org.mineacademy.fo.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TabUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.command.SimpleCommandGroup.MainCommand;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.exception.InvalidCommandArgException;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.NonNull;

/**
 * 替代 Bukkit/Spigot 全部命令功能的简单命令，
 * 可供任何插件使用。
 */
public abstract class SimpleCommand extends Command {

	/**
	 * 表示禁用 tab 补全的空列表
	 */
	protected static final List<String> NO_COMPLETE = Collections.unmodifiableList(new ArrayList<>());

	/**
	 * 默认权限格式：{pluginName}.command.{label}
	 */
	private static String defaultPermission = SimplePlugin.getNamed().toLowerCase() + ".command.{label}";

	/**
	 * 返回默认权限格式
	 *
	 * @return
	 */
	public static String getDefaultPermission() {
		return defaultPermission;
	}

	/**
	 * 设置默认权限格式
	 *
	 * @param permission
	 */
	public static void setDefaultPermission(final String permission) {
		defaultPermission = permission;
	}

	/**
	 * 可设置再次执行命令前的冷却时间。该映射
	 * 存储玩家 uuid 及其上次执行命令的时间。
	 */
	private final ExpiringMap<UUID, Long> cooldownMap = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	/**
	 * 命令标签，例如 /boss 的 boss
	 */
	private final String label;

	/**
	 * 上次执行命令时使用的命令标签
	 *
	 * 该变量在命令运行时动态更新为
	 * 最后已知的标签，例如 /boss 或 /b 会分别
	 * 将其设为 boss 或 b
	 */
	private String currentLabel;

	/**
	 * 该命令是否已注册？
	 */
	private boolean registered = false;

	/**
	 * 仅用于在 {@link #onCommand()} 方法中发送消息的 {@link Common#getTellPrefix()} 自定义前缀，
	 * 为 null 则用 Common#getTellPrefix 的，为空则强制无前缀。
	 */
	private String tellPrefix = null;

	/**
	 * 运行该命令所需的最少参数
	 */
	@Getter
	private int minArguments = 0;

	/**
	 * 再次运行该命令前的冷却时间
	 */
	@Getter
	private int cooldownSeconds = 0;

	/**
	 * 玩家绕过该命令冷却的权限（若已设置冷却）
	 */
	@Getter
	private String cooldownBypassPermission = null;

	/**
	 * 玩家在 {@link #cooldownSeconds} 内尝试运行该命令时的自定义消息。
	 * 默认使用
	 * {@link SimpleLocalization.Commands#COOLDOWN_WAIT} 中的消息
	 * <p>
	 * 提示：用 {duration} 替换距离下次可运行的剩余时间
	 */
	private String cooldownMessage = null;

	/**
	 * 运行该命令的权限。设为 null 则始终允许。
	 *
	 * 默认为 {@link #getDefaultPermission()}
	 */
	@Nullable
	private String permission = null;

	/**
	 * 当首个参数等于 "help" 或 "?" 时，
	 * 是否自动发送用法消息？
	 */
	private boolean autoHandleHelp = true;

	// ----------------------------------------------------------------------
	// Temporary variables
	// ----------------------------------------------------------------------

	/**
	 * 命令发送者，不存在则为 null
	 * <p>
	 * 该变量在命令运行时动态更新为
	 * 最后已知的发送者
	 */
	protected CommandSender sender;

	/**
	 * 命令上次执行时使用的参数
	 * <p>
	 * 该变量在命令运行时动态更新为
	 * 最后已知的参数
	 */
	protected String[] args;

	// ----------------------------------------------------------------------

	/**
	 * 用给定标签创建新的简单命令。
	 * <p>
	 * 用 | 分隔标签以区分标签和别名。
	 * 示例：remove|r|rm 会创建 /remove 命令，
	 * 也可用 /r 和 /rm 别名运行。
	 *
	 * @param label
	 */
	protected SimpleCommand(final String label) {
		this(parseLabel0(label), parseAliases0(label));
	}

	/**
	 * 从列表创建新的简单命令。列表
	 * 首项为主标签，其余为别名。
	 */
	protected SimpleCommand(final StrictList<String> labels) {
		this(parseLabelList0(labels), labels.size() > 1 ? labels.subList(1, labels.size()) : null);
	}

	/**
	 * 创建新的简单命令
	 *
	 * @param label
	 * @param aliases
	 */
	protected SimpleCommand(final String label, final List<String> aliases) {
		super(label);

		// NOTICE:
		// Any usages of "this instanceof" is considered a poor quality code. The reason we use it
		// is that we know for certain these parent classes exist, and we need to navigate
		// developers on their proper usage. We recommend you avoid using "this instanceof" if possible.
		Valid.checkBoolean(!(this instanceof CommandExecutor), "Please do not write 'implements CommandExecutor' for /" + super.getLabel() + " cmd, we already have a listener there");
		Valid.checkBoolean(!(this instanceof TabCompleter), "Please do not write 'implements TabCompleter' for /" + super.getLabel() + " cmd, simply override tabComplete method");

		this.label = label;
		this.setLabel(label);

		if (aliases != null)
			this.setAliases(aliases);

		// Set a default permission for this command
		this.permission = defaultPermission;
	}

	/*
	 * Split the given label by | and get the first part, used as the main label
	 */
	private static String parseLabel0(final String label) {
		Valid.checkNotNull(label, "Label must not be null!");

		return label.split("(\\||\\/)")[0];
	}

	/*
	 * Split the given label by | and use the second and further parts as aliases
	 */
	private static List<String> parseAliases0(final String label) {
		final String[] aliases = label.split("(\\||\\/)");

		return aliases.length > 0 ? Arrays.asList(Arrays.copyOfRange(aliases, 1, aliases.length)) : new ArrayList<>();
	}

	/*
	 * Return the first index from the list or thrown an error if list empty
	 */
	private static String parseLabelList0(final StrictList<String> labels) {
		Valid.checkBoolean(!labels.isEmpty(), "Command label must not be empty!");

		return labels.get(0);
	}

	// ----------------------------------------------------------------------
	// Registration
	// ----------------------------------------------------------------------

	/**
	 * 向 Bukkit 注册此命令。
	 * <p>
	 * 若命令已 {@link #isRegistered()} 则抛错。
	 */
	public final void register() {
		this.register(true);
	}

	/**
	 * 向 Bukkit 注册此命令。
	 * <p>
	 * 若命令已 {@link #isRegistered()} 则抛错。
	 *
	 * @param unregisterOldAliases 若已存在同标签命令，是否
	 *                             移除旧命令的关联别名？这解决了一个
	 *                             ChatControl 中的问题：从 Essentials 插件注销 /tell 会连带
	 *                             注销 Towny 的 /t，而这是不希望发生的。
	 */
	public final void register(final boolean unregisterOldAliases) {
		this.register(true, unregisterOldAliases);
	}

	/**
	 * 向 Bukkit 注册此命令。
	 * <p>
	 * 若命令已 {@link #isRegistered()} 则抛错。
	 *
	 * @param unregisterOldCommand 若存在同标签旧命令是否注销它？
	 * @param unregisterOldAliases 若已存在同标签命令，是否
	 *                             移除旧命令的关联别名？这解决了一个
	 *                             ChatControl 中的问题：从 Essentials 插件注销 /tell 会连带
	 *                             注销 Towny 的 /t，而这是不希望发生的。
	 */
	public final void register(final boolean unregisterOldCommand, final boolean unregisterOldAliases) {
		Valid.checkBoolean(!(this instanceof SimpleSubCommand), "Sub commands cannot be registered!");
		Valid.checkBoolean(!this.registered, "The command /" + this.getLabel() + " has already been registered!");

		if (!this.canRegister())
			return;

		final PluginCommand oldCommand = Bukkit.getPluginCommand(this.getLabel());

		if (oldCommand != null && unregisterOldCommand)
			Remain.unregisterCommand(oldCommand.getLabel(), unregisterOldAliases);

		Remain.registerCommand(this);
		this.registered = true;
	}

	/**
	 * 从 Bukkit 移除该命令。
	 * <p>
	 * 若命令未 {@link #isRegistered()} 则抛错。
	 */
	public final void unregister() {
		Valid.checkBoolean(!(this instanceof SimpleSubCommand), "Sub commands cannot be unregistered!");
		Valid.checkBoolean(this.registered, "The command /" + this.getLabel() + " is not registered!");

		Remain.unregisterCommand(this.getLabel());
		this.registered = false;
	}

	/**
	 * 若该命令可通过 {@link #register()} 方法注册则返回 true。
	 * 默认为 true。
	 *
	 * @return
	 */
	protected boolean canRegister() {
		return true;
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * 执行该命令，更新 sender、label 和 args 变量，
	 * 检查权限（发送者无权限则返回），
	 * 检查最少参数，最后将命令交给子类处理。
	 * <p>
	 * 还包含各种错误处理场景
	 */
	@Override
	public final boolean execute(final CommandSender sender, final String label, final String[] args) {

		if (SimplePlugin.isReloading() || !SimplePlugin.getInstance().isEnabled()) {
			Common.tell(sender, SimpleLocalization.Commands.CANNOT_USE_WHILE_NULL.replace("{state}", SimplePlugin.isReloading() ? SimpleLocalization.Commands.RELOADING : SimpleLocalization.Commands.DISABLED));

			return false;
		}

		// Set variables to re-use later
		this.sender = sender;
		this.currentLabel = label;
		this.args = args;

		// Optional sublabel if this is a sub command
		final String sublabel = this instanceof SimpleSubCommand ? " " + ((SimpleSubCommand) this).getSublabel() : "";

		// Catch "errors" that contain a message to send to the player
		// Measure performance of all commands
		final String lagSection = "Command /" + this.getLabel() + sublabel + (args.length > 0 ? " " + String.join(" ", args) : "");

		try {
			// Prevent duplication since MainCommand delegates this
			if (!(this instanceof MainCommand))
				LagCatcher.start(lagSection);

			// Check if sender has the proper permission
			if (this.getPermission() != null)
				this.checkPerm(this.getPermission());

			// Check for minimum required arguments and print help
			if (args.length < this.getMinArguments() || this.autoHandleHelp && args.length == 1 && ("help".equals(args[0]) || "?".equals(args[0]))) {
				final List<String> messages = new ArrayList<>();

				if (this.getDescription() != null) {
					final String descriptionLabel = SimpleLocalization.Commands.LABEL_DESCRIPTION.trim();

					messages.add(descriptionLabel.contains("{description}") ? descriptionLabel.replace("{description}", "&c" + this.getDescription()) : descriptionLabel + " &c" + this.getDescription());
				}

				if (this.getMultilineUsageMessage() != null) {
					messages.add(SimpleLocalization.Commands.LABEL_USAGE);

					for (final String usage : this.getMultilineUsageMessage())
						messages.add("&c" + usage);

				} else if (this.getUsage() != null) {
					final String usage = this.getUsage();

					messages.add("&c" + (usage.startsWith("/") ? usage : "/{label} " + (this instanceof SimpleSubCommand ? "{sublabel} " : "") + usage));

				} else
					throw new FoException("Either getUsage() or getMultilineUsageMessage() must be implemented for '/" + this.getLabel() + sublabel + "' command!");

				for (final String message : messages)
					Common.tellNoPrefix(sender, this.replacePlaceholders(message));

				return true;
			}

			// Check if we can run this command in time
			if (this.cooldownSeconds > 0)
				this.handleCooldown();

			this.onCommand();

		} catch (final InvalidCommandArgException ex) {
			if (this.getMultilineUsageMessage() == null)
				this.dynamicTellError(ex.getMessage() != null ? ex.getMessage() : SimpleLocalization.Commands.INVALID_SUB_ARGUMENT);
			else {
				this.dynamicTellError(SimpleLocalization.Commands.INVALID_ARGUMENT_MULTILINE);

				for (final String line : this.getMultilineUsageMessage())
					this.tellNoPrefix("&c" + line);
			}

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				this.dynamicTellError(ex.getMessages());

		} catch (final Throwable t) {
			this.dynamicTellError(SimpleLocalization.Commands.ERROR.replace("{error}", t.toString()));

			Common.error(t, "Failed to execute command /" + this.getLabel() + sublabel + " " + String.join(" ", args));

		} finally {

			// Prevent duplication since MainCommand delegates this
			if (!(this instanceof MainCommand))
				LagCatcher.end(lagSection, 8, "{section} took {time} ms");
		}

		return true;
	}

	/*
	 * If messenger is on, we send the message prefixed with Messenger.getErrorPrefix()
	 * otherwise we just send a normal message
	 */
	private void dynamicTellError(final String... messages) {
		if (Messenger.ENABLED)
			for (final String message : messages)
				this.tellError(message);
		else
			this.tell(messages);
	}

	/**
	 * 检查命令冷却是否生效，若在限制时间内运行，
	 * 我们会阻止并告知玩家
	 */
	private void handleCooldown() {
		if (this.isPlayer()) {
			final Player player = this.getPlayer();

			if (this.cooldownBypassPermission != null && this.hasPerm(this.cooldownBypassPermission))
				return;

			if (!this.isCooldownApplied(player))
				return;

			final long lastRun = this.cooldownMap.getOrDefault(player.getUniqueId(), 0L);
			final long difference = (System.currentTimeMillis() - lastRun) / 1000;

			// Check if the command was run earlier within the wait threshold
			if (lastRun != 0)
				this.checkBoolean(difference > this.cooldownSeconds, Common.getOrDefault(this.cooldownMessage, SimpleLocalization.Commands.COOLDOWN_WAIT)
						.replace("{duration}", String.valueOf(this.cooldownSeconds - difference + 1)));

			// Update the last try with the current time
			this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
		}
	}

	/**
	 * 若需自定义特定玩家是否应有该命令的冷却，
	 * 请重写此方法。
	 *
	 * @param player
	 * @return
	 */
	protected boolean isCooldownApplied(final Player player) {
		return true;
	}

	/**
	 * 命令运行时执行。可直接获取 sender 和 args 变量，
	 * 并使用简单命令类中的便捷检查。
	 */
	protected abstract void onCommand();

	/**
	 * 获取自定义多行用法消息，用于替代单行用法消息
	 *
	 * @return 多行自定义用法消息，或 null
	 */
	protected String[] getMultilineUsageMessage() {
		return null;
	}

	// ----------------------------------------------------------------------
	// Convenience checks
	//
	// Here is how they work: When you command is executed, simply call any
	// of these checks. If they fail, an error will be thrown inside of
	// which will be a message for the player.
	//
	// We catch that error and send the message to the player without any
	// harm or console errors to your plugin. That is intended and saves time.
	// ----------------------------------------------------------------------

	/**
	 * 检查玩家是否为控制台，是则抛错
	 *
	 * @throws CommandException
	 */
	protected final void checkConsole() throws CommandException {
		if (!this.isPlayer())
			throw new CommandException("&c" + SimpleLocalization.Commands.NO_CONSOLE);
	}

	/**
	 * 检查当前发送者是否有给定权限
	 *
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull final String perm) throws CommandException {
		if (this.isPlayer() && !this.hasPerm(perm))
			throw new CommandException(this.getPermissionMessage().replace("{permission}", perm));
	}

	/**
	 * 检查给定发送者是否有给定权限
	 *
	 * @param sender
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull final CommandSender sender, @NonNull final String perm) throws CommandException {
		if (this.isPlayer() && !this.hasPerm(sender, perm))
			throw new CommandException(this.getPermissionMessage().replace("{permission}", perm));
	}

	/**
	 * 检查命令参数是否达到最小长度
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(final int minimumLength, final String falseMessage) throws CommandException {
		if (this.args.length < minimumLength)
			this.returnTell((Messenger.ENABLED ? "" : "&c") + falseMessage);
	}

	/**
	 * 便捷方法：条件不满足时，用 {@link SimpleLocalization.Commands#INVALID_ARGUMENT}
	 * 消息为玩家返回命令
	 */
	protected final void checkArgs(final boolean condition) {
		this.checkBoolean(condition, SimpleLocalization.Commands.INVALID_ARGUMENT.replace("{label}", this.getLabel()));
	}

	/**
	 * 检查给定布尔值是否为 true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkBoolean(final boolean value, final String falseMessage) throws CommandException {
		if (!value)
			this.returnTell((Messenger.ENABLED ? "" : "&c") + falseMessage);
	}

	/**
	 * 检查给定布尔值是否为 true，否则返回 {@link #returnInvalidArgs()}
	 *
	 * @param value
	 *
	 * @throws CommandException
	 */
	protected final void checkUsage(final boolean value) throws CommandException {
		if (!value)
			this.returnInvalidArgs();
	}

	/**
	 * 检查给定对象是否不为 null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	protected final void checkNotNull(final Object value, final String messageIfNull) throws CommandException {
		if (value == null)
			this.returnTell((Messenger.ENABLED ? "" : "&c") + messageIfNull);
	}

	/**
	 * 尝试按名称或字符串 UUID 查找离线玩家，若其从未玩过则向发送者发送错误消息，
	 * 查找成功则运行指定回调。
	 *
	 * 离线玩家查找异步执行，回调同步执行。
	 *
	 * @param name 名称或字符串 UUID
	 * @param syncCallback
	 * @throws CommandException
	 */
	protected final void findOfflinePlayer(final String name, final Consumer<OfflinePlayer> syncCallback) throws CommandException {
		if (name.length() == 36 && name.charAt(8) == '-' && name.charAt(13) == '-' && name.charAt(18) == '-' && name.charAt(23) == '-') {
			UUID uuid = null;

			try {
				uuid = UUID.fromString(name);

			} catch (final IllegalArgumentException ex) {
				this.returnTell("&cInvalid UUID '" + name + "'");
			}

			this.findOfflinePlayer(uuid, syncCallback);

		} else
			this.runAsync(() -> {
				final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(name);
				this.checkBoolean(targetPlayer != null && (targetPlayer.isOnline() || targetPlayer.hasPlayedBefore()), SimpleLocalization.Player.NOT_PLAYED_BEFORE.replace("{player}", name));

				this.runLater(() -> syncCallback.accept(targetPlayer));
			});
	}

	/**
	 * 尝试按 UUID 查找离线玩家，将触发回调
	 *
	 * @param uniqueId
	 * @param syncCallback
	 * @throws CommandException
	 */
	protected final void findOfflinePlayer(final UUID uniqueId, final Consumer<OfflinePlayer> syncCallback) throws CommandException {
		this.runAsync(() -> {
			final OfflinePlayer targetPlayer = Remain.getOfflinePlayerByUUID(uniqueId);
			this.checkBoolean(targetPlayer != null && (targetPlayer.isOnline() || targetPlayer.hasPlayedBefore()), SimpleLocalization.Player.INVALID_UUID.replace("{uuid}", uniqueId.toString()));

			this.runLater(() -> syncCallback.accept(targetPlayer));
		});
	}

	/**
	 * 尝试查找未隐身的在线玩家，失败时使用
	 * {@link SimpleLocalization.Player#NOT_ONLINE} 处的消息
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayer(final String name) throws CommandException {
		return this.findPlayer(name, SimpleLocalization.Player.NOT_ONLINE);
	}

	/**
	 * 尝试查找未隐身的在线玩家，失败时使用错误消息
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayer(final String name, final String falseMessage) throws CommandException {
		final Player player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isOnline() && !PlayerUtil.isVanished(player), falseMessage.replace("{player}", name));

		return player;
	}

	/**
	 * 按给定参数索引返回玩家；参数不足时，若发送者是玩家则返回发送者。
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayerOrSelf(final int argsIndex) throws CommandException {
		if (argsIndex >= this.args.length) {
			this.checkBoolean(this.isPlayer(), SimpleLocalization.Commands.CONSOLE_MISSING_PLAYER_NAME);

			return this.getPlayer();
		}

		final String name = this.args[argsIndex];
		final Player player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isOnline(), SimpleLocalization.Player.NOT_ONLINE.replace("{player}", name));

		return player;
	}

	/**
	 * 按给定名称返回玩家；名称为 null 时，若发送者是玩家则返回发送者。
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final Player findPlayerOrSelf(final String name) throws CommandException {
		if (name == null) {
			this.checkBoolean(this.isPlayer(), SimpleLocalization.Commands.CONSOLE_MISSING_PLAYER_NAME);

			return this.getPlayer();
		}

		final Player player = this.findPlayerInternal(name);
		this.checkBoolean(player != null && player.isOnline(), SimpleLocalization.Player.NOT_ONLINE.replace("{player}", name));

		return player;
	}

	/**
	 * 对 Bukkit.getPlayer(name) 的简单调用，若你有按名称获取玩家的
	 * 自定义实现可重写它。
	 *
	 * 用法示例：ChatControl 也可按昵称查找玩家
	 *
	 * @param name
	 * @return
	 */
	protected Player findPlayerInternal(final String name) {
		return Bukkit.getPlayer(name);
	}

	/**
	 * 尝试将给定输入（如 1 hour）转换为
	 * {@link SimpleTime} 对象
	 *
	 * @param raw
	 * @return
	 */
	protected final SimpleTime findTime(final String raw) {
		try {
			return SimpleTime.from(raw);

		} catch (final IllegalArgumentException ex) {
			this.returnTell(SimpleLocalization.Commands.INVALID_TIME.replace("{input}", raw));

			return null;
		}
	}

	/**
	 * 尝试将给定名称转换为 Bukkit 世界，
	 * 若该世界不存在则发送本地化错误消息。
	 *
	 * @param name
	 * @return
	 */
	protected final World findWorld(final String name) {
		if ("~".equals(name)) {
			this.checkBoolean(this.isPlayer(), SimpleLocalization.Commands.CANNOT_AUTODETECT_WORLD);

			return this.getPlayer().getWorld();
		}

		final World world = Bukkit.getWorld(name);

		this.checkNotNull(world, SimpleLocalization.Commands.INVALID_WORLD.replace("{world}", name).replace("{available}", Common.join(Bukkit.getWorlds())));
		return world;
	}

	/**
	 * 尝试将给定名称解析为 CompMaterial，现代和旧版材质均可：
	 * MONSTER_EGG 和 SHEEP_SPAWN_EGG
	 * <p>
	 * 可用 {enum} 或 {item} 变量替换给定名称
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final CompMaterial findMaterial(final String name, final String falseMessage) throws CommandException {
		final CompMaterial found = CompMaterial.fromString(name);

		this.checkNotNull(found, falseMessage.replace("{enum}", name).replace("{item}", name));
		return found;
	}

	/**
	 * 查找某类型的枚举，失败则向玩家打印错误消息。
	 * 可在错误消息中用 {enum} 变量表示名称参数
	 *
	 * @param <T>
	 * @param enumType
	 * @param enumValue
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue, final String falseMessage) throws CommandException {
		return this.findEnum(enumType, enumValue, null, falseMessage);
	}

	/**
	 * 查找某类型的枚举，失败则向玩家打印错误消息。
	 * 可在错误消息中用 {enum} 变量表示名称参数
	 *
	 * 也可用条件过滤某些枚举，若你的函数对它们返回 false，
	 * 则视作它们不存在
	 *
	 * @param <T>
	 * @param enumType
	 * @param enumValue
	 * @param condition
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String enumValue, final Function<T, Boolean> condition, final String falseMessage) throws CommandException {
		T found = null;

		try {
			found = ReflectionUtil.lookupEnum(enumType, enumValue);

			if (!condition.apply(found))
				found = null;

		} catch (final Throwable t) {
			// Not found, pass through below to error out
		}

		this.checkNotNull(found, falseMessage.replace("{enum}", enumValue).replace("{available}", Common.join(enumType.getEnumConstants())));
		return found;
	}

	/**
	 * 便捷方法：解析介于两界限之间的数字。
	 * 可在消息中用 {min} 和 {max} 自动替换
	 *
	 * @param index
	 * @param min
	 * @param max
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final int min, final int max, final String falseMessage) {
		return this.findNumber(Integer.class, index, min, max, falseMessage);
	}

	/**
	 * 便捷方法：解析给定参数索引处的数字
	 *
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final String falseMessage) {
		return this.findNumber(Integer.class, index, falseMessage);
	}

	/**
	 * 便捷方法：解析介于两界限之间的任意数字类型。
	 * 数字可为支持 valueOf(String) 方法的任意类型。
	 * 可在消息中用 {min} 和 {max} 自动替换
	 *
	 * @param <T>
	 * @param numberType
	 * @param index
	 * @param min
	 * @param max
	 * @param falseMessage
	 * @return
	 */
	protected final <T extends Number & Comparable<T>> T findNumber(final Class<T> numberType, final int index, final T min, final T max, String falseMessage) {
		falseMessage = falseMessage.replace("{min}", min + "").replace("{max}", max + "");

		final T number = this.findNumber(numberType, index, falseMessage);
		this.checkBoolean(number.compareTo(min) >= 0 && number.compareTo(max) <= 0, falseMessage);

		return number;
	}

	/**
	 * 便捷方法：解析给定参数索引处的任意数字类型。
	 * 数字可为支持 valueOf(String) 方法的任意类型
	 *
	 * @param <T>
	 * @param numberType
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final <T extends Number> T findNumber(final Class<T> numberType, final int index, final String falseMessage) {
		this.checkBoolean(index < this.args.length, falseMessage);

		try {
			return (T) numberType.getMethod("valueOf", String.class).invoke(null, this.args[index]); // Method valueOf is part of all main Number sub classes, eg. Short, Integer, Double, etc.
		}

		catch (final IllegalAccessException | NoSuchMethodException e) {
			e.printStackTrace();

		} catch (final InvocationTargetException e) {

			// Print stack trace for all exceptions, except NumberFormatException
			// NumberFormatException is expected to happen, in this case we just want to display falseMessage without stack trace
			if (!(e.getCause() instanceof NumberFormatException))
				e.printStackTrace();
		}

		throw new CommandException(this.replacePlaceholders((Messenger.ENABLED ? "" : "&c") + falseMessage.replace("{number}", this.args[index])));
	}

	/**
	 * 便捷方法：解析给定参数索引处的布尔值
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final boolean findBoolean(final int index, final String invalidMessage) {
		this.checkBoolean(index < this.args.length, invalidMessage);

		if (this.args[index].equalsIgnoreCase("true"))
			return true;

		else if (this.args[index].equalsIgnoreCase("false"))
			return false;

		throw new CommandException(this.replacePlaceholders((Messenger.ENABLED ? "" : "&c") + invalidMessage));
	}

	// ----------------------------------------------------------------------
	// Other checks
	// ----------------------------------------------------------------------

	/**
	 * 便捷检查：快速判断发送者是否有
	 * 给定权限。
	 *
	 * 提示：如需更完整的检查，请用 {@link #checkPerm(String)}，
	 * 对方无权限时会自动返回你的命令。
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(final String permission) {
		return this.hasPerm(this.sender, permission);
	}

	/**
	 * 便捷检查：快速判断发送者是否有
	 * 给定权限。
	 *
	 * 提示：如需更完整的检查，请用 {@link #checkPerm(String)}，
	 * 对方无权限时会自动返回你的命令。
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(final CommandSender sender, final String permission) {
		return permission == null ? true : PlayerUtil.hasPerm(sender, permission.replace("{label}", this.getLabel()));
	}

	// ----------------------------------------------------------------------
	// Messaging
	// ----------------------------------------------------------------------

	/**
	 * 向玩家发送消息
	 *
	 * @see Replacer#replaceArray
	 *
	 * @param message
	 * @param replacements
	 */
	protected final void tellReplaced(final String message, final Object... replacements) {
		this.tell(Replacer.replaceArray(message, replacements));
	}

	/**
	 * 向发送者发送交互式聊天组件，不替换任何特殊
	 * 变量，只是执行 {@link SimpleComponent#send(CommandSender...)} 方法
	 * 作为快捷方式
	 *
	 * @param components
	 */
	protected final void tell(final List<SimpleComponent> components) {
		if (components != null)
			this.tell(components.toArray(new SimpleComponent[components.size()]));
	}

	/**
	 * 向发送者发送交互式聊天组件，不替换任何特殊
	 * 变量，只是执行 {@link SimpleComponent#send(CommandSender...)} 方法
	 * 作为快捷方式
	 *
	 * @param components
	 */
	protected final void tell(final SimpleComponent... components) {
		if (components != null)
			for (final SimpleComponent component : components)
				component.send(this.sender);
	}

	/**
	 * 向玩家发送消息列表
	 *
	 * @param messages
	 */
	protected final void tell(final Collection<String> messages) {
		if (messages != null)
			this.tell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * 向玩家发送无插件前缀的多行消息。
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(final Collection<String> messages) {
		this.tellNoPrefix(messages.toArray(new String[messages.size()]));
	}

	/**
	 * 向玩家发送无插件前缀的多行消息。
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(final String... messages) {
		final String oldLocalPrefix = this.tellPrefix;

		this.tellPrefix = "";

		this.tell(messages);

		this.tellPrefix = oldLocalPrefix;
	}

	/**
	 * 向玩家发送多行消息，3 行及以上时省略前缀
	 *
	 * @param messages
	 */
	protected final void tell(String... messages) {

		if (messages == null)
			return;

		final String oldTellPrefix = Common.getTellPrefix();

		if (this.tellPrefix != null)
			Common.setTellPrefix(this.tellPrefix);

		try {
			messages = this.replacePlaceholders(messages);

			if (messages.length > 2)
				Common.tellNoPrefix(this.sender, messages);
			else
				Common.tell(this.sender, messages);

		} finally {
			Common.setTellPrefix(oldTellPrefix);
		}
	}

	/**
	 * 向玩家发送无前缀消息
	 *
	 * @param message
	 */
	protected final void tellSuccess(String message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			Messenger.success(this.sender, message);
		}
	}

	/**
	 * 向玩家发送无前缀消息
	 *
	 * @param message
	 */
	protected final void tellInfo(String message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			Messenger.info(this.sender, message);
		}
	}

	/**
	 * 向玩家发送无前缀消息
	 *
	 * @param message
	 */
	protected final void tellWarn(String message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			Messenger.warn(this.sender, message);
		}
	}

	/**
	 * 向玩家发送无前缀消息
	 *
	 * @param message
	 */
	protected final void tellError(String message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			Messenger.error(this.sender, message);
		}
	}

	/**
	 * 向玩家发送无前缀消息
	 *
	 * @param message
	 */
	protected final void tellQuestion(String message) {
		if (message != null) {
			message = this.replacePlaceholders(message);

			Messenger.question(this.sender, message);
		}
	}

	/**
	 * 便捷方法：用 {@link SimpleLocalization.Commands#INVALID_ARGUMENT}
	 * 消息为玩家返回命令
	 */
	protected final void returnInvalidArgs() {
		this.tellError(SimpleLocalization.Commands.INVALID_ARGUMENT.replace("{label}", this.getLabel()));

		throw new CommandException();
	}

	/**
	 * 向玩家发送消息并抛出消息错误，阻止继续执行
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(final Collection<String> messages) throws CommandException {
		this.returnTell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * 向玩家发送消息并抛出消息错误，阻止继续执行
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(final String... messages) throws CommandException {
		throw new CommandException(this.replacePlaceholders(messages));
	}

	/**
	 * 呵呵呵，向发送者返回命令用法
	 *
	 * @throws InvalidCommandArgException
	 */
	protected final void returnUsage() throws InvalidCommandArgException {
		throw new InvalidCommandArgException();
	}

	// ----------------------------------------------------------------------
	// Placeholder
	// ----------------------------------------------------------------------

	/**
	 * 替换全部消息中的占位符。
	 * 要修改它们请重写 {@link #replacePlaceholders(String)}
	 *
	 * @param messages
	 * @return
	 */
	protected final String[] replacePlaceholders(final String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = this.replacePlaceholders(messages[i]).replace("{prefix}", Common.getTellPrefix());

		return messages;
	}

	/**
	 * 替换消息中的占位符
	 *
	 * @param message
	 * @return
	 */
	protected String replacePlaceholders(String message) {
		// Replace basic labels
		message = this.replaceBasicPlaceholders0(message);

		// Replace {X} with arguments
		for (int i = 0; i < this.args.length; i++)
			message = message.replace("{" + i + "}", Common.getOrEmpty(this.args[i]));

		return message;
	}

	/**
	 * 替换 {label} 和 {sublabel} 的内部方法
	 *
	 * @param message
	 * @return
	 */
	private String replaceBasicPlaceholders0(String message) {

		// First, replace label and sublabel
		message = message
				.replace("{label}", Common.getOrDefault(this.label, this.label))
				.replace("{current_label}", Common.getOrDefault(this.currentLabel, this.label))
				.replace("{sublabel}", this instanceof SimpleSubCommand ? ((SimpleSubCommand) this).getSublabels()[0] : this.args != null && this.args.length > 0 ? this.args[0] : super.getLabel())
				.replace("{current_sublabel}", this instanceof SimpleSubCommand ? ((SimpleSubCommand) this).getSublabel() : this.args != null && this.args.length > 0 ? this.args[0] : super.getLabel());

		// Replace hard variables
		message = Variables.replace(message, null);

		return message;
	}

	/**
	 * 安全更新参数的工具方法，若位置过高则扩充参数
	 * <p>
	 * 用于占位符
	 *
	 * @param position
	 * @param value
	 */
	protected final void setArg(final int position, final String value) {
		if (this.args.length <= position)
			this.args = Arrays.copyOf(this.args, position + 1);

		this.args[position] = value;
	}

	/**
	 * 返回参数中最后一个词的便捷方法
	 *
	 * @return
	 */
	protected final String getLastArg() {
		return this.args.length > 0 ? this.args[this.args.length - 1] : "";
	}

	/**
	 * 复制并返回 {@link #args} 中从给定范围
	 * 到末尾的参数
	 *
	 * @param from
	 * @return
	 */
	protected final String[] rangeArgs(final int from) {
		return this.rangeArgs(from, this.args.length);
	}

	/**
	 * 复制并返回 {@link #args} 中从给定范围
	 * 到给定末尾的参数
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final String[] rangeArgs(final int from, final int to) {
		return Arrays.copyOfRange(this.args, from, to);
	}

	/**
	 * 复制并返回 {@link #args} 中从给定范围
	 * 到末尾并用空格连接的参数
	 *
	 * @param from
	 * @return
	 */
	protected final String joinArgs(final int from) {
		return this.joinArgs(from, this.args.length);
	}

	/**
	 * 复制并返回 {@link #args} 中从给定范围
	 * 到给定末尾并用空格连接的参数
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final String joinArgs(final int from, final int to) {
		String message = "";

		for (int i = from; i < this.args.length && i < to; i++)
			message += this.args[i] + (i + 1 == this.args.length ? "" : " ");

		return message;
	}

	// ----------------------------------------------------------------------
	// Tab completion
	// ----------------------------------------------------------------------

	/**
	 * 当给定发送者用给定参数输入命令时显示 tab 补全建议。
	 * <p>
	 * 仅当发送者拥有 {@link #getPermission()} 时才显示 tab 补全
	 *
	 * @param sender
	 * @param alias
	 * @param args
	 * @param location
	 * @return
	 * @deprecated 请勿使用
	 */
	@Deprecated
	@Override
	public final List<String> tabComplete(final CommandSender sender, final String alias, final String[] args, final Location location) throws IllegalArgumentException {
		return this.tabComplete(sender, alias, args);
	}

	/**
	 * 当给定发送者用给定参数输入命令时显示 tab 补全建议。
	 * <p>
	 * 仅当发送者拥有 {@link #getPermission()} 时才显示 tab 补全
	 *
	 * @param sender
	 * @param alias
	 * @param args
	 * @return
	 */
	@Override
	public final List<String> tabComplete(final CommandSender sender, final String alias, final String[] args) throws IllegalArgumentException {
		this.sender = sender;
		this.currentLabel = alias;
		this.args = args;

		if (this.hasPerm(this.getPermission())) {
			List<String> suggestions = this.tabComplete();

			// Return online player names when suggestions are null - simulate Bukkit behaviour
			if (suggestions == null)
				suggestions = this.completeLastWordPlayerNames();

			return suggestions;
		}

		return new ArrayList<>();
	}

	/**
	 * 重写此方法以支持命令的 tab 补全。
	 * <p>
	 * 然后可照常使用 {@link SimpleCommand} 类中的 "sender"、"label" 或 "args" 字段，
	 * 并返回 tab 补全建议列表。
	 * <p>
	 * 我们已检查 {@link #getPermission()}，仅当
	 * 发送者拥有权限时才调用此方法。
	 * <p>
	 * 提示：为方便起见，请使用 {@link #completeLastWord(Iterable)} 和
	 * {@link #getLastArg()} 方法（位于 {@link SimpleCommand} 中）
	 *
	 * @return 要补全的建议列表，或 null（自动补全玩家名）
	 */
	protected List<String> tabComplete() {
		return null;
	}

	/**
	 * 便捷方法：补全发送者可见且未隐身的全部玩家名。
	 * <p>
	 * 提示：直接返回 null 效果相同
	 *
	 * @return
	 */
	protected List<String> completeLastWordPlayerNames() {
		return TabUtil.complete(this.getLastArg(), this.isPlayer() ? Common.getPlayerNames(false) : Common.getPlayerNames());
	}

	/**
	 * 补全全部世界名的便捷方法
	 *
	 * @return
	 */
	protected List<String> completeLastWordWorldNames() {
		return this.completeLastWord(Common.getWorldNames());
	}

	/**
	 * 便捷方法：用给定建议自动补全最后一个词。
	 * 我们会排序并只选择最后一个词
	 * 开头匹配的建议。
	 *
	 * @param <T>
	 * @param suggestions
	 * @return
	 */
	@SafeVarargs
	protected final <T> List<String> completeLastWord(final T... suggestions) {
		return TabUtil.complete(this.getLastArg(), suggestions);
	}

	/**
	 * 便捷方法：用给定建议自动补全最后一个词。
	 * 我们会排序并只选择最后一个词
	 * 开头匹配的建议。
	 *
	 * @param <T>
	 * @param suggestions
	 * @return
	 */
	protected final <T> List<String> completeLastWord(final Iterable<T> suggestions) {
		final List<T> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(suggestion);

		return TabUtil.complete(this.getLastArg(), list.toArray());
	}

	/**
	 * 便捷方法：用给定建议（先转为字符串）自动补全
	 * 最后一个词。我们会排序并只选择最后一个词
	 * 开头匹配的建议。
	 *
	 * @param <T>
	 * @param suggestions
	 * @param toString
	 * @return
	 */
	protected final <T> List<String> completeLastWord(final Iterable<T> suggestions, final Function<T, String> toString) {
		final List<String> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(toString.apply(suggestion));

		return TabUtil.complete(this.getLastArg(), list.toArray());
	}

	// ----------------------------------------------------------------------
	// Temporary variables and safety
	// ----------------------------------------------------------------------

	/**
	 * 尝试将发送者作为玩家获取，仅当发送者确为玩家时有效，
	 * 否则返回 null
	 *
	 * @return
	 */
	protected final Player getPlayer() {
		return this.isPlayer() ? (Player) this.getSender() : null;
	}

	/**
	 * 返回发送者是否为存活玩家
	 *
	 * @return
	 */
	protected final boolean isPlayer() {
		return this.sender instanceof Player;
	}

	/**
	 * 设置该命令 tell 消息中使用的自定义前缀。
	 * 这会覆盖 {@link Common#getTellPrefix()}，但若
	 * {@link #addTellPrefix} 被禁用则无效
	 *
	 * @param tellPrefix
	 */
	protected final void setTellPrefix(final String tellPrefix) {
		this.tellPrefix = tellPrefix;
	}

	/**
	 * 设置运行该命令所需的最少参数
	 *
	 * @param minArguments
	 */
	protected final void setMinArguments(final int minArguments) {
		Valid.checkBoolean(minArguments >= 0, "Minimum arguments must be 0 or greater");

		this.minArguments = minArguments;
	}

	/**
	 * 设置同一玩家再次执行该命令前的等待时间
	 *
	 * @param cooldown
	 * @param unit
	 */
	protected final void setCooldown(final int cooldown, final TimeUnit unit) {
		Valid.checkBoolean(cooldown >= 0, "Cooldown must be >= 0 for /" + this.getLabel());

		this.cooldownSeconds = (int) unit.toSeconds(cooldown);
	}

	/**
	 * 设置绕过冷却的权限，仅在已设置 {@link #setCooldown(int, TimeUnit)} 时有效
	 *
	 * @param cooldownBypassPermission
	 */
	protected final void setCooldownBypassPermission(final String cooldownBypassPermission) {
		this.cooldownBypassPermission = cooldownBypassPermission;
	}

	/**
	 * 设置自定义冷却消息，默认使用 {@link SimpleLocalization.Commands#COOLDOWN_WAIT} 中的消息
	 * <p>
	 * 用 {duration} 动态替换剩余时间
	 *
	 * @param cooldownMessage
	 */
	protected final void setCooldownMessage(final String cooldownMessage) {
		this.cooldownMessage = cooldownMessage;
	}

	/**
	 * 获取该命令的权限，你设置的或本地化中的
	 */
	@Override
	public final String getPermissionMessage() {
		return Common.getOrDefault(super.getPermissionMessage(), SimpleLocalization.NO_PERMISSION);
	}

	/**
	 * 默认检查玩家是否有你在 setPermission 中设置的权限。
	 * <p>
	 * 若为 null，我们检查以下权限：
	 * {yourpluginname}.command.{label}（{@link SimpleCommand} 用）
	 * {yourpluginname}.command.{label}.{sublabel}（{@link SimpleSubCommand} 用）
	 * <p>
	 * 我们会自动处理缺权限的情况，玩家无权限时
	 * 返回无权限消息。
	 *
	 * @return
	 */
	@Override
	public final String getPermission() {
		return this.permission == null ? null : this.replaceBasicPlaceholders0(this.permission);
	}

	/**
	 * 获取不替换变量的权限
	 *
	 * @return
	 * @deprecated 仅供内部使用
	 */
	@Deprecated
	protected final String getRawPermission() {
		return this.permission;
	}

	/**
	 * 设置运行该命令所需的权限。若将权限设为
	 * null，我们将不要求任何权限（不安全）。
	 *
	 * @param permission
	 */
	@Override
	public final void setPermission(final String permission) {
		// Apparently Spigot/Paper sends "Unknown" command when this is set
		//super.setPermission(permission);

		this.permission = permission;
	}

	/**
	 * 获取该命令的发送者
	 *
	 * @return
	 */
	protected final CommandSender getSender() {
		Valid.checkNotNull(this.sender, "Sender cannot be null");

		return this.sender;
	}

	/**
	 * 获取该命令的别名
	 */
	@Override
	public final List<String> getAliases() {
		return super.getAliases();
	}

	/**
	 * 获取该命令的描述
	 */
	@Override
	public final String getDescription() {
		return super.getDescription();
	}

	/**
	 * 获取该命令的名称
	 */
	@Override
	public final String getName() {
		return super.getName();
	}

	/**
	 * 获取该命令的用法消息
	 */
	@Override
	public final String getUsage() {
		final String bukkitUsage = super.getUsage();

		return bukkitUsage.equals("/" + this.getLabel()) || bukkitUsage.equals("/" + this.getCurrentLabel()) ? "" : bukkitUsage;
	}

	/**
	 * 获取该命令最近使用的标签
	 */
	@Override
	public final String getLabel() {
		return this.label;
	}

	/**
	 * 更新该命令的标签
	 */
	@Override
	public final boolean setLabel(final String label) {
		this.currentLabel = label;

		return super.setLabel(label);
	}

	/**
	 * 获取命令创建时或最后更新时的标签
	 *
	 * @return
	 */
	public final String getCurrentLabel() {
		return Common.getOrDefault(this.currentLabel, this.label);
	}

	/**
	 * 设置是否在 {@link #getMinArguments()} 中及
	 * 首个参数为 "help" 或 "?" 时自动显示用法参数
	 * <p>
	 * 默认为 true
	 *
	 * @param autoHandleHelp
	 */
	protected final void setAutoHandleHelp(final boolean autoHandleHelp) {
		this.autoHandleHelp = autoHandleHelp;
	}

	// ----------------------------------------------------------------------
	// Scheduling
	// ----------------------------------------------------------------------

	/**
	 * 延后运行给定任务，支持 checkX 方法，
	 * 我们会自动处理向玩家发送消息
	 *
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runLater(final Runnable runnable) {
		return Common.runLater(() -> this.delegateTask(runnable));
	}

	/**
	 * 延后运行给定任务，支持 checkX 方法，
	 * 我们会自动处理向玩家发送消息
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runLater(final int delayTicks, final Runnable runnable) {
		return Common.runLater(delayTicks, () -> this.delegateTask(runnable));
	}

	/**
	 * 异步运行给定任务，支持 checkX 方法，
	 * 我们会自动处理向玩家发送消息
	 *
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runAsync(final Runnable runnable) {
		return Common.runAsync(() -> this.delegateTask(runnable));
	}

	/**
	 * 异步运行给定任务，支持 checkX 方法，
	 * 我们会自动处理向玩家发送消息
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final BukkitTask runAsync(final int delayTicks, final Runnable runnable) {
		return Common.runLaterAsync(delayTicks, () -> this.delegateTask(runnable));
	}

	/*
	 * A helper method to catch command-related exceptions from runnables
	 */
	private void delegateTask(final Runnable runnable) {
		try {
			runnable.run();

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				for (final String message : ex.getMessages())
					if (Messenger.ENABLED)
						Messenger.error(this.sender, message);
					else
						Common.tell(this.sender, message);

		} catch (final Throwable t) {
			final String errorMessage = SimpleLocalization.Commands.ERROR.replace("{error}", t.toString());

			if (Messenger.ENABLED)
				Messenger.error(this.sender, errorMessage);
			else
				Common.tell(this.sender, errorMessage);

			throw t;
		}
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SimpleCommand ? ((SimpleCommand) obj).getLabel().equals(this.getLabel()) && ((SimpleCommand) obj).getAliases().equals(this.getAliases()) : false;
	}

	@Override
	public String toString() {
		return "Command{label=/" + this.label + "}";
	}
}
