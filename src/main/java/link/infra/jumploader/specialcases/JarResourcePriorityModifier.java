package link.infra.jumploader.specialcases;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public interface JarResourcePriorityModifier extends SpecialCase {
	boolean shouldPrioritiseResource(String resourceName);
	void modifyPriorities(List<URL> resourceList) throws IOException;
}
