package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public interface ArgumentsModifier extends SpecialCase {
	void apply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments);

	List<ArgumentsModifier> MODIFIERS = Collections.singletonList(new ServerSideRemoveFMLArgs());
}
