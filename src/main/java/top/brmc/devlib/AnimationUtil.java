package top.brmc.devlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import top.brmc.devlib.model.SimpleRunnable;
import top.brmc.devlib.model.SimpleScoreboard;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.CompBarColor;
import top.brmc.devlib.remain.CompBarStyle;
import top.brmc.devlib.remain.Remain;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 为 BossBar、记分板、HUD 标题和物品栏创建文本动画的工具类。
 *
 * @author parpar8090
 */
public class AnimationUtil {

	// ------------------------------------------------------------------------------------------------------------
	// Frame generators
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 让文本带有从左向右移动的颜色动画。
	 * 动画在 firstColor 到达消息末尾时结束，因此是半个周期。如需完整周期，请使用 {@link #rightToLeftFull}
	 *
	 * @param message     要做成动画的消息
	 * @param firstColor  消息第一段的颜色（示例颜色：{@link ChatColor#YELLOW}）
	 * @param middleColor 消息中间段的颜色，一段长度为 1 个字符、分隔第一段与最后一段的区域（示例颜色：{@link ChatColor#WHITE}）。
	 * @param lastColor   消息最后一段的颜色（示例颜色：{@link ChatColor#GOLD}）。
	 * @return 按顺序排列的字符串帧列表。
	 */
	public static List<String> leftToRight(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final List<String> result = new ArrayList<>();
		final String msg = Common.colorize(message);

		for (int frame = 0; frame < message.length(); frame++) {
			final String first = msg.substring(0, frame);
			final String middle = frame == msg.length() ? "" : String.valueOf(msg.charAt(frame));
			final String last = frame == msg.length() ? "" : msg.substring(frame + 1);

			final ChatColor middleColorFinal = middleColor != null ? middleColor : firstColor;

			result.add(firstColor + first + middleColorFinal + middle + lastColor + last);
		}
		return result;
	}

	/**
	 * 让文本带有从右向左移动的颜色动画。
	 * 动画在 firstColor 到达消息开头时结束，因此是半个周期。如需完整周期，请使用 {@link #rightToLeftFull}
	 *
	 * @param message     要做成动画的消息
	 * @param firstColor  消息第一段的颜色（示例颜色：{@link ChatColor#YELLOW}）
	 * @param middleColor 消息中间段的颜色，一段长度为 1 个字符、分隔第一段与最后一段的区域（示例颜色：{@link ChatColor#WHITE}）。
	 * @param lastColor   消息最后一段的颜色（示例颜色：{@link ChatColor#GOLD}）。
	 * @return 按顺序排列的字符串帧列表。
	 */
	public static List<String> rightToLeft(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final String msg = Common.colorize(message);
		final List<String> result = new ArrayList<>();

		for (int frame = msg.length(); frame >= 0; frame--) {
			final String first = msg.substring(0, frame);
			final String middle = frame == msg.length() ? "" : String.valueOf(msg.charAt(frame));
			final String last = frame == msg.length() ? "" : msg.substring(frame + 1);

			final ChatColor middleColorFinal = middleColor != null ? middleColor : firstColor;

			result.add(firstColor + first + middleColorFinal + middle + lastColor + last);
		}
		return result;
	}

	/**
	 * 让文本带有从左向右移动的颜色动画（完整周期）。
	 * 完整周期动画按以下模式循环：lastColor -> firstColor -> lastColor。
	 *
	 * @param message     要做成动画的消息
	 * @param firstColor  消息第一段的颜色（示例颜色：{@link ChatColor#YELLOW}）
	 * @param middleColor 消息中间段的颜色，一段长度为 1 个字符、分隔第一段与最后一段的区域（示例颜色：{@link ChatColor#WHITE}）。
	 * @param lastColor   消息最后一段的颜色（示例颜色：{@link ChatColor#GOLD}）。
	 * @return 按顺序排列的字符串帧列表。
	 */
	public static List<String> leftToRightFull(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final List<String> result = new ArrayList<>();

		result.addAll(leftToRight(message, firstColor, middleColor, lastColor));
		result.addAll(leftToRight(message, lastColor, middleColor, firstColor));

		return result;
	}

	/**
	 * 让文本带有从右向左移动的颜色动画（完整周期）。
	 * 完整周期动画按以下模式循环：lastColor -> firstColor -> lastColor。
	 *
	 * @param message     要做成动画的消息
	 * @param firstColor  消息第一段的颜色（示例颜色：{@link ChatColor#YELLOW}）
	 * @param middleColor 消息中间段的颜色，一段长度为 1 个字符、分隔第一段与最后一段的区域（示例颜色：{@link ChatColor#WHITE}）。
	 * @param lastColor   消息最后一段的颜色（示例颜色：{@link ChatColor#GOLD}）。
	 * @return 按顺序排列的字符串帧列表。
	 */
	public static List<String> rightToLeftFull(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final List<String> result = new ArrayList<>();

		result.addAll(rightToLeft(message, firstColor, middleColor, lastColor));
		result.addAll(rightToLeft(message, lastColor, middleColor, firstColor));

		return result;
	}

	/**
	 * 让文本按编排好的颜色闪烁。
	 * 注意：如需随机效果，请在 {@link #shuffle(List)} 方法内部调用本方法。
	 *
	 * @param message  要做成动画的消息
	 * @param amount   消息闪烁的次数。
	 * @param duration 每帧复制多少份。复制份数越多，帧停留越久。
	 * @param colors   闪烁颜色，按数组下标排序。
	 * @return 按顺序排列的字符串帧列表。
	 */
	public static List<String> flicker(String message, int amount, int duration, ChatColor[] colors) {
		final List<String> result = new ArrayList<>();

		for (int frame = 0; frame < amount; frame++)
			for (int i = 0; i < duration; i++)
				result.add(colors[amount % colors.length] + message);

		return result;
	}

	/**
	 * 将所有帧复制指定份数，可用于放慢动画。
	 *
	 * @param frames 要复制的帧。
	 * @param amount 复制的份数。
	 * @return 复制后的字符串帧列表。
	 */
	public static List<String> duplicate(List<String> frames, int amount) {
		final List<String> result = new ArrayList<>();

		for (int i = 0; i < frames.size(); i++)
			//duplicate j times;
			for (int j = 0; j < amount; j++) {
				final String duplicated = frames.get(i);

				result.add(i, duplicated);
			}
		return result;
	}

	/**
	 * 将指定帧复制指定份数，可用于放慢动画。
	 *
	 * @param frame 要复制的帧
	 * @param frames 包含该帧的帧列表。
	 * @param amount 复制的份数。
	 * @return 复制后得到的新帧列表。
	 */
	public static List<String> duplicateFrame(int frame, List<String> frames, int amount) {
		final List<String> result = new ArrayList<>();

		for (int i = 0; i < amount; i++) {
			final String duplicated = frames.get(frame);

			result.add(frame, duplicated);
		}

		return result;
	}

	/**
	 * 打乱帧的顺序。
	 *
	 * @param animatedFrames 要打乱的帧。
	 * @return 打乱后的 animatedFrames 新列表。
	 */
	public static List<String> shuffle(List<String> animatedFrames) {
		Collections.shuffle(animatedFrames);

		return animatedFrames;
	}

	/**
	 * 按顺序合并多个动画。
	 *
	 * @param animationsToCombine 要合并的动画（按列表顺序）
	 * @return 合并后的帧列表。
	 */
	public static List<String> combine(@NonNull List<String>[] animationsToCombine) {
		final List<String> combined = new ArrayList<>();

		for (final List<String> animation : animationsToCombine)
			combined.addAll(animation);

		return combined;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Animators
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 为 BossBar 的标题制作动画。
	 *
	 * @param player
	 * @param animatedFrames 要在 BossBar 中显示的帧（按顺序）。
	 * @param delay          动画周期之间的延迟。
	 * @param period         显示下一帧之前等待的周期（单位：tick）。
	 * @return 重复执行的 BukkitTask（重载或关服时可取消它）。
	 */
	public static BukkitTask animateBossBar(Player player, List<String> animatedFrames, long delay, long period) {
		return new SimpleRunnable() {
			int frame = 0;

			@Override
			public void run() {
				Remain.sendBossbarPercent(player, animatedFrames.get(this.frame), 100);

				this.frame++;

				if (this.frame == animatedFrames.size())
					this.frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * 这将为 BossBar 制作动画
	 *
	 * @param player
	 * @param animatedFrames BossBar 标题帧列表
	 * @param animatedColors 要循环的 BossBar 颜色
	 * @param delay          开始动画前的延迟
	 * @param period         开始动画前的周期
	 * @param animateOnce    播完所有帧后是否停止动画？
	 * @param countdownBar   若不想让它充当倒计时条，请保持为 null。
	 * @return 该动画的 BukkitTask
	 */
	public static BukkitTask animateBossBar(Player player, List<String> animatedFrames, @Nullable List<CompBarColor> animatedColors, long delay, long period, boolean animateOnce, @Nullable CountdownBar countdownBar) {
		int smoothnessLevel = 1;

		if (countdownBar != null && countdownBar.isSmooth)
			smoothnessLevel = 10;

		final int finalSmoothnessLevel = smoothnessLevel;

		return new SimpleRunnable() {
			boolean run = true;
			int frame = 0;
			float health = 1F;

			@Override
			public void run() {
				if (!this.run)
					return;

				final String title = animatedFrames.get(this.frame % (animatedFrames.size() * finalSmoothnessLevel));

				if (animatedColors != null)
					Remain.sendBossbarPercent(player, title, this.health, animatedColors.get(this.frame % (animatedColors.size() * finalSmoothnessLevel)), CompBarStyle.SOLID);

				else
					Remain.sendBossbarPercent(player, title, this.health);

				if (countdownBar != null)
					if (countdownBar.isSmooth)
						this.health -= countdownBar.duration / (10D * finalSmoothnessLevel);
					else
						this.health -= countdownBar.duration / 10D;

				this.frame++;

				if (this.frame >= animatedFrames.size()) {
					this.frame = 0;
					this.health = 1F;

					if (animateOnce) {
						this.run = false;

						Remain.removeBossbar(player);
					}
				}
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period / smoothnessLevel);
	}

	/**
	 * 为记分板的标题制作动画。
	 *
	 * @param scoreboard     要做动画的记分板。
	 * @param animatedFrames 要在 BossBar 中显示的帧（按顺序）。
	 * @param delay          动画周期之间等待的延迟（单位：tick）。
	 * @param period         显示下一帧之前等待的周期（单位：tick）。
	 * @return 重复执行的 BukkitTask（重载或关服时可取消它）。
	 */

	public static BukkitTask animateScoreboardTitle(SimpleScoreboard scoreboard, List<String> animatedFrames, long delay, long period) {
		return new SimpleRunnable() {
			int frame = 0;

			@Override
			public void run() {
				scoreboard.setTitle(animatedFrames.get(this.frame));
				this.frame++;

				if (this.frame == animatedFrames.size())
					this.frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * 为玩家播放标题动画（不重复）。
	 *
	 * @param who            显示标题的玩家。
	 * @param titleFrames    要在标题中显示的帧（按顺序）。（设为 null 可隐藏）
	 * @param subtitleFrames 要在副标题中显示的帧（按顺序）。（设为 null 可隐藏）
	 * @param period         显示下一帧之前等待的周期（单位：tick）。
	 * @return 动画结束后可取消的任务
	 */
	public static BukkitTask animateTitle(Player who, @Nullable List<String> titleFrames, @Nullable List<String> subtitleFrames, long period) {
		return new SimpleRunnable() {
			int frame = 0;
			String title = "", subtitle = "";

			@Override
			public void run() {
				if (titleFrames != null)
					this.title = titleFrames.get(this.frame % titleFrames.size());
				if (subtitleFrames != null)
					this.subtitle = subtitleFrames.get(this.frame % subtitleFrames.size());

				Remain.sendTitle(who, 10, 70, 20, this.title, this.subtitle);

				this.frame++;

				if (this.frame == Math.max(titleFrames != null ? titleFrames.size() : 0,
						subtitleFrames != null ? subtitleFrames.size() : 0) || SimplePlugin.isReloading())
					this.cancel();
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, period);
	}

	/**
	 * 为物品的名称制作动画。
	 *
	 * @param item           显示标题的玩家。
	 * @param animatedFrames 要在标题中显示的帧（按顺序）。
	 * @param delay          动画周期之间等待的延迟（单位：tick）。
	 * @param period         显示下一帧之前等待的周期（单位：tick）。
	 * @return 重复执行的 BukkitTask（重载或关服时可取消它）。
	 */
	public static BukkitTask animateItemTitle(ItemStack item, List<String> animatedFrames, long delay, long period) {
		return new SimpleRunnable() {
			int frame = 0;

			@Override
			public void run() {

				if (!Remain.hasItemMeta()) {
					this.cancel();

					return;
				}

				final ItemMeta meta = checkMeta(item);

				meta.setDisplayName(animatedFrames.get(this.frame));
				item.setItemMeta(meta);

				this.frame++;
				if (this.frame > animatedFrames.size())
					this.frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * @param item           要做动画的物品。
	 * @param line           Lore 中的行，若行号为
	 * @param animatedFrames 要在标题中显示的帧（按顺序）。
	 * @param delay          动画周期之间等待的延迟（单位：tick）。
	 * @param period         显示下一帧之前等待的周期（单位：tick）。
	 * @return 重复执行的 BukkitTask（重载或关服时可取消它）。
	 * @throws IndexOutOfBoundsException 若行号超出范围
	 *                                   ({@code line < 0 || line > lore.size()})
	 */
	public static BukkitTask animateItemLore(ItemStack item, int line, List<String> animatedFrames, long delay, long period) {

		return new SimpleRunnable() {
			int frame = 0;

			@Override
			public void run() {

				if (!Remain.hasItemMeta()) {
					this.cancel();

					return;
				}

				final String frameText = animatedFrames.get(this.frame % animatedFrames.size());
				final ItemMeta meta = checkMeta(item);
				List<String> lore = meta.getLore();
				if (lore == null)
					lore = new ArrayList<>(); // prevents NPE

				if (lore.size() < line)
					throw new IndexOutOfBoundsException("line #" + line + " is out of range!");

				lore.set(line, frameText); // update line

				meta.setLore(lore);
				item.setItemMeta(meta);

				this.frame++;
				if (this.frame > animatedFrames.size())
					this.frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * 为物品栏的标题制作动画（该物品栏正被玩家查看）。
	 *
	 * @param viewer         查看该物品栏的玩家
	 * @param animatedFrames 要在标题中显示的帧（按顺序）。
	 * @param delay          动画周期之间等待的延迟（单位：tick）。
	 * @param period         显示下一帧之前等待的周期（单位：tick）。
	 * @return 重复执行的 BukkitTask（重载或关服时可取消它）。
	 */
	public static BukkitTask animateInventoryTitle(Player viewer, List<String> animatedFrames, long delay, long period) {
		return new SimpleRunnable() {
			int frame = 0;

			@Override
			public void run() {
				PlayerUtil.updateInventoryTitle(viewer, animatedFrames.get(this.frame));
				this.frame++;
				if (this.frame > animatedFrames.size())
					this.frame = 0;
			}
		}.runTaskTimer(SimplePlugin.getInstance(), delay, period);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 检查物品是否拥有 ItemMeta（用于防止 {@link NullPointerException}）。
	 *
	 * @param item 要检查的物品。
	 * @return 使用 {@link Bukkit#getItemFactory} 中的 getItemMeta 方法创建的新 ItemMeta；若物品已有 ItemMeta 则返回现有的。
	 */
	private static ItemMeta checkMeta(@NonNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();

		if (meta == null || !item.hasItemMeta())
			meta = Bukkit.getItemFactory().getItemMeta(item.getType());

		return meta;
	}

	@RequiredArgsConstructor
	public static class CountdownBar {

		/**
		 * 持续时间
		 */
		private final long duration;

		/**
		 * 是否平滑？
		 */
		private final boolean isSmooth;
	}
}