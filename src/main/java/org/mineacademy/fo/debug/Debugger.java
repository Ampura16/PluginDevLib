package org.mineacademy.fo.debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.constants.FoConstants;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 用于排查问题和错误的工具类
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Debugger {

	/**
	 * 存放最后一次性打印的消息，
	 * 键为调试段，列表包含将被拼接
	 * 并打印的消息。
	 */
	private static final Map<String, ArrayList<String>> pendingMessages = new HashMap<>();

	/**
	 * 当插件文件夹中存在 debug.lock 文件时，会自动启用调试模式
	 */
	@Getter
	private static boolean debugModeEnabled = false;

	/**
	 * 加载调试模式，由 {@link SimplePlugin} 自动调用
	 */
	public static void detectDebugMode() {
		if (new File(SimplePlugin.getData(), "debug.lock").exists()) {
			debugModeEnabled = true;

			Bukkit.getLogger().info("Detected debug.lock file, debug features enabled!");

		} else
			debugModeEnabled = false;
	}

	/**
	 * 如果给定段正在被调试，则向控制台打印调试消息
	 * <p>
	 * 你可以在 settings.yml 的 "Debug" 键中设置要调试的段，
	 * 默认对应你继承 {@link SimpleSettings} 的类
	 *
	 * @param section
	 * @param messages
	 */
	public static void debug(String section, String... messages) {
		if (isDebugged(section))
			for (final String message : messages)
				print("[" + section + "] " + message);
	}

	/**
	 * 将指定段的消息放入队列。这些消息会一直保存，直到
	 * 你调用 {@link #push(String)}，然后拼接在一起打印。
	 *
	 * @param section
	 * @param message
	 */
	public static void put(String section, String message) {
		if (!isDebugged(section))
			return;

		final ArrayList<String> list = pendingMessages.getOrDefault(section, new ArrayList<>());
		list.add(message);

		pendingMessages.put(section, list);
	}

	/**
	 * 将消息放到待处理消息队列的末尾，并把最终日志推送
	 * 到控制台
	 *
	 * @param section
	 * @param message
	 */
	public static void push(String section, String message) {
		put(section, message);
		push(section);
	}

	/**
	 * 清空来自 {@link #put(String, String)} 的所有待处理消息，将它们拼接在一起
	 * 并打印到控制台
	 *
	 * @param section
	 */
	public static void push(String section) {
		if (!isDebugged(section))
			return;

		final List<String> parts = pendingMessages.remove(section);

		if (parts == null)
			return;

		final String whole = String.join("", parts);

		for (final String message : whole.split("\n"))
			debug(section, message);
	}

	/**
	 * 获取给定段是否正在被调试
	 * <p>
	 * 你可以在 settings.yml 的 "Debug" 键中设置要调试的段，
	 * 默认对应你继承 {@link SimpleSettings} 的类
	 * <p>
	 * 如果将 Debug 设置为 ["*"]，此方法始终返回 true
	 *
	 * @param section
	 * @return
	 */
	public static boolean isDebugged(String section) {
		return SimpleSettings.DEBUG_SECTIONS.contains(section) || SimpleSettings.DEBUG_SECTIONS.contains("*");
	}

	// ----------------------------------------------------------------------------------------------------
	// Saving errors to file
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 在控制台记录错误，并将所有细节写入 errors.log 文件
	 *
	 * @param t
	 * @param messages
	 */
	public static void saveError(Throwable t, String... messages) {

		if (Bukkit.getServer() == null) // Instance not set, e.g. when not using Bukkit
			return;

		final String systemInfo = "Running " + Bukkit.getName() + " " + Bukkit.getBukkitVersion() + " and Java " + System.getProperty("java.version");

		try {
			final List<String> lines = new ArrayList<>();
			final String header = SimplePlugin.getNamed() + " " + SimplePlugin.getVersion() + " encountered " + Common.article(t.getClass().getSimpleName());

			// Write out header and server info
			fill(lines,
					"------------------------------------[ " + TimeUtil.getFormattedDate() + " ]-----------------------------------",
					header,
					systemInfo,
					"Plugins: " + Common.join(Bukkit.getPluginManager().getPlugins(), ", "),
					"----------------------------------------------------------------------------------------------");

			// Write additional data
			if (messages != null && !String.join("", messages).isEmpty()) {
				fill(lines, "\nMore Information: ");
				fill(lines, messages);
			}

			{ // Write the stack trace

				do {
					// Write the error header
					fill(lines, t == null ? "Unknown error" : t.getClass().getSimpleName() + " " + Common.getOrDefault(t.getMessage(), Common.getOrDefault(t.getLocalizedMessage(), "(Unknown cause)")));

					int count = 0;

					for (final StackTraceElement el : t.getStackTrace()) {
						count++;

						final String trace = el.toString();

						if (trace.contains("sun.reflect"))
							continue;

						if (count > 6 && trace.startsWith("net.minecraft.server"))
							break;

						fill(lines, "\t at " + el.toString());
					}
				} while ((t = t.getCause()) != null);
			}

			fill(lines, "----------------------------------------------------------------------------------------------", System.lineSeparator());

			// Log to the console
			Bukkit.getLogger().severe(header + "! Please check your error.log and report this issue with the information in that file. " + systemInfo);

			// Finally, save the error file
			FileUtil.write(FoConstants.File.ERRORS, lines);

		} catch (final Throwable secondError) {
			Bukkit.getLogger().log(Level.SEVERE, "Got error when saving another error! Saving error:", secondError);
			Bukkit.getLogger().log(Level.SEVERE, "Original error that is not saved:", t);
		}
	}

	private static void fill(List<String> list, String... messages) {
		list.addAll(Arrays.asList(messages));
	}

	// ----------------------------------------------------------------------------------------------------
	// Utility methods
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 打印你的方法是从哪里被调用的
	 * 例如：YourClass > YourMainClass > MinecraftServer > Thread
	 * <p>
	 * 也可以打印行号 YourClass#LineNumber
	 *
	 * @param trackLineNumbers
	 * @return
	 */
	public static List<String> traceRoute(boolean trackLineNumbers) {
		final Exception exception = new RuntimeException("I love horses");
		final List<String> paths = new ArrayList<>();

		for (final StackTraceElement el : exception.getStackTrace()) {
			final String[] classNames = el.getClassName().split("\\.");
			final String className = classNames[classNames.length - 1];
			final String line = el.toString();

			if (line.contains("net.minecraft.server") || line.contains("org.bukkit.craftbukkit"))
				break;

			if (line.contains("org.bukkit.plugin.java.JavaPluginLoader") || line.contains("org.bukkit.plugin.SimplePluginManager") || line.contains("org.bukkit.plugin.JavaPlugin"))
				continue;

			if (!paths.contains(className))
				paths.add(className + "#" + el.getMethodName() + (trackLineNumbers ? "(" + el.getLineNumber() + ")" : ""));
		}

		// Remove call to self
		if (!paths.isEmpty())
			paths.remove(0);

		return paths;
	}

	/**
	 * 逐行打印数组的值及其索引
	 *
	 * @param values
	 */
	public static void printValues(Object[] values) {
		if (values != null) {
			print(Common.consoleLine());
			print("Enumeration of " + Common.plural(values.length, values.getClass().getSimpleName().toLowerCase().replace("[]", "")));

			for (int i = 0; i < values.length; i++)
				print("&8[" + i + "] &7" + values[i]);
		} else
			print("Value are null");
	}

	/**
	 * 打印堆栈跟踪，直到到达原生 MC/Bukkit 为止，并附带自定义消息
	 *
	 * @param debugLogMessage 仅用于说明的消息，包裹在抛出的堆栈跟踪外
	 */
	public static void printStackTrace(String debugLogMessage) {
		final StackTraceElement[] trace = new Exception().getStackTrace();

		print("!----------------------------------------------------------------------------------------------------------!");
		print(debugLogMessage);
		print("!----------------------------------------------------------------------------------------------------------!");

		for (int i = 1; i < trace.length; i++) {
			final String line = trace[i].toString();

			if (canPrint(line))
				print("\tat " + line);
		}

		print("--------------------------------------------------------------------------------------------------------end-");
	}

	/**
	 * 打印 Throwable 的第一行及其堆栈跟踪。
	 * <p>
	 * 忽略原生 Bukkit/Minecraft 服务器部分。
	 *
	 * @param throwable 要打印的 Throwable
	 */
	public static void printStackTrace(@NonNull Throwable throwable) {

		// Load all causes
		final List<Throwable> causes = new ArrayList<>();

		if (throwable.getCause() != null) {
			Throwable cause = throwable.getCause();

			do
				causes.add(cause);
			while ((cause = cause.getCause()) != null);
		}

		if (throwable instanceof FoException && !causes.isEmpty())
			// Do not print parent exception if we are only wrapping it, saves console spam
			print(throwable.getMessage());
		else {
			print(throwable.toString());

			printStackTraceElements(throwable);
		}

		if (!causes.isEmpty()) {
			final Throwable lastCause = causes.get(causes.size() - 1);

			print(lastCause.toString());
			printStackTraceElements(lastCause);
		}
	}

	private static void printStackTraceElements(Throwable throwable) {
		for (final StackTraceElement element : throwable.getStackTrace()) {
			final String line = element.toString();

			if (canPrint(line))
				print("\tat " + line);
		}
	}

	/**
	 * 返回某一行是否适合作为错误行打印——我们会忽略 NMS 等无用的刷屏内容
	 *
	 * @param message
	 * @return
	 */
	private static boolean canPrint(String message) {
		return !message.contains("net.minecraft") &&
				!message.contains("org.bukkit.craftbukkit") &&
				!message.contains("org.github.paperspigot.ServerScheduler") &&
				!message.contains("nashorn") &&
				!message.contains("javax.script") &&
				!message.contains("org.yaml.snakeyaml") &&
				!message.contains("sun.reflect") &&
				!message.contains("sun.misc") &&
				!message.contains("java.lang.Thread.run") &&
				!message.contains("java.util.concurrent.ThreadPoolExecutor");
	}

	// Print a simple console message
	private static void print(String message) {
		if (Bukkit.getConsoleSender() != null)
			Bukkit.getConsoleSender().sendMessage(Common.colorize(message));
		else
			System.out.println(Common.stripColors(message)); // our instance may or may not be available yet to log
	}
}
