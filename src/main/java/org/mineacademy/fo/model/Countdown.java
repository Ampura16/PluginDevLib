package org.mineacademy.fo.model;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * 表示一个倒数到 0 后停止的可运行定时任务
 */
public abstract class Countdown implements Runnable {

	/**
	 * 开始倒计时前要等待多久（刻）？
	 * <p>
	 * 设为 1 秒。
	 */
	private static final int START_DELAY = 20;

	/**
	 * 每次计数之间要等待多久（刻）？
	 * <p>
	 * 设为 1 秒。
	 */
	private static final int TICK_PERIOD = 20;

	/**
	 * 倒计时的起始秒数
	 */
	@Getter
	private final int countdownSeconds;

	/**
	 * 从开始到现在已过去多少秒？
	 */
	@Getter(AccessLevel.PROTECTED)
	private int secondsSinceStart = 0;

	/**
	 * 此倒计时当前是否已暂停？
	 */
	private boolean paused = false;

	/**
	 * 与此倒计时关联的 Bukkit 内部任务
	 */
	private SimpleTask task = null;

	/**
	 * 根据给定时间创建新的倒计时
	 *
	 * @param time
	 */
	protected Countdown(final SimpleTime time) {
		this((int) time.getTimeSeconds());
	}

	/**
	 * 创建新的倒计时
	 *
	 * @param countdownSeconds
	 */
	protected Countdown(final int countdownSeconds) {
		this.countdownSeconds = countdownSeconds;
	}

	@Override
	public final void run() {
		this.secondsSinceStart++;

		if (this.secondsSinceStart < this.countdownSeconds)
			try {
				this.onTick();

			} catch (final Throwable t) {
				try {
					this.onTickError(t);
				} catch (final Throwable tt) {
					Common.log("Unable to handle onTickError, got " + t + ": " + tt.getMessage());
				}

				Common.error(t,
						"Error in countdown!",
						"Seconds since start: " + this.secondsSinceStart,
						"Counting till: " + this.countdownSeconds,
						"%error");
			}
		else {
			this.cancel();
			this.onEnd();
		}
	}

	/**
	 * 此倒计时启动时调用
	 */
	protected void onStart() {
	}

	/**
	 * 每次计时（默认每秒）时调用，直到倒数到 0
	 */
	protected abstract void onTick();

	/**
	 * 计时到达最终的 0 并停止时调用。
	 */
	protected abstract void onEnd();

	/**
	 * 当 {@link #onTick()} 方法抛出错误时调用（错误已被记录）
	 *
	 * @param t
	 */
	protected void onTickError(final Throwable t) {
	}

	/**
	 * 返回剩余时间（秒）
	 *
	 * @return
	 */
	public int getTimeLeft() {
		return this.countdownSeconds - this.secondsSinceStart;
	}

	/**
	 * 返回从开始到现在已过去的时间（秒）
	 *
	 * @return
	 */
	public int getElapsedTime() {
		return this.secondsSinceStart;
	}

	/**
	 * 设置此倒计时已经过去的时间。
	 *
	 * @param time
	 */
	public void setElapsedTime(final SimpleTime time) {
		this.setElapsedTime((int) time.getTimeSeconds());
	}

	/**
	 * 设置此倒计时已经过去的时间。
	 *
	 * @param secondsElapsed 以秒为单位的时间
	 */
	public void setElapsedTime(final int secondsElapsed) {
		this.secondsSinceStart = secondsElapsed;
	}

	/**
	 * 启动此倒计时；若已在运行则失败
	 */
	public final void launch() {
		Valid.checkBoolean(!this.isRunning(), "Task " + this + " already scheduled!");
		Valid.checkBoolean(!this.paused, "You cannot launch a countdown that is paused!");

		this.task = Common.runTimer(START_DELAY, TICK_PERIOD, this);

		this.onStart();
	}

	/**
	 * 暂停此倒计时；若未被调度则失败
	 */
	public final void pause() {
		Valid.checkBoolean(this.isRunning(), "Countdown must be scheduled in order to pause it!");

		this.task.cancel();

		this.task = null;
		this.paused = true;
	}

	/**
	 * 恢复此倒计时；若并未暂停则失败（可用 {@link #isPaused()} 判断）
	 */
	public final void resume() {
		Valid.checkBoolean(this.paused, "Countdown must be paused in order to resume it!");

		this.task = Common.runTimer(START_DELAY, TICK_PERIOD, this);
		this.paused = false;
	}

	/**
	 * 取消此倒计时；若未被调度则失败（可用 {@link #isRunning()} 判断）
	 */
	public final void cancel() {
		this.task.cancel();

		this.task = null;
		this.secondsSinceStart = 0;
	}

	/**
	 * 若此倒计时正在运行则返回 true
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return this.task != null;
	}

	/**
	 * 返回此倒计时是否已暂停。
	 *
	 * @return
	 */
	public final boolean isPaused() {
		return this.paused;
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName() + "{" + this.countdownSeconds + ", taskId=" + (this.isRunning() ? this.task.getTaskId() : "not running") + "}";
	}
}
