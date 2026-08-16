package org.mineacademy.fo.model;

import org.bukkit.entity.Player;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompToastStyle;
import org.mineacademy.fo.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 表示一条简单的 Toast 提示消息
 */
@RequiredArgsConstructor
public final class ToastMessage implements ConfigSerializable {

	/**
	 * Toast 的材质（图标）
	 */
	private final CompMaterial icon;

	/**
	 * Toast 的样式
	 */
	private final CompToastStyle style;

	/**
	 * 要显示的消息
	 */
	@Getter
	private final String message;

	public ToastMessage(CompToastStyle style, String message) {
		this(CompMaterial.BOOK, style, message);
	}

	public ToastMessage(CompMaterial material, String message) {
		this(material, CompToastStyle.CHALLENGE, message);
	}

	public ToastMessage(String message) {
		this(CompMaterial.BOOK, CompToastStyle.CHALLENGE, message);
	}

	/**
	 * 向给定玩家显示此 Toast 消息
	 *
	 * @param player
	 * @param message 在此替换变量
	 */
	public void displayTo(Player player, String message) {
		Remain.sendToast(player, message, this.icon, this.style);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.icon + " " + this.style + " " + " " + this.message;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"icon", this.icon.toString(),
				"style", this.style.toString(),
				"message", this.message);
	}

	public static ToastMessage deserialize(SerializedMap map) {
		CompMaterial icon = CompMaterial.fromString(map.getString("icon"));
		CompToastStyle style = CompToastStyle.valueOf(map.getString("style"));
		String message = map.getString("message");

		return new ToastMessage(icon, style, message);
	}
}