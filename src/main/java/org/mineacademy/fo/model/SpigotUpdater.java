package org.mineacademy.fo.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.bukkit.Bukkit;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.Setter;

/**
 * 对 Spigot 免费和付费资源执行更新检查的简单类
 */
public class SpigotUpdater extends SimpleRunnable {

	/**
	 * 你的插件在 Spigot 上的资源 ID
	 */
	@Getter
	private final int resourceId;

	/**
	 * 是否自动将新版本下载到 {@link Bukkit#getUpdateFolder()}？
	 */
	private final boolean download;

	/**
	 * 是否发现有可用的新版本？
	 */
	@Getter
	private boolean newVersionAvailable = false;

	/**
	 * 新版本号
	 */
	@Getter
	private String newVersion = "";

	/**
	 * 进服时接收“有新版本”提醒所需的权限。
	 */
	@Getter
	@Setter
	private String permission = "{plugin_name}.notify.update";

	/**
	 * 初始化新实例，只检查更新而不下载
	 *
	 * @param resourceId 插件在 Spigot 页面上的 id
	 */
	public SpigotUpdater(final int resourceId) {
		this(resourceId, false);
	}

	/**
	 * 初始化一个新实例。
	 *
	 * @param resourceId 插件在 Spigot 页面上的 id。
	 * @param download   是否尝试自动下载新版本？
	 *                   请注意：只能从 Spigot 下载免费资源，不能下载付费资源
	 */
	public SpigotUpdater(final int resourceId, final boolean download) {
		this.resourceId = resourceId;
		this.download = download;
	}

	/**
	 * 执行更新检查的主方法。
	 */
	@Override
	public void run() {
		if (this.resourceId == -1)
			return;

		final String currentVersion = SimplePlugin.getVersion();

		if (!this.canUpdateFrom(currentVersion))
			return;

		try {
			HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openConnection();
			connection.setRequestMethod("GET");

			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				final String line = reader.readLine();

				this.newVersion = line;
			}

			if (this.newVersion.isEmpty())
				return;

			if (this.isNewerVersion(currentVersion, this.newVersion) && this.canUpdateTo(this.newVersion)) {
				this.newVersionAvailable = true;

				if (this.download) {
					final ReadableByteChannel channel;

					connection = (HttpURLConnection) new URL("https://api.spiget.org/v2/resources/" + this.resourceId + "/download").openConnection();
					connection.setRequestProperty("User-Agent", SimplePlugin.getNamed());
					Valid.checkBoolean(connection.getResponseCode() == 200, "Downloading update for " + SimplePlugin.getNamed() + " returned " + connection.getResponseCode() + ", aborting.");

					channel = Channels.newChannel(connection.getInputStream());

					final File updateFolder = Bukkit.getUpdateFolderFile();
					FileUtil.createIfNotExists(updateFolder);

					final File destination = new File(updateFolder, SimplePlugin.getNamed() + "-" + this.newVersion + ".jar");
					final FileOutputStream output = new FileOutputStream(destination);

					output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
					output.flush();
					output.close();

					Common.log(this.getDownloadMessage());
				} else
					Common.log(this.getNotifyMessage());
			}

		} catch (final UnknownHostException ex) {
			Common.log("Could not check for update from " + ex.getMessage() + ".");

		} catch (final IOException ex) {
			if (ex.getMessage().startsWith("Server returned HTTP response code: 403")) {
				// no permission
			} else if (ex.getMessage().startsWith("Server returned HTTP response code:"))
				Common.log("Could not check for update, SpigotMC site appears to be down (or unaccessible): " + ex.getMessage());
			else
				Common.error(ex, "IOException performing update from SpigotMC.org check for " + SimplePlugin.getNamed());

		} catch (final Exception ex) {
			Common.error(ex, "Unknown error performing update from SpigotMC.org check for " + SimplePlugin.getNamed());
		}
	}

	/**
	 * 返回当前版本是否适合作为升级的起点。
	 * <p>
	 * 默认情况下，如果版本号包含 SNAPSHOT 或 DEV 则不更新。
	 *
	 * @param currentVersion
	 * @return
	 */
	protected boolean canUpdateFrom(final String currentVersion) {
		return !currentVersion.contains("SNAPSHOT") && !currentVersion.contains("DEV");
	}

	/**
	 * 返回新版本是否适合升级到。
	 * <p>
	 * 默认情况下，如果版本号包含 SNAPSHOT 或 DEV 则不更新。
	 *
	 * @param newVersion
	 * @return
	 */
	protected boolean canUpdateTo(final String newVersion) {
		return !newVersion.contains("SNAPSHOT") && !newVersion.contains("DEV");
	}

	/**
	 * 若远程版本高于当前版本则返回 true
	 *
	 * @param current
	 * @param remote
	 * @return
	 */
	private boolean isNewerVersion(final String current, final String remote) {
		if (remote.contains("-LEGACY"))
			return false;

		String[] currParts = this.removeTagsInNumber(current).split("\\.");
		String[] remoteParts = this.removeTagsInNumber(remote).split("\\.");

		if (currParts.length != remoteParts.length) {
			final boolean olderIsLonger = currParts.length > remoteParts.length;
			final String[] modifiedParts = new String[olderIsLonger ? currParts.length : remoteParts.length];

			for (int i = 0; i < (olderIsLonger ? currParts.length : remoteParts.length); i++)
				modifiedParts[i] = olderIsLonger ? remoteParts.length > i ? remoteParts[i] : "0" : currParts.length > i ? currParts[i] : "0";

			if (olderIsLonger)
				remoteParts = modifiedParts;
			else
				currParts = modifiedParts;
		}

		for (int i = 0; i < currParts.length; i++) {
			if (Integer.parseInt(currParts[i]) > Integer.parseInt(remoteParts[i]))
				return false;

			if (Integer.parseInt(remoteParts[i]) > Integer.parseInt(currParts[i]))
				return true;
		}

		return false;
	}

	/**
	 * 去除当前/新版本号中的某些标签（如 -BETA）：按 "-" 拆分版本号，
	 * 然后返回第一部分
	 *
	 * @param raw
	 * @return
	 */
	protected String removeTagsInNumber(final String raw) {
		return raw.split("\\-")[0];
	}

	/**
	 * 返回更新提示消息，默认为 {@link org.mineacademy.fo.settings.SimpleLocalization.Update#AVAILABLE}
	 * <p>
	 * 如需修改此消息，请修改你的本地化文件，并参考 replaceVariables 方法
	 *
	 * @return
	 */
	public final String getNotifyMessage() {
		return this.replaceVariables(SimpleLocalization.Update.AVAILABLE);
	}

	/**
	 * 返回下载成功消息，默认为 {@link org.mineacademy.fo.settings.SimpleLocalization.Update#DOWNLOADED}
	 * <p>
	 * 如需修改此消息，请修改你的本地化文件，并参考 replaceVariables 方法
	 *
	 * @return
	 */
	public final String getDownloadMessage() {
		return this.replaceVariables(SimpleLocalization.Update.DOWNLOADED);
	}

	/**
	 * 用于替换更新日志/通知/下载完成消息中的变量，
	 * 例如 {new}、{current} 和 {plugin_name}
	 *
	 * @param message
	 * @return
	 */
	protected String replaceVariables(final String message) {
		return message
				.replace("{resource_id}", this.resourceId + "")
				.replace("{plugin_name}", SimplePlugin.getNamed())
				.replace("{new}", this.newVersion)
				.replace("{current}", SimplePlugin.getVersion())
				.replace("{user_id}", "%%__USER__%%");
	}
}
