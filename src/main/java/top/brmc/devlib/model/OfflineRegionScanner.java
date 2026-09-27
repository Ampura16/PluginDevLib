package top.brmc.devlib.model;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import top.brmc.devlib.Common;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.ReflectionUtil.ReflectionException;
import top.brmc.devlib.event.RegionScanCompleteEvent;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.Remain;

import lombok.Getter;
import lombok.Setter;

/**
 * 能够扫描磁盘上已保存的区域，并对每个已保存区块
 * 执行操作的类。
 */
public abstract class OfflineRegionScanner {

	/**
	 * 将被扫描的文件夹
	 */
	private static final String[] FOLDERS = { "region", "DIM-1/region", "DIM1/region" };

	/**
	 * 有效的文件名模式
	 */
	private static final Pattern FILE_PATTERN = Pattern.compile("r\\.(.+)\\.(.+)\\.mca");

	/**
	 * 每次文件处理操作之间间隔的秒数。
	 */
	private static int WAIT_TIME_BETWEEN_SCAN_SECONDS = 1;

	/**
	 * 变化标记：已处理的文件数（占总数）
	 */
	private int processedFilesCount = 0;

	/**
	 * 变化标记：要扫描的区域文件总数
	 */
	private int totalFilesCount = 0;

	/**
	 * 变化标记：正在扫描的世界
	 */
	@Getter
	private World world;

	/**
	 * 变化标记：上次成功完成操作的时间
	 */
	private long lastTick = System.currentTimeMillis();

	/**
	 * 快速模式下我们不会加载区块，只返回它们的 x-z 坐标
	 *
	 * false = 调用 {@link #onChunkScan(Chunk)}
	 * true = 调用 {@link #onChunkScanFast(int, int)}
	 */
	@Setter
	private boolean fastMode = false;

	/**
	 * 开始扫描给定世界（警告：此操作是阻塞的，
	 * 且耗时较长，参见 {@link #getEstimatedWaitTimeSec(World)}）
	 *
	 * @param world
	 */
	public final void scan(World world) {
		final boolean hadAutoSave = world.isAutoSave();

		try {
			world.setAutoSave(false);
			this.scan0(world);

		} finally {
			world.setAutoSave(hadAutoSave);
		}
	}

	/*
	 * Invoke the main scan of all chunks within this world on the disk, both loaded and unloaded
	 */
	private void scan0(World world) {

		Common.log(
				Common.consoleLine(),
				"Scanning regions in " + world.getName(),
				Common.consoleLine());

		// Disable watch dog
		this.disableWatchdog();

		// Collect files
		final File[] files = getRegionFiles(world);

		if (files == null || files.length == 0) {
			Common.warning("Unable to locate the region files for: " + world.getName());

			return;
		}

		final Queue<File> queue = new LimitedQueue<>(files.length + 1);
		queue.addAll(Arrays.asList(files));

		this.totalFilesCount = files.length;
		this.world = world;

		// Start the schedule
		this.schedule0(queue);
	}

	/*
	 * Disable to prevent lag warnings since we scan chunks on the main thread
	 */
	private void disableWatchdog() {
		try {
			final Class<?> watchDog = Class.forName("org.spigotmc.WatchdogThread");
			final Method doStop = ReflectionUtil.getMethod(watchDog, "doStop");

			ReflectionUtil.invokeStatic(doStop);

		} catch (final ReflectiveOperationException err) {
			// pass through, probably not using Spigot
		}
	}

	/*
	 * Self-repeating cycle of loading chunks from the disk until
	 * we reach the end of the queue
	 */
	private void schedule0(Queue<File> queue) {
		new SimpleRunnable() {

			@Override
			public void run() {
				final File file = queue.poll();

				// Queue finished
				if (file == null) {
					Common.log(
							Common.consoleLine(),
							"Region scanner finished. World saved.",
							Common.consoleLine());

					Common.callEvent(new RegionScanCompleteEvent(OfflineRegionScanner.this.world));

					OfflineRegionScanner.this.onScanFinished();
					this.cancel();

					return;
				}

				OfflineRegionScanner.this.scanFile(file, queue);
			}
		}.runTask(SimplePlugin.getInstance());
	}

	/*
	 * Scans the given region file
	 */
	private void scanFile(File file, Queue<File> queue) {
		final Matcher matcher = FILE_PATTERN.matcher(file.getName());

		if (!matcher.matches())
			return;

		final int regionX = Integer.parseInt(matcher.group(1));
		final int regionZ = Integer.parseInt(matcher.group(2));

		System.out.print("[" + Math.round((double) this.processedFilesCount++ / (double) this.totalFilesCount * 100) + "%] Processing " + file);

		// Calculate time, collect memory and increase pauses in between if running out of memory
		if (System.currentTimeMillis() - this.lastTick > 4000) {
			final long free = Runtime.getRuntime().freeMemory() / 1_000_000;

			if (free < 200) {
				System.out.print(" [Low memory (" + free + "Mb)! Running GC and increasing delay between operations ..]");

				WAIT_TIME_BETWEEN_SCAN_SECONDS = +2;

				System.gc();
				Common.sleep(5_000);
			} else
				System.out.print(" [free memory = " + free + " mb]");

			this.lastTick = System.currentTimeMillis();
		}

		System.out.println();

		// Load the file
		final Object region = RegionAccessor.getRegionFile(this.world.getName(), file);

		// Load each chunk within that file
		scan:
		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++) {
				final int chunkX = x + (regionX << 5);
				final int chunkZ = z + (regionZ << 5);

				if (RegionAccessor.isChunkSaved(region, x, z))
					if (this.fastMode)
						this.onChunkScanFast(chunkX, chunkZ);

					else {
						final Chunk chunk = this.world.getChunkAt(chunkX, chunkZ);

						try {
							this.onChunkScan(chunk);

						} catch (final Throwable t) {
							Common.error(t, "Failed to scan chunk " + chunk + ", aborting for safety");

							break scan;
						}
					}
			}

		// Save
		try {
			RegionAccessor.save(region);

		} catch (final Throwable t) {
			Common.log("Failed to save region " + file + ", operation stopped.");
			Remain.sneaky(t);
		}

		if (this.fastMode)
			this.schedule0(queue);

		else
			Common.runLater(WAIT_TIME_BETWEEN_SCAN_SECONDS, () -> this.schedule0(queue));

	}

	/**
	 * 在区块被扫描并加载时调用
	 * 仅在未启用快速模式时调用（默认情况）
	 * 否则我们会调用你需要覆盖的 {@link #onChunkScanFast(int, int)} 方法
	 *
	 * @param chunk
	 */
	protected abstract void onChunkScan(Chunk chunk);

	/**
	 * 在区块被扫描并加载时调用
	 * 仅在启用快速模式时调用
	 *
	 * @param chunkX
	 * @param chunkZ
	 */
	protected void onChunkScanFast(int chunkX, int chunkZ) {
	}

	/**
	 * 扫描完成时调用，在 {@link RegionScanCompleteEvent} 之后
	 */
	protected void onScanFinished() {
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回磁盘上为给定世界存储的所有区域文件
	 *
	 * @param world
	 * @return
	 */
	public static File[] getRegionFiles(World world) {
		final File regionDir = getRegionDirectory(world);

		return regionDir == null ? null : regionDir.listFiles((FilenameFilter) (dir, name) -> name.toLowerCase().endsWith(".mca"));
	}

	/**
	 * 返回给定世界的区域目录
	 *
	 * @param world
	 * @return
	 */
	private static final File getRegionDirectory(World world) {
		for (final String folder : FOLDERS) {
			final File file = new File(world.getWorldFolder(), folder);

			if (file.isDirectory())
				return file;
		}

		return null;
	}

	/**
	 * 根据给定世界的区域文件数量，
	 * 获取扫描所需的时长
	 *
	 * @param world
	 * @return
	 */
	public static int getEstimatedWaitTimeSec(World world) {
		final File[] files = getRegionFiles(world);

		return (int) (Math.round(WAIT_TIME_BETWEEN_SCAN_SECONDS * 1.5D) * files.length);
	}
}

/**
 * 用于访问区域文件的反射辅助类
 */
class RegionAccessor {

	private static Constructor<?> regionFileConstructor;
	private static Method isChunkSaved;

	private static final boolean atleast1_13, atleast1_14, atleast1_15, atleast1_16, atleast1_18;
	private static final String saveMethodName;

	static {
		atleast1_13 = MinecraftVersion.atLeast(V.v1_13);
		atleast1_14 = MinecraftVersion.atLeast(V.v1_14);
		atleast1_15 = MinecraftVersion.atLeast(V.v1_15);
		atleast1_16 = MinecraftVersion.atLeast(V.v1_16);
		atleast1_18 = MinecraftVersion.atLeast(V.v1_18);

		saveMethodName = atleast1_13 ? "close" : "c";

		try {
			final Class<?> regionFileClass = ReflectionUtil.getNMSClass("RegionFile", "net.minecraft.world.level.chunk.storage.RegionFile");
			regionFileConstructor = atleast1_18 ? regionFileClass.getConstructor(Path.class, Path.class, boolean.class)
					: atleast1_16 ? regionFileClass.getConstructor(File.class, File.class, boolean.class)
							: atleast1_15 ? regionFileClass.getConstructor(File.class, File.class)
									: regionFileClass.getConstructor(File.class);

			isChunkSaved = atleast1_14 ? regionFileClass.getMethod("b", ReflectionUtil.getNMSClass("ChunkCoordIntPair", "net.minecraft.world.level.ChunkCoordIntPair"))
					: regionFileClass.getMethod(atleast1_13 ? "b" : "c", int.class, int.class);

		} catch (final ReflectiveOperationException ex) {
			Remain.sneaky(ex);
		}
	}

	static Object getRegionFile(String worldName, File file) {
		try {
			final File container = new File(Bukkit.getWorldContainer(), worldName);

			return atleast1_18 ? regionFileConstructor.newInstance(file.toPath(), container.toPath(), false)
					: atleast1_16 ? regionFileConstructor.newInstance(file, container, false)
							: atleast1_15 ? regionFileConstructor.newInstance(file, container)
									: regionFileConstructor.newInstance(file);

		} catch (final Throwable ex) {
			throw new RuntimeException("Could not create region file from " + file, ex);
		}
	}

	static boolean isChunkSaved(Object region, int x, int z) {
		try {
			if (MinecraftVersion.newerThan(V.v1_13)) {
				final Object chunkCoordinates = ReflectionUtil.getNMSClass("ChunkCoordIntPair", "net.minecraft.world.level.ChunkCoordIntPair")
						.getConstructor(int.class, int.class).newInstance(x, z);

				return (boolean) isChunkSaved.invoke(region, chunkCoordinates);
			}

			return (boolean) isChunkSaved.invoke(region, x, z);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Could not find if region file " + region + " has chunk at " + x + " " + z);
		}
	}

	static void save(Object region) {
		try {
			region.getClass().getDeclaredMethod(saveMethodName).invoke(region);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException(ex, "Error saving region " + region);
		}
	}
}
