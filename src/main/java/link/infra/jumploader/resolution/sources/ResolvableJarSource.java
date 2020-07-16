package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.ConfigFile;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;
import link.infra.jumploader.resolution.resources.ResolvableJar;

import java.util.List;

public interface ResolvableJarSource<T extends MetadataCacheHelper.InvalidationKey<T>> {
	List<ResolvableJar> getJars(MetadataCacheHelper.MetadataCacheView cache, ResolutionContext ctx);
	Class<T> getInvalidationKeyType();
	T getInvalidationKey(ResolutionContext ctx);
}
