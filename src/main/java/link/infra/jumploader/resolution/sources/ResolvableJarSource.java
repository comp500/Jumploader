package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.resolution.ResolvableJar;

import java.io.IOException;
import java.util.List;

public interface ResolvableJarSource<T extends MetadataCacheHelper.InvalidationKey<T>> {
	List<ResolvableJar> getJars(MetadataCacheHelper.MetadataCacheView cache, ResolutionContext ctx) throws IOException;
	Class<T> getInvalidationKeyType();
	T getInvalidationKey(ResolutionContext ctx);
}
