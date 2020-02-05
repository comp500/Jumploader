package link.infra.jumploader.reflectionhacks;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public interface ReflectionHack {
	void applyHack(ClassLoader loadingClassloader);
	boolean hackApplies(URL[] loadedJars);

	List<ReflectionHack> HACKS = Collections.singletonList(new FabricLoaderReflectionHack());
}
