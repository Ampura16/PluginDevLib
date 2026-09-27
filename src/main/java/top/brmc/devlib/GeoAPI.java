package top.brmc.devlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import top.brmc.devlib.collection.expiringmap.ExpiringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * 用于解析玩家地理信息的工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeoAPI {

	/**
	 * 按 IP 地址缓存的响应，记录 1 小时后移除，防止在内存中堆积。
	 */
	private static final ExpiringMap<String, GeoResponse> cache = ExpiringMap.builder().expiration(1, TimeUnit.HOURS).build();

	/**
	 * 返回包含指定 IP 地址地理数据的 {@link GeoResponse}
	 * 这是一个阻塞操作，应当异步执行。为保证最佳性能，
	 * 若该 IP 已查询过，将直接返回缓存的响应。
	 *
	 * @param ip
	 * @return
	 */
	public static GeoResponse getCountry(InetSocketAddress ip) {
		GeoResponse response = new GeoResponse("", "", "", "");

		if (ip == null)
			return response;

		if (ip.getHostString().equals("127.0.0.1") || ip.getHostString().equals("0.0.0.0"))
			return new GeoResponse("local", "-", "local", "-");

		if (cache.containsKey(ip.toString()) || cache.containsValue(response))
			return cache.get(ip.toString());

		try {
			final URL url = new URL("http://ip-api.com/json/" + ip.getHostName());
			final URLConnection con = url.openConnection();
			con.setConnectTimeout(3000);
			con.setReadTimeout(3000);

			try (final BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String page = "";
				String input;

				while ((input = r.readLine()) != null)
					page += input;

				response = new GeoResponse(getJson(page, "country"), getJson(page, "countryCode"), getJson(page, "regionName"), getJson(page, "isp"));
				cache.put(ip.toString(), response);
			}

		} catch (final NoRouteToHostException ex) {
			// Firewall or internet access denied

		} catch (final SocketTimeoutException ex) { // hide
		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		return response;
	}

	private static String getJson(String page, String element) {
		return page.contains("\"" + element + "\":\"") ? page.split("\"" + element + "\":\"")[1].split("\",")[0] : "";
	}

	/**
	 * 从外部服务器获取的响应；同一 IP 的国家不会变化（对吧？:)），因此会被缓存。
	 */
	@RequiredArgsConstructor
	@Getter
	public static final class GeoResponse {
		private final String countryName, countryCode, regionName, isp;
	}
}
