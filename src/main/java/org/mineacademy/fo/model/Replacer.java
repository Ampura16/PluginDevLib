package org.mineacademy.fo.model;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.AllArgsConstructor;

/**
 * 一种查找 {variables} 并替换它们的优雅方式。
 */
@AllArgsConstructor
public final class Replacer {

	/**
	 * 以 {@link SerializedMap#ofArray(Object...)} 格式替换所有变量，
	 * 若变量尚未带有 {} 则自动为其添加
	 *
	 * @param list
	 * @param replacements
	 * @return
	 */
	public static List<String> replaceArray(List<String> list, Object... replacements) {
		String joined = String.join("%FLPV%", list);
		joined = replaceArray(joined, replacements);

		return java.util.Arrays.asList(joined.split("%FLPV%"));
	}

	/**
	 * 以 {@link SerializedMap#ofArray(Object...)} 格式替换所有变量，
	 * 若变量尚未带有 {} 则自动为其添加
	 *
	 * @param message
	 * @param replacements
	 * @return
	 */
	public static String replaceArray(String message, Object... replacements) {
		final SerializedMap map = SerializedMap.ofArray(replacements);

		return replaceVariables(message, map);
	}

	/**
	 * 以 {@link SerializedMap#ofArray(Object...)} 格式替换所有变量，
	 * 若变量尚未带有 {} 则自动为其添加
	 *
	 * @param list
	 * @param replacements
	 * @return
	 */
	public static List<String> replaceVariables(List<String> list, SerializedMap replacements) {
		String joined = String.join("%FLPV%", list);
		joined = replaceVariables(joined, replacements);

		return java.util.Arrays.asList(joined.split("%FLPV%"));
	}

	/**
	 * 替换消息中的键值对
	 *
	 * @param message
	 * @param variables
	 * @return
	 */
	public static String replaceVariables(String message, SerializedMap variables) {
		if (message == null)
			return null;

		if ("".equals(message))
			return "";

		message = replaceVariables(message, variables, Variables.VARIABLE_PATTERN.matcher(message));
		message = replaceVariables(message, variables, Variables.BRACKET_VARIABLE_PATTERN.matcher(message));

		return message;
	}

	private static String replaceVariables(String message, SerializedMap variables, Matcher matcher) {
		while (matcher.find()) {
			String variable = matcher.group(1);

			boolean frontSpace = false;
			boolean backSpace = false;

			if (variable.startsWith("+")) {
				variable = variable.substring(1);

				frontSpace = true;
			}

			if (variable.endsWith("+")) {
				variable = variable.substring(0, variable.length() - 1);

				backSpace = true;
			}

			String value = null;

			for (final Map.Entry<String, Object> entry : variables.entrySet()) {
				String variableKey = entry.getKey();

				variableKey = variableKey.startsWith("{") ? variableKey.substring(1) : variableKey;
				variableKey = variableKey.endsWith("}") ? variableKey.substring(0, variableKey.length() - 1) : variableKey;

				if (variableKey.equals(variable))
					value = entry.getValue() == null ? "null" : entry.getValue().toString();
			}

			if (value != null) {
				final boolean emptyColorless = Common.stripColors(value).isEmpty();
				value = value.isEmpty() ? "" : (frontSpace && !emptyColorless ? " " : "") + Common.colorize(value) + (backSpace && !emptyColorless ? " " : "");

				message = message.replace(matcher.group(), value);
			}
		}

		return message;
	}
}
