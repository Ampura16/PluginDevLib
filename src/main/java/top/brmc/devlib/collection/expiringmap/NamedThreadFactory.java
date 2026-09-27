package top.brmc.devlib.collection.expiringmap;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 命名线程工厂。
 */
public final class NamedThreadFactory implements ThreadFactory {
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String nameFormat;

	/**
	 * 创建按 {@code nameFormat} 命名线程的线程工厂，向格式提供代表线程号的
	 * 单个参数。
	 * @param nameFormat
	 */
	public NamedThreadFactory(String nameFormat) {
		this.nameFormat = nameFormat;
	}

	@Override
	public Thread newThread(Runnable r) {
		final Thread thread = new Thread(r, String.format(this.nameFormat, this.threadNumber.getAndIncrement()));
		thread.setDaemon(true);
		return thread;
	}
}
