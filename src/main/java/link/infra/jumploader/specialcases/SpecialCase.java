package link.infra.jumploader.specialcases;

import java.net.URL;
import java.util.List;

public interface SpecialCase {
	boolean shouldApply(List<URL> loadedJars, String mainClass);
}
