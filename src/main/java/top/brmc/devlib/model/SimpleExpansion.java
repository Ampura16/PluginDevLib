package top.brmc.devlib.model;

import org.bukkit.command.CommandSender;
import top.brmc.devlib.Common;

import lombok.NonNull;

/**
 * 表示一个占位符扩展，用于需要根据变量内容
 * 即时动态替换的变量。
 *
 * 它也会按需接入 PlaceholderAPI
 */
public abstract class SimpleExpansion {

	/**
	 * 表示没有替换值，占位符应原样
	 * 输出到控制台/游戏聊天中。
	 */
	protected static final String NO_REPLACE = null;

	/**
	 * 当前参数，每次调用扩展时都会改变，
	 * 我们只是在插件名之后按 _ 拆分占位符标识符，
	 * 例如 corearena_player_health 会得到 [player, health]
	 */
	protected String[] args;

	/**
	 * 返回占位符的值，例如 arena_name
	 *
	 * @param sender
	 * @param params
	 *
	 * @return 值；无效时返回 null
	 */
	public final String replacePlaceholders(CommandSender sender, String params) {
		this.args = params.split("\\_");

		return this.onReplace(sender, params);
	}

	/**
	 * 返回针对给定玩家和标识符应替换的
	 * 变量值。
	 *
	 * @param sender
	 * @param identifier 插件名之后的全部内容，例如用户输入 {corearena_player_health} 时，
	 * 		  我们只返回 "player_health"。这里也可以使用 {@link #args}。
	 * @return
	 */
	protected abstract String onReplace(@NonNull CommandSender sender, String identifier);

	/**
	 * 从给定索引开始自动拼接 {@link #args}
	 *
	 * @param startIndex
	 * @return
	 */
	protected final String join(int startIndex) {
		return Common.joinRange(startIndex, this.args);
	}

	/**
	 * 自动拼接 {@link #args} 中从起始索引到结束索引的部分
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @return
	 */
	protected final String join(int startIndex, int stopIndex) {
		return Common.joinRange(startIndex, stopIndex, this.args);
	}
}
