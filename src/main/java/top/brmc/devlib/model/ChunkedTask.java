package top.brmc.devlib.model;

import top.brmc.devlib.Common;
import top.brmc.devlib.Valid;

import lombok.Getter;
import lombok.Setter;

/**
 * 将列表中大量条目的处理
 * 拆分成更小的块
 */
public abstract class ChunkedTask {

	/**
	 * 处理下一批之前应等待多少刻？
	 */
	@Setter
	private int waitPeriodTicks = 20;

	/**
	 * 每次应处理多少个条目？
	 */
	private final int processAmount;

	/*
	 * The current index where we are processing at, right now
	 */
	@Getter
	private int currentIndex = 0;

	/*
	 * Private flag to prevent dupe executions and cancel running tasks
	 */
	@Getter
	private boolean processing = false;
	private boolean firstLaunch = false;

	/**
	 * 创建一个新任务，每次运行处理给定数量的条目
	 * （参见 getWaitPeriodTicks()），每次之间等待 1 秒
	 *
	 * @param processAmount
	 */
	public ChunkedTask(int processAmount) {
		this.processAmount = processAmount;
	}

	/**
	 * 创建一个新任务，每次运行处理给定数量的条目，
	 * 并在每次之间等待给定的刻数
	 *
	 * @param processAmount
	 * @param waitPeriodTicks
	 */
	public ChunkedTask(int processAmount, int waitPeriodTicks) {
		this.processAmount = processAmount;
		this.waitPeriodTicks = waitPeriodTicks;
	}

	/**
	 * 启动任务链，会运行多个同步任务直到完成
	 */
	public final void startChain() {

		if (!this.firstLaunch) {
			this.processing = true;

			this.firstLaunch = true;
		}

		Common.runLater(() -> {

			// Cancelled prematurely
			if (!this.processing) {
				this.onFinish(false);
				this.firstLaunch = false;

				return;
			}

			final long now = System.currentTimeMillis();

			boolean finished = false;
			int processed = 0;

			for (int i = this.currentIndex; i < this.currentIndex + this.processAmount; i++) {
				if (!this.canContinue(i)) {
					finished = true;

					break;
				}

				processed++;

				try {
					this.onProcess(i);

				} catch (final Throwable t) {
					Common.error(t, "Error in " + this + " processing index " + processed);
					this.processing = false;
					this.firstLaunch = false;

					this.onFinish(false);
					return;
				}
			}

			if (processed > 0 || !finished)
				Common.log(this.getProcessMessage(now, processed));

			if (!finished) {
				this.currentIndex += this.processAmount;

				Common.runLaterAsync(this.waitPeriodTicks, this::startChain);

			} else {
				this.processing = false;
				this.firstLaunch = false;

				this.onFinish(true);
			}
		});
	}

	/**
	 * 尝试取消正在运行的此任务；若任务未在运行则抛出错误（可用 {@link #isProcessing()} 判断）
	 */
	public final void cancel() {
		Valid.checkBoolean(this.processing, "Chunked task is not running: " + this);

		this.processing = false;
	}

	/**
	 * 处理单个条目时调用
	 *
	 * @param item
	 */
	protected abstract void onProcess(int index) throws Throwable;

	/**
	 * 返回该任务是否可以执行下一个索引
	 *
	 * @param index
	 * @return 可以继续时返回 true
	 */
	protected abstract boolean canContinue(int index);

	/**
	 * 获取每次进度时发送到控制台的消息，没有消息时返回 null
	 *
	 * @param initialTime
	 * @param processed
	 * @return
	 */
	protected String getProcessMessage(long initialTime, int processed) {
		return "Processed " + String.format("%,d", processed) + " " + this.getLabel() + ". Took " + (System.currentTimeMillis() - initialTime) + " ms";
	}

	/**
	 * 处理完成时调用
	 *
	 * @param gracefully 自然结束时为 true，使用 {@link #cancel()} 时为 false
	 */
	protected void onFinish(boolean gracefully) {
		this.onFinish();
	}

	/**
	 * @see #onFinish(boolean)
	 * @deprecated 建议改为调用 {@link #onFinish(boolean)}
	 */
	@Deprecated
	protected void onFinish() {
	}

	/**
	 * 获取进度消息中使用的标签
	 * 默认为 "blocks"
	 *
	 * @return
	 */
	protected String getLabel() {
		return "blocks";
	}
}