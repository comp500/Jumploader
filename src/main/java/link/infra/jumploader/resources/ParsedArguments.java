package link.infra.jumploader.resources;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ParsedArguments {
	public final Map<String, String> arguments;

	public final String mcVersion;
	public final Path gameDir;

	public final String accessToken;

	public final int windowWidth;
	public final int windowHeight;

	public ParsedArguments(String[] args) {
		Map<String, String> parsedArgs = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			if (i < (args.length - 1) && args[i].startsWith("--")) {
				parsedArgs.put(args[i].substring(2), args[i+1]);
				i++;
			}
		}
		arguments = Collections.unmodifiableMap(parsedArgs);

		mcVersion = get("fml.mcVersion");
		gameDir = getPathOrDefault("gameDir", Paths.get("."));
		accessToken = get("accessToken");
		windowWidth = getIntOrDefault("width", 854);
		windowHeight = getIntOrDefault("height", 480);
	}

	public String get(String key) {
		return arguments.get(key);
	}

	public String getOrDefault(String key, String def) {
		return arguments.getOrDefault(key, def);
	}

	public Path getPath(String key) {
		String value = arguments.get(key);
		if (value == null) {
			return null;
		}
		return Paths.get(value);
	}

	public Path getPathOrDefault(String key, Path def) {
		String value = arguments.get(key);
		if (value == null) {
			return def;
		}
		return Paths.get(value);
	}

	public int getInt(String key) {
		try {
			String value = arguments.get(key);
			if (value == null) {
				return 0;
			}
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public int getIntOrDefault(String key, int def) {
		try {
			String value = arguments.get(key);
			if (value == null) {
				return 0;
			}
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
