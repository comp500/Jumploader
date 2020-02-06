package link.infra.jumploader.meta;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import link.infra.jumploader.util.RequestJson;

import java.io.IOException;
import java.net.URL;

public class MinecraftDownloadApi {
	private MinecraftDownloadApi() {}

	public static class LoginValidationException extends IOException {
		private LoginValidationException() {
			super("Authentication token is invalid, please go online to download the Minecraft JAR!");
		}
	}

	public static void validate(String accessToken) throws IOException {
		JsonObject req = new JsonObject();
		req.addProperty("accessToken", accessToken);
		int resCode = RequestJson.postJsonForResCode(new URL("https://authserver.mojang.com/validate"), req);
		if (resCode != 204 && resCode != 200) {
			throw new LoginValidationException();
		}
	}

	public static URL retrieveVersionMetaUrl(String minecraftVersion) throws IOException {
		JsonObject manifestData = RequestJson.getJson(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")).getAsJsonObject();
		JsonArray versions = manifestData.getAsJsonArray("versions");
		for (JsonElement version : versions) {
			JsonObject versionObj = version.getAsJsonObject();
			if (minecraftVersion.equals(versionObj.get("id").getAsString())) {
				return new URL(versionObj.get("url").getAsString());
			}
		}
		throw new RuntimeException("Invalid Minecraft version, not found in manifest");
	}

	public static class DownloadDetails {
		private DownloadDetails() {}

		public URL url;
		public String sha1;
	}

	public static DownloadDetails retrieveDownloadDetails(URL versionMetaUrl, String downloadType) throws IOException {
		JsonObject manifestData = RequestJson.getJson(versionMetaUrl).getAsJsonObject();
		JsonObject downloads = manifestData.getAsJsonObject("downloads");
		JsonObject download = downloads.getAsJsonObject(downloadType);
		Gson gson = new Gson();
		return gson.fromJson(download, DownloadDetails.class);
	}
}
