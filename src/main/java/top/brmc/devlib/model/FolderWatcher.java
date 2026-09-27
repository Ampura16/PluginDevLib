package top.brmc.devlib.model;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.scheduler.BukkitTask;
import top.brmc.devlib.Common;
import top.brmc.devlib.Valid;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.plugin.SimplePlugin;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(value = AccessLevel.PROTECTED)
public abstract class FolderWatcher extends Thread {

	/**
	 * 帮助 Foundation 在重载时停止线程的列表
	 */
	private static final Set<FolderWatcher> activeThreads = new HashSet<>();

	/**
	 * 停止所有活动线程
	 */
	public static void stopThreads() {
		for (final FolderWatcher thread : activeThreads)
			thread.stopWatching();

		activeThreads.clear();
	}

	/**
	 * 解决循环中出现重复值的变通方案
	 */
	private final Map<String, BukkitTask> scheduledUpdates = new HashMap<>();

	/**
	 * 正在被监视的文件夹
	 */
	private final Path folder;

	/**
	 * 单向标记，用于停止线程的 while 循环死锁
	 */
	@Getter
	private boolean watching = true;

	/**
	 * 启动一个新的文件监视器，开始监视给定文件夹
	 *
	 * @param folder
	 */
	public FolderWatcher(File folder) {
		Valid.checkBoolean(folder.exists(), folder + " does not exists!");
		Valid.checkBoolean(folder.isDirectory(), folder + " must be a directory!");

		this.folder = folder.toPath();
		this.start();

		for (final FolderWatcher other : activeThreads)
			//Valid.checkBoolean(other.folder.toString().equals(this.folder.toString()), "Tried to add a duplicate file watcher for " + this.folder);
			if (other.folder.toString().equals(this.folder.toString()))
				Common.warning("A duplicate file watcher for '" + folder.getPath() + "' was added. This is untested and may causes fatal issues!");

		activeThreads.add(this);
	}

	/**
	 * 开始监视给定文件夹并报告变更
	 */
	@Override
	public final void run() {
		final FileSystem fileSystem = this.folder.getFileSystem();

		try (WatchService service = fileSystem.newWatchService()) {
			final WatchKey registration = this.folder.register(service, ENTRY_MODIFY);

			while (this.watching)
				try {
					final WatchKey watchKey = service.take();

					for (final WatchEvent<?> watchEvent : watchKey.pollEvents()) {
						final Kind<?> kind = watchEvent.kind();

						if (kind == ENTRY_MODIFY) {
							final Path watchEventPath = (Path) watchEvent.context();
							final File fileModified = new File(SimplePlugin.getData(), watchEventPath.toFile().getName());

							final String path = fileModified.getAbsolutePath();
							final BukkitTask pendingTask = this.scheduledUpdates.remove(path);

							// Cancel the old task and reschedule
							if (pendingTask != null)
								pendingTask.cancel();

							// Force run sync -- reschedule five seconds later to ensure no further edits take place
							this.scheduledUpdates.put(path, Common.runLater(10, () -> {
								if (!this.watching)
									return;

								try {
									this.onModified(fileModified);

									this.scheduledUpdates.remove(path);

								} catch (final Throwable t) {
									Common.error(t, "Error in calling onModified when watching changed file " + fileModified);
								}
							}));

							break;
						}
					}

					if (!watchKey.reset())
						Common.error(new FoException("Failed to reset watch key! Restarting sync engine.."));

				} catch (final Throwable t) {
					Common.error(t, "Error in handling watching thread loop for folder " + this.getFolder());
				}

			registration.cancel();

		} catch (final Throwable t) {
			Common.error(t, "Error in initializing watching thread loop for folder " + this.getFolder());
		}

	}

	/**
	 * 文件被修改时自动调用
	 *
	 * @param file
	 */
	protected abstract void onModified(File file);

	/**
	 * 停止监听文件夹变更
	 */
	public void stopWatching() {
		Valid.checkBoolean(this.watching, "The folder watcher for folder " + this.folder + " is no longer watching!");

		this.watching = false;

		for (final BukkitTask task : this.scheduledUpdates.values())
			try {
				task.cancel();
			} catch (final Exception ex) {
				// ignore
			}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FolderWatcher && ((FolderWatcher) obj).folder.toString().equals(this.folder.toString());
	}
}