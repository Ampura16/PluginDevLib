package org.mineacademy.fo.model;

import java.io.File;

import org.mineacademy.fo.Common;

public interface Rule {

	/**
	 * 返回唯一标识此规则的表达式
	 *
	 * @return
	 */
	String getUniqueName();

	/**
	 * 返回此规则所在的文件
	 *
	 * @return
	 */
	File getFile();

	/**
	 * 尝试为正在创建的规则解析来自 {@link RuleSetReader} 的给定行，该行包含一个操作符，例如
	 * "then warn Do not spam please." 等：
	 * 此时 args 为 "then" "warn" "Do" "not" "spam" "please."
	 *
	 * 你可以使用 {@link Common#joinRange(int, String[])} 将消息拼接起来。
	 *
	 * @param rule
	 * @param args
	 *
	 * @return 若操作符解析成功则返回 true
	 */
	boolean onOperatorParse(String[] args);

	/**
	 * 在所有操作符解析完成后调用
	 */
	default void onLoadFinish() {
	}
}
