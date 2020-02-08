package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;

import java.net.URL;
import java.util.List;

public interface SpecialCase {
	boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments);
}
