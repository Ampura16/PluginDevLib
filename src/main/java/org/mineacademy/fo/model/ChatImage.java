package org.mineacademy.fo.model;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompChatColor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * 表示在聊天中显示图片的一种方式
 *
 * @author bobacadodl and kangarko
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatImage {

	/**
	 * 返回默认长度 8
	 */
	public final static int DEFAULT_HEIGHT = 8;

	/**
	 * 表示空字符
	 */
	private final static char TRANSPARENT_CHAR = ' ';

	/**
	 * 表示获取图片的 Minotar API 端点
	 */
	@Getter
	@Setter
	public static String chatHeadEndpoint = "https://mc-heads.net/avatar/{PLAYER_NAME}/{HEIGHT}.png";

	/**
	 * 缩放图片的策略。默认的 TYPE_NEAREST_NEIGHBOR 不会
	 * “平滑”边缘，适合头像和 Minecraft 素材。改为
	 * {@link AffineTransformOp#TYPE_BILINEAR} 可对缩小后的图片进行抗锯齿。
	 *
	 * @see AffineTransformOp
	 */
	@Getter
	@Setter
	private static int resizeMethod = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;

	/**
	 * 修改 PNG 图片的背景色，默认为 WHITE
	 */
	@Getter
	@Setter
	private static Color backgroundColor = Color.WHITE;

	/**
	 * 表示当前已加载的行
	 */
	@Getter
	private String[] lines;

	/**
	 * 在图片旁追加给定文本
	 *
	 * @param text
	 * @return
	 */
	public ChatImage appendText(String... text) {
		for (int y = 0; y < this.lines.length; y++)
			if (text.length > y) {
				final String line = text[y];

				this.lines[y] += " " + line;
			}

		return this;
	}

	/**
	 * 在图片旁以居中方式追加给定文本
	 * 注意：使用格式颜色或 unicode 可能会破坏居中效果！
	 *
	 * @param text
	 * @return
	 */
	public ChatImage appendCenteredText(String... text) {
		for (int y = 0; y < this.lines.length; y++)
			if (text.length > y) {
				final int len = ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH - this.lines[y].length();

				this.lines[y] = this.lines[y] + this.center(text[y], len);

			} else
				return this;

		return this;
	}

	/*
	 * Centers the given message according to the given length
	 */
	private String center(String message, int length) {
		if (message.length() > length)
			return message.substring(0, length);

		else if (message.length() == length)
			return message;

		else {
			final int leftPadding = (length - message.length()) / 2;
			final StringBuilder leftBuilder = new StringBuilder();

			for (int i = 0; i < leftPadding; i++)
				leftBuilder.append(" ");

			return leftBuilder.toString() + message;
		}
	}

	/**
	 * 将此图片发送给给定玩家
	 *
	 * @param sender
	 */
	public void send(CommandSender sender) {
		for (final String line : this.lines)
			sender.sendMessage(Variables.replace(line, sender));
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	/**
	 * 根据玩家用户名创建玩家头像图片。使用 DARK_SHADE 字符
	 * 和 {@link #DEFAULT_HEIGHT}。会向 {@link #chatHeadEndpoint}
	 * 发起阻塞式网络请求，任何失败都会抛出错误。
	 *
	 * @param playerName
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromHead(String playerName) throws IOException {
		return fromHead(playerName, DEFAULT_HEIGHT);
	}

	/**
	 * 根据玩家用户名创建玩家头像图片。使用 DARK_SHADE 字符。
	 * 会向 {@link #chatHeadEndpoint} 发起阻塞式网络请求，
	 * 任何失败都会抛出错误。
	 *
	 * @param playerName
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromHead(String playerName, int height) throws IOException {
		return fromHead(playerName, height, ChatImage.Type.DARK_SHADE);
	}

	/**
	 * 根据玩家用户名创建玩家头像图片。会向
	 * {@link #chatHeadEndpoint} 发起阻塞式网络请求，任何失败都会抛出错误。
	 *
	 * @param playerName
	 * @param height
	 * @param characterType
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromHead(String playerName, int height, Type characterType) throws IOException {
		return fromImage(chatHeadEndpoint.replace("{PLAYER_NAME}", playerName).replace("{HEIGHT}", String.valueOf(height)), height, characterType);
	}

	/**
	 * 根据给定远程 URL、{@link #DEFAULT_HEIGHT} 和 DARK_SHADE
	 * 字符类型创建聊天图片。会发起阻塞式网络请求，任何失败都会抛出错误。
	 *
	 * @param webUrl
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromImage(String webUrl) throws IOException {
		return fromImage(webUrl, DEFAULT_HEIGHT);
	}

	/**
	 * 根据给定远程 URL、给定行高和 DARK_SHADE 字符类型创建聊天图片。
	 * 会发起阻塞式网络请求，任何失败都会抛出错误。
	 *
	 * @param webUrl
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromImage(String webUrl, int height) throws IOException {
		return fromImage(webUrl, height, Type.DARK_SHADE);
	}

	/**
	 * 根据给定远程 URL、给定行高和字符类型创建聊天图片。
	 * 会发起阻塞式网络请求，任何失败都会抛出错误。
	 *
	 * @param webUrl
	 * @param height
	 * @param characterType
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromImage(@NonNull String webUrl, int height, Type characterType) throws IOException {
		final BufferedImage image = ImageIO.read(new URL(webUrl));

		if (image == null)
			throw new NullPointerException("Unable to load image from URL ");

		else
			return fromSource(image, height, characterType);
	}

	/**
	 * 根据插件 JAR 中的给定路径创建用于聊天消息的图片，
	 * 使用 {@link #DEFAULT_HEIGHT} 和 DARK_SHADE 字符类型。
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromFile(@NonNull File file) throws IOException {
		return fromFile(file, DEFAULT_HEIGHT);
	}

	/**
	 * 根据插件 JAR 中的给定路径创建用于聊天消息的图片，
	 * 使用给定高度和 DARK_SHADE 字符类型。
	 *
	 * @param file
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromFile(@NonNull File file, int height) throws IOException {
		return fromFile(file, height, ChatImage.Type.DARK_SHADE);
	}

	/**
	 * 根据插件 JAR 中的给定路径创建用于聊天消息的图片，
	 * 使用给定高度和给定字符类型。
	 *
	 * @param file
	 * @param height
	 * @param characterType
	 * @return
	 * @throws IOException
	 */
	public static ChatImage fromFile(@NonNull File file, int height, Type characterType) throws IOException {
		Valid.checkBoolean(file.exists(), "Cannot load image from non existing file " + file.toPath());

		final BufferedImage image = ImageIO.read(file);

		if (image == null)
			throw new NullPointerException("Unable to load image size " + file.length() + " bytes from " + file.toPath());

		else
			return fromSource(image, height, characterType);
	}

	/*
	 * Helper to load the image
	 */
	private static ChatImage fromSource(@NonNull BufferedImage image, int height, @NonNull Type characterType) {
		Valid.checkBoolean(height >= 2, "File image height must be equal or above 2");

		final BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		newImage.createGraphics().drawImage(image, 0, 0, backgroundColor, null);

		final CompChatColor[][] chatColors = parseImage(newImage, height);
		final ChatImage chatImage = new ChatImage();

		chatImage.lines = parseColors(chatColors, characterType);

		return chatImage;
	}

	/**
	 * 根据已由 {@link #fromFile(File, int, Type)} 生成的现成行返回聊天图片
	 * 适用于从磁盘文件或远程数据库加载的场景
	 *
	 * @param lines
	 * @return
	 */
	public static ChatImage fromLines(String[] lines) {
		final ChatImage chatImage = new ChatImage();
		chatImage.lines = lines;

		return chatImage;
	}

	/*
	 * Parse the given image into chat colors
	 */
	private static CompChatColor[][] parseImage(BufferedImage newImage, int height) {
		final double ratio = (double) newImage.getHeight() / newImage.getWidth();
		int width = (int) (height / ratio);

		if (width > 10)
			width = 10;

		final BufferedImage resized = resizeImage(newImage, (int) (height / ratio), height);
		final CompChatColor[][] chatImg = new CompChatColor[resized.getWidth()][resized.getHeight()];

		for (int x = 0; x < resized.getWidth(); x++)
			for (int y = 0; y < resized.getHeight(); y++) {
				final int rgb = resized.getRGB(x, y);
				final CompChatColor closest = CompChatColor.getClosestLegacyColor(new Color(rgb, true));

				chatImg[x][y] = closest;
			}

		return chatImg;
	}

	/*
	 * Resize the given image
	 */
	private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
		final AffineTransform af = new AffineTransform();

		af.scale(
				width / (double) originalImage.getWidth(),
				height / (double) originalImage.getHeight());

		final AffineTransformOp operation = new AffineTransformOp(af, resizeMethod);

		return operation.filter(originalImage, null);
	}

	/*
	 * Parse the given 2D colors to fit lines
	 */
	private static String[] parseColors(CompChatColor[][] colors, Type imgchar) {
		final String[] lines = new String[colors[0].length];

		for (int y = 0; y < colors[0].length; y++) {
			String line = "";

			for (final CompChatColor[] color2 : colors) {
				final CompChatColor color = color2[y];

				line += color != null ? color2[y].toString() + imgchar : TRANSPARENT_CHAR;
			}

			lines[y] = line + ChatColor.RESET;
		}

		return lines;
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	/**
	 * 表示常用的图片字符
	 *
	 * @author bobacadodl
	 */
	public enum Type {

		BLOCK('\u2588'),
		DARK_SHADE('\u2593'),
		MEDIUM_SHADE('\u2592'),
		LIGHT_SHADE('\u2591');

		/**
		 * 用于构建图片的字符
		 */
		@Getter
		private final char character;

		Type(char c) {
			this.character = c;
		}

		/**
		 * 返回该字符
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return String.valueOf(this.character);
		}
	}
}