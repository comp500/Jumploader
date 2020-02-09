package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;

import java.net.URL;
import java.util.List;

public interface ArgumentsModifier extends SpecialCase {
	void modifyArguments(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments);
}
