package top.brmc.devlib.model;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.RequiredArgsConstructor;

/**
 * 连接 Mojang 服务器、根据给定 UUID 获取玩家名称的
 * 工具类
 */
@RequiredArgsConstructor
public class UUIDToNameConverter implements Callable<String> {

	/**
	 * 要连接的 URL
	 */
	private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

	/**
	 * JSON 解析库
	 */
	private final Gson gson = new Gson();

	/**
	 * 要转换为名称的 UUID
	 */
	private final UUID uuid;

	/**
	 * 尝试连接 Mojang 服务器，根据玩家的唯一 ID
	 * 获取其当前用户名
	 * <p>
	 * 在主线程上运行
	 */
	@Override
	public String call() throws Exception {

		final HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL + this.uuid.toString().replace("-", "")).openConnection();
		final JsonObject response = this.gson.fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);
		final String name = response.get("name").getAsString();

		if (name == null)
			return "";

		final String cause = response.get("cause").getAsString();
		final String errorMessage = response.get("errorMessage").getAsString();

		if (cause != null && cause.length() > 0)
			throw new IllegalStateException(errorMessage);

		return name;
	}
}
