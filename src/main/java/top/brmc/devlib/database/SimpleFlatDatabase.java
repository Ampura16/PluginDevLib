package top.brmc.devlib.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.brmc.devlib.ChatUtil;
import top.brmc.devlib.Common;
import top.brmc.devlib.MathUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.debug.LagCatcher;
import top.brmc.devlib.settings.SimpleSettings;

import lombok.NonNull;

/**
 * 表示一个简单数据库，其中的值被扁平化并按
 * {@link UUID} 存储。
 * <p>
 * 表结构如下：
 * <p>
 * UUID varchar(64) | Name text       | Data text      | Updated bigint
 * ------------------------------------------------------------
 * 玩家的 uuid      | 最后已知名称    | {json 数据}    | 最后一次调用保存的日期
 * <p>
 * 我们使用 JSON 将这些值扁平化，并提供 onLoad 和 onSave 便捷方法
 * 供你覆盖，让你可以轻松地向 MySQL 保存/加载数据。
 * <p>
 * 另请参见 getExpirationDays()，默认会移除最近 90 天内
 * 未被访问的值。
 * <p>
 * 如需限制更少的方案，请参见 {@link SimpleDatabase}，不过你需要
 * 自行执行查询并实现自己的表结构，这需要具备 MySQL
 * 命令语法知识。
 *
 * @param <T> 用于加载/保存条目的模型，例如你的玩家缓存
 */
public abstract class SimpleFlatDatabase<T> extends SimpleDatabase {

	/**
	 * 内部标记，用于防止死锁，使我们不会在
	 * {@link #load(UUID, Object)} 或 {@link #save(UUID, Object)} 方法中再发起其他查询
	 */
	private boolean isQuerying = false;

	/**
	 * 若表不存在则创建
	 * <p>
	 * 如需覆盖此行为，请覆盖 {@link #onConnectFinish()}
	 */
	@Override
	protected final void onConnected() {

		Valid.checkBoolean(this.hasVariable("table"), "Please call addVariable in the constructor of your " + this);

		// First, see if the database exists, create it if not
		this.update("CREATE TABLE IF NOT EXISTS {table}(UUID varchar(64), Name text, Data text, Updated bigint, PRIMARY KEY (`UUID`))");

		// Remove entries that have not been updated in the last X days
		this.removeOldEntries();

		// Call any hooks
		this.onConnectFinish();
	}

	/**
	 * 你可以覆盖此方法，在连接建立、
	 * 表创建及清理（{@link #removeOldEntries()}）之后运行代码
	 */
	protected void onConnectFinish() {
	}

	/*
	 * Remove entries that have not been updated (called {@link #save(Identifiable)} method) for the
	 * last given X amount of days
	 */
	private void removeOldEntries() {
		final long threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(this.getExpirationDays());

		this.update("DELETE FROM {table} WHERE Updated < " + threshold + "");
	}

	/**
	 * 调用保存方法时，我们会把最后更新时间写入条目。
	 * 插件加载时，可以移除在给定天数内未被保存/更新的
	 * 条目。
	 * <p>
	 * 默认：90 天
	 *
	 * @return
	 */
	protected int getExpirationDays() {
		return 90;
	}

	/**
	 * 加载给定唯一 ID 及其缓存的数据
	 *
	 * @param player
	 * @param cache
	 */
	public final void load(final Player player, final T cache) {
		this.load(player.getUniqueId(), cache, null);
	}

	/**
	 * 加载给定唯一 ID 及其缓存的数据
	 *
	 * @param player
	 * @param cache
	 * @param runAfterLoad 在主线程上同步执行的回调
	 */
	public final void load(final Player player, final T cache, @Nullable Runnable runAfterLoad) {
		this.load(player.getUniqueId(), cache, runAfterLoad);
	}

	/**
	 * 加载给定唯一 ID 及其缓存的数据
	 *
	 * @param uuid
	 * @param cache
	 */
	public final void load(final UUID uuid, final T cache) {
		this.load(uuid, cache, null);
	}

	/**
	 * 异步加载给定唯一 ID 及其缓存的数据。
	 *
	 * @param uuid
	 * @param cache
	 * @param runAfterLoad 在主线程上同步执行的回调
	 */
	public final void load(final UUID uuid, final T cache, @Nullable Runnable runAfterLoad) {
		if (!this.isLoaded() || this.isQuerying)
			return;

		LagCatcher.start("mysql");
		this.isQuerying = true;

		Debugger.debug("mysql", "---------------- MySQL - Loading data for " + uuid);

		Common.runAsync(() -> {

			try {
				final ResultSet resultSet = this.query("SELECT * FROM {table} WHERE UUID='" + uuid + "'");
				final String dataRaw = resultSet.next() ? resultSet.getString("Data") : "{}";
				Debugger.debug("mysql", "JSON: " + dataRaw);

				Common.runLater(() -> {

					try {
						final SerializedMap data = SerializedMap.fromJson(dataRaw);
						Debugger.debug("mysql", "Deserialized data: " + data);

						// Call the user specified load method
						this.onLoad(data, cache);

						// Invoke sync callback when load finish
						if (runAfterLoad != null)
							runAfterLoad.run();

					} catch (final Throwable t) {
						Common.error(t,
								"Failed to parse loaded data from MySQL!",
								"UUID: " + uuid,
								"Raw data: " + dataRaw,
								"Error: %error");

					}
				});

			} catch (final Throwable t) {
				Common.error(t,
						"Failed to load data from MySQL!",
						"UUID: " + uuid,
						"Error: %error");

			} finally {
				this.isQuerying = false;

				this.logPerformance("loading");
			}
		});
	}

	/**
	 * 你用于加载给定唯一 ID 及其缓存数据的方法
	 *
	 * @param map  由数据库中存储的 JSON 数组
	 *             自动转换而来的映射
	 * @param data 你要填充的数据
	 */
	protected abstract void onLoad(SerializedMap map, T data);

	/**
	 * 保存给定名称、唯一 ID 及其缓存的数据
	 * <p>
	 * 如果 onSave 返回空数据，我们会删除该行
	 *
	 * @param player
	 * @param cache
	 */
	public final void save(final Player player, final T cache) {
		this.save(player.getName(), player.getUniqueId(), cache);
	}

	/**
	 * 保存给定名称、唯一 ID 及其缓存的数据
	 * <p>
	 * 如果 onSave 返回空数据，我们会删除该行
	 *
	 * @param name
	 * @param uuid
	 * @param cache
	 */
	public final void save(final String name, final UUID uuid, final T cache) {
		this.save(name, uuid, cache, null);
	}

	/**
	 * 保存给定名称、唯一 ID 及其缓存的数据
	 * <p>
	 * 如果 onSave 返回空数据，我们会删除该行
	 *
	 * @param player
	 * @param cache
	 * @param runAfterSave 保存完成后执行的同步回调
	 */
	public final void save(final Player player, final T cache, @Nullable final Runnable runAfterSave) {
		this.save(player.getName(), player.getUniqueId(), cache, runAfterSave);
	}

	/**
	 * 异步保存给定名称、唯一 ID 及其缓存的数据。
	 *
	 * 如果 onSave 返回空数据，我们会删除该行
	 *
	 * @param name
	 * @param uuid
	 * @param cache
	 * @param runAfterSave 保存完成后执行的同步回调
	 */
	public final void save(final String name, final UUID uuid, final T cache, @Nullable final Runnable runAfterSave) {
		if (!this.isLoaded() || this.isQuerying)
			return;

		LagCatcher.start("mysql");
		this.isQuerying = true;

		// Save using the user configured save method
		final SerializedMap data = this.onSave(cache);

		Debugger.debug("mysql", "---------------- MySQL - Saving data for " + uuid);
		Debugger.debug("mysql", "Raw data: " + data);
		Debugger.debug("mysql", "JSON: " + (data == null ? "null" : data.toJson()));

		Common.runAsync(() -> {

			try {
				// Remove data if empty
				if (data == null || data.isEmpty()) {
					this.update("DELETE FROM {table} WHERE UUID= '" + uuid + "';");

					if (Debugger.isDebugged("mysql"))
						Debugger.debug("mysql", "Data was empty, row has been removed.");

				} else if (this.isStored(uuid))
					this.update("UPDATE {table} SET Data='" + data.toJson() + "', Updated='" + System.currentTimeMillis() + "' WHERE UUID='" + uuid + "';");
				else
					this.update("INSERT INTO {table}(UUID, Name, Data, Updated) VALUES ('" + uuid + "', '" + name + "', '" + data.toJson() + "', '" + System.currentTimeMillis() + "');");

				if (runAfterSave != null)
					Common.runLater(() -> runAfterSave.run());

			} catch (final Throwable ex) {
				Common.error(ex,
						"Failed to save data to MySQL!",
						"UUID: " + uuid,
						"Error: %error");

			} finally {
				this.isQuerying = false;

				this.logPerformance("saving");
			}
		});
	}

	/*
	 * Utility method to finish LagCatcher mysql measure and log
	 * if there was some lag, or if we detected mysql being run
	 * from the main thread.
	 *
	 * @param operation
	 */
	private void logPerformance(final String operation) {
		final boolean isMainThread = Bukkit.isPrimaryThread();

		LagCatcher.end("mysql", isMainThread ? 10 : MathUtil.atLeast(200, SimpleSettings.LAG_THRESHOLD_MILLIS),
				ChatUtil.capitalize(operation) + " data to MySQL took {time} ms" + (isMainThread ? " - To prevent slowing the server, " + operation + " can be made async (carefully)" : ""));
	}

	/*
	 * Checks if the given unique id is stored in the database
	 *
	 * @param uuid
	 * @return
	 * @throws SQLException
	 */
	private boolean isStored(@NonNull final UUID uuid) throws SQLException {
		final ResultSet resultSet = this.query("SELECT * FROM {table} WHERE UUID= '" + uuid.toString() + "'");

		if (resultSet == null)
			return false;

		if (resultSet.next())
			return resultSet.getString("UUID") != null;

		return false;
	}

	/**
	 * 你用于保存给定唯一 ID 及其缓存数据的方法
	 * <p>
	 * 返回空数据表示删除该行
	 *
	 * @param data
	 * @return
	 */
	protected abstract SerializedMap onSave(T data);
}
