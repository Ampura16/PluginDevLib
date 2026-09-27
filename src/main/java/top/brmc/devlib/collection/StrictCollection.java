package top.brmc.devlib.collection;

import top.brmc.devlib.SerializeUtil.Mode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 严格集合不允许添加重复元素，
 * 当你试图从 list/map 中移除
 * 不存在的元素时抛错。
 */
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public abstract class StrictCollection {

	/**
	 * 决定此列表的保存方式，JSON 或 YAML 文件。
	 * 用于 {@link #serialize()}。默认为 YAML
	 */
	@Setter
	@Getter
	private Mode mode = Mode.YAML;

	/**
	 * 移除不存在的键时的错误消息
	 */
	private final String cannotRemoveMessage;

	/**
	 * 添加重复键时的错误消息
	 */
	private final String cannotAddMessage;

	/**
	 * 将此对象转换为可安全存入设置文件的内容
	 *
	 * @return
	 */
	public abstract Object serialize();
}
