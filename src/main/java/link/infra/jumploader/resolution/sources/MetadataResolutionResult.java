package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.resolution.ResolvableJar;

import java.util.List;

public class MetadataResolutionResult {
	public final List<ResolvableJar> jars;
	public final String mainClass;

	public MetadataResolutionResult(List<ResolvableJar> jars, String mainClass) {
		this.jars = jars;
		this.mainClass = mainClass;
	}
}
