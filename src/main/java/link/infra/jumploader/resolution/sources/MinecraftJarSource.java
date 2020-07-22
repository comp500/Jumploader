package link.infra.jumploader.resolution.sources;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import link.infra.jumploader.resolution.ResolvableJar;
import link.infra.jumploader.resolution.download.PreDownloadCheck;
import link.infra.jumploader.resolution.download.verification.SHA1HashingInputStream;
import link.infra.jumploader.util.RequestUtils;
import link.infra.jumploader.util.Side;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MinecraftJarSource implements ResolvableJarSource<MinecraftJarSource.MinecraftInvalidationKey> {
	public static class MinecraftInvalidationKey implements MetadataCacheHelper.InvalidationKey<MinecraftInvalidationKey> {
		public final String gameVersion;
		public final Side side;
		public final String os;

		protected MinecraftInvalidationKey(String gameVersion, Side side, String os) {
			this.gameVersion = gameVersion;
			this.side = side;
			this.os = os;
		}

		@Override
		public boolean isValid(MinecraftInvalidationKey key) {
			return equals(key);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MinecraftInvalidationKey that = (MinecraftInvalidationKey) o;
			return gameVersion.equals(that.gameVersion) &&
				side == that.side &&
				os.equals(that.os);
		}

		@Override
		public int hashCode() {
			return Objects.hash(gameVersion, side, os);
		}
	}

	private static class MinecraftGameJar {
		public final String version;
		public final URL source;
		public final String hash;
		public final Side side;

		private MinecraftGameJar(String version, URL source, String hash, Side side) {
			this.version = version;
			this.source = source;
			this.hash = hash;
			this.side = side;
		}
	}

	private static class MinecraftLibraryJar {
		public final String mavenPath;
		public final URL source;
		public final String hash;

		private MinecraftLibraryJar(String mavenPath, URL source, String hash) {
			this.mavenPath = mavenPath;
			this.source = source;
			this.hash = hash;
		}
	}

	private static class MinecraftMetadata {
		public final MinecraftGameJar gameJar;
		public final String mainClassClient;
		public final List<MinecraftLibraryJar> libs = new ArrayList<>();

		private MinecraftMetadata(MinecraftGameJar gameJar, String mainClassClient) {
			this.gameJar = gameJar;
			this.mainClassClient = mainClassClient;
		}
	}

	public static URL retrieveVersionMetaUrl(String minecraftVersion) throws IOException {
		JsonObject manifestData = RequestUtils.getJson(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")).getAsJsonObject();
		JsonArray versions = manifestData.getAsJsonArray("versions");
		for (JsonElement version : versions) {
			JsonObject versionObj = version.getAsJsonObject();
			if (minecraftVersion.equals(versionObj.get("id").getAsString())) {
				return new URL(versionObj.get("url").getAsString());
			}
		}
		throw new IOException("Invalid Minecraft version, not found in manifest");
	}

	private static class Rule {
		String action;
		Os os;

		private static class Os {
			String name;
		}
	}

	@Override
	public MetadataResolutionResult resolve(MetadataCacheHelper.MetadataCacheView cacheView, ResolutionContext ctx) throws IOException {
		String gameVersion = ctx.getLoadingVersion();
		Side side = ctx.getLoadingSide();
		MinecraftMetadata meta = cacheView.getObject("minecraft.json", MinecraftMetadata.class, () -> {
			URL versionMetaUrl = retrieveVersionMetaUrl(gameVersion);
			JsonObject manifestData = RequestUtils.getJson(versionMetaUrl).getAsJsonObject();
			JsonObject downloads = manifestData.getAsJsonObject("downloads");

			JsonObject download = downloads.getAsJsonObject(side.name);
			MinecraftGameJar jar = new MinecraftGameJar(gameVersion,
				new URL(download.get("url").getAsString()), download.get("sha1").getAsString(), side);

			Gson gson = new Gson();
			String currentOS = ctx.getEnvironment().os;

			MinecraftMetadata newMetadata = new MinecraftMetadata(jar, manifestData.get("mainClass").getAsString());
			for (JsonElement lib : manifestData.getAsJsonArray("libraries")) {
				JsonObject libObj = lib.getAsJsonObject();

				JsonElement rulesEl = libObj.get("rules");
				if (rulesEl != null) {
					Type rulesListType = new TypeToken<ArrayList<Rule>>() {}.getType();
					List<Rule> rules = gson.fromJson(rulesEl, rulesListType);
					Collections.reverse(rules);
					boolean isAllowed = false;
					for (Rule rule : rules) {
						if (rule.os != null) {
							if (!currentOS.equals(rule.os.name)) {
								continue;
							}
						}
						if (rule.action.equals("disallow")) {
							break;
						}
						if (rule.action.equals("allow")) {
							isAllowed = true;
							break;
						}
					}
					if (!isAllowed) {
						continue;
					}
				}

				JsonObject downloadsObj = libObj.getAsJsonObject("downloads");
				JsonObject nativesObj = libObj.getAsJsonObject("natives");
				if (nativesObj != null) {
					// TODO: should natives be ignored anyway? - we don't/can't handle them properly (see wiki)
					String nativesClassifier = nativesObj.get(currentOS).getAsString();
					JsonObject nativesDownloadObj = downloadsObj.getAsJsonObject("classifiers").getAsJsonObject(nativesClassifier);
					if (nativesDownloadObj == null) {
						throw new RuntimeException("No natives available in " + downloadsObj + " classifier " + nativesClassifier);
					}
					newMetadata.libs.add(new MinecraftLibraryJar(libObj.get("name").getAsString() + ":" + nativesClassifier,
						new URL(nativesDownloadObj.get("url").getAsString()), nativesDownloadObj.get("sha1").getAsString()));
					continue;
				}

				JsonObject artifactObj = downloadsObj.getAsJsonObject("artifact");
				newMetadata.libs.add(new MinecraftLibraryJar(libObj.get("name").getAsString(),
					new URL(artifactObj.get("url").getAsString()), artifactObj.get("sha1").getAsString()));
			}

			return newMetadata;
		});
		cacheView.completeUpdate();

		List<ResolvableJar> jars = new ArrayList<>();
		jars.add(new ResolvableJar(meta.gameJar.source,
			ctx.getEnvironment().jarStorage.getGameJar(meta.gameJar.version, meta.gameJar.side),
			SHA1HashingInputStream.verifier(meta.gameJar.hash, meta.gameJar.source.toString()),
			() -> {
				if (side == Side.CLIENT) {
					try {
						JsonObject req = new JsonObject();
						req.addProperty("accessToken", ctx.getArguments().accessToken);
						int resCode = RequestUtils.postJsonForResCode(new URL("https://authserver.mojang.com/validate"), req);
						if (resCode != 204 && resCode != 200) {
							throw new PreDownloadCheck.PreDownloadCheckException("Authentication token is invalid, please go online to download the Minecraft JAR!");
						}
					} catch (IOException e) {
						throw new PreDownloadCheck.PreDownloadCheckException("Failed to check authentication, please go online to download the Minecraft JAR!", e);
					}
				}
			}, "Minecraft " + side + " " + gameVersion));
		for (MinecraftLibraryJar libraryJar : meta.libs) {
			jars.add(new ResolvableJar(libraryJar.source,
				ctx.getEnvironment().jarStorage.getLibraryMaven(libraryJar.mavenPath),
				SHA1HashingInputStream.verifier(libraryJar.hash, libraryJar.source.toString()), "Minecraft library " + libraryJar.mavenPath));
		}

		if (side == Side.SERVER) {
			return new MetadataResolutionResult(jars, "net.minecraft.server.Main");
		} else {
			return new MetadataResolutionResult(jars, meta.mainClassClient);
		}
	}

	@Override
	public Class<MinecraftInvalidationKey> getInvalidationKeyType() {
		return MinecraftInvalidationKey.class;
	}

	@Override
	public MinecraftInvalidationKey getInvalidationKey(ResolutionContext ctx) {
		return new MinecraftInvalidationKey(ctx.getLoadingVersion(), ctx.getLoadingSide(), ctx.getEnvironment().os);
	}
}
