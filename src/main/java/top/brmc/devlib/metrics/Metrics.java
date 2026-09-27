package top.brmc.devlib.metrics;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import top.brmc.devlib.Common;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.Remain;

/**
 * bStats 为插件作者收集一些数据。
 * <p>
 * 访问 https://bStats.org/ 了解更多关于 bStats 的信息！
 *
 * ** 重要 **
 * 请勿修改此类的任何部分
 * 这完全不受支持，并可能导致你的 bStats
 * 账户被封禁
 *
 * ** 法律声明 **
 * 以下类是 Bastian Opperman 的作品，出于善意收录于此，
 * 旨在帮助推广他出色的服务，
 * 让更多人使用 bStats。
 *
 * 更多信息见：https://bstats.org/getting-started/include-metrics
 */
public class Metrics {

	private final MetricsBase metricsBase;

	/**
	 * 创建一个新的 Metrics 实例。
	 *
	 * @param serviceId 服务的 id。可在 <a href="https://bstats.org/what-is-my-plugin-id">What is my plugin id?</a> 找到
	 */
	public Metrics(int serviceId) {

		// Get the config file
		final File bStatsFolder = new File(SimplePlugin.getData().getParentFile(), "bStats");
		final File configFile = new File(bStatsFolder, "config.yml");
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		if (!config.isSet("serverUuid")) {
			config.addDefault("enabled", true);
			config.addDefault("serverUuid", UUID.randomUUID().toString());
			config.addDefault("logFailedRequests", false);
			config.addDefault("logSentData", false);
			config.addDefault("logResponseStatusText", false);
			// Inform the server owners about bStats
			config
					.options()
					.header(
							"bStats (https://bStats.org) collects some basic information for plugin authors, like how\n"
									+ "many people use their plugin and their total player count. It's recommended to keep bStats\n"
									+ "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n"
									+ "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n"
									+ "anonymous.")
					.copyDefaults(true);
			try {
				config.save(configFile);
			} catch (final IOException ignored) {
			}
		}
		// Load the data
		final boolean enabled = config.getBoolean("enabled", true);
		final String serverUUID = config.getString("serverUuid");
		final boolean logErrors = config.getBoolean("logFailedRequests", false);
		final boolean logSentData = config.getBoolean("logSentData", false);
		final boolean logResponseStatusText = config.getBoolean("logResponseStatusText", false);
		this.metricsBase = new MetricsBase(
				"bukkit",
				serverUUID,
				serviceId,
				enabled,
				this::appendPlatformData,
				this::appendServiceData,
				Common::runLater,
				() -> true,
				(message, error) -> Common.error(error, message),
				Common::log,
				logErrors,
				logSentData,
				logResponseStatusText);
	}

	/**
	 * 添加一个自定义图表。
	 *
	 * @param chart 要添加的图表。
	 */
	public void addCustomChart(CustomChart chart) {
		this.metricsBase.addCustomChart(chart);
	}

	private void appendPlatformData(JsonObjectBuilder builder) {
		builder.appendField("playerAmount", Remain.getOnlinePlayers().size());
		builder.appendField("onlineMode", Bukkit.getOnlineMode() ? 1 : 0);
		builder.appendField("bukkitVersion", Bukkit.getVersion());
		builder.appendField("bukkitName", Bukkit.getName());
		builder.appendField("javaVersion", System.getProperty("java.version"));
		builder.appendField("osName", System.getProperty("os.name"));
		builder.appendField("osArch", System.getProperty("os.arch"));
		builder.appendField("osVersion", System.getProperty("os.version"));
		builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
	}

	private void appendServiceData(JsonObjectBuilder builder) {
		builder.appendField("pluginVersion", SimplePlugin.getVersion());
	}

	public static class MetricsBase {

		/** Metrics 类的版本。 */
		public static final String METRICS_VERSION = "3.0.0";

		private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, task -> new Thread(task, "bStats-Metrics"));

		private static final String REPORT_URL = "https://bStats.org/api/v2/data/%s";

		private final String platform;

		private final String serverUuid;

		private final int serviceId;

		private final Consumer<JsonObjectBuilder> appendPlatformDataConsumer;

		private final Consumer<JsonObjectBuilder> appendServiceDataConsumer;

		private final Consumer<Runnable> submitTaskConsumer;

		private final Supplier<Boolean> checkServiceEnabledSupplier;

		private final BiConsumer<String, Throwable> errorLogger;

		private final Consumer<String> infoLogger;

		private final boolean logErrors;

		private final boolean logSentData;

		private final boolean logResponseStatusText;

		private final Set<CustomChart> customCharts = new HashSet<>();

		private final boolean enabled;

		/**
		 * 创建一个新的 MetricsBase 类实例。
		 *
		 * @param platform 服务所在的平台。
		 * @param serviceId 服务的 id。
		 * @param serverUuid 服务器 uuid。
		 * @param enabled 是否启用数据发送。
		 * @param appendPlatformDataConsumer 接收 {@code JsonObjectBuilder} 并
		 *     追加所有平台相关数据的 Consumer。
		 * @param appendServiceDataConsumer 接收 {@code JsonObjectBuilder} 并
		 *     追加所有服务相关数据的 Consumer。
		 * @param submitTaskConsumer 接收提交任务 Runnable 的 Consumer。可用于
		 *     将数据收集委托给另一个线程，以避免并发导致的错误。
		 *     可以为 {@code null}。
		 * @param checkServiceEnabledSupplier 用于检查服务是否仍处于启用状态的 Supplier。
		 * @param errorLogger 接收日志消息和错误的 Consumer。
		 * @param infoLogger 接收信息日志消息的 Consumer。
		 * @param logErrors 是否记录错误日志。
		 * @param logSentData 是否记录已发送的数据。
		 * @param logResponseStatusText 是否记录响应状态文本。
		 */
		public MetricsBase(
				String platform,
				String serverUuid,
				int serviceId,
				boolean enabled,
				Consumer<JsonObjectBuilder> appendPlatformDataConsumer,
				Consumer<JsonObjectBuilder> appendServiceDataConsumer,
				Consumer<Runnable> submitTaskConsumer,
				Supplier<Boolean> checkServiceEnabledSupplier,
				BiConsumer<String, Throwable> errorLogger,
				Consumer<String> infoLogger,
				boolean logErrors,
				boolean logSentData,
				boolean logResponseStatusText) {
			this.platform = platform;
			this.serverUuid = serverUuid;
			this.serviceId = serviceId;
			this.enabled = enabled;
			this.appendPlatformDataConsumer = appendPlatformDataConsumer;
			this.appendServiceDataConsumer = appendServiceDataConsumer;
			this.submitTaskConsumer = submitTaskConsumer;
			this.checkServiceEnabledSupplier = checkServiceEnabledSupplier;
			this.errorLogger = errorLogger;
			this.infoLogger = infoLogger;
			this.logErrors = logErrors;
			this.logSentData = logSentData;
			this.logResponseStatusText = logResponseStatusText;
			this.checkRelocation();
			if (enabled)
				// WARNING: Removing the option to opt-out will get your plugin banned from bStats
				this.startSubmitting();
		}

		public void addCustomChart(CustomChart chart) {
			this.customCharts.add(chart);
		}

		private void startSubmitting() {
			final Runnable submitTask = () -> {
				if (!this.enabled || !this.checkServiceEnabledSupplier.get()) {
					// Submitting data or service is disabled
					scheduler.shutdown();
					return;
				}
				if (this.submitTaskConsumer != null)
					this.submitTaskConsumer.accept(this::submitData);
				else
					this.submitData();
			};
			// Many servers tend to restart at a fixed time at xx:00 which causes an uneven distribution
			// of requests on the
			// bStats backend. To circumvent this problem, we introduce some randomness into the initial
			// and second delay.
			// WARNING: You must not modify and part of this Metrics class, including the submit delay or
			// frequency!
			// WARNING: Modifying this code will get your plugin banned on bStats. Just don't do it!
			final long initialDelay = (long) (1000 * 60 * (3 + Math.random() * 3));
			final long secondDelay = (long) (1000 * 60 * (Math.random() * 30));
			scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS);
			scheduler.scheduleAtFixedRate(
					submitTask, initialDelay + secondDelay, 1000 * 60 * 30, TimeUnit.MILLISECONDS);
		}

		private void submitData() {
			final JsonObjectBuilder baseJsonBuilder = new JsonObjectBuilder();
			this.appendPlatformDataConsumer.accept(baseJsonBuilder);
			final JsonObjectBuilder serviceJsonBuilder = new JsonObjectBuilder();
			this.appendServiceDataConsumer.accept(serviceJsonBuilder);
			final JsonObjectBuilder.JsonObject[] chartData = this.customCharts.stream()
					.map(customChart -> customChart.getRequestJsonObject(this.errorLogger, this.logErrors))
					.filter(Objects::nonNull)
					.toArray(JsonObjectBuilder.JsonObject[]::new);
			serviceJsonBuilder.appendField("id", this.serviceId);
			serviceJsonBuilder.appendField("customCharts", chartData);
			baseJsonBuilder.appendField("service", serviceJsonBuilder.build());
			baseJsonBuilder.appendField("serverUUID", this.serverUuid);
			baseJsonBuilder.appendField("metricsVersion", METRICS_VERSION);
			final JsonObjectBuilder.JsonObject data = baseJsonBuilder.build();
			scheduler.execute(
					() -> {
						try {
							// Send the data
							this.sendData(data);
						} catch (final Exception e) {
							// Something went wrong! :(
							if (this.logErrors)
								this.errorLogger.accept("Could not submit bStats metrics data", e);
						}
					});
		}

		private void sendData(JsonObjectBuilder.JsonObject data) throws Exception {
			if (this.logSentData)
				this.infoLogger.accept("Sent bStats metrics data: " + data.toString());
			final String url = String.format(REPORT_URL, this.platform);
			final HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
			// Compress the data to save bandwidth
			final byte[] compressedData = compress(data.toString());
			connection.setRequestMethod("POST");
			connection.addRequestProperty("Accept", "application/json");
			connection.addRequestProperty("Connection", "close");
			connection.addRequestProperty("Content-Encoding", "gzip");
			connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("User-Agent", "Metrics-Service/1");
			connection.setDoOutput(true);
			try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
				outputStream.write(compressedData);
			}
			final StringBuilder builder = new StringBuilder();
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = bufferedReader.readLine()) != null)
					builder.append(line);
			}
			if (this.logResponseStatusText)
				this.infoLogger.accept("Sent data to bStats and received response: " + builder);
		}

		/** 检查该类是否已被正确重定位（relocate）。 */
		private void checkRelocation() {
			// You can use the property to disable the check in your test environment
			if (System.getProperty("bstats.relocatecheck") == null
					|| !System.getProperty("bstats.relocatecheck").equals("false")) {
				// Maven's Relocate is clever and changes strings, too. So we have to use this little
				// "trick" ... :D
				final String defaultPackage = new String(new byte[] { 'o', 'r', 'g', '.', 'b', 's', 't', 'a', 't', 's' });
				final String examplePackage = new String(new byte[] { 'y', 'o', 'u', 'r', '.', 'p', 'a', 'c', 'k', 'a', 'g', 'e' });
				// We want to make sure no one just copy & pastes the example and uses the wrong package
				// names
				if (MetricsBase.class.getPackage().getName().startsWith(defaultPackage)
						|| MetricsBase.class.getPackage().getName().startsWith(examplePackage))
					throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
			}
		}

		/**
		 * 对给定字符串进行 gzip 压缩。
		 *
		 * @param str 要压缩的字符串。
		 * @return 压缩后的字符串。
		 */
		private static byte[] compress(final String str) throws IOException {
			if (str == null)
				return null;
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
				gzip.write(str.getBytes(StandardCharsets.UTF_8));
			}
			return outputStream.toByteArray();
		}
	}

	public static class DrilldownPie extends CustomChart {

		private final Callable<Map<String, Map<String, Integer>>> callable;

		/**
		 * 类构造器。
		 *
		 * @param chartId 图表的 id。
		 * @param callable 用于请求图表数据的 Callable。
		 */
		public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		public JsonObjectBuilder.JsonObject getChartData() throws Exception {
			final JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			final Map<String, Map<String, Integer>> map = this.callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean reallyAllSkipped = true;
			for (final Map.Entry<String, Map<String, Integer>> entryValues : map.entrySet()) {
				final JsonObjectBuilder valueBuilder = new JsonObjectBuilder();
				boolean allSkipped = true;
				for (final Map.Entry<String, Integer> valueEntry : map.get(entryValues.getKey()).entrySet()) {
					valueBuilder.appendField(valueEntry.getKey(), valueEntry.getValue());
					allSkipped = false;
				}
				if (!allSkipped) {
					reallyAllSkipped = false;
					valuesBuilder.appendField(entryValues.getKey(), valueBuilder.build());
				}
			}
			if (reallyAllSkipped)
				// Null = skip the chart
				return null;
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class AdvancedPie extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * 类构造器。
		 *
		 * @param chartId 图表的 id。
		 * @param callable 用于请求图表数据的 Callable。
		 */
		public AdvancedPie(String chartId, Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			final JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			final Map<String, Integer> map = this.callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean allSkipped = true;
			for (final Map.Entry<String, Integer> entry : map.entrySet()) {
				if (entry.getValue() == 0)
					// Skip this invalid
					continue;
				allSkipped = false;
				valuesBuilder.appendField(entry.getKey(), entry.getValue());
			}
			if (allSkipped)
				// Null = skip the chart
				return null;
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class MultiLineChart extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * 类构造器。
		 *
		 * @param chartId 图表的 id。
		 * @param callable 用于请求图表数据的 Callable。
		 */
		public MultiLineChart(String chartId, Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			final JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			final Map<String, Integer> map = this.callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean allSkipped = true;
			for (final Map.Entry<String, Integer> entry : map.entrySet()) {
				if (entry.getValue() == 0)
					// Skip this invalid
					continue;
				allSkipped = false;
				valuesBuilder.appendField(entry.getKey(), entry.getValue());
			}
			if (allSkipped)
				// Null = skip the chart
				return null;
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class SimpleBarChart extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * 类构造器。
		 *
		 * @param chartId 图表的 id。
		 * @param callable 用于请求图表数据的 Callable。
		 */
		public SimpleBarChart(String chartId, Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			final JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			final Map<String, Integer> map = this.callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			for (final Map.Entry<String, Integer> entry : map.entrySet())
				valuesBuilder.appendField(entry.getKey(), new int[] { entry.getValue() });
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public abstract static class CustomChart {

		private final String chartId;

		protected CustomChart(String chartId) {
			if (chartId == null)
				throw new IllegalArgumentException("chartId must not be null");
			this.chartId = chartId;
		}

		public JsonObjectBuilder.JsonObject getRequestJsonObject(
				BiConsumer<String, Throwable> errorLogger, boolean logErrors) {
			final JsonObjectBuilder builder = new JsonObjectBuilder();
			builder.appendField("chartId", this.chartId);
			try {
				final JsonObjectBuilder.JsonObject data = this.getChartData();
				if (data == null)
					// If the data is null we don't send the chart.
					return null;
				builder.appendField("data", data);
			} catch (final Throwable t) {
				if (logErrors)
					errorLogger.accept("Failed to get data for custom chart with id " + this.chartId, t);
				return null;
			}
			return builder.build();
		}

		protected abstract JsonObjectBuilder.JsonObject getChartData() throws Exception;
	}

	public static class SimplePie extends CustomChart {

		private final Callable<String> callable;

		/**
		 * 类构造器。
		 *
		 * @param chartId 图表的 id。
		 * @param callable 用于请求图表数据的 Callable。
		 */
		public SimplePie(String chartId, Callable<String> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			final String value = this.callable.call();
			if (value == null || value.isEmpty())
				// Null = skip the chart
				return null;
			return new JsonObjectBuilder().appendField("value", value).build();
		}
	}

	public static class AdvancedBarChart extends CustomChart {

		private final Callable<Map<String, int[]>> callable;

		/**
		 * 类构造器。
		 *
		 * @param chartId 图表的 id。
		 * @param callable 用于请求图表数据的 Callable。
		 */
		public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			final JsonObjectBuilder valuesBuilder = new JsonObjectBuilder();
			final Map<String, int[]> map = this.callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean allSkipped = true;
			for (final Map.Entry<String, int[]> entry : map.entrySet()) {
				if (entry.getValue().length == 0)
					// Skip this invalid
					continue;
				allSkipped = false;
				valuesBuilder.appendField(entry.getKey(), entry.getValue());
			}
			if (allSkipped)
				// Null = skip the chart
				return null;
			return new JsonObjectBuilder().appendField("values", valuesBuilder.build()).build();
		}
	}

	public static class SingleLineChart extends CustomChart {

		private final Callable<Integer> callable;

		/**
		 * 类构造器。
		 *
		 * @param chartId 图表的 id。
		 * @param callable 用于请求图表数据的 Callable。
		 */
		public SingleLineChart(String chartId, Callable<Integer> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
			final int value = this.callable.call();
			if (value == 0)
				// Null = skip the chart
				return null;
			return new JsonObjectBuilder().appendField("value", value).build();
		}
	}

	/**
	 * 一个极其简单的 JSON 构建器。
	 *
	 * <p>虽然此类功能不多，性能也不是最好的，但对于它的用途来说
	 * 已经足够。
	 */
	public static class JsonObjectBuilder {

		private StringBuilder builder = new StringBuilder();

		private boolean hasAtLeastOneField = false;

		public JsonObjectBuilder() {
			this.builder.append("{");
		}

		/**
		 * 向 JSON 追加一个 null 字段。
		 *
		 * @param key 字段的键。
		 * @return 对当前对象的引用。
		 */
		public JsonObjectBuilder appendNull(String key) {
			this.appendFieldUnescaped(key, "null");
			return this;
		}

		/**
		 * 向 JSON 追加一个字符串字段。
		 *
		 * @param key 字段的键。
		 * @param value 字段的值。
		 * @return 对当前对象的引用。
		 */
		public JsonObjectBuilder appendField(String key, String value) {
			if (value == null)
				throw new IllegalArgumentException("JSON value must not be null");
			this.appendFieldUnescaped(key, "\"" + escape(value) + "\"");
			return this;
		}

		/**
		 * 向 JSON 追加一个整数字段。
		 *
		 * @param key 字段的键。
		 * @param value 字段的值。
		 * @return 对当前对象的引用。
		 */
		public JsonObjectBuilder appendField(String key, int value) {
			this.appendFieldUnescaped(key, String.valueOf(value));
			return this;
		}

		/**
		 * 向 JSON 追加一个对象。
		 *
		 * @param key 字段的键。
		 * @param object 对象。
		 * @return 对当前对象的引用。
		 */
		public JsonObjectBuilder appendField(String key, JsonObject object) {
			if (object == null)
				throw new IllegalArgumentException("JSON object must not be null");
			this.appendFieldUnescaped(key, object.toString());
			return this;
		}

		/**
		 * 向 JSON 追加一个字符串数组。
		 *
		 * @param key 字段的键。
		 * @param values 字符串数组。
		 * @return 对当前对象的引用。
		 */
		public JsonObjectBuilder appendField(String key, String[] values) {
			if (values == null)
				throw new IllegalArgumentException("JSON values must not be null");
			final String escapedValues = Arrays.stream(values)
					.map(value -> "\"" + escape(value) + "\"")
					.collect(Collectors.joining(","));
			this.appendFieldUnescaped(key, "[" + escapedValues + "]");
			return this;
		}

		/**
		 * 向 JSON 追加一个整数数组。
		 *
		 * @param key 字段的键。
		 * @param values 整数数组。
		 * @return 对当前对象的引用。
		 */
		public JsonObjectBuilder appendField(String key, int[] values) {
			if (values == null)
				throw new IllegalArgumentException("JSON values must not be null");
			final String escapedValues = Arrays.stream(values).mapToObj(String::valueOf).collect(Collectors.joining(","));
			this.appendFieldUnescaped(key, "[" + escapedValues + "]");
			return this;
		}

		/**
		 * 向 JSON 追加一个对象数组。
		 *
		 * @param key 字段的键。
		 * @param values 对象数组。
		 * @return 对当前对象的引用。
		 */
		public JsonObjectBuilder appendField(String key, JsonObject[] values) {
			if (values == null)
				throw new IllegalArgumentException("JSON values must not be null");
			final String escapedValues = Arrays.stream(values).map(JsonObject::toString).collect(Collectors.joining(","));
			this.appendFieldUnescaped(key, "[" + escapedValues + "]");
			return this;
		}

		/**
		 * 向对象追加一个字段。
		 *
		 * @param key 字段的键。
		 * @param escapedValue 字段已转义的值。
		 */
		private void appendFieldUnescaped(String key, String escapedValue) {
			if (this.builder == null)
				throw new IllegalStateException("JSON has already been built");
			if (key == null)
				throw new IllegalArgumentException("JSON key must not be null");
			if (this.hasAtLeastOneField)
				this.builder.append(",");
			this.builder.append("\"").append(escape(key)).append("\":").append(escapedValue);
			this.hasAtLeastOneField = true;
		}

		/**
		 * 构建 JSON 字符串，并使此构建器失效。
		 *
		 * @return 构建出的 JSON 字符串。
		 */
		public JsonObject build() {
			if (this.builder == null)
				throw new IllegalStateException("JSON has already been built");
			final JsonObject object = new JsonObject(this.builder.append("}").toString());
			this.builder = null;
			return object;
		}

		/**
		 * 按照 https://www.ietf.org/rfc/rfc4627.txt 中的规定对给定字符串进行转义。
		 *
		 * <p>此方法只转义必要的字符 '"'、'\' 以及 '\u0000' - '\u001F'。
		 * 不使用紧凑转义（例如 '\n' 会被转义为 "\u000a" 而不是 "\n"）。
		 *
		 * @param value 要转义的值。
		 * @return 转义后的值。
		 */
		private static String escape(String value) {
			final StringBuilder builder = new StringBuilder();
			for (int i = 0; i < value.length(); i++) {
				final char c = value.charAt(i);
				if (c == '"')
					builder.append("\\\"");
				else if (c == '\\')
					builder.append("\\\\");
				else if (c <= '\u000F')
					builder.append("\\u000").append(Integer.toHexString(c));
				else if (c <= '\u001F')
					builder.append("\\u00").append(Integer.toHexString(c));
				else
					builder.append(c);
			}
			return builder.toString();
		}

		/**
		 * JSON 对象的一个极简表示。
		 *
		 * <p>此类仅用于让 {@link JsonObjectBuilder} 的方法类型安全，
		 * 不允许像 {@link JsonObjectBuilder#appendField(String,
		 * JsonObject)} 这样的方法接收原始字符串输入。
		 */
		public static class JsonObject {

			private final String value;

			private JsonObject(String value) {
				this.value = value;
			}

			@Override
			public String toString() {
				return this.value;
			}
		}
	}
}