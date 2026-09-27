package top.brmc.devlib.model;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.Valid;
import top.brmc.devlib.remain.CompSound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * 存放声音、音量和音调的类
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimpleSound {

	/**
	 * Bukkit 声音值
	 */
	@NonNull
	private Sound sound;

	/**
	 * 音量值
	 */
	private float volume = 1.0F;

	/**
	 * 音调值
	 */
	private float pitch = 1.0F;

	/**
	 * 音调是否随机？
	 */
	private boolean randomPitch = false;

	/**
	 * 此声音是否启用？
	 */
	private boolean enabled = true;

	/**
	 * 创建一个新声音
	 *
	 * @param sound
	 * @param volume
	 * @param pitch
	 */
	public SimpleSound(Sound sound, float volume, float pitch) {
		this(sound, volume, pitch, false, true);
	}

	/**
	 * 创建一个音调随机的新声音
	 *
	 * @param sound
	 * @param volume
	 */
	public SimpleSound(Sound sound, float volume) {
		this(sound, volume, 1.0F, true, true);
	}

	/**
	 * 从原始行创建一个新声音，
	 * 我们接受纯 SOUND 名称，或如下语法：SOUND VOLUME PITCH
	 * 例如：ENTITY_PLAYER_HURT 1.0F 1.0F
	 * <p>
	 * 设置为 'none' 表示禁用
	 *
	 * @param line
	 */
	public SimpleSound(@NonNull String line) {

		if ("none".equals(line)) {
			this.sound = CompSound.UI_BUTTON_CLICK.getSound();
			this.volume = 0.0F;
			this.enabled = false;

			return;
		}

		final String[] values = line.contains(", ") ? line.split(", ") : line.split(" ");
		final CompSound compSound = CompSound.fromName(values[0]);

		Valid.checkNotNull(compSound, "Sound '" + values[0] + "' does not exists (in your Minecraft version " + MinecraftVersion.getFullVersion() + ")! Pick one from mineacademy.org/sounds");
		this.sound = compSound.getSound();

		if (values.length == 1) {
			this.volume = 1F;
			this.pitch = 1.5F;
			return;
		}

		Valid.checkBoolean(values.length == 3, "Malformed sound type, use format: 'sound' OR 'sound volume pitch'. Got: " + line);
		Valid.checkNotNull(this.sound, "Unable to parse sound from: " + line);

		final String volumeRaw = values[1];
		final String pitchRaw = values[2];

		this.volume = Float.parseFloat(volumeRaw);

		if ("random".equals(pitchRaw)) {
			this.pitch = 1.0F;
			this.randomPitch = true;
		}

		else
			this.pitch = Float.parseFloat(pitchRaw);
	}

	/**
	 * 向给定玩家播放该声音
	 *
	 * @param players
	 */
	public void play(Iterable<Player> players) {
		if (this.enabled)
			for (final Player player : players)
				this.play(player);
	}

	/**
	 * 向给定玩家播放该声音
	 *
	 * @param player
	 */
	public void play(Player player) {
		if (this.enabled) {
			Valid.checkNotNull(this.sound);

			try {
				player.playSound(player.getLocation(), this.sound, this.volume, this.getPitch());
			} catch (final NoSuchMethodError err) {
				// Legacy MC
			}
		}
	}

	/**
	 * 在给定位置播放该声音
	 *
	 * @param location
	 */
	public void play(Location location) {
		if (this.enabled) {
			Valid.checkNotNull(this.sound);

			try {
				location.getWorld().playSound(location, this.sound, this.volume, this.getPitch());
			} catch (final NoSuchMethodError err) {
				// Legacy MC
			}
		}
	}

	/**
	 * 若 {@link #isRandomPitch()} 为 true 则返回随机音调，否则返回音调
	 *
	 * @return
	 */
	public float getPitch() {
		return this.randomPitch ? (float) Math.random() : this.pitch;
	}

	/**
	 * 返回序列化后的声音，不支持随机音调
	 */
	@Override
	public String toString() {
		return this.enabled ? this.sound + " " + this.volume + " " + (this.randomPitch ? "random" : this.pitch) : "none";
	}

}