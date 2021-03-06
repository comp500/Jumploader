package link.infra.jumploader.resolution.sources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import link.infra.jumploader.resolution.ResolvableJar;
import link.infra.jumploader.resolution.download.verification.SHA1HashingInputStream;
import link.infra.jumploader.util.RequestUtils;
import link.infra.jumploader.util.Side;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FabricJarSource implements ResolvableJarSource<FabricJarSource.FabricInvalidationKey> {
	public static class FabricInvalidationKey implements MetadataCacheHelper.InvalidationKey<FabricInvalidationKey> {
		public final String gameVersion;
		public final Side side;
		public final String pinnedFabricVersion;

		protected FabricInvalidationKey(String gameVersion, Side side, String pinnedFabricVersion) {
			this.gameVersion = gameVersion;
			this.side = side;
			this.pinnedFabricVersion = pinnedFabricVersion;
		}

		@Override
		public boolean isValid(FabricInvalidationKey key) {
			return equals(key) && key.pinnedFabricVersion != null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FabricInvalidationKey that = (FabricInvalidationKey) o;
			return Objects.equals(gameVersion, that.gameVersion) &&
				side == that.side &&
				Objects.equals(pinnedFabricVersion, that.pinnedFabricVersion);
		}

		@Override
		public int hashCode() {
			return Objects.hash(gameVersion, side, pinnedFabricVersion);
		}
	}

	private static class FabricLibraryJar {
		public final String mavenPath;
		public final URL source;
		public final String hash;

		private FabricLibraryJar(String mavenPath, URL source, String hash) {
			this.mavenPath = mavenPath;
			this.source = source;
			this.hash = hash;
		}
	}

	private static class FabricMetadata {
		public final String mainClass;
		public final List<FabricLibraryJar> libs = new ArrayList<>();

		private FabricMetadata(String mainClass) {
			this.mainClass = mainClass;
		}
	}

	private static final URI FABRIC_MAVEN;

	static {
		try {
			FABRIC_MAVEN = new URI("https://maven.fabricmc.net/");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MetadataResolutionResult resolve(MetadataCacheHelper.MetadataCacheView cacheView, ResolutionContext ctx) throws IOException {
		String gameVersion = ctx.getLoadingVersion();
		Side side = ctx.getLoadingSide();
		FabricMetadata meta;
		meta = cacheView.getObject("fabric.json", FabricMetadata.class, () -> {
			try {
				JsonObject latestLoaderData;
				if (ctx.getConfigFile().pinFabricLoaderVersion != null) {
					URL loaderJsonUrl = new URI("https", "meta.fabricmc.net", "/v2/versions/loader/" + gameVersion + "/" + ctx.getConfigFile().pinFabricLoaderVersion, null).toURL();
					latestLoaderData = RequestUtils.getJson(loaderJsonUrl).getAsJsonObject();
				} else {
					URL loaderJsonUrl = new URI("https", "meta.fabricmc.net", "/v2/versions/loader/" + gameVersion, null).toURL();
					JsonArray manifestData = RequestUtils.getJson(loaderJsonUrl).getAsJsonArray();
					if (manifestData.size() == 0) {
						throw new IOException("Failed to update configuration: no Fabric versions available!");
					}
					latestLoaderData = manifestData.get(0).getAsJsonObject();
				}

				if (ctx.getConfigFile().pinFabricLoaderVersion == null) {
					ctx.getConfigFile().pinFabricLoaderVersion = latestLoaderData.getAsJsonObject("loader").get("version").getAsString();
					ctx.getConfigFile().save();
				}

				JsonObject launcherMeta = latestLoaderData.getAsJsonObject("launcherMeta");
				JsonObject mainClass = launcherMeta.getAsJsonObject("mainClass");

				FabricMetadata newMetadata = new FabricMetadata(mainClass.get(side.name).getAsString());

				String loaderMaven = latestLoaderData.getAsJsonObject("loader").get("maven").getAsString();
				URL loaderMavenUrl = RequestUtils.resolveMavenPath(FABRIC_MAVEN, loaderMaven).toURL();
				newMetadata.libs.add(new FabricLibraryJar(loaderMaven, loaderMavenUrl, RequestUtils.getSha1Hash(loaderMavenUrl)));
				String intermediaryMaven = latestLoaderData.getAsJsonObject("intermediary").get("maven").getAsString();
				URL intermediaryMavenUrl = RequestUtils.resolveMavenPath(FABRIC_MAVEN, intermediaryMaven).toURL();
				newMetadata.libs.add(new FabricLibraryJar(intermediaryMaven, intermediaryMavenUrl, RequestUtils.getSha1Hash(intermediaryMavenUrl)));

				JsonObject libraries = launcherMeta.getAsJsonObject("libraries");
				for (JsonElement library : libraries.getAsJsonArray("common")) {
					JsonObject libraryObj = library.getAsJsonObject();
					URL libUrl = RequestUtils.resolveMavenPath(new URI(libraryObj.get("url").getAsString()), libraryObj.get("name").getAsString()).toURL();
					newMetadata.libs.add(new FabricLibraryJar(
						libraryObj.get("name").getAsString(),
						libUrl,
						RequestUtils.getSha1Hash(libUrl)
					));
				}
				for (JsonElement library : libraries.getAsJsonArray(side.name)) {
					JsonObject libraryObj = library.getAsJsonObject();
					// Special-case guava on server ("jimfs in fabric-server-launch requires guava on the system classloader")
					// We already work around this behaviour, and we can't get it on the system classloader directly anyway
					if (side == Side.SERVER && libraryObj.get("name").getAsString().contains("com.google.guava:guava")) {
						continue;
					}
					URL libUrl = RequestUtils.resolveMavenPath(new URI(libraryObj.get("url").getAsString()), libraryObj.get("name").getAsString()).toURL();
					newMetadata.libs.add(new FabricLibraryJar(
						libraryObj.get("name").getAsString(),
						libUrl,
						RequestUtils.getSha1Hash(libUrl)
					));
				}

				return newMetadata;
			} catch (URISyntaxException e) {
				throw new IOException("Failed to parse Fabric source URL", e);
			}
		});
		cacheView.completeUpdate();

		List<ResolvableJar> jars = new ArrayList<>();
		for (FabricLibraryJar libraryJar : meta.libs) {
			jars.add(new ResolvableJar(libraryJar.source,
				ctx.getEnvironment().jarStorage.getLibraryMaven(libraryJar.mavenPath),
				SHA1HashingInputStream.verifier(libraryJar.hash, libraryJar.source.toString()), "Fabric library " + libraryJar.source));
		}

		return new MetadataResolutionResult(jars, meta.mainClass);
	}

	@Override
	public Class<FabricInvalidationKey> getInvalidationKeyType() {
		return FabricInvalidationKey.class;
	}

	@Override
	public FabricInvalidationKey getInvalidationKey(ResolutionContext ctx) {
		return new FabricInvalidationKey(ctx.getLoadingVersion(), ctx.getLoadingSide(), ctx.getConfigFile().pinFabricLoaderVersion);
	}
}
