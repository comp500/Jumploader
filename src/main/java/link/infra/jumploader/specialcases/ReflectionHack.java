package link.infra.jumploader.specialcases;

import java.util.Collections;
import java.util.List;

public interface ReflectionHack extends SpecialCase {
	void applyHack(ClassLoader loadingClassloader);

	List<ReflectionHack> HACKS = Collections.singletonList(new FabricLoaderReflectionHack());
}
