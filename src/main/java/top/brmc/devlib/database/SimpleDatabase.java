package top.brmc.devlib.database;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.brmc.devlib.Common;
import top.brmc.devlib.FileUtil;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.SerializeUtil;
import top.brmc.devlib.SerializeUtil.Mode;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.collection.StrictMap;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.model.ConfigSerializable;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.Remain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 表示一个简单的 MySQL 数据库
 * <p>
 * 执行查询之前，请确保已调用 connect() 方法。
 * <p>
 * 你也可以覆盖 onConnected()，在连接建立后
 * 运行你的代码。
 * <p>
 * 要使用此类，你必须了解 MySQL 命令语法！
 */
public class SimpleDatabase {

	/**
	 * 是否使用更现代的 HikariCP 连接器（如果可用）？
	 */
	@Getter
	@Setter
	private static boolean connectUsingHikari = true;

	/**
	 * 已建立的连接，没有则为 null
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private Connection connection;

	/**
	 * 可在 SQL 中通过 {} 语法使用的变量映射
	 */
	private final StrictMap<String, String> sqlVariables = new StrictMap<>();

	/**
	 * connect 函数最近一次使用的凭据，从未调用过则为 null
	 */
	private LastCredentials lastCredentials;

	/**
	 * 私有标记，表示当前正在连接数据库
	 */
	private boolean connecting = false;

	/*
	 * Optional Hikari data source (you plugin needs to include com.zaxxer.HikariCP library in its plugin.yml (MC 1.16+ required)
	 */
	private Object hikariDataSource;

	/*
	 * Is this a SQLite connection?
	 */
	private boolean isSQLite = false;

	// --------------------------------------------------------------------
	// Connecting
	// --------------------------------------------------------------------

	/**
	 * 尝试建立新的数据库连接
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 */
	public final void connect(final String host, final int port, final String database, final String user, final String password) {
		this.connect(host, port, database, user, password, null);
	}

	/**
	 * 尝试建立新的数据库连接，
	 * 之后可以在 SQL 中使用 {table}，它会被替换为你的表名
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 * @param table
	 */
	public final void connect(final String host, final int port, final String database, final String user, final String password, final String table) {
		this.connect(host, port, database, user, password, table, true);
	}

	/**
	 * 尝试建立新的数据库连接，
	 * 之后可以在 SQL 中使用 {table}，它会被替换为你的表名
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 * @param table
	 * @param autoReconnect
	 */
	public final void connect(final String host, final int port, final String database, final String user, final String password, final String table, final boolean autoReconnect) {
		this.connect("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&autoReconnect=" + autoReconnect, user, password, table);
	}

	/**
	 * 连接到数据库。
	 *
	 * 警告：要求数据库类型既不需要用户名也不需要密码！
	 *
	 * @param url
	 */
	public final void connect(final String url) {
		this.connect(url, null, null);
	}

	/**
	 * 连接到数据库
	 *
	 * @param url
	 * @param user
	 * @param password
	 */
	public final void connect(final String url, final String user, final String password) {
		this.connect(url, user, password, null);
	}

	/**
	 * 连接到数据库，
	 * 之后可以在 SQL 中使用 {table}，它会被替换为你的表名*
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @param table
	 */
	public final void connect(final String url, final String user, final String password, final String table) {
		try {
			this.connecting = true;
			final boolean librariesWontWork = Remain.getJavaVersion() >= 15 && MinecraftVersion.olderThan(V.v1_16);
			final String prefixMessage = librariesWontWork ? "" : " You might need to use Java 8 or update your Minecraft to 1.16 or higher because using legacy Minecraft with new Java is limited.";

			if (url.startsWith("jdbc:sqlite")) {

				if (!ReflectionUtil.isClassAvailable("org.sqlite.JDBC"))
					throw new FoException("SQLite driver is not available. Alert plugin author to add org.xerial:sqlite-jdbc onto the plugin's library in plugin.yml" + prefixMessage);

				Class.forName("org.sqlite.JDBC");

				final String urlHeadless = url.replace("jdbc:sqlite://", "");

				if (urlHeadless.split("\\.").length == 2 && !urlHeadless.contains("\\") && !urlHeadless.contains("/")) {
					final String path = FileUtil.getFile(urlHeadless).getPath();

					this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
				} else
					this.connection = DriverManager.getConnection(url);

				this.isSQLite = true;
			}

			else if (connectUsingHikari) {

				if (!ReflectionUtil.isClassAvailable("com.zaxxer.hikari.HikariConfig"))
					throw new FoException("Hikari driver is not available. " + (MinecraftVersion.olderThan(V.v1_16) && Remain.getJavaVersion() >= 9
							? "Download LibraryHelper plugin: https://mineacademy.org/libraryhelper to use Hikari."
							: "Alert plugin author to add com.zaxxer:HikariCP onto the plugin's library in plugin.yml" + prefixMessage));

				final Object hikariConfig = ReflectionUtil.instantiate("com.zaxxer.hikari.HikariConfig");

				if (url.startsWith("jdbc:mysql://")) {

					if (!ReflectionUtil.isClassAvailable("com.mysql.cj.jdbc.Driver") && !ReflectionUtil.isClassAvailable("com.mysql.jdbc.Driver"))
						throw new FoException("MySQL driver is not available. Alert plugin author to add com.mysql:mysql-connector-j onto the plugin's library in plugin.yml" + prefixMessage);

					try {
						ReflectionUtil.invoke("setDriverClassName", hikariConfig, "com.mysql.cj.jdbc.Driver");

					} catch (final Throwable t) {

						// Fall back to legacy driver
						ReflectionUtil.invoke("setDriverClassName", hikariConfig, "com.mysql.jdbc.Driver");
					}

				} else if (url.startsWith("jdbc:mariadb://")) {

					if (!ReflectionUtil.isClassAvailable("org.mariadb.jdbc.Driver"))
						throw new FoException("MariaDB driver is not available. Alert plugin author to add org.mariadb.jdbc:mariadb-java-client onto the plugin's library in plugin.yml" + prefixMessage);

					ReflectionUtil.invoke("setDriverClassName", hikariConfig, "org.mariadb.jdbc.Driver");

				} else
					throw new FoException("Unknown database driver, expected jdbc:mysql or jdbc:mariadb, got: " + url);

				ReflectionUtil.invoke("setJdbcUrl", hikariConfig, url);

				if (user != null)
					ReflectionUtil.invoke("setUsername", hikariConfig, user);

				if (password != null)
					ReflectionUtil.invoke("setPassword", hikariConfig, password);

				final Constructor<?> dataSourceConst = ReflectionUtil.getConstructor("com.zaxxer.hikari.HikariDataSource", hikariConfig.getClass());
				final Object hikariSource = ReflectionUtil.instantiate(dataSourceConst, hikariConfig);

				this.hikariDataSource = hikariSource;

				final Method getConnection = hikariSource.getClass().getDeclaredMethod("getConnection");

				try {
					this.connection = ReflectionUtil.invoke(getConnection, hikariSource);

				} catch (final Throwable t) {
					Common.warning("Could not get HikariCP connection, please report this with the information below to github.com/kangarko/foundation");
					Common.warning("Method: " + getConnection);
					Common.warning("Arguments: " + Common.join(getConnection.getParameters()));

					t.printStackTrace();
				}
			}

			/*
			 * Check for JDBC Drivers (MariaDB, MySQL or Legacy MySQL)
			 */
			else {
				if (url.startsWith("jdbc:mariadb://")) {

					if (!ReflectionUtil.isClassAvailable("org.mariadb.jdbc.Driver"))
						throw new FoException("MariaDB driver is not available. Alert plugin author to add org.mariadb.jdbc:mariadb-java-client onto the plugin's library in plugin.yml" + prefixMessage);

					Class.forName("org.mariadb.jdbc.Driver");

				} else if (url.startsWith("jdbc:mysql://")) {

					if (!ReflectionUtil.isClassAvailable("com.mysql.cj.jdbc.Driver") && !ReflectionUtil.isClassAvailable("com.mysql.jdbc.Driver"))
						throw new FoException("MySQL driver is not available. Alert plugin author to add com.mysql:mysql-connector-j onto the plugin's library in plugin.yml" + prefixMessage);

					try {
						Class.forName("com.mysql.cj.jdbc.Driver");

					} catch (final ClassNotFoundException ex) {
						Class.forName("com.mysql.jdbc.Driver");
					}
				}

				this.connection = user != null && password != null ? DriverManager.getConnection(url, user, password) : DriverManager.getConnection(url);
			}

			this.lastCredentials = new LastCredentials(url, user, password, table);
			this.onConnected();

		} catch (final Exception ex) {

			if (Common.getOrEmpty(ex.getMessage()).contains("No suitable driver found"))
				Common.logFramed(
						"Failed to look up database driver! If you had database disabled,",
						"then enable it and reload - this is expected.",
						"",
						"You have have access to your server machine, try installing",
						"https://mariadb.com/downloads/connectors/connectors-data-access/",
						"",
						"If this problem persists after a restart, please contact",
						"your hosting provider with the error message below.");
			else
				Common.logFramed(
						"Failed to connect to database",
						"URL: " + url,
						"Error: " + ex.getMessage());

			Remain.sneaky(ex);

		} finally {
			this.connecting = false;
		}
	}

	/**
	 * 尝试使用最近一次已知的凭据进行连接。若未提供凭据
	 * （即从未调用过 connect 函数），则安全地失败
	 */
	protected final void connectUsingLastCredentials() {
		if (this.lastCredentials != null)
			this.connect(this.lastCredentials.url, this.lastCredentials.user, this.lastCredentials.password, this.lastCredentials.table);
	}

	/**
	 * 首次建立连接后自动调用
	 */
	protected void onConnected() {
	}

	// --------------------------------------------------------------------
	// Disconnecting
	// --------------------------------------------------------------------

	/**
	 * 尝试关闭结果集（若尚未关闭）
	 *
	 * @param resultSet
	 */
	public final void close(final ResultSet resultSet) {
		try {
			if (!resultSet.isClosed())
				resultSet.close();

		} catch (final SQLException e) {
			Common.error(e, "Error closing database result set!");
		}
	}

	/**
	 * 尝试关闭连接（若不为 null）
	 */
	public final void close() {
		try {
			if (this.connection != null)
				this.connection.close();

			if (this.hikariDataSource != null)
				ReflectionUtil.invoke("close", this.hikariDataSource);

		} catch (final SQLException e) {
			Common.error(e, "Error closing database connection!");
		}
	}

	// --------------------------------------------------------------------
	// Querying
	// --------------------------------------------------------------------

	/**
	 * 创建数据库表，在 onConnected 中使用
	 *
	 * @param creator
	 */
	protected final void createTable(final TableCreator creator) {
		synchronized (this.connection) {
			String columns = "";

			for (final TableRow column : creator.getColumns()) {
				String dataType = column.getDataType().toLowerCase();

				if (this.isSQLite) {
					if (dataType.equals("datetime") || dataType.equals("longtext"))
						dataType = "text";

					else if (dataType.startsWith("varchar"))
						dataType = "text";

					else if (dataType.startsWith("bigint"))
						dataType = "integer";

					else if (creator.getPrimaryColumn() != null && creator.getPrimaryColumn().equals(column.getName()))
						dataType = "INTEGER PRIMARY KEY";
				}

				columns += (columns.isEmpty() ? "" : ", ") + "`" + column.getName() + "` " + dataType;

				if (column.getAutoIncrement() != null && column.getAutoIncrement())
					if (this.isSQLite)
						columns += " AUTOINCREMENT";

					else
						columns += " NOT NULL AUTO_INCREMENT";

				else if (column.getNotNull() != null && column.getNotNull())
					columns += " NOT NULL";

				if (column.getDefaultValue() != null)
					columns += " DEFAULT " + column.getDefaultValue();
			}

			if (creator.getPrimaryColumn() != null && !this.isSQLite)
				columns += ", PRIMARY KEY (`" + creator.getPrimaryColumn() + "`)";

			try {
				this.update("CREATE TABLE IF NOT EXISTS `" + creator.getName() + "` (" + columns + ") " + (this.isSQLite ? "" : "DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci") + ";");

			} catch (final Throwable t) {
				if (t.toString().contains("Unknown collation")) {
					Common.log("You need to update your database driver to support utf8mb4_unicode_520_ci collation. We switched to support unicode using 4 bits length because the previous system only supported 3 bits.");
					Common.log("Some characters such as smiley or Chinese are stored in 4 bits so they would crash the 3-bit database leading to more problems. Most hosting providers have now widely adopted the utf8mb4_unicode_520_ci encoding you seem lacking. Disable database connection or update your driver to fix this.");
				}

				else
					throw t;
			}
		}
	}

	/**
	 * 将给定的列-值对插入 {@link #getTable()}
	 *
	 * @param columsAndValues
	 */
	protected final void insert(@NonNull final SerializedMap columsAndValues) {
		this.insert("{table}", columsAndValues);
	}

	/**
	 * 将给定的可序列化对象以列-值对形式插入给定表
	 *
	 * @param <T>
	 * @param table
	 * @param serializableObject
	 */
	protected final <T extends ConfigSerializable> void insert(final String table, @NonNull final T serializableObject) {
		this.insert(table, serializableObject.serialize());
	}

	/**
	 * 将给定的列-值对插入给定表
	 *
	 * @param table
	 * @param columnsAndValues
	 */
	protected final void insert(final String table, @NonNull final SerializedMap columnsAndValues) {
		synchronized (this.connection) {
			final String columns = Common.join(columnsAndValues.keySet());
			final String values = Common.join(columnsAndValues.values(), ", ", value -> value == null || value.equals("NULL") ? "NULL" : (value instanceof Number ? String.valueOf(value) : "'" + value + "'"));
			final String duplicateUpdate = Common.join(columnsAndValues.entrySet(), ", ", entry -> entry.getKey() + "=VALUES(" + entry.getKey() + ")");

			this.update("INSERT INTO " + this.replaceVariables(table) + " (" + columns + ") VALUES (" + values + ")" + (this.isSQLite ? "" : " ON DUPLICATE KEY UPDATE " + duplicateUpdate + ";"));
		}
	}

	/**
	 * 将批量映射插入 {@link #getTable()}
	 *
	 * @param maps
	 */
	protected final void insertBatch(@NonNull final List<SerializedMap> maps) {
		this.insertBatch("{table}", maps);
	}

	/**
	 * 将批量映射插入数据库
	 *
	 * @param table
	 * @param maps
	 */
	protected final void insertBatch(final String table, @NonNull final List<SerializedMap> maps) {
		synchronized (this.connection) {
			final List<String> sqls = new ArrayList<>();

			for (final SerializedMap map : maps)
				try {
					final String columns = Common.join(map.keySet());
					final String values = Common.join(map.values(), ", ", this::parseValue);
					final String duplicateUpdate = Common.join(map.entrySet(), ", ", entry -> entry.getKey() + "=VALUES(" + entry.getKey() + ")");

					final String sql = "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ")" + (this.isSQLite ? "" : " ON DUPLICATE KEY UPDATE " + duplicateUpdate + ";");
					Debugger.debug("mysql", "Inserting batch SQL: " + sql);

					sqls.add(sql);

				} catch (final Throwable t) {
					Common.error(t, "Error inserting batch map: " + map);
				}

			this.batchUpdate(sqls);
		}
	}

	/*
	 * A helper method to insert compatible value to db
	 */
	private final String parseValue(final Object value) {
		final Object serialized = SerializeUtil.serialize(this.getTableMode(), value);

		return value == null || value.equals("NULL") ? "NULL" : "'" + serialized.toString() + "'";
	}

	/**
	 * 尝试执行新的更新查询
	 * <p>
	 * 请确保先调用 connect()，否则会抛出错误
	 *
	 * @param sql
	 */
	protected final void update(String sql) {
		if (!this.connecting)
			Valid.checkAsync("Updating database must be done async! Call: " + sql);

		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);
			Valid.checkBoolean(!sql.contains("{table}"), "Table not set! Either use connect() method that specifies it or call addVariable(table, 'yourtablename') in your constructor!");

			Debugger.debug("mysql", "Updating database with: " + sql);

			try (Statement statement = this.connection.createStatement()) {
				statement.executeUpdate(sql);

			} catch (final SQLException e) {
				this.handleError(e, "Error on updating database with: " + sql);
			}
		}
	}

	/**
	 * 列出给定表中的所有行
	 *
	 * @param table
	 * @param consumer
	 */
	protected final void selectAll(final String table, final ResultReader consumer) {
		this.select(table, (String) null, consumer);
	}

	/**
	 * 列出给定表中符合给定 where 子句的所有行。用法示例：
	 *
	 * select(table, "PlayerUid = " + player.getUniqueId(), resultSet);
	 *
	 * 别忘了在 consumer 中用完后关闭连接。
	 *
	 * @param table
	 * @param where
	 * @param consumer
	 */
	protected final void select(final String table, @Nullable final String where, final ResultReader consumer) {
		synchronized (this.connection) {
			if (!this.isLoaded())
				return;

			final String tableName = this.replaceVariables(table);

			try (ResultSet resultSet = this.query("SELECT * FROM " + table + (where == null ? "" : " WHERE " + where))) {
				while (resultSet.next())
					try {
						consumer.accept(new SimpleResultSet(tableName, resultSet));

					} catch (final InvalidRowException ex) {
						// Pardoned

					} catch (final Throwable t) {
						Common.log("Error reading a row from table " + tableName + " where " + (where == null ? "all" : where) + ", aborting...");

						t.printStackTrace();
						break;
					}

			} catch (final Throwable t) {
				Common.error(t, "Error selecting rows from table " + table + " where " + (where == null ? "all" : where));
			}
		}
	}

	/**
	 * 列出给定表中符合给定 where 条件的所有行。用法示例：
	 *
	 * Map<String, Object> conditions = new HashMap<>();
	 *
	 * conditions.put("name", "John");
	 * conditions.put("age", 30);
	 * conditions.put("city", "%New York%");
	 *
	 * 别忘了在 consumer 中用完后关闭连接。
	 *
	 * @param table
	 * @param where
	 * @param consumer
	 */
	protected final void select(final String table, @Nullable final Map<String, Object> where, final ResultReader consumer) {
		synchronized (this.connection) {
			if (!this.isLoaded())
				return;

			final String tableName = this.replaceVariables(table);

			try (ResultSet resultSet = this.query("SELECT * FROM " + table + " " + buildWhere(where))) {
				while (resultSet.next())
					try {
						consumer.accept(new SimpleResultSet(tableName, resultSet));

					} catch (final InvalidRowException ex) {
						// Pardoned

					} catch (final Throwable t) {
						Common.log("Error reading a row from table " + tableName + " where " + (where == null ? "all" : where) + ", aborting...");

						t.printStackTrace();
						break;
					}

			} catch (final Throwable t) {
				Common.error(t, "Error selecting rows from table " + table + " where " + (where == null ? "all" : where));
			}
		}
	}

	private static String buildWhere(Map<String, Object> conditions) {
		if (conditions == null || conditions.isEmpty())
			return "";

		final List<String> clauses = new ArrayList<>();

		conditions.forEach((key, value) -> {
			String clause;

			if (value instanceof String)
				clause = String.format("%s = '%s'", key, value);

			else
				clause = String.format("%s = %s", key, value);

			clauses.add(clause);
		});

		return "WHERE " + String.join(" AND ", clauses);
	}

	/**
	 * 按键-值条件返回给定表中的行数。
	 *
	 * 条件示例：count("MyTable", "Player", "kangarko, "Status", "PENDING")
	 * 此示例会返回 Player 列等于 kangarko 且 Status 列等于 PENDING 的所有行。
	 *
	 * @param table
	 * @param array
	 * @return
	 */
	protected final int count(final String table, final Object... array) {
		return this.count(table, SerializedMap.ofArray(array));
	}

	/**
	 * 按条件返回给定表中的行数。
	 *
	 * 条件示例：SerializedMap.ofArray("Player", "kangarko, "Status", "PENDING")
	 * 此示例会返回 Player 列等于 kangarko 且 Status 列等于 PENDING 的所有行。
	 *
	 * @param table
	 * @param conditions
	 * @return
	 */
	protected final int count(final String table, final SerializedMap conditions) {
		synchronized (this.connection) {
			// Convert conditions into SQL syntax
			final Set<String> conditionsList = Common.convertSet(conditions.entrySet(), entry -> entry.getKey() + " = '" + SerializeUtil.serialize(this.getTableMode(), entry.getValue()) + "'");

			// Run the query
			final String sql = "SELECT * FROM " + table + (conditionsList.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditionsList)) + ";";

			try (ResultSet resultSet = this.query(sql)) {
				int count = 0;

				while (resultSet.next())
					count++;

				return count;

			} catch (final SQLException ex) {
				Common.throwError(ex,
						"Unable to count rows!",
						"Table: " + this.replaceVariables(table),
						"Conditions: " + conditions,
						"Query: " + sql);
			}

			return 0;
		}
	}

	/**
	 * 尝试执行新的查询
	 * <p>
	 * 请确保先调用 connect()，否则会抛出错误
	 *
	 * @param sql
	 * @return
	 */
	protected final ResultSet query(String sql) {
		Valid.checkAsync("Sending database query must be called async, command: " + sql);

		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);

			Debugger.debug("mysql", "Querying database with: " + sql);

			try {
				final Statement statement = this.connection.createStatement();
				final ResultSet resultSet = statement.executeQuery(sql);

				return resultSet;

			} catch (final SQLException ex) {
				if (ex instanceof SQLSyntaxErrorException && ex.getMessage().startsWith("Table") && ex.getMessage().endsWith("doesn't exist"))
					return new DummyResultSet();

				this.handleError(ex, "Error on querying database with: " + sql);
			}

			return null;
		}
	}

	/**
	 * 执行大批量更新
	 *
	 * @param sqls
	 */
	protected final void batchUpdate(@NonNull final List<String> sqls) {
		if (sqls.isEmpty())
			return;

		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			try (Statement batchStatement = this.getConnection().createStatement(this.isSQLite ? ResultSet.TYPE_FORWARD_ONLY : ResultSet.TYPE_SCROLL_SENSITIVE, this.isSQLite ? ResultSet.CONCUR_READ_ONLY : ResultSet.CONCUR_UPDATABLE)) {
				final int processedCount = sqls.size();

				for (final String sql : sqls)
					batchStatement.addBatch(this.replaceVariables(sql));

				if (processedCount > 10_000)
					Common.log("Updating your database (" + processedCount + " entries)... PLEASE BE PATIENT THIS WILL TAKE "
							+ (processedCount > 50_000 ? "10-20 MINUTES" : "5-10 MINUTES") + " - If server will print a crash report, ignore it, update will proceed.");

				// Prevent automatically sending db instructions
				this.getConnection().setAutoCommit(false);

				try {
					// Execute
					batchStatement.executeBatch();

					// This will block the thread
					this.getConnection().commit();

				} catch (final Throwable t) {
					final List<String> errorMessage = new ArrayList<>();

					errorMessage.add("Error executing a batch update with " + sqls.size() + " SQLs:");

					for (final String sql : sqls)
						errorMessage.add(sql);

					Common.error(t, Common.toArray(errorMessage));

					// Cancel the task but handle the error upstream
					throw t;
				}

			} catch (final Throwable t) {
				t.printStackTrace();

			} finally {
				try {
					this.getConnection().setAutoCommit(true);

				} catch (final SQLException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * 尝试返回预编译语句
	 * <p>
	 * 请确保先调用 connect()，否则会抛出错误
	 *
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	protected final java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);

			Debugger.debug("mysql", "Preparing statement: " + sql);
			return this.connection.prepareStatement(sql);
		}
	}

	/**
	 * 尝试返回预编译语句
	 * <p>
	 * 请确保先调用 connect()，否则会抛出错误
	 *
	 * @param sql
	 * @param type
	 * @param concurrency
	 *
	 * @return
	 * @throws SQLException
	 */
	protected final java.sql.PreparedStatement prepareStatement(String sql, final int type, final int concurrency) throws SQLException {
		synchronized (this.connection) {
			this.checkEstablished();

			if (!this.isConnected())
				this.connectUsingLastCredentials();

			sql = this.replaceVariables(sql);

			Debugger.debug("mysql", "Preparing statement: " + sql);
			return this.connection.prepareStatement(sql, type, concurrency);
		}
	}

	/**
	 * 连接是否已建立、处于打开状态且有效？
	 * 会向数据库发起阻塞式 ping 请求
	 *
	 * @return 是否已设置连接驱动
	 */
	protected final boolean isConnected() {
		if (!this.isLoaded())
			return false;

		try {
			if (!this.connection.isValid(0))
				return false;
		} catch (SQLException | AbstractMethodError err) {
			// Pass through silently
		}

		try {
			return !this.connection.isClosed();

		} catch (final SQLException ex) {
			return false;
		}
	}

	/*
	 * Checks if there's a collation-related error and prints warning message for the user to
	 * update his database.
	 */
	private void handleError(final Throwable t, final String fallbackMessage) {
		if (t.toString().contains("Unknown collation")) {
			Common.log("You need to update your database provider driver. We switched to support unicode using 4 bits length because the previous system only supported 3 bits.");
			Common.log("Some characters such as smiley or Chinese are stored in 4 bits so they would crash the 3-bit database leading to more problems. Most hosting providers have now widely adopted the utf8mb4_unicode_520_ci encoding you seem lacking. Disable database connection or update your driver to fix this.");
		}

		else if (t.toString().contains("Incorrect string value")) {
			Common.log("Attempted to save unicode letters (e.g. coors) to your database with invalid encoding, see https://stackoverflow.com/a/10959780 and adjust it. MariaDB may cause issues, use MySQL 8.0 for best results.");

			t.printStackTrace();

		} else
			Common.throwError(t, fallbackMessage);
	}

	// --------------------------------------------------------------------
	// Non-blocking checking
	// --------------------------------------------------------------------

	/**
	 * 返回开发者是否足够早地调用了 {@link #addVariable(String, String)}，
	 * 使变量得以注册
	 *
	 * @param key
	 * @return
	 */
	final boolean hasVariable(final String key) {
		return this.sqlVariables.containsKey(key);
	}

	/**
	 * 返回最近一次连接的表，若从未连接则抛出错误
	 *
	 * @return
	 */
	protected final String getTable() {
		this.checkEstablished();

		return Common.getOrEmpty(this.lastCredentials.table);
	}

	/**
	 * 检查是否调用过 connect() 函数
	 */
	private final void checkEstablished() {
		Valid.checkBoolean(this.isLoaded(), "Connection was never established, did you call connect() on " + this + "? Use isLoaded() to check.");
	}

	/**
	 * 若调用过 connect 函数从而已加载驱动，则返回 true
	 *
	 * @return
	 */
	public final boolean isLoaded() {
		return this.connection != null;
	}

	// --------------------------------------------------------------------
	// Variables
	// --------------------------------------------------------------------

	/**
	 * 添加一个可在查询中使用的新变量。
	 * 变量名会自动加上 {} 括号。
	 *
	 * @param name
	 * @param value
	 */
	protected final void addVariable(final String name, final String value) {
		this.sqlVariables.put(name, value);
	}

	/**
	 * 替换 sql 查询中的 {table} 和 {@link #sqlVariables}
	 *
	 * @param sql
	 * @return
	 */
	protected final String replaceVariables(String sql) {

		for (final Entry<String, String> entry : this.sqlVariables.entrySet())
			sql = sql.replace("{" + entry.getKey() + "}", entry.getValue());

		return sql.replace("{table}", this.getTable());
	}

	/**
	 * 获取默认的序列化模式
	 *
	 * @return
	 */
	protected Mode getTableMode() {
		return SerializeUtil.Mode.YAML;
	}

	/**
	 * 返回数据库是否为 SQLite
	 *
	 * @return
	 */
	protected final boolean isSQLite() {
		return this.isSQLite;
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	/**
	 * 帮助创建新的数据库表，避免 SQL 语法错误
	 */
	@Getter
	@RequiredArgsConstructor
	public final static class TableCreator {

		/**
		 * 表名
		 */
		private final String name;

		/**
		 * 表的列
		 */
		private final List<TableRow> columns = new ArrayList<>();

		/**
		 * 主键列
		 */
		private String primaryColumn;

		/**
		 * 添加一个给定名称和数据类型的新列
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator add(final String name, final String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).build());

			return this;
		}

		/**
		 * 添加一个给定名称和数据类型、且为 "NOT NULL" 的新列
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator addNotNull(final String name, final String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).notNull(true).build());

			return this;
		}

		/**
		 * 添加一个给定名称和数据类型、且为 "NOT NULL AUTO_INCREMENT" 的新列
		 *
		 * @param name
		 * @param dataType
		 * @return
		 */
		public TableCreator addAutoIncrement(final String name, final String dataType) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).autoIncrement(true).build());

			return this;
		}

		/**
		 * 添加一个给定名称和数据类型、且带默认值的新列
		 *
		 * @param name
		 * @param dataType
		 * @param def
		 * @return
		 */
		public TableCreator addDefault(final String name, final String dataType, final String def) {
			this.columns.add(TableRow.builder().name(name).dataType(dataType).defaultValue(def).build());

			return this;
		}

		/**
		 * 标记哪一列为主键
		 *
		 * @param primaryColumn
		 * @return
		 */
		public TableCreator setPrimaryColumn(final String primaryColumn) {
			this.primaryColumn = primaryColumn;

			return this;
		}

		/**
		 * 创建新表
		 *
		 * @param name
		 * @return
		 */
		public static TableCreator of(final String name) {
			return new TableCreator(name);
		}
	}

	/*
	 * Internal helper to create table rows
	 */
	@Data
	@Builder
	private final static class TableRow {

		/**
		 * 表的行名
		 */
		private final String name;

		/**
		 * 数据类型
		 */
		private final String dataType;

		/**
		 * 此行是否为 NOT NULL？
		 */
		private final Boolean notNull;

		/**
		 * 此行是否有默认值？
		 */
		private final String defaultValue;

		/**
		 * 此行是否为 NOT NULL AUTO_INCREMENT？
		 */
		private final Boolean autoIncrement;
	}

	/**
	 * 用于读取结果集的辅助类。（我们不能使用简单的 Consumer，因为它不会
	 * 自动捕获异常。）
	 */
	protected interface ResultReader {

		/**
		 * 读取并处理给定的结果集，我们会替你处理异常
		 *
		 * @param set
		 * @throws SQLException
		 */
		void accept(SimpleResultSet set) throws SQLException;
	}

	private static class InvalidRowException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public final static class SimpleResultSet {

		private final String tableName;
		private final ResultSet delegate;

		public boolean next() throws SQLException {
			return delegate.next();
		}

		public void close() throws SQLException {
			delegate.close();
		}

		public String getString(int columnIndex) throws SQLException {
			return Common.getOrEmpty(delegate.getString(columnIndex));
		}

		public boolean getBoolean(int columnIndex) throws SQLException {
			return delegate.getBoolean(columnIndex);
		}

		public byte getByte(int columnIndex) throws SQLException {
			return delegate.getByte(columnIndex);
		}

		public short getShort(int columnIndex) throws SQLException {
			return delegate.getShort(columnIndex);
		}

		public int getInt(int columnIndex) throws SQLException {
			return delegate.getInt(columnIndex);
		}

		public long getLong(int columnIndex) throws SQLException {
			return delegate.getLong(columnIndex);
		}

		public float getFloat(int columnIndex) throws SQLException {
			return delegate.getFloat(columnIndex);
		}

		public double getDouble(int columnIndex) throws SQLException {
			return delegate.getDouble(columnIndex);
		}

		public Date getDate(int columnIndex) throws SQLException {
			return delegate.getDate(columnIndex);
		}

		public Time getTime(int columnIndex) throws SQLException {
			return delegate.getTime(columnIndex);
		}

		public Timestamp getTimestamp(int columnIndex) throws SQLException {
			return delegate.getTimestamp(columnIndex);
		}

		public Object getObject(int columnIndex) throws SQLException {
			return delegate.getObject(columnIndex);
		}

		public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
			return delegate.getObject(columnIndex, type);
		}

		public String getString(String columnLabel) throws SQLException {
			return Common.getOrEmpty(delegate.getString(columnLabel));
		}

		public String getStringStrict(String columnLabel) throws SQLException {
			final String value = this.getString(columnLabel);

			if (value == null || "".equals(value)) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}

			return value;
		}

		public int[] getLocationArrayStrict(String columnLabel) throws SQLException {
			final String value = this.getString(columnLabel);

			if (value == null || "".equals(value)) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}

			final String[] split = value.split(" ");

			if (split.length != 3) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid location value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}

			return new int[] {
					Integer.parseInt(split[0]),
					Integer.parseInt(split[1]),
					Integer.parseInt(split[2])
			};
		}

		public ItemStack[] getItemArray(String columnLabel) throws SQLException {
			final String value = this.getString(columnLabel);

			if (value == null || "".equals(value))
				return new ItemStack[0];

			return getItemArrayStrict(columnLabel);
		}

		public ItemStack[] getItemArrayStrict(String columnLabel) throws SQLException {
			final String value = this.getString(columnLabel);

			if (value == null || "".equals(value)) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}

			return SerializeUtil.deserialize(Mode.JSON, ItemStack[].class, value);
		}

		public <T extends Enum<T>> T getEnum(String columnLabel, Class<T> typeOf) throws SQLException {
			final String value = this.getString(columnLabel);

			if (value != null && !"".equals(value)) {
				final T enumValue = ReflectionUtil.lookupEnumSilent(typeOf, value);

				if (enumValue == null) {
					Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid " + typeOf.getSimpleName() + " enum value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring. Valid values: " + Common.join(typeOf.getEnumConstants(), ", "));

					throw new InvalidRowException();
				}

				return enumValue;
			}

			return null;
		}

		public <T extends Enum<T>> T getEnumStrict(String columnLabel, Class<T> typeOf) throws SQLException {
			final String value = this.getStringStrict(columnLabel);
			final T enumValue = ReflectionUtil.lookupEnumSilent(typeOf, value);

			if (enumValue == null) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid " + typeOf.getSimpleName() + " enum value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring. Valid values: " + Common.join(typeOf.getEnumConstants(), ", "));

				throw new InvalidRowException();
			}

			return enumValue;
		}

		public boolean getBoolean(String columnLabel) throws SQLException {
			return delegate.getBoolean(columnLabel);
		}

		public boolean getBooleanStrict(String columnLabel) throws SQLException {
			final String value = this.getStringStrict(columnLabel);

			try {
				return Boolean.parseBoolean(value);

			} catch (final Throwable t) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid boolean value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public int getInt(String columnLabel) throws SQLException {
			return delegate.getInt(columnLabel);
		}

		public int getIntStrict(String columnLabel) throws SQLException {
			final String value = this.getStringStrict(columnLabel);

			try {
				return Integer.parseInt(value);

			} catch (final Throwable t) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid integer value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public long getLong(String columnLabel) throws SQLException {
			return delegate.getLong(columnLabel);
		}

		public long getLongStrict(String columnLabel) throws SQLException {
			final String value = this.getStringStrict(columnLabel);

			try {
				return Long.parseLong(value);

			} catch (final Throwable t) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid long value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public double getDouble(String columnLabel) throws SQLException {
			return delegate.getDouble(columnLabel);
		}

		public double getDoubleStrict(String columnLabel) throws SQLException {
			final String value = this.getStringStrict(columnLabel);

			try {
				return Double.parseDouble(value);

			} catch (final Throwable t) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid double value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public UUID getUniqueId(String columnLabel) throws SQLException {
			final String value = this.getString(columnLabel);

			if (value == null || "".equals(value))
				return null;

			try {
				return UUID.fromString(value);

			} catch (final Throwable ex) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid UUID value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public UUID getUniqueIdStrict(String columnLabel) throws SQLException {
			final String value = this.getStringStrict(columnLabel);

			try {
				return UUID.fromString(value);

			} catch (final Throwable ex) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid UUID value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public ItemStack getItem(String columnLabel) throws SQLException {
			final String value = this.getString(columnLabel);

			if (value == null || "".equals(value))
				return null;

			try {
				return SerializeUtil.deserialize(Mode.JSON, ItemStack.class, value);

			} catch (final Throwable ex) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid item value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public ItemStack getItemStrict(String columnLabel) throws SQLException {
			final String value = this.getStringStrict(columnLabel);

			try {
				return SerializeUtil.deserialize(Mode.JSON, ItemStack.class, value);

			} catch (final Throwable ex) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with invalid item value '" + value + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public Date getDate(String columnLabel) throws SQLException {
			return delegate.getDate(columnLabel);
		}

		public Time getTime(String columnLabel) throws SQLException {
			return delegate.getTime(columnLabel);
		}

		public long getTimestamp(String columnLabel) throws SQLException {
			final String rawTimestamp = delegate.getString(columnLabel);

			if (rawTimestamp == null)
				return 0;

			try {
				return Timestamp.valueOf(rawTimestamp).getTime();

			} catch (final IllegalArgumentException ex) {
				Common.warning("Failed to parse timestamp '" + rawTimestamp + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public long getTimestampStrict(String columnLabel) throws SQLException {
			final String rawTimestamp = delegate.getString(columnLabel);

			if (rawTimestamp == null) {
				Common.warning(SimplePlugin.getNamed() + " found invalid row with null/empty column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}

			try {
				return Timestamp.valueOf(rawTimestamp).getTime();

			} catch (final IllegalArgumentException ex) {
				Common.warning("Failed to parse timestamp '" + rawTimestamp + "' in column '" + columnLabel + "' in table " + this.tableName + ", ignoring.");

				throw new InvalidRowException();
			}
		}

		public Object getObject(String columnLabel) throws SQLException {
			return delegate.getObject(columnLabel);
		}

		public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
			return delegate.getObject(columnLabel, type);
		}

		public int findColumn(String columnLabel) throws SQLException {
			return delegate.findColumn(columnLabel);
		}

		public boolean isFirst() throws SQLException {
			return delegate.isFirst();
		}

		public boolean isLast() throws SQLException {
			return delegate.isLast();
		}

		public boolean first() throws SQLException {
			return delegate.first();
		}

		public boolean last() throws SQLException {
			return delegate.last();
		}

		public int getRow() throws SQLException {
			return delegate.getRow();
		}

		public boolean previous() throws SQLException {
			return delegate.previous();
		}

		public void insertRow() throws SQLException {
			delegate.insertRow();
		}

		public void deleteRow() throws SQLException {
			delegate.deleteRow();
		}

		public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
			return delegate.getObject(columnIndex, map);
		}

		public Ref getRef(int columnIndex) throws SQLException {
			return delegate.getRef(columnIndex);
		}

		public Array getArray(int columnIndex) throws SQLException {
			return delegate.getArray(columnIndex);
		}

		public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
			return delegate.getObject(columnLabel, map);
		}

		public Ref getRef(String columnLabel) throws SQLException {
			return delegate.getRef(columnLabel);
		}

		public Array getArray(String columnLabel) throws SQLException {
			return delegate.getArray(columnLabel);
		}

		public RowId getRowId(int columnIndex) throws SQLException {
			return delegate.getRowId(columnIndex);
		}

		public RowId getRowId(String columnLabel) throws SQLException {
			return delegate.getRowId(columnLabel);
		}

		public boolean isClosed() throws SQLException {
			return delegate.isClosed();
		}

	}

	/**
	 * 存储 connect() 函数最近一次已知的凭据
	 */
	@RequiredArgsConstructor
	private final class LastCredentials {

		/**
		 * 连接 URL，例如：
		 * <p>
		 * jdbc:mysql://host:port/database
		 */
		private final String url;

		/**
		 * 数据库用户名
		 */
		private final String user;

		/**
		 * 数据库密码
		 */
		private final String password;

		/**
		 * 表名。此类中从未使用，仅为方便你而存储
		 */
		private final String table;
	}
}