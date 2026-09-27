package top.brmc.devlib.plugin;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.Valid;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 表示一个可加载的独立库。你还可以设置
 * 该库可运行的最低和最高 MC 版本，
 * 以及用于限制加载的 Java 版本。
 */
@Accessors(chain = true)
@Setter
public final class Library {

	/*
	 * Stored for convenience and performance purposes
	 */
	private static final int JAVA_VERSION = SimplePlugin.getJavaVersion();

	/**
	 * 符合 Maven 规范的 groupID，例如 "top.brmc.devlib"
	 */
	@Getter
	private final String groupId;

	/**
	 * 符合 Maven 规范的 artifactID，例如 "foundation"
	 */
	@Getter
	private final String artifactId;

	/**
	 * 库的版本，例如 "1.0.0"
	 */
	@Getter
	private final String version;

	/**
	 * 下载库 JAR 的 jar 路径。
	 */
	private final String jarPath;

	/**
	 * 该库所需的最低 Minecraft 版本。
	 */

	private MinecraftVersion.V minimumMinecraftVersion;

	/**
	 * 该库可加载的最高 Minecraft 版本。
	 */

	private MinecraftVersion.V maximumMinecraftVersion;

	/**
	 * 该库可加载的最低 Java 版本。
	 */

	private Integer minimumJavaVersion;

	/**
	 * 该库可加载的最高 Java 版本。
	 */

	private Integer maximumJavaVersion;

	/**
	 * 从你自己的仓库下载 JAR 时可选设置的 User-Agent。
	 */

	private String userAgent = null;

	/*
	 * Create a new library
	 */
	private Library(String groupId, String artifactId, String version, String repositoryPath) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;

		this.jarPath = repositoryPath;
	}

	/**
	 * 加载此库。加载成功返回 true，
	 * 不满足最低/最高 Java 或 Minecraft 版本时返回 false
	 *
	 * @return
	 */
	public boolean load() {
		Valid.checkBoolean(JAVA_VERSION <= 8, "Library feature requires Java 8 and does not work on Java " + JAVA_VERSION);

		if (this.minimumJavaVersion != null && JAVA_VERSION < this.minimumJavaVersion)
			return false;

		if (this.maximumJavaVersion != null && JAVA_VERSION > this.maximumJavaVersion)
			return false;

		if (this.minimumMinecraftVersion != null && MinecraftVersion.olderThan(this.minimumMinecraftVersion))
			return false;

		if (this.maximumMinecraftVersion != null && MinecraftVersion.newerThan(this.maximumMinecraftVersion))
			return false;

		try {
			final File libraries = new File(Bukkit.getWorldContainer(), "libraries");
			final File file = new File(libraries, this.groupId.replace(".", "/") + "/" + this.artifactId.replace(".", "/") + "/" + this.version + "/" + this.artifactId + "-" + this.version + ".jar");

			// Download file from repository to our disk
			if (!file.exists()) {
				file.getParentFile().mkdirs();

				Bukkit.getLogger().info("Downloading library: " + this.getName());

				final URL url = new URL(this.jarPath);
				final URLConnection connection = url.openConnection();

				if (this.userAgent != null)
					connection.setRequestProperty("User-Agent", this.userAgent);

				try (InputStream in = connection.getInputStream()) {
					Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}

			//Bukkit.getLogger().info("Loading library: " + this.getName());

			// Load the library into the plugin's class loader
			final URL url = file.toURI().toURL();
			final ClassLoader classLoader = SimplePlugin.class.getClassLoader();
			final Method method = ReflectionUtil.getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);

			ReflectionUtil.invoke(method, classLoader, url);

		} catch (final Throwable throwable) {
			throw new RuntimeException("Unable to load library " + this.getName() + ".", throwable);
		}

		return true;
	}

	/**
	 * 以字符串形式返回 groupId:artifactId:version
	 *
	 * @return
	 */
	public String getName() {
		return this.groupId + ":" + this.artifactId + ":" + this.version;
	}

	/**
	 * 参见 {@link #getName()}
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return this.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 从 Maven 中央仓库创建一个新库
	 *
	 * 如果你喜欢的库位于 https://mvnrepository.com/repos/central，请使用此方法
	 *
	 * 路径语法为："groupId:artifactId:version"，例如 "org.jsoup:jsoup:1.14.3"
	 *
	 * @param path
	 * @return
	 */
	public static Library fromMavenRepo(String path) {
		final String[] split = path.split("\\:");
		Valid.checkBoolean(split.length == 3, "Malformed library path, expected <groupId>:<name>:<version>, got: " + path);

		return fromMavenRepo(split[0], split[1], split[2]);
	}

	/**
	 * 从 Maven 中央仓库创建一个新库
	 *
	 * 如果你喜欢的库位于 https://mvnrepository.com/repos/central，请使用此方法
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return
	 */
	public static Library fromMavenRepo(String groupId, String artifactId, String version) {
		final String jarPath = "https://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";

		return new Library(groupId, artifactId, version, jarPath);
	}

	/**
	 * 从你自己的自定义仓库路径创建一个新库。
	 *
	 * 路径必须是指向 JAR 的完整在线 URL。在 {@link #fromMavenRepo(String, String, String)} 中
	 * 我们使用 "https://repo1.maven.org/maven2/{groupId}/{artifactId}/{version}/{artifactId}-{version}.jar"，
	 * 但实际上它可以是任何地址，例如 yourdomain.com/yourlibrary.jar
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param jarPath
	 * @return
	 */
	public static Library fromPath(String groupId, String artifactId, String version, String jarPath) {
		return new Library(groupId, artifactId, version, jarPath);
	}
}