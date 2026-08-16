package org.mineacademy.fo.model;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;

/**
 * 从文件中读取规则集的引擎，例如 ChatControl 中的规则。
 * @param <T>
 */
public abstract class RuleSetReader<T extends Rule> {

	/**
	 * 用于创建新规则的匹配关键字，
	 * 例如 ChatControl 中 "match bitch" 操作符里的 "match"
	 */
	private final String newKeyword;

	/**
	 * 为决定新规则创建的匹配关键字
	 * 创建一个新的规则集读取器
	 *
	 * @param newKeyword
	 */
	public RuleSetReader(String newKeyword) {
		this.newKeyword = newKeyword;
	}

	/**
	 * 加载此规则集中的所有条目
	 */
	public abstract void load();

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * 开启/关闭给定规则
	 *
	 * @param rule
	 * @param disabled
	 */
	public final void toggleMessage(Rule rule, boolean disabled) {

		final File file = rule.getFile();
		Valid.checkBoolean(file.exists(), "No such file: " + file + " Rule: " + rule);

		final List<String> lines = FileUtil.readLines(file);
		boolean found = false;

		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);

			// Found our rule
			if (line.equals(this.newKeyword + " " + rule.getUniqueName()))
				found = true;

			// Found something else
			else if (line.startsWith("#") || line.isEmpty() || line.startsWith("match ")) {
				if (found && i > 0 && disabled) {
					lines.add(i, "disabled");

					break;
				}
			}

			// Found the disabled operator
			else if (line.equals("disabled"))
				if (found && !disabled) {
					lines.remove(i);

					break;
				}
		}

		Valid.checkBoolean(found, "Failed to disable rule " + rule);
		this.saveAndLoad(file, lines);
	}

	/**
	 * 用给定的行保存给定文件并重新加载
	 *
	 * @param rule
	 * @param lines
	 */
	protected final void saveAndLoad(File file, List<String> lines) {
		FileUtil.write(file, lines, StandardOpenOption.TRUNCATE_EXISTING);

		this.load();
	}

	/**
	 * 从插件文件夹中给定的文件路径加载规则
	 * 该文件会从你的 jar 中解压出来，因此它必须存在。
	 *
	 * @param path
	 * @return
	 */
	protected final List<T> loadFromFile(String path) {
		final File file = FileUtil.extract(path);

		return this.loadFromFile(file);
	}

	/**
	 * 从给定文件加载规则
	 *
	 * @param file
	 * @return
	 */
	protected final List<T> loadFromFile(File file) {
		final List<T> rules = new ArrayList<>();
		final List<String> lines = FileUtil.readLines(file);

		// The temporary rule being created
		T rule = null;
		String match = null;

		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i).trim();

			if (!line.isEmpty() && !line.startsWith("#"))
				// If a line starts with matcher then assume a new rule is found and start creating it. This makes a new instance of the object.
				if (line.startsWith(this.newKeyword + " ")) {

					// Found another match, assuming previous rule is finished creating.
					if (rule != null)
						if (this.canFinish(rule)) {
							rule.onLoadFinish();

							rules.add(rule);
						}

					try {
						match = line.replace(this.newKeyword + " ", "");
						rule = this.createRule(file, match);

					} catch (final Throwable t) {
						Common.throwError(t,
								"Error creating rule from line (" + (i + 1) + "): " + line,
								"File: " + file,
								"Error: %error",
								"Processing aborted.");

						return rules;
					}
				}

				// If something is being created then attempt to parse operators.
				else {
					if (!this.onNoMatchLineParse(file, line))
						Valid.checkNotNull(match, "Cannot define operator when no rule is being created! File: '" + file + "' Line (" + (i + 1) + "): '" + line + "'");

					if (rule != null)
						try {
							rule.onOperatorParse(line.split(" "));

						} catch (final Throwable t) {
							Common.throwError(t,
									"Error parsing rule operator from line (" + (i + 1) + "): " + line,
									"File: " + file,
									"Error: %error");
						}
				}

			// Reached end of the file and a rule is still being created, finish it.
			if (i + 1 == lines.size() && rule != null && this.canFinish(rule)) {
				rule.onLoadFinish();

				rules.add(rule);
			}
		}

		return rules;
	}

	/**
	 * 当某行没有匹配 {@link #newKeyword} 但有其他内容时调用，
	 * 使你可以注入自己的自定义操作符和设置
	 *
	 * 如果你处理了该行则返回 true，若应抛出错误则返回 false
	 *
	 * @param file 当前文件
	 * @param line
	 * @return
	 */
	protected boolean onNoMatchLineParse(File file, String line) {
		return false;
	}

	/**
	 * 若可以完成给定规则的创建则返回 true
	 *
	 * @param rule
	 * @return
	 */
	protected boolean canFinish(T rule) {
		return true;
	}

	/**
	 * 根据 {@link #newKeyword} 的值创建新规则，
	 * 其中新关键字会从值中去掉。
	 *
	 * 示例：当规则以 "match one two" 开头时，值只有 "one two" 等。
	 *
	 * @param file
	 * @param value
	 *
	 * @return 创建的规则；值无效时返回 null
	 */
	protected abstract T createRule(File file, String value);
}
