package top.brmc.devlib;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.jsonsimple.JSONObject;
import top.brmc.devlib.jsonsimple.JSONParseException;
import top.brmc.devlib.jsonsimple.JSONParser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 网络操作工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkUtil {

	/**
	 * 默认将 UA 伪装为 Chrome 126，你可以在请求属性中覆盖它。
	 */
	public final static String HTTP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

	/**
	 * 向指定端点发起 GET 请求，并将响应作为 JSON 对象返回。
	 *
	 * @param endpoint 发送 GET 请求的 URL。
	 * @return 包含响应的 JSONObject，若请求或解析失败则为 null。
	 */
	public static JSONObject getJson(String endpoint) {
		return getJson(endpoint, null);
	}

	/**
	 * 用给定参数向指定端点发起 GET 请求，并将响应作为 JSON 对象返回。
	 *
	 * @param endpoint 发送 GET 请求的 URL。
	 * @param params 请求中包含的查询参数映射。
	 * @return
	 */
	public static JSONObject getJson(String endpoint, SerializedMap params) {
		return getJson(endpoint, params, null);
	}

	/**
	 * 用给定参数向指定端点发起 GET 请求，并将响应作为 JSON 对象返回。
	 *
	 * @param endpoint 发送 GET 请求的 URL。
	 * @param params   请求中包含的查询参数映射。
	 * @param requestProperties 请求中包含的请求属性，为 null 则忽略。
	 *
	 * @return 包含响应的 JSONObject，若请求或解析失败则为 null。
	 */
	public static JSONObject getJson(String endpoint, SerializedMap params, SerializedMap requestProperties) {
		final String response = get(endpoint, params, requestProperties);

		try {
			final Object json = JSONParser.deserialize(response);

			try {
				return (JSONObject) json;

			} catch (final ClassCastException ex) {
				Common.throwError(ex,
						"Failed to cast JSON to JSONObject!",
						"",
						"Endpoint: " + endpoint,
						"Params: " + params,
						"Request Properties: " + requestProperties,
						"Response: " + response,
						"Raw JSON (" + json.getClass().getSimpleName() + "): " + json);
			}

		} catch (final JSONParseException ex) {
			Common.throwError(ex,
					"Failed to get JSON!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties,
					"Response: " + response);
		}

		return null;
	}

	/**
	 * 向指定端点发起 POST 请求，并将响应作为 JSON 对象返回。
	 *
	 * @param endpoint 发送 POST 请求的 URL。
	 * @return 包含响应的 JSONObject，若请求或解析失败则为 null。
	 */
	public static JSONObject postJson(String endpoint) {
		return postJson(endpoint, null);
	}

	/**
	 * 用给定参数向指定端点发起 POST 请求，并将响应作为 JSON 对象返回。
	 *
	 * @param endpoint 发送 POST 请求的 URL。
	 * @param params 请求中包含的参数映射。
	 * @return
	 */
	public static JSONObject postJson(String endpoint, SerializedMap params) {
		return postJson(endpoint, params, null);
	}

	/**
	 * 用给定参数向指定端点发起 POST 请求，并将响应作为 JSON 对象返回。
	 *
	 * @param endpoint 发送 POST 请求的 URL。
	 * @param params  请求中包含的参数映射。
	 * @param requestProperties 请求中包含的请求属性，为 null 则忽略。
	 *
	 * @return 包含响应的 JSONObject，若请求或解析失败则为 null。
	 */
	public static JSONObject postJson(String endpoint, SerializedMap params, SerializedMap requestProperties) {
		final String response = post(endpoint, params, requestProperties);

		try {
			final Object json = JSONParser.deserialize(response);

			try {
				return (JSONObject) json;

			} catch (final ClassCastException ex) {
				Common.throwError(ex,
						"Failed to cast JSON to JSONObject!",
						"",
						"Endpoint: " + endpoint,
						"Params: " + params,
						"Request Properties: " + requestProperties,
						"Response: " + response,
						"Raw JSON (" + json.getClass().getSimpleName() + "): " + json);
			}

		} catch (final JSONParseException ex) {
			Common.throwError(ex,
					"Failed to post JSON!",
					"",
					"Endpoint: " + endpoint,
					"Params: " + params,
					"Request Properties: " + requestProperties,
					"Response: " + response);
		}

		return null;
	}

	/**
	 * 向指定端点发起 GET 请求，并将响应作为字符串返回。
	 *
	 * @param endpoint 发送 GET 请求的 URL。
	 * @return 包含响应的字符串，若请求失败则为空字符串。
	 */
	public static String get(String endpoint) {
		return get(endpoint, null);
	}

	/**
	 * 用给定参数向指定端点发起 GET 请求，并将响应作为字符串返回。
	 *
	 * @param endpoint 发送 GET 请求的 URL。
	 * @param params 请求中包含的查询参数映射，为 null 则忽略。
	 * @return
	 */
	public static String get(String endpoint, SerializedMap params) {
		return get(endpoint, params, null);
	}

	/**
	 * 用给定参数向指定端点发起 GET 请求，并将响应作为字符串返回。
	 *
	 * @param endpoint 发送 GET 请求的 URL。
	 * @param params   请求中包含的查询参数映射，为 null 则忽略。
	 * @param requestProperties 请求中包含的请求属性，为 null 则忽略。
	 *
	 * @return 包含响应的字符串，若请求失败则为空字符串。
	 */
	public static String get(String endpoint, SerializedMap params, SerializedMap requestProperties) {

		if (params == null)
			params = new SerializedMap();

		// Bust the cache
		params.put("t", System.currentTimeMillis());

		try {
			if (params != null && !params.isEmpty()) {
				final StringBuilder endpointBuilder = new StringBuilder(endpoint).append("?");

				for (final Map.Entry<String, Object> entry : params.entrySet())
					endpointBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue().toString(), "UTF-8")).append("&");

				endpoint = endpointBuilder.toString();
			}

			final URL url = new URL(endpoint);
			final URLConnection connection = url.openConnection();

			connection.setRequestProperty("User-Agent", HTTP_USER_AGENT);

			if (requestProperties != null)
				for (final Map.Entry<String, Object> entry : requestProperties.entrySet())
					connection.setRequestProperty(entry.getKey(), entry.getValue().toString());

			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				final StringBuilder responseBuilder = new StringBuilder();
				String input;

				while ((input = reader.readLine()) != null)
					responseBuilder.append(input);

				return responseBuilder.toString();
			}

		} catch (final Exception ex) {
			Common.throwError(ex, "Failed to read response from " + endpoint + " with params " + params);

			return "";
		}
	}

	/**
	 * 向指定端点发起 POST 请求，并将响应作为字符串返回。
	 *
	 * @param endpoint 发送 POST 请求的 URL。
	 * @return 包含响应的字符串，若请求失败则为空字符串。
	 */
	public static String post(String endpoint) {
		return post(endpoint, null);
	}

	/**
	 * 用给定参数向指定端点发起 POST 请求，并将响应作为字符串返回。
	 *
	 * @param endpoint 发送 POST 请求的 URL。
	 * @param params 请求中包含的参数映射，为 null 则忽略。
	 * @return
	 */
	public static String post(String endpoint, SerializedMap params) {
		return post(endpoint, params, null);
	}

	/**
	 * 用给定参数向指定端点发起 POST 请求，并将响应作为字符串返回。
	 *
	 * @param endpoint 发送 POST 请求的 URL。
	 * @param params   请求中包含的参数映射，为 null 则忽略。
	 * @param requestProperties 请求中包含的请求属性，为 null 则忽略。
	 *
	 * @return 包含响应的字符串，若请求失败则为空字符串。
	 */
	public static String post(String endpoint, SerializedMap params, SerializedMap requestProperties) {

		if (params == null)
			params = new SerializedMap();

		// Bust the cache
		params.put("t", System.currentTimeMillis());

		try {
			final URL url = new URL(endpoint);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setDoOutput(true);

			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("User-Agent", HTTP_USER_AGENT);

			if (requestProperties != null)
				for (final Map.Entry<String, Object> entry : requestProperties.entrySet())
					connection.setRequestProperty(entry.getKey(), entry.getValue().toString());

			if (params != null && !params.isEmpty())
				try (OutputStream output = connection.getOutputStream()) {
					final byte[] input = params.toJson().getBytes("utf-8");

					output.write(input, 0, input.length);
				}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				final StringBuilder responseBuilder = new StringBuilder();
				String input;

				while ((input = reader.readLine()) != null)
					responseBuilder.append(input);

				return responseBuilder.toString();
			}

		} catch (final Exception ex) {
			Common.throwError(ex, "Failed to read response from " + endpoint + " with params " + params);

			return "";
		}
	}
}
