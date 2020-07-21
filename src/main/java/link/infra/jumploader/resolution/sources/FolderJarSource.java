package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.resolution.ResolvableJar;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class FolderJarSource implements ResolvableJarSource<FolderJarSource.FolderInvalidationKey> {
	public static class FolderInvalidationKey implements MetadataCacheHelper.InvalidationKey<FolderInvalidationKey> {
		@Override
		public boolean isValid(FolderInvalidationKey key) {
			return true;
		}
	}

	@Override
	public MetadataResolutionResult resolve(MetadataCacheHelper.MetadataCacheView cache, ResolutionContext ctx) throws IOException {
		return new MetadataResolutionResult(Files.walk(ctx.getArguments().gameDir.resolve(ctx.getConfigFile().loadJarsFromFolder))
			.filter(path -> path.endsWith(".jar"))
			.map(path -> new ResolvableJar(path, "File " + path)).collect(Collectors.toList()), null);
	}

	@Override
	public Class<FolderInvalidationKey> getInvalidationKeyType() {
		return FolderInvalidationKey.class;
	}

	@Override
	public FolderInvalidationKey getInvalidationKey(ResolutionContext ctx) {
		return new FolderInvalidationKey();
	}
}
