package top.brmc.devlib.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import top.brmc.devlib.Common;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.SerializeUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.plugin.SimplePlugin;

import lombok.Getter;
import lombok.NonNull;

/**
 * 一种为玩家渲染自定义计分板的简单方式，几乎不会闪烁。
 * 使用 &c 占 2 个字符。由于文本会被拆分两次，带颜色的文本长度 = (total - (2 * 2)) = (total - 4)
 * 最大行长度：
 * - 1.8：带颜色 66，不带颜色 70
 * - 1.13：带颜色 98，不带颜色 104
 * - 1.18：带颜色 32889，不带颜色 32895
 * 最大标题长度：
 * - 1.8：带颜色 30，不带颜色 32
 * - 1.13：带颜色 126，不带颜色 128
 *
 * @author kangarko and Tijn (<a href="https://github.com/Tvhee-Dev">Tvhee-Dev</a>)
 */
public class SimpleScoreboard {

	// ------------------------------------------------------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------------------------------------------------------

	private static final String COLOR_CHAR = "\u00A7";

	/**
	 * 用于特定队伍条目的唯一聊天颜色标识符
	 */
	private static final String[] COLORS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	/**
	 * 所有活动计分板的列表（在创建新实例时加入）
	 */
	@Getter
	private static final List<SimpleScoreboard> registeredBoards = new ArrayList<>();

	/**
	 * 存储的计分板行
	 */
	private final List<String> rows = new ArrayList<>();

	/**
	 * 存储的正在查看此计分板的玩家
	 */

	private final List<UUID> viewers = new ArrayList<>();

	/**
	 * “键: 值”对的配色主题，例如
	 * <p>
	 * Players: 12
	 * Mode: playing
	 */
	private final String[] theme = new String[2];

	/**
	 * 此计分板的标题
	 */
	private String title;

	/**
	 * 更新间隔（tick）
	 */
	@Getter
	private int updateDelayTicks = 20;

	/**
	 * 正在运行的更新任务
	 */
	private BukkitTask updateTask;

	/**
	 * 创建一个每秒更新一次的新计分板
	 */

	public SimpleScoreboard() {
		registeredBoards.add(this);
	}

	public SimpleScoreboard(String title) {
		this(title, 20);
	}

	/**
	 * 创建一个可自定义 updateDelayTicks 的新计分板
	 * @param title
	 * @param updateDelayTicks
	 */
	public SimpleScoreboard(String title, int updateDelayTicks) {
		// Scoreboards were introduced in 1.5, objectives were added in 1.7.2
		Valid.checkBoolean(MinecraftVersion.atLeast(MinecraftVersion.V.v1_7), "Scoreboards (with objectives) are not supported below Minecraft version 1.7!");

		this.setTitle(title);
		this.setUpdateDelayTicks(updateDelayTicks);

		registeredBoards.add(this);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 清除已注册的计分板，通常在重载时调用
	 */
	public static void clearBoards() {
		registeredBoards.clear();
	}

	/**
	 * 移除玩家的所有计分板
	 *
	 * @param player
	 */
	public static void clearBoardsFor(final Player player) {
		for (final SimpleScoreboard scoreboard : registeredBoards)
			if (scoreboard.isViewing(player))
				scoreboard.hide(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Public entries
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 为给定玩家替换消息中的变量
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	protected String replaceVariables(final @NonNull Player player, final @NonNull String message) {
		return message;
	}

	/**
	 * 此计分板每次 tick 时调用
	 */
	protected void onUpdate() {
	}

	public final String getTitle() {
		return this.title;
	}

	/**
	 * @param title 要设置的标题
	 */
	public final void setTitle(String title) {
		final int maxTitleLength = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13) ? 128 : 32;

		this.title = title.length() > maxTitleLength ? title.substring(0, maxTitleLength) : title;
		this.title = this.title.endsWith(COLOR_CHAR) ? this.title.substring(0, this.title.length() - 1) : this.title;
	}

	/**
	 * 返回可修改的行列表
	 *
	 * @return
	 */
	public List<String> getRows() {
		return this.rows;
	}

	/**
	 * 若计分板正在运行和渲染则返回 true
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return this.updateTask != null;
	}

	/**
	 * @param updateDelayTicks 要设置的 updateDelayTicks
	 */
	public final void setUpdateDelayTicks(int updateDelayTicks) {
		this.updateDelayTicks = updateDelayTicks;
	}

	/**
	 * 向玩家显示此计分板
	 *
	 * @param player
	 */
	public final void show(final Player player) {
		Valid.checkBoolean(!this.isViewing(player), "Player " + player.getName() + " is already viewing scoreboard: " + this);

		if (this.title == null)
			this.title = "";

		if (this.updateTask == null)
			this.start();

		final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		this.viewers.add(player.getUniqueId());
		player.setScoreboard(scoreboard);
	}

	/**
	 * 对玩家隐藏此计分板
	 *
	 * @param player
	 */
	public final void hide(final Player player) {
		Valid.checkBoolean(this.isViewing(player), "Player " + player.getName() + " is not viewing scoreboard: " + this.getTitle());

		player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		this.viewers.remove(player.getUniqueId());

		if (this.viewers.isEmpty())
			this.cancelUpdateTask();
	}

	/**
	 * 若给定玩家正在查看此计分板则返回 true
	 *
	 * @param player
	 * @return
	 */
	public final boolean isViewing(final Player player) {
		return this.viewers.contains(player.getUniqueId());
	}

	/**
	 * 为包含 : 的行设置配色主题，例如
	 * <p>
	 * Players: 12
	 * Mode: playing
	 * <p>
	 * 直接填写颜色代码即可使用
	 *
	 * @param primary
	 * @param secondary
	 */
	public final void setTheme(@NonNull final ChatColor primary, final ChatColor secondary) {
		if (secondary != null) {
			this.theme[0] = "&" + primary.getChar();
			this.theme[1] = "&" + secondary.getChar();
		} else
			this.theme[0] = "&" + primary.getChar();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Add new rows
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 向计分板添加行
	 *
	 * @param entries
	 */
	public final void addRows(final Object... entries) {
		this.addRows(Arrays.asList(entries));
	}

	/**
	 * 向计分板添加行
	 *
	 * @param entries
	 */
	public final void addRows(final List<Object> entries) {
		Valid.checkBoolean((this.rows.size() + entries.size()) <= 15, "You are trying to add too many rows (the limit is 15)");
		final List<String> lines = new ArrayList<>();

		for (final Object object : entries)
			lines.add(object == null ? "" : Common.colorize(SerializeUtil.serialize(SerializeUtil.Mode.YAML, object).toString()));

		this.rows.addAll(lines);
	}

	/**
	 * 修改给定索引处的行（若存在）
	 *
	 * @param index
	 * @param value
	 */
	public final void setRow(final int index, final String value) {
		Valid.checkBoolean(index < this.rows.size(), "The row for index " + index + " is currently not existing. Please use addRows()!");

		this.rows.set(index, value == null ? "" : value);
	}

	/**
	 * 移除所有行
	 */
	public final void clearRows() {
		this.rows.clear();
	}

	/**
	 * 移除给定索引处的行
	 *
	 * @param index
	 */
	public final void removeRow(final int index) {
		this.rows.remove(index);
	}

	/**
	 * 移除包含给定文本的行
	 *
	 * @param thatContains
	 */
	public final void removeRow(final String thatContains) {
		this.rows.removeIf(row -> row.contains(thatContains));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Start / stop
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 开始显示此计分板
	 */
	private void start() {
		Valid.checkBoolean(this.updateTask == null, "Scoreboard " + this + " already running");

		this.updateTask = Bukkit.getScheduler().runTaskTimer(SimplePlugin.getInstance(), () -> {
			try {
				this.onUpdate();

				for (final UUID viewerId : new ArrayList<>(this.viewers)) {
					final Player viewer = Bukkit.getPlayer(viewerId);

					if (viewer == null || !viewer.isOnline()) {
						this.viewers.remove(viewerId);
						continue;
					}

					this.reloadEntries(viewer);
				}

			} catch (final Throwable t) {
				Common.error(t,
						"Error displaying " + this,
						"Entries: " + this.rows,
						"Title: " + this.title,
						"%error",
						"Stopping rendering for safety.");

				this.stop();
			}
		}, 0, this.updateDelayTicks);
	}

	/**
	 * 停止此计分板并将其从所有查看者处移除
	 */
	public final void stop() {
		this.viewers.forEach(viewerId -> {
			final Player viewer = Bukkit.getPlayer(viewerId);

			if (viewer != null && viewer.isOnline())
				viewer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		});

		this.viewers.clear();

		if (this.updateTask != null)
			this.cancelUpdateTask();
	}

	@Override
	public final String toString() {
		return "Scoreboard{title=" + this.getTitle() + "}";
	}

	// ------------------------------------------------------------------------------------------------------------
	// Private
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 如适用，为该行添加主题颜色
	 *
	 * @param row
	 * @return
	 */
	private String replaceTheme(final String row) {
		if (row.contains(":"))
			if (this.theme.length == 1)
				return this.theme[0] + row;

			else if (this.theme[0] != null) {
				final String[] split = row.split("\\:");

				if (split.length > 1)
					return this.theme[0] + split[0] + ":" + this.theme[1] + split[1];
			}

		return row;
	}

	/**
	 * 取消更新任务
	 */
	private void cancelUpdateTask() {
		Valid.checkNotNull(this.updateTask, "Scoreboard " + this + " not running");

		this.updateTask.cancel();
		this.updateTask = null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 为给定玩家重新加载条目
	 *
	 * @param player
	 */
	private void reloadEntries(Player player) throws IllegalArgumentException {
		final String colorizedTitle = Common.colorize(this.title);
		final Scoreboard scoreboard = player.getScoreboard();
		final List<String> rowsDone = new ArrayList<>();
		Objective mainboard = scoreboard.getObjective("mainboard");

		if (mainboard == null) {
			mainboard = scoreboard.registerNewObjective("mainboard", "dummy");
			mainboard.setDisplayName(colorizedTitle);
			mainboard.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		if (!mainboard.getDisplayName().equals(colorizedTitle))
			mainboard.setDisplayName(colorizedTitle);

		for (int lineNumber = 0; lineNumber < 15; lineNumber++) {
			final int scoreboardLineNumber = this.rows.size() - lineNumber;
			Team line = scoreboard.getTeam("line" + scoreboardLineNumber);

			if (lineNumber < this.rows.size()) {
				if (line == null)
					line = scoreboard.registerNewTeam("line" + scoreboardLineNumber);

				final String scoreboardLineRaw = this.rows.get(lineNumber).replace("{player}", player.getName());
				final boolean mc1_13 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);
				final boolean mc1_18 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_18);
				final String finishedRow = Common.colorize(replaceTheme(this.replaceVariables(player, scoreboardLineRaw)));
				final boolean rowUsed = rowsDone.contains(finishedRow);
				final int[] splitPoints = { mc1_13 ? 64 : 16, mc1_18 ? 32767 : 40, mc1_13 ? 64 : 16 };

				if (rowUsed)
					splitPoints[1] = splitPoints[1] - 2;

				final List<String> copy = copyColors(finishedRow, splitPoints);
				final String prefix = copy.isEmpty() ? "" : copy.get(0);
				String entry = copy.size() < 2 ? COLOR_CHAR + COLORS[lineNumber] + COLOR_CHAR + "r" : copy.get(1) + (rowUsed ? COLOR_CHAR + COLORS[lineNumber] : "");

				if (MinecraftVersion.olderThan(V.v1_13) && entry.length() > 16)
					entry = entry.substring(0, 16);

				final String suffix = copy.size() < 3 ? "" : copy.get(2);
				String oldEntry = null;

				if (!line.getPrefix().equals(prefix))
					line.setPrefix(prefix);

				if (line.getEntries().size() > 1) {
					for (final String teamEntry : line.getEntries()) {
						line.removeEntry(teamEntry);
						scoreboard.resetScores(teamEntry);
					}
				}

				if (!line.getEntries().contains(entry)) {
					if (!line.getEntries().isEmpty()) {
						oldEntry = new ArrayList<>(line.getEntries()).get(0);

						line.removeEntry(oldEntry);
					}

					line.addEntry(entry);
				}

				if (!line.getSuffix().equals(suffix))
					line.setSuffix(suffix);

				if (oldEntry != null)
					scoreboard.resetScores(oldEntry);

				mainboard.getScore(entry).setScore(scoreboardLineNumber);
				rowsDone.add(finishedRow);
			} else if (line != null) {
				for (final String oldEntry : line.getEntries())
					scoreboard.resetScores(oldEntry);

				line.unregister();
			}
		}
	}

	/**
	 * @param text        包含颜色代码的文本
	 * @param splitPoints 拆分文本的位置
	 * @return 此方法会在给定的 splitPoints 处拆分文本，并把颜色延续过去
	 */
	private List<String> copyColors(String text, int... splitPoints) {
		//Removes useless colors in front of only spaces (e.g. [§a     §aText] becomes [     §aText])
		final Pattern spaceMatcher = Pattern.compile("^( )+(" + COLOR_CHAR + ")");
		final List<String> splitText = new ArrayList<>();

		for (final int splitPoint : splitPoints) {
			final String lastEntry = splitText.isEmpty() ? "" : splitText.get(splitText.size() - 1);
			final String lastColor = ChatColor.getLastColors(lastEntry);

			final boolean addColor = !text.startsWith(COLOR_CHAR) && !lastColor.isEmpty() && !spaceMatcher.matcher(text).find();
			final int realSplitPoint = Math.min(splitPoint - (addColor ? 2 : 0), text.length());
			String line = (addColor ? lastColor : "") + text.substring(0, realSplitPoint);

			text = text.substring(realSplitPoint);

			if (line.endsWith(COLOR_CHAR)) {
				line = line.substring(0, line.length() - 1);
				text = COLOR_CHAR + text;
			}

			splitText.add(line);

			if (text.isEmpty())
				break;
		}

		return splitText;
	}
}