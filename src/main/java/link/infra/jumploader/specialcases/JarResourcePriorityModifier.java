package link.infra.jumploader.specialcases;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public interface JarResourcePriorityModifier extends SpecialCase {
	boolean shouldApplyClass(String className);
	void apply(List<URL> resourceList) throws IOException;

	List<JarResourcePriorityModifier> MODIFIERS = Collections.singletonList(new UnpatchedGameResourcePriorityModifier());
}
