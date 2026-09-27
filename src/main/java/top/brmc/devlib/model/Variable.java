package top.brmc.devlib.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import top.brmc.devlib.Common;
import top.brmc.devlib.PlayerUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.constants.FoConstants;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.exception.FoScriptException;
import top.brmc.devlib.settings.ConfigItems;
import top.brmc.devlib.settings.YamlConfig;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public final class Variable extends YamlConfig {

	/**
	 * 返回给定变量字段名对应的原型文件路径
	 */
	public static Function<String, String> PROTOTYPE_PATH = t -> NO_DEFAULT;

	/**
	 * 所有已加载变量的列表
	 */
	private static final ConfigItems<Variable> loadedVariables = ConfigItems.fromFolder("variables", Variable.class);

	/**
	 * 此变量的种类
	 */
	@Getter
	private Type type;

	/**
	 * 要查找的变量键
	 */
	@Getter
	private String key;

	/**
	 * 用于替换该键的变量值
	 * JavaScript 引擎
	 */
	private String value;

	/**
	 * 必须返回 TRUE 才会显示此变量的 JavaScript 条件
	 */
	@Getter
	private String senderCondition;

	/**
	 * 必须返回 TRUE 才会向接收者显示此变量的 JavaScript 条件
	 */
	@Getter
	private String receiverCondition;

	/**
	 * 发送者显示此部分所需的权限
	 */
	@Getter
	private String senderPermission;

	/**
	 * 接收者看到此部分所需的权限
	 */
	@Getter
	private String receiverPermission;

	/**
	 * 悬停文本，未设置则为 null
	 */
	@Getter
	private List<String> hoverText;

	/**
	 * 指向特定 {@link ItemStack} 的 JavaScript
	 */
	@Getter
	private String hoverItem;

	/**
	 * 点击时应打开的 URL？没有则为 null
	 */
	@Getter
	private String openUrl;

	/**
	 * 点击时应建议的命令？没有则为 null
	 */
	@Getter
	private String suggestCommand;

	/**
	 * 点击时应执行的命令？没有则为 null
	 */
	@Getter
	private String runCommand;

	/*
	 * Create and load a new variable (automatically called)
	 */
	private Variable(String file) {
		final String prototypePath = PROTOTYPE_PATH.apply(file);

		this.setHeader(FoConstants.Header.VARIABLE_FILE);
		this.loadConfiguration(prototypePath, "variables/" + file + ".yml");
	}

	// ----------------------------------------------------------------------------------
	// Loading
	// ----------------------------------------------------------------------------------

	/**
	 * @see top.brmc.devlib.settings.YamlConfig#onLoad()
	 */
	@Override
	protected void onLoad() {

		this.type = this.get("Type", Type.class);
		this.key = this.getString("Key");
		this.value = this.getString("Value");
		this.senderCondition = this.getString("Sender_Condition");
		this.receiverCondition = this.getString("Receiver_Condition");
		this.senderPermission = this.getString("Sender_Permission");
		this.receiverPermission = this.getString("Receiver_Permission");

		// Correct common mistakes
		if (this.type == null) {
			this.type = Type.FORMAT;

			this.save();
		}

		// Check for known mistakes
		if (this.key == null || this.key.isEmpty())
			throw new NullPointerException("(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Key' as variable name in " + this.getFileName());

		if (this.value == null)
			throw new NullPointerException("(DO NOT REPORT, PLEASE FIX YOURSELF) Please set 'Value' key as what the variable shows in " + this.getFileName() + " (this can be a JavaScript code)");

		if (this.key.startsWith("{") || this.key.startsWith("[")) {
			this.key = this.key.substring(1);

			this.save();
		}

		if (this.key.endsWith("}") || this.key.endsWith("]")) {
			this.key = this.key.substring(0, this.key.length() - 1);

			this.save();
		}

		if (this.type == Type.MESSAGE) {
			this.hoverText = this.getStringList("Hover");
			this.hoverItem = this.getString("Hover_Item");
			this.openUrl = this.getString("Open_Url");
			this.suggestCommand = this.getString("Suggest_Command");
			this.runCommand = this.getString("Run_Command");
		}

		// Test for key validity
		if (!Common.regExMatch("^\\w+$", this.key))
			throw new IllegalArgumentException("(DO NOT REPORT, PLEASE FIX YOURSELF) The 'Key' variable in " + this.getFileName() + " must only contains letters, numbers or underscores. Do not write [] or {} there!");
	}

	@Override
	public void onSave() {
		this.set("Type", this.type);
		this.set("Key", this.key);
		this.set("Value", this.value);
		this.set("Sender_Condition", this.senderCondition);
		this.set("Receiver_Condition", this.receiverCondition);
		this.set("Hover", this.hoverText);
		this.set("Hover_Item", this.hoverItem);
		this.set("Open_Url", this.openUrl);
		this.set("Suggest_Command", this.suggestCommand);
		this.set("Run_Command", this.runCommand);
		this.set("Sender_Permission", this.senderPermission);
		this.set("Receiver_Permission", this.receiverPermission);
	}

	// ----------------------------------------------------------------------------------
	// Getters
	// ----------------------------------------------------------------------------------

	/**
	 * 为给定玩家和替换项运行脚本，
	 * 并返回输出
	 *
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public String getValue(CommandSender sender, Map<String, Object> replacements) {

		// Replace variables in script
		final String script;

		try {
			script = Variables.replace(this.value, sender, replacements, true, false);

		} catch (final Throwable t) {
			final String errorHeadline = "Error replacing placeholders in variable!";

			Common.logFramed(
					errorHeadline,
					"",
					"Variable: " + this.value,
					"Sender: " + sender,
					"Replacements: " + replacements,
					"Error: " + t.getMessage(),
					"",
					"Please report this issue!");

			if (FoException.isErrorSavedAutomatically())
				Debugger.saveError(t, errorHeadline);

			return "";
		}

		Object result = null;

		try {
			result = JavaScriptExecutor.run(script, sender);

		} catch (final FoScriptException ex) {
			Common.logFramed(
					"Error executing JavaScript in a variable!",
					"Variable: " + this.getFileName(),
					"Line: " + ex.getErrorLine(),
					"Sender: " + sender,
					"Replacements: " + replacements,
					"Error: " + ex.getMessage(),
					"",
					"This is likely NOT our plugin bug, check Value key in " + this.getFileName(),
					"that it returns a valid JavaScript code before reporting!");

			throw ex;
		}

		return result != null ? result.toString() : "";
	}

	/**
	 * 构建此变量，不附加其他组件
	 *
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public String buildPlain(CommandSender sender, Map<String, Object> replacements) {

		if (this.senderPermission != null && !this.senderPermission.isEmpty() && !PlayerUtil.hasPerm(sender, this.senderPermission))
			return "";

		if (this.senderCondition != null && !this.senderCondition.isEmpty()) {

			try {
				final Object result = JavaScriptExecutor.run(Variables.replace(this.senderCondition, sender, replacements, true, false), sender);

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "Variable '" + this.getFileName() + "' option Condition must return boolean not " + (result == null ? "null" : result.getClass()));

					if (!((boolean) result))
						return "";
				}

			} catch (final FoScriptException ex) {
				Common.logFramed(
						"Error executing Sender_Condition in a variable!",
						"Variable: " + this.getFileName(),
						"Sender condition: " + this.senderCondition,
						"Sender: " + sender,
						"Replacements: " + replacements,
						"Error: " + ex.getMessage(),
						"",
						"This is likely NOT a plugin bug,",
						"check your JavaScript code in",
						this.getFileName() + " in the 'Sender_Condition' key",
						"before reporting it to us.");

				throw ex;
			}
		}

		final String value = this.getValue(sender, replacements);

		return value == null || value.isEmpty() || "null".equals(value) ? "" : value;
	}

	/**
	 * 创建变量并追加到现有组件中，如同由玩家发起
	 *
	 * @param sender
	 * @param existingComponent
	 * @param replacements
	 * @return
	 */
	public SimpleComponent build(CommandSender sender, SimpleComponent existingComponent, Map<String, Object> replacements) {

		if (this.senderPermission != null && !this.senderPermission.isEmpty() && !PlayerUtil.hasPerm(sender, this.senderPermission))
			return SimpleComponent.of("");

		if (this.senderCondition != null && !this.senderCondition.isEmpty()) {

			try {
				final Object result = JavaScriptExecutor.run(Variables.replace(this.senderCondition, sender, replacements, true, false), sender);

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "Variable '" + this.getFileName() + "' option Condition must return boolean not " + (result == null ? "null" : result.getClass()));

					if (!((boolean) result))
						return SimpleComponent.of("");
				}

			} catch (final FoScriptException ex) {
				Common.logFramed(
						"Error executing Sender_Condition in a variable!",
						"Variable: " + this.getFileName(),
						"Sender condition: " + this.senderCondition,
						"Sender: " + sender,
						"Replacements: " + replacements,
						"Error: " + ex.getMessage(),
						"",
						"This is likely NOT a plugin bug,",
						"check your JavaScript code in",
						this.getFileName() + " in the 'Sender_Condition' key",
						"before reporting it to us.");

				throw ex;
			}
		}

		final String value = this.getValue(sender, replacements);

		if (value == null || value.isEmpty() || "null".equals(value))
			return SimpleComponent.of("");

		final SimpleComponent component = existingComponent
				.append(value)
				.viewPermission(this.receiverPermission)
				.viewCondition(this.receiverCondition);

		if (!Valid.isNullOrEmpty(this.hoverText)) {
			// Trick: Join the lines to only parse variables at once -- performance++ -- then split again
			final String deliminer = "%FLVJ%";

			component.onHover(Variables.replace(String.join(deliminer, this.hoverText), sender, replacements, true, false).split(deliminer));
		}

		if (this.hoverItem != null && !this.hoverItem.isEmpty()) {

			try {
				final Object result = JavaScriptExecutor.run(Variables.replace(this.hoverItem, sender, replacements, true, false), sender);

				Valid.checkBoolean(result instanceof ItemStack, "Variable '" + this.getFileName() + "' option Hover_Item must return ItemStack not " + result.getClass());
				component.onHover((ItemStack) result);

			} catch (final FoScriptException ex) {
				Common.logFramed(
						"Error executing Hover_Item in a variable!",
						"Variable: " + this.getFileName(),
						"Hover Item: " + this.hoverItem,
						"Sender: " + sender,
						"Replacements: " + replacements,
						"Error: " + ex.getMessage(),
						"",
						"This is likely NOT a plugin bug,",
						"check your JavaScript code in",
						this.getFileName() + " in the 'Hover_Item' key",
						"before reporting it to us.");

				throw ex;
			}
		}

		if (this.openUrl != null && !this.openUrl.isEmpty())
			component.onClickOpenUrl(Variables.replace(this.openUrl, sender, replacements, true, false));

		if (this.suggestCommand != null && !this.suggestCommand.isEmpty())
			component.onClickSuggestCmd(Variables.replace(this.suggestCommand, sender, replacements, true, false));

		if (this.runCommand != null && !this.runCommand.isEmpty())
			component.onClickRunCmd(Variables.replace(this.runCommand, sender, replacements, true, false));

		return component;
	}

	/**
	 * @see top.brmc.devlib.settings.YamlConfig#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Variable && this.key.equals(((Variable) obj).getKey());
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * 创建新变量并加载
	 *
	 * @param name
	 */
	public static void createVariable(String name) {
		loadedVariables.loadOrCreateItem(name);
	}

	/**
	 * 从 variables/ 文件夹加载所有变量
	 */
	public static void loadVariables() {
		loadedVariables.loadItems();
	}

	/**
	 * 移除给定变量（若存在）
	 *
	 * @param variable
	 */
	public static void removeVariable(final Variable variable) {
		loadedVariables.removeItem(variable);
	}

	/**
	 * 若给定键的变量已加载则返回 true
	 *
	 * @param name
	 * @return
	 */
	public static boolean isVariableLoaded(final String name) {
		return loadedVariables.isItemLoaded(name);
	}

	/**
	 * 返回变量，未加载则返回 null
	 *
	 * @param name
	 * @return
	 */
	public static Variable findVariable(@NonNull final String name) {
		for (final Variable item : getVariables())
			if (item.getKey().equalsIgnoreCase(name))
				return item;

		return null;
	}

	/**
	 * 返回所有变量的列表
	 *
	 * @return
	 */
	public static Collection<Variable> getVariables() {
		return loadedVariables.getItems();
	}

	/**
	 * 返回所有变量名称的列表
	 *
	 * @return
	 */
	public static Set<String> getVariableNames() {
		return loadedVariables.getItemNames();
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * 表示变量类型
	 */
	@RequiredArgsConstructor
	public enum Type {

		/**
		 * 此变量用于聊天格式和“服务器发给玩家”的消息，
		 * 玩家无法使用。示例：[{channel}] {player}: {message}
		 */
		FORMAT("format"),

		/**
		 * 此变量可由玩家在聊天中使用，例如 "I have an [item]"
		 */
		MESSAGE("message"),;

		/**
		 * 可保存的未混淆键
		 */
		@Getter
		private final String key;

		/**
		 * 尝试根据给定配置键加载类型
		 *
		 * @param key
		 * @return
		 */
		public static Type fromKey(String key) {
			for (final Type mode : values())
				if (mode.key.equalsIgnoreCase(key))
					return mode;

			throw new IllegalArgumentException("No such item type: " + key + ". Available: " + Common.join(values()));
		}

		/**
		 * 返回 {@link #getKey()}
		 */
		@Override
		public String toString() {
			return this.key;
		}
	}
}