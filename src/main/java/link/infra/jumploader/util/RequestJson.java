package link.infra.jumploader.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import link.infra.jumploader.Jumploader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class RequestJson {
	private RequestJson() {}

	public static JsonElement getJson(URL requestUrl) throws IOException {
		URLConnection conn = requestUrl.openConnection();
		conn.setRequestProperty("User-Agent", Jumploader.USER_AGENT);
		conn.setRequestProperty("Accept", "application/json");

		try (InputStream res = conn.getInputStream(); InputStreamReader isr = new InputStreamReader(res)) {
			JsonParser parser = new JsonParser();
			return parser.parse(isr);
		}
	}

	public static int postJsonForResCode(URL requestUrl, JsonElement requestData) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("User-Agent", Jumploader.USER_AGENT);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");

		try (OutputStream req = conn.getOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(req)) {
			Gson gson = new Gson();
			gson.toJson(requestData, writer);
		}

		return conn.getResponseCode();
	}

}
