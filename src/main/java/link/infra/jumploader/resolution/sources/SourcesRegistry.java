package link.infra.jumploader.resolution.sources;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourcesRegistry {
	private static final Map<String, ResolvableJarSource<?>> sources = new HashMap<>();
	static {
		sources.put("minecraft", null);
		sources.put("fabric", null);
	}

	public static ResolvableJarSource<?> getSource(String sourceId) {
		return sources.get(sourceId);
	}

	public static List<String> getDefaultSources() {
		return Arrays.asList(null, null);
	}
}
