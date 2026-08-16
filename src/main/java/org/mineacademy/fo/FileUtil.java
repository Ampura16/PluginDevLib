package org.mineacademy.fo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 管理文件的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil {

	/**
	 * 返回给定路径的文件名，剥离
	 * 任何扩展名和文件夹。
	 * <p>
	 * 例如：classes/Archer.yml 只返回 Archer
	 *
	 * @param file
	 * @return
	 */
	public static String getFileName(File file) {
		return getFileName(file.getName());
	}

	/**
	 * 返回给定路径的文件名，剥离
	 * 任何扩展名和文件夹。
	 * <p>
	 * 例如：classes/Archer.yml 只返回 Archer
	 *
	 * @param path
	 * @return
	 */
	public static String getFileName(String path) {
		Valid.checkBoolean(path != null && !path.isEmpty(), "The given path must not be empty!");

		int pos = path.lastIndexOf("/");

		if (pos > 0)
			path = path.substring(pos + 1);

		pos = path.lastIndexOf(".");

		if (pos > 0)
			path = path.substring(0, pos);

		return path;
	}

	// ----------------------------------------------------------------------------------------------------
	// Getting files
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 返回插件文件夹内给定路径的文件，若文件
	 * 不存在则创建它
	 *
	 * @param path
	 * @return
	 */
	public static File getOrMakeFile(String path) {
		final File file = getFile(path);

		return file.exists() ? file : createIfNotExists(path);
	}

	/**
	 * 检查文件是否存在，若不存在则创建新文件
	 *
	 * @param file
	 * @return
	 */
	public static File createIfNotExists(File file) {
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (final Throwable t) {
				Common.throwError(t, "Could not create new file '" + file + "' due to " + t);
			}

		return file;
	}

	/**
	 * 在插件文件夹中创建新文件，支持多级目录路径
	 * <p>
	 * 例如：logs/admin/console.log 或 worlds/nether.yml 都是有效路径
	 *
	 * @param path
	 * @return
	 */
	public static File createIfNotExists(String path) {
		final File datafolder = SimplePlugin.getData();
		final int lastIndex = path.lastIndexOf('/');
		final File directory = new File(datafolder, path.substring(0, lastIndex >= 0 ? lastIndex : 0));

		directory.mkdirs();

		final File destination = new File(datafolder, path);

		try {
			destination.createNewFile();

		} catch (final IOException ex) {
			Common.error(ex, "Failed to create a new file " + path);
		}

		return destination;
	}

	/**
	 * 返回插件文件夹中某路径的文件，文件可能不存在
	 *
	 * @param path
	 * @return
	 */
	public static File getFile(String path) {
		return new File(SimplePlugin.getData(), path);
	}

	/**
	 * 返回插件目录中给定路径下以给定扩展名结尾的所有文件
	 *
	 * @param directory 插件文件夹内的目录
	 * @param extension 扩展名，若缺少点号会自动补上
	 * @return
	 */
	public static File[] getFiles(@NonNull String directory, @NonNull String extension) {

		// Remove initial dot, if any
		if (extension.startsWith("."))
			extension = extension.substring(1);

		final File dataFolder = new File(SimplePlugin.getData(), directory);

		if (!dataFolder.exists())
			dataFolder.mkdirs();

		final String finalExtension = extension;

		return dataFolder.listFiles((FileFilter) file -> !file.isDirectory() && file.getName().endsWith("." + finalExtension));
	}

	// ----------------------------------------------------------------------------------------------------
	// Reading
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 返回给定 URL 的所有行，先用伪造的 UA 打开连接
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static List<String> readLines(URL url) throws IOException {

		final URLConnection connection = url.openConnection();

		// Set a random user agent to prevent most webhostings from rejecting with 403
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7) Gecko/20040803 Firefox/0.9.3");
		connection.setConnectTimeout(6000);
		connection.setReadTimeout(6000);
		connection.setDoOutput(true);

		return readLines(connection);
	}

	/**
	 * 返回给定连接的所有行
	 *
	 * @param connection
	 * @return
	 */
	public static List<String> readLines(URLConnection connection) {
		final List<String> lines = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String inputLine;

			while ((inputLine = reader.readLine()) != null)
				lines.add(inputLine);

		} catch (final IOException ex) {
			Remain.sneaky(ex);
		}

		return lines;
	}

	/**
	 * 返回插件文件夹中某路径文件的所有行，文件必须存在。
	 *
	 * @param fileName
	 * @return
	 */
	public static List<String> readLines(String fileName) {
		return readLines(getFile(fileName));
	}

	/**
	 * 返回文件中的所有行，若文件不存在则失败
	 *
	 * @param file
	 * @return
	 */
	public static List<String> readLines(@NonNull File file) {
		Valid.checkBoolean(file.exists(), "File: " + file + " does not exists!");

		// Older method, missing libraries
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			final List<String> lines = new ArrayList<>();
			String line;

			while ((line = br.readLine()) != null)
				lines.add(line);

			return lines;

		} catch (final IOException ee) {
			throw new FoException(ee, "Could not read lines from " + file.getName());
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Writing
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 向文件写入一行
	 * <p>
	 * 行的格式如下：[date] msg
	 *
	 * @param to
	 * @param message
	 */
	public static void writeFormatted(String to, String message) {
		writeFormatted(to, null, message);
	}

	/**
	 * 向文件写入一行，可带前缀，前缀可为 null。
	 * <p>
	 * 行的格式如下：[date] prefix msg
	 *
	 * @param to      插件文件夹内文件的路径
	 * @param prefix  可选前缀，可为 null
	 * @param message 行内容，按 \n 分割
	 */
	public static void writeFormatted(String to, String prefix, String message) {
		message = Common.stripColors(message).trim();

		if (!message.equalsIgnoreCase("none") && !message.isEmpty())
			for (final String line : message.split("\n"))
				if (!line.isEmpty())
					write(to, "[" + TimeUtil.getFormattedDate() + "] " + (prefix != null ? prefix + ": " : "") + line);
	}

	/**
	 * 向插件目录中的文件路径写入多行，
	 * 若文件不存在则创建，在末尾追加行
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(String to, String... lines) {
		write(to, Arrays.asList(lines));
	}

	/**
	 * 向文件写入多行，若文件不存在则创建，在末尾追加行
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(File to, String... lines) {
		write(createIfNotExists(to), Arrays.asList(lines), StandardOpenOption.APPEND);
	}

	/**
	 * 向插件目录中的文件路径写入多行，
	 * 若文件不存在则创建，在末尾追加行
	 *
	 * @param to
	 * @param lines
	 */
	public static void write(String to, Collection<String> lines) {
		write(getOrMakeFile(to), lines, StandardOpenOption.APPEND);
	}

	/**
	 * 将给定行写入文件
	 *
	 * @param to
	 * @param lines
	 * @param options
	 */
	public static void write(File to, Collection<String> lines, StandardOpenOption... options) {
		try {
			final Path path = Paths.get(to.toURI());

			try {
				if (!to.exists())
					createIfNotExists(to);

				Files.write(path, lines, StandardCharsets.UTF_8, options);

			} catch (final ClosedByInterruptException ex) {
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(to, true))) {
					for (final String line : lines)
						bw.append(System.lineSeparator() + line);

				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

		} catch (final Exception ex) {

			// do not throw our exception since it would cause an infinite loop if there is a problem due to error writing
			Common.error(ex, "Failed to write to " + to);
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Extracting from our plugin .jar file
	// ----------------------------------------------------------------------------------------------------

	/**
	 * 将文件从插件 jar 包复制到目标位置。
	 * 若文件已存在则不做任何操作。
	 *
	 * @param path 插件内文件的路径
	 * @return 提取出的文件
	 */
	public static File extract(String path) {
		return extract(path, path);
	}

	/**
	 * 将文件从插件 jar 包复制到目标位置 - 可自定义目标文件
	 * 名。
	 *
	 * @param from     插件内文件的路径
	 * @param to       文件将被复制到的目标路径，位于插件
	 *                 文件夹内
	 * @return 提取出的文件
	 */
	public static File extract(String from, String to) {
		File file = new File(SimplePlugin.getData(), to);

		final List<String> lines = getInternalFileContent(from);
		Valid.checkNotNull(lines, "Inbuilt " + file.getAbsolutePath() + " not found! Did you reload?");

		if (file.exists())
			return file;

		file = createIfNotExists(to);

		try {
			final String fileName = getFileName(file);

			// Replace variables in lines
			for (int i = 0; i < lines.size(); i++)
				lines.set(i, replaceVariables(lines.get(i), fileName));

			Files.write(file.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (final IOException ex) {
			Common.error(ex,
					"Failed to extract " + from + " to " + to,
					"Error: %error");
		}

		return file;
	}

	/**
	 * 与 {@link #extract(String, String)} 类似，但用于
	 * 图片等非文本文件类型。
	 *
	 * @param path
	 * @return
	 */
	public static File extractRaw(String path) {
		File file = new File(SimplePlugin.getData(), path);

		try (JarFile jarFile = new JarFile(SimplePlugin.getSource())) {

			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry entry = it.nextElement();

				if (entry.toString().equals(path)) {
					final InputStream is = jarFile.getInputStream(entry);

					if (file.exists())
						return file;

					file = createIfNotExists(path);

					try {
						Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

					} catch (final IOException ex) {
						Common.error(ex,
								"Failed to extract " + path,
								"Error: %error");
					}

					return file;

				}
			}

		} catch (final Throwable ex) {
			ex.printStackTrace();
		}

		throw new FoException("Inbuilt file not found: " + path);
	}

	/*
	 * A helper method to replace variables in files we are extracting.
	 *
	 * Saves us time so that we can distribute the same file across multiple
	 * plugins each having its own unique plugin name and file name.
	 */
	private static String replaceVariables(String line, String fileName) {
		return line
				.replace("{plugin_name}", SimplePlugin.getNamed())
				.replace("{plugin_name_lower}", SimplePlugin.getNamed().toLowerCase())
				.replace("{file}", fileName)
				.replace("{file_lowercase}", fileName);
	}

	/**
	 * 将文件夹及其所有内容从 JAR 文件提取到
	 * 插件文件夹中的给定路径
	 *
	 * @param folder      插件 JAR 文件中的源文件夹
	 * @param destination 插件文件夹中的目标文件夹名
	 */
	public static void extractFolderFromJar(String folder, final String destination) {
		Valid.checkBoolean(folder.endsWith("/"), "Folder must end with '/'! Given: " + folder);
		Valid.checkBoolean(!folder.startsWith("/"), "Folder must not start with '/'! Given: " + folder);

		if (getFile(folder).exists())
			return;

		try (JarFile jarFile = new JarFile(SimplePlugin.getSource())) {
			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry jarEntry = it.nextElement();
				final String entryName = jarEntry.getName();

				// Copy each individual file manually
				if (entryName.startsWith(folder) && !entryName.equals(folder))
					extract(entryName);
			}

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to copy folder " + folder + " to " + destination);
		}
	}

	/**
	 * 返回插件 jar 包内的内部资源
	 *
	 * @param path
	 * @return 内部文件的内容
	 */
	public static List<String> getInternalFileContent(@NonNull String path) {

		try (JarFile jarFile = new JarFile(SimplePlugin.getSource())) {

			for (final Enumeration<JarEntry> it = jarFile.entries(); it.hasMoreElements();) {
				final JarEntry entry = it.nextElement();

				if (entry.toString().equals(path)) {

					final InputStream is = jarFile.getInputStream(entry);
					final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
					final List<String> lines = reader.lines().collect(Collectors.toList());

					reader.close();
					return lines;
				}
			}

		} catch (final Throwable ex) {
			ex.printStackTrace();
		}

		return null;
	}

	/**
	 * 删除给定文件及所有子文件夹
	 *
	 * @param file
	 */
	public static void deleteRecursivelly(File file) {
		if (file.isDirectory())
			for (final File subfolder : file.listFiles())
				deleteRecursivelly(subfolder);

		if (file.exists())
			Valid.checkBoolean(file.delete(), "Failed to delete file: " + file);
	}

	/**
	 * 从给定源目录（插件文件夹内）创建 ZIP 压缩包到给定完整路径（插件文件夹内），
	 * 请不要指定任何扩展名，只写目录和文件名
	 *
	 * @param sourceDirectory
	 * @param to
	 * @throws IOException
	 */
	public static void zip(String sourceDirectory, String to) throws IOException {
		final File parent = SimplePlugin.getData().getParentFile().getParentFile();
		final File toFile = new File(parent, to + ".zip");

		if (toFile.exists())
			Valid.checkBoolean(toFile.delete(), "Failed to delete old file " + toFile);

		final Path pathTo = Files.createFile(Paths.get(toFile.toURI()));

		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(pathTo))) {
			final Path pathFrom = Paths.get(new File(parent, sourceDirectory).toURI());

			Files.walk(pathFrom).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				final ZipEntry zipEntry = new ZipEntry(pathFrom.relativize(path).toString());

				try {
					zs.putNextEntry(zipEntry);

					Files.copy(path, zs);
					zs.closeEntry();
				} catch (final IOException ex) {
					ex.printStackTrace();
				}
			});
		}
	}
}
