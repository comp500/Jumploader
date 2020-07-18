package link.infra.jumploader.resolution.sources;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourcesRegistry {
	private static final Map<String, ResolvableJarSource<?>> sources = new HashMap<>();
	static {
		sources.put("minecraft", new MinecraftJarSource());
		sources.put("fabric", new FabricJarSource());
		sources.put("folder", new FolderJarSource());
	}

	public static ResolvableJarSource<?> getSource(String sourceId) {
		return sources.get(sourceId);
	}

	public static List<String> getDefaultSources() {
		return Arrays.asList("minecraft", "fabric");
	}
}
