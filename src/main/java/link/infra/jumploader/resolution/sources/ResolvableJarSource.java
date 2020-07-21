package link.infra.jumploader.resolution.sources;

import java.io.IOException;

public interface ResolvableJarSource<T extends MetadataCacheHelper.InvalidationKey<T>> {
	MetadataResolutionResult resolve(MetadataCacheHelper.MetadataCacheView cache, ResolutionContext ctx) throws IOException;
	Class<T> getInvalidationKeyType();
	T getInvalidationKey(ResolutionContext ctx);
}
