package jp.timeline.api.sekai;

import io.github.encryptorcode.httpclient.HTTPRequest;
import io.github.encryptorcode.httpclient.HTTPResponse;

import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SekaiUtil {
	public static final String game_api = "https://production-game-api.sekai.colorfulpalette.org/api";
	public static final String issue_api = "https://issue.sekai.colorfulpalette.org/api";
	public static String token = null;
	public static String cookie = null;

	private static void SetupHeaders(HttpURLConnection connection) throws IllegalAccessException {
		for (Field field : EnvironmentInfo.class.getDeclaredFields())
		{
			if (!(field.get(EnvironmentInfo.class) instanceof String))
				continue;

			connection.setRequestProperty(field.getName().replace("_", "-"), (String) field.get(EnvironmentInfo.class));
		}

		if (token != null)
			connection.setRequestProperty("X-Session-Token", token);

		connection.setRequestProperty("X-Request-Id", UUID.randomUUID().toString());

		if (cookie != null)
			connection.setRequestProperty("Cookie", cookie);
	}

	private static boolean AllIsZero(byte[] bytes)
	{
		for (byte b : bytes)
			if (b != 0)
				return false;
		return true;
	}

	public static HttpURLConnection CallSekai(String url, HTTPMethod method, String content) {
		byte[] querybytes = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			SetupHeaders(connection);
			connection.setRequestMethod(method.name().toUpperCase());
			if (method != HTTPMethod.GET) {
				try {
					if (content != null)
						querybytes = PackHelper.Pack(content);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			connection.connect();
			if(content != null) {
				if(querybytes != null) {
					try (OutputStream os = connection.getOutputStream()) {
						os.write(querybytes);
					}
				}
				else
				{
					try (OutputStream os = connection.getOutputStream()) {
						os.write(content.getBytes(StandardCharsets.UTF_8));
					}
				}
			}

			Map<String, List<String>> headers = connection.getHeaderFields();

			String nextToken = null;

			if(headers.containsKey("X-Session-Token")) {
				nextToken = headers.get("X-Session-Token").get(0);
			}

			if (nextToken != null) {
				token = nextToken;
			}

			connection.disconnect();
			return connection;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String CallApi(String apiurl, HTTPMethod method, String content) {
		String result = null;
		byte[] querybytes = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(game_api + apiurl).openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			SetupHeaders(connection);
			connection.setRequestMethod(method.name().toUpperCase());
			if (method != HTTPMethod.GET) {
				try {
					if (content != null)
						querybytes = PackHelper.Pack(content);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			connection.connect();
			if(content != null) {
				if(querybytes != null) {
					try (OutputStream os = connection.getOutputStream()) {
						os.write(querybytes);
					}
				}
				else
				{
					try (OutputStream os = connection.getOutputStream()) {
						os.write(content.getBytes(StandardCharsets.UTF_8));
					}
				}
			}

			if (connection.getResponseCode() != 200)
			{
				if (connection.getResponseCode() == 403)
				{
					if (cookie == null)
					{
						System.out.println("Cookie expired, refreshing... URL: " + game_api + apiurl);
						HttpURLConnection resp = CallSekai(issue_api + "/signature", HTTPMethod.POST, null);
						if (resp != null) {
							cookie = resp.getHeaderFields().get("Set-Cookie").get(0);
							return CallApi(apiurl, method, content);
						}
					}
				}
				throw new IOException("REQUEST ERROR! CODE: " + connection.getResponseCode() + " MESSAGE: " + connection.getResponseMessage());
			}

			InputStream is = (InputStream) connection.getContent();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			int buffer = 1024;
			byte[] b = new byte[buffer];
			int n;
			while ((n = is.read(b, 0, buffer)) > 0) {
				baos.write(b, 0, n);
			}
			if(AllIsZero(b))
			{
				is.close();
				baos.close();

				Map<String, List<String>> headers = connection.getHeaderFields();

				String nextToken = null;

				if(headers.containsKey("X-Session-Token")) {
					nextToken = headers.get("X-Session-Token").get(0);
				}

				if (nextToken != null) {
					token = nextToken;
				}

				connection.disconnect();

				System.out.println(game_api + apiurl + " Failed!");

				return "";
			}
			String s = PackHelper.Unpack(baos.toByteArray());
			is.close();
			baos.close();

			result = s;
			Map<String, List<String>> headers = connection.getHeaderFields();

			String nextToken = null;

			if(headers.containsKey("X-Session-Token")) {
				nextToken = headers.get("X-Session-Token").get(0);
			}

			if (nextToken != null) {
				token = nextToken;
			}

			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static String CallUserApi(String apiurl, String uid, HTTPMethod method, String content)
	{
		return CallApi("/user/" + uid + apiurl, method, content);
	}

	private static void SetupHeaders(HTTPRequest connection) throws IllegalAccessException {
		for (Field field : EnvironmentInfo.class.getDeclaredFields())
		{
			if (!(field.get(EnvironmentInfo.class) instanceof String))
				continue;

			connection.header(field.getName().replace("_", "-"), (String) field.get(EnvironmentInfo.class));
		}

		if (SekaiUtil.token != null)
			connection.header("X-Session-Token", SekaiUtil.token);

		connection.header("X-Request-Id", UUID.randomUUID().toString());

		if (cookie != null)
			connection.header("Cookie", cookie);
	}

	public static String CallUserApiNew(String apiurl, String uid, HTTPRequest.Method method, String content)
	{
		try
		{
			HTTPRequest request = new HTTPRequest(method, game_api + "/user/" + uid + apiurl);
			SetupHeaders(request);
			request.setJsonData(content);
			HTTPResponse response = request.getResponse();

			if (response.getResponseCode() != 200)
			{
				if (response.getResponseCode() == 403)
				{
					if (cookie == null)
					{
						System.out.println("Cookie expired, refreshing... URL: " + game_api + apiurl);
						HttpURLConnection resp = CallSekai(issue_api + "/signature", HTTPMethod.POST, null);
						if (resp != null)
						{
							cookie = resp.getHeaderFields().get("Set-Cookie").get(0);
							return CallUserApiNew(apiurl, uid, method, content);
						}
					}
				}
				throw new IOException("REQUEST ERROR! CODE: " + response.getResponseCode() + " MESSAGE: " + response.getResponseMessage());
			}

			String nextToken = null;

			if(response.getHeaders().get("X-Session-Token") != null && !Objects.requireNonNull(response.getHeaders().get("X-Session-Token")).isEmpty()) {
				nextToken = response.getHeaders().get("X-Session-Token");
			}

			if (nextToken != null) {
				token = nextToken;
			}

			return request.getResponse().getData();
		}
		catch (IOException | IllegalAccessException e)
		{
			e.printStackTrace();
		}

		return "";
	}
}
