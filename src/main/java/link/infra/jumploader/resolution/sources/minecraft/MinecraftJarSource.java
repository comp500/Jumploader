package link.infra.jumploader.resolution.sources.minecraft;

import link.infra.jumploader.ConfigFile;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;
import link.infra.jumploader.resolution.resources.ResolvableJar;
import link.infra.jumploader.resolution.sources.MetadataCacheHelper;
import link.infra.jumploader.resolution.sources.ResolutionContext;
import link.infra.jumploader.resolution.sources.ResolvableJarSource;

import java.util.List;

public class MinecraftJarSource implements ResolvableJarSource<MinecraftJarSource.MinecraftInvalidationKey> {
	public static class MinecraftInvalidationKey implements MetadataCacheHelper.InvalidationKey<MinecraftInvalidationKey> {
		public final String gameVersion;
		public final String side;
		public final String os;

		protected MinecraftInvalidationKey(String gameVersion, String side, String os) {
			this.gameVersion = gameVersion;
			this.side = side;
			this.os = os;
		}

		@Override
		public boolean isValid(MinecraftInvalidationKey key) {
			return equals(key);
		}
	}

	@Override
	public List<ResolvableJar> getJars(MetadataCacheHelper.MetadataCacheView cache, ResolutionContext ctx) {
		// TODO: implement
		return null;
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
