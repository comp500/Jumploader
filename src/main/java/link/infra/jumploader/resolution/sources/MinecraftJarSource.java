package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.resolution.ResolvableJar;
import link.infra.jumploader.resolution.download.verification.SHA1HashingInputStream;
import link.infra.jumploader.util.Side;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
		public final List<MinecraftLibraryJar> libs = new ArrayList<>();

		private MinecraftMetadata(MinecraftGameJar gameJar) {
			this.gameJar = gameJar;
		}
	}

	@Override
	public List<ResolvableJar> getJars(MetadataCacheHelper.MetadataCacheView cacheView, ResolutionContext ctx) throws IOException {
		String gameVersion = ctx.getLoadingVersion();
		Side side = ctx.getLoadingSide();
		MinecraftMetadata meta = ((MetadataCacheHelper.GsonMetadataCacheView) cacheView)
			.getObject("minecraft.json", MinecraftMetadata.class, () -> {
				URL versionMetaUrl = MinecraftDownloadApi.retrieveVersionMetaUrl(gameVersion);
				// TODO: also grab libraries
				MinecraftDownloadApi.DownloadDetails details = MinecraftDownloadApi.retrieveDownloadDetails(versionMetaUrl, side.name);

				MinecraftGameJar jar = new MinecraftGameJar(gameVersion, details.url, details.sha1, side);
				MinecraftMetadata newMetadata = new MinecraftMetadata(jar);

				return newMetadata;
			});
		cacheView.completeUpdate();

		List<ResolvableJar> jars = new ArrayList<>();
		jars.add(new ResolvableJar(meta.gameJar.source,
			ctx.getEnvironment().jarStorage.getGameJar(gameVersion, side),
			SHA1HashingInputStream.verifier(meta.gameJar.hash),
			() -> {
				// TODO: pass up message
				if (side == Side.CLIENT) {
					try {
						MinecraftDownloadApi.validate(ctx.getArguments().accessToken);
					} catch (IOException e) {
						return false;
					}
				}
				return true;
			}, "Minecraft " + side + " " + gameVersion));
		for (MinecraftLibraryJar libraryJar : meta.libs) {
			jars.add(new ResolvableJar(libraryJar.source,
				ctx.getEnvironment().jarStorage.getLibraryMaven(libraryJar.mavenPath),
				SHA1HashingInputStream.verifier(libraryJar.hash), "Minecraft library " + libraryJar.mavenPath));
		}

		return jars;
	}

	@Override
	public Class<MinecraftInvalidationKey> getInvalidationKeyType() {
		return MinecraftInvalidationKey.class;
	}

	@Override
	public MinecraftInvalidationKey getInvalidationKey(ResolutionContext ctx) {
		// TODO: os detect
		return new MinecraftInvalidationKey(ctx.getLoadingVersion(), ctx.getLoadingSide(), "");
	}
}
