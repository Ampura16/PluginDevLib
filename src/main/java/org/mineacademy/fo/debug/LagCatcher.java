package org.mineacademy.fo.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 一种简单而有效的方式，用于计算代码中
 * 两个位置之间的耗时
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LagCatcher {

	/**
	 * 存放各代码段及其开始计时的时间
	 */
	private static final Map<String, Long> startTimesMap = new HashMap<>();

	/**
	 * 存放各代码段及其对应的卡顿耗时列表
	 */
	private static final Map<String, List<Long>> durationsMap = new HashMap<>();

	/**
	 * 将代码段及当前毫秒时间放入计时映射中
	 *
	 * @param section
	 */
	public static void start(String section) {
		if (SimpleSettings.LAG_THRESHOLD_MILLIS == -1)
			return;

		startTimesMap.put(section, System.nanoTime());
	}

	/**
	 * 停止对代码段计时；若耗时超过 {@link SimpleSettings} 中设置的阈值，
	 * 则在控制台打印一条消息
	 *
	 * @param section
	 */
	public static void end(String section) {
		end(section, false);
	}

	/**
	 * 停止对代码段计时；若耗时超过阈值，则在控制台打印一条消息。
	 * rapid 为 true 表示总是记录耗时，
	 * 为 false 表示仅在超过 {@link SimpleSettings} 中设置的上限时记录
	 *
	 * @param section
	 * @param rapid
	 */
	public static void end(String section, boolean rapid) {
		end(section, rapid ? 0 : SimpleSettings.LAG_THRESHOLD_MILLIS, "{section} took {time} ms");
	}

	/**
	 * 停止对代码段计时；若耗时超过给定阈值，
	 * 则在控制台打印自定义消息
	 * <p>
	 * 使用 {section} 和 {time} 替换被调试的代码段及其耗时
	 *
	 * @param section
	 * @param thresholdMs
	 * @param message
	 */
	public static void end(String section, int thresholdMs, String message) {
		final double lag = finishAndCalculate(section);

		if (lag > thresholdMs && SimpleSettings.LAG_THRESHOLD_MILLIS != -1) {
			message = (SimplePlugin.hasInstance() ? "[" + SimplePlugin.getNamed() + " " + SimplePlugin.getVersion() + "] " : "") + message
					.replace("{section}", section)
					.replace("{time}", MathUtil.formatTwoDigits(lag));

			System.out.println(message);
		}
	}

	/**
	 * 尝试将给定代码连续快速运行给定的次数，
	 * 把卡顿时间累加起来，以观察执行次数
	 * 成倍增加时总共需要多长时间
	 *
	 * @param cycles
	 * @param name   卡顿代码段名称
	 * @param code
	 */
	public static void performanceTest(int cycles, String name, Runnable code) {
		Valid.checkBoolean(cycles > 0, "Cycles must be above 0");

		LagCatcher.start(name + "-whole");

		final List<Double> lagMap = new ArrayList<>();

		for (int i = 0; i < cycles; i++) {
			LagCatcher.start(name);
			code.run();
			lagMap.add(finishAndCalculate(name));
		}

		System.out.println("Test '" + name + "' took " + MathUtil.formatTwoDigits(finishAndCalculate(name + "-whole")) + " ms. Average " + MathUtil.average(lagMap) + " ms");

		// Measure individual sub sections of the performance test
		if (!durationsMap.isEmpty()) {
			for (final Map.Entry<String, List<Long>> entry : durationsMap.entrySet()) {
				final String section = entry.getKey();
				long duration = 0;

				for (final long sectionDuration : entry.getValue())
					duration += sectionDuration;

				System.out.println("\tSection '" + section + "' took " + MathUtil.formatTwoDigits(duration / 1_000_000D));
			}

			System.out.println("Section measurement ended.");

			durationsMap.clear();
		}
	}

	/**
	 * 与 {@link #start(String)} 方法类似，不同之处在于每次调用都会累加，
	 * 并在 {@link #performanceTest(int, String, Runnable)} 中显示！
	 *
	 * @param section
	 */
	public static void performancePartStart(String section) {
		List<Long> sectionDurations = durationsMap.get(section);

		if (sectionDurations == null) {
			sectionDurations = new ArrayList<>();

			durationsMap.put(section, sectionDurations);
		}

		// Do not calculate duration, just append last time at the end
		sectionDurations.add(System.nanoTime());
	}

	/**
	 * 与 {@link #start(String)} 方法类似，不同之处在于每次调用都会累加，
	 * 并在 {@link #performanceTest(int, String, Runnable)} 中显示！
	 *
	 * 它会捕获上一次 {@link #performancePartStart(String)} 调用以来的耗时，
	 * 并把卡顿时间放入性能测试结束后显示的映射中。
	 *
	 * @param section
	 */
	public static void performancePartSnap(String section) {
		Valid.checkBoolean(durationsMap.containsKey(section), "Section " + section + " is not measured! Are you calling it from performanceTest?");

		final List<Long> sectionDurations = durationsMap.get(section);

		final int index = sectionDurations.size() - 1;
		final long nanoTime = sectionDurations.get(index);
		final long duration = System.nanoTime() - nanoTime;

		sectionDurations.set(index, duration);
	}

	/**
	 * 计算代码段的耗时（毫秒），并将其从计时映射中移除
	 *
	 * @param section
	 * @return
	 */
	private static double finishAndCalculate(String section) {
		final Long nanoTime = startTimesMap.remove(section);

		return nanoTime == null ? 0D : (System.nanoTime() - nanoTime) / 1_000_000D;
	}
}