package top.brmc.devlib.model;

import org.bukkit.entity.Player;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.remain.Remain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 表示一条简单的标题消息
 */
@RequiredArgsConstructor
public final class TitleMessage implements ConfigSerializable {

	/**
	 * 标题
	 */
	@Getter
	private final String titleMessage;

	/**
	 * 标题
	 */
	@Getter
	private final String subtitleMessage;

	/**
	 * fadeIn
	 */
	private final int fadeIn;

	/**
	 * 停留时间
	 */
	private final int stay;

	/**
	 * 停留时间
	 */
	private final int fadeOut;

	/**
	 * 向给定玩家显示此标题消息
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 */
	public void displayTo(Player player, String title, String subtitle) {
		Remain.sendTitle(player, this.fadeIn, this.stay, this.fadeOut, title, subtitle);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.titleMessage + " " + this.subtitleMessage + " " + this.fadeIn + " " + this.stay + " " + this.fadeOut;
	}

	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"title", this.titleMessage,
				"subtitle", this.subtitleMessage,
				"fadeIn", this.fadeIn,
				"stay", this.stay,
				"fadeOut", this.fadeOut);
	}

	/**
	 * 根据给定的 map 创建一条新的标题消息
	 *
	 * @param map
	 * @return
	 */
	public static TitleMessage deserialize(SerializedMap map) {
		String title = map.getString("title");
		String subtitle = map.getString("subtitle");
		int fadeIn = map.getInteger("fadeIn");
		int stay = map.getInteger("stay");
		int fadeOut = map.getInteger("fadeOut");

		return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
	}
}