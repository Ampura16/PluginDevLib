package top.brmc.devlib.model;

import org.bukkit.entity.Player;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.remain.CompBarColor;
import top.brmc.devlib.remain.CompBarStyle;
import top.brmc.devlib.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 表示一条简单的 Boss 栏消息
 */
@RequiredArgsConstructor
public final class BossBarMessage implements ConfigSerializable {

	/**
	 * Boss 栏颜色
	 */
	private final CompBarColor color;

	/**
	 * Boss 栏样式
	 */
	private final CompBarStyle style;

	/**
	 * 显示该 Boss 栏的秒数
	 */
	private final int seconds;

	/**
	 * 要显示的消息
	 */
	@Getter
	private final String message;

	/**
	 * 向给定玩家显示此 Boss 栏
	 *
	 * @param player
	 * @param message 在此替换变量
	 */
	public void displayTo(Player player, String message) {
		Remain.sendBossbarTimed(player, message, this.seconds, this.color, this.style);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.color + " " + this.style + " " + this.seconds + " " + this.message;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Color", this.color,
				"Style", this.style,
				"Seconds", this.seconds,
				"Message", this.message);
	}

	public static BossBarMessage deserialize(SerializedMap map) {
		CompBarColor color = CompBarColor.valueOf(map.getString("Color"));
		CompBarStyle style = CompBarStyle.valueOf(map.getString("Style"));
		int seconds = map.getInteger("Seconds");
		String message = map.getString("Message");

		return new BossBarMessage(color, style, seconds, message);
	}
}