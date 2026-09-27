package top.brmc.devlib.settings;

import java.util.Arrays;
import java.util.List;

import top.brmc.devlib.Common;
import top.brmc.devlib.Messenger;
import top.brmc.devlib.SerializeUtil;
import top.brmc.devlib.SerializeUtil.Mode;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.exception.FoScriptException;
import top.brmc.devlib.model.JavaScriptExecutor;
import top.brmc.devlib.model.SimpleComponent;

/**
 * 表示新的国际化方式，最大的
 * 优点是节省开发时间。
 *
 * 缺点是加载时不会检查键，因此任何
 * 格式错误或缺失的键都会在之后才失败，且可能不被察觉。
 */
public final class Lang extends YamlConfig {

	/**
	 * 此类的实例
	 */
	private static Lang instance;

	/*
	 * Create a new instance and load the given file
	 */
	private Lang(String filePath) {
		this.loadConfiguration(filePath);
	}

	/*
	 * Return a key from our localization, failing if not exists
	 */
	private String getStringStrict(String path) {
		final String key = this.getString(path);
		Valid.checkNotNull(key, "Missing localization key '" + path + "' from " + this.getFileName());

		return key;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static access - loading
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 在 onPluginPreStart 中调用此方法以使用 Lang 功能，
	 * Lang 类将使用以下路径中的文件：
	 * "localization/messages_" + SimpleSettings.LOCALE_PREFIX ".yml"
	 */
	public static void init() {
		init("localization/messages_" + SimpleSettings.LOCALE_PREFIX + ".yml");
	}

	/**
	 * 在 onPluginPreStart 中调用此方法以使用 Lang 功能，
	 * Lang 类将使用给定路径中的文件。
	 *
	 * 示例："localization/messages_" + SimpleSettings.LOCALE_PREFIX ".yml"
	 * @param filePath
	 */
	public static void init(String filePath) {
		instance = new Lang(filePath);

		loadPrefixes();
	}

	/**
	 * 重新加载语言文件
	 *
	 * @deprecated 仅供内部使用
	 */
	@Deprecated
	public static void reloadLang() {
		if (instance != null) {
			instance.reload();
			instance.save();
		}
	}

	/**
	 * 从语言文件重新加载前缀
	 *
	 * @deprecated 仅供内部使用
	 */
	@Deprecated
	public static void loadPrefixes() {
		if (instance != null) {
			if (instance.isSet("Prefix.Announce"))
				Messenger.setAnnouncePrefix(Lang.of("Prefix.Announce"));

			if (instance.isSet("Prefix.Error"))
				Messenger.setErrorPrefix(Lang.of("Prefix.Error"));

			if (instance.isSet("Prefix.Info"))
				Messenger.setInfoPrefix(Lang.of("Prefix.Info"));

			if (instance.isSet("Prefix.Question"))
				Messenger.setQuestionPrefix(Lang.of("Prefix.Question"));

			if (instance.isSet("Prefix.Success"))
				Messenger.setSuccessPrefix(Lang.of("Prefix.Success"));

			if (instance.isSet("Prefix.Warn"))
				Messenger.setWarnPrefix(Lang.of("Prefix.Warn"));

			instance.save();
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回路径上的布尔值
	 *
	 * @param path
	 * @return
	 */
	public static boolean getOption(String path) {
		checkInit();

		return instance.getBoolean(path);
	}

	/**
	 * 从本地化文件返回组件列表，并替换 {0} {1} 等变量。
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static List<SimpleComponent> ofComponentList(String path, Object... variables) {
		return Common.convert(ofList(path, variables), SimpleComponent::of);
	}

	/**
	 * 从本地化文件返回列表，并替换 {0} {1} 等变量。
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static List<String> ofList(String path, Object... variables) {
		return Arrays.asList(ofArray(path, variables));
	}

	/**
	 * 从本地化文件返回数组，并替换 {0} {1} 等变量。
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static String[] ofArray(String path, Object... variables) {
		return of(path, variables).split("\n");
	}

	/**
	 * 从本地化文件返回组件，并替换 {0} {1} 等变量。
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static SimpleComponent ofComponent(String path, Object... variables) {
		return SimpleComponent.of(of(path, variables));
	}

	/**
	 * 根据给定数量自动返回给定键的
	 * 单数或复数形式（包含数量）
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String ofCase(long amount, String path) {
		return amount + " " + ofCaseNoAmount(amount, path);
	}

	/**
	 * 根据给定数量自动返回给定键的
	 * 单数或复数形式（不含数量）
	 *
	 * @param amount
	 * @param path
	 * @return
	 */
	public static String ofCaseNoAmount(long amount, String path) {
		final String key = of(path);
		final String[] split = key.split(", ");

		Valid.checkBoolean(split.length == 1 || split.length == 2, "Invalid syntax of key at '" + path + "', this key is a special one and "
				+ "it needs singular and plural form separated with , such as: second, seconds");

		final String singular = split[0];
		final String plural = split[split.length == 2 ? 1 : 0];

		return amount == 0 || amount > 1 ? plural : singular;
	}

	/**
	 * 从本地化文件返回数组，替换 {0} {1} 等变量，
	 * 并解析脚本变量。我们将语言键视为有效的 JavaScript
	 *
	 * @param path
	 * @param scriptVariables
	 * @param stringVariables
	 * @deprecated 不稳定，JavaScript 执行器可能失去同步并破坏 scriptVariables
	 *
	 * @return
	 */
	@Deprecated
	public static String ofScript(String path, SerializedMap scriptVariables, Object... stringVariables) {
		String script = of(path, stringVariables);
		Object result;

		// Our best guess is that the user has removed the script completely but forgot to put the entire message in '',
		// so we attempt to do so
		if (!script.contains("?") && !script.contains(":") && !script.contains("+") && !script.startsWith("'") && !script.endsWith("'"))
			script = "'" + script + "'";

		try {
			result = JavaScriptExecutor.run(script, scriptVariables.asMap());

		} catch (final FoScriptException ex) {
			Common.logFramed("Failed to compile localization key!",
					"It must be a valid JavaScript code, if you modified it, check the syntax!",
					"",
					"Locale path: '" + path + "'",
					"Variables: " + scriptVariables,
					"String variables: " + Common.join(stringVariables),
					"Script: " + script,
					"Error: %error%");

			throw ex;
		}

		return result.toString();
	}

	/**
	 * 从本地化文件返回键值，并替换 {0} {1} 等变量。
	 *
	 * @param path
	 * @param variables
	 * @return
	 */
	public static String of(String path, Object... variables) {
		checkInit();

		String key = instance.getStringStrict(path);

		key = Messenger.replacePrefixes(key);
		key = translate(key, variables);

		return key;
	}

	/*
	 * Replace placeholders in the message
	 */
	private static String translate(String key, Object... variables) {
		Valid.checkNotNull(key, "Cannot translate a null key with variables " + Common.join(variables));

		if (variables != null)
			for (int i = 0; i < variables.length; i++) {
				Object variable = variables[i];

				variable = Common.getOrDefaultStrict(SerializeUtil.serialize(Mode.YAML /* ĺocale is always .yml */, variable), SimpleLocalization.NONE);
				Valid.checkNotNull(variable, "Failed to replace {" + i + "} as " + variable + " (raw = " + variables[i] + ")");

				key = key.replace("{" + i + "}", variable.toString());
			}

		return key;
	}

	/*
	 * Check if this class has properly been initialized
	 */
	private static void checkInit() {

		// Automatically load when not loaded in onPluginPreStart
		if (instance == null)
			init();
	}
}
