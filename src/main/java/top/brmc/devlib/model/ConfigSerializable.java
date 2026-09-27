package top.brmc.devlib.model;

import top.brmc.devlib.collection.SerializedMap;

/**
 * <p>实现此接口的类可以从设置文件中存储/加载</p>
 *
 * <p>** 所有类还必须实现以下方法：**</p>
 * <p>public static T deserialize(SerializedMap map)</p>
 */
public interface ConfigSerializable {

	/**
	 * 创建此类的 Map 表示形式，
	 * 可保存到你的 yaml 或 json 设置文件中。
	 *
	 * @return 包含此类当前状态的 Map
	 */
	SerializedMap serialize();
}
