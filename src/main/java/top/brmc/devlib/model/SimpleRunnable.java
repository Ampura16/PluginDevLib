package top.brmc.devlib.model;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import top.brmc.devlib.Common;

/**
 * 兼容 Folia 的 Bukkit Runnable 替代方案。
 */
public abstract class SimpleRunnable implements Runnable {

	private BukkitTask task;

	public final synchronized void cancel() throws IllegalStateException {
		checkScheduled();

		task.cancel();
	}

	/**
	 * 将此任务加入调度器，在下一刻运行。
	 *
	 * @param plugin 调度该任务的插件引用
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException 如果 plugin 为 null
	 * @throws IllegalStateException    如果此任务已被调度
	 */
	public final synchronized BukkitTask runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runLater(this));
	}

	/**
	 * 将此任务加入调度器，在下一刻异步运行。
	 *
	 * @param plugin 调度该任务的插件引用
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException 如果 plugin 为 null
	 * @throws IllegalStateException    如果此任务已被调度
	 */
	public final synchronized BukkitTask runTaskAsync(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runAsync(this));
	}

	/**
	 * 调度此任务在指定的服务器刻数之后运行。
	 *
	 * @param plugin 调度该任务的插件引用
	 * @param delay  运行任务前要等待的刻数
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException 如果 plugin 为 null
	 * @throws IllegalStateException    如果此任务已被调度
	 */
	public final synchronized BukkitTask runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runLater((int) delay, this));
	}

	/**
	 * 调度此任务在指定的服务器刻数之后异步运行。
	 *
	 * @param plugin 调度该任务的插件引用
	 * @param delay  运行任务前要等待的刻数
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException 如果 plugin 为 null
	 * @throws IllegalStateException    如果此任务已被调度
	 */
	public final synchronized BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runLaterAsync((int) delay, this));
	}

	/**
	 * 调度此任务在指定的服务器刻数之后运行。
	 *
	 * @param plugin 调度该任务的插件引用
	 * @param delay 首次运行任务前要等待的刻数
	 * @param period 再次运行任务前要等待的刻数
	 *
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException 如果 plugin 为 null
	 * @throws IllegalStateException    如果此任务已被调度
	 */
	public final synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runTimer((int) delay, (int) period, this));
	}

	/**
	 * 调度此任务在指定的服务器刻数之后异步运行。
	 *
	 * @param plugin 调度该任务的插件引用
	 * @param delay 首次运行任务前要等待的刻数
	 * @param period 再次运行任务前要等待的刻数
	 *
	 * @return {@link SimpleTask}
	 * @throws IllegalArgumentException 如果 plugin 为 null
	 * @throws IllegalStateException    如果此任务已被调度
	 */
	public final synchronized BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		checkNotYetScheduled();

		return setupTask(Common.runTimerAsync((int) delay, (int) period, this));
	}

	private void checkScheduled() {
		if (task == null)
			throw new IllegalStateException("Not scheduled yet");
	}

	private void checkNotYetScheduled() {
		if (task != null)
			throw new IllegalStateException("Already scheduled");
	}

	/**
	 * @deprecated 仅供内部使用
	 *
	 * @param task
	 * @return
	 */
	@Deprecated
	public BukkitTask setupTask(final BukkitTask task) {
		this.task = task;

		return task;
	}

}