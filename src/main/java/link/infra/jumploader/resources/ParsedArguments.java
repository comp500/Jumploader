package link.infra.jumploader.resources;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ParsedArguments {
	public final Map<String, String> arguments = new HashMap<>();
	private static Logger LOGGER = LogManager.getLogger();

	public final String mcVersion;
	public final Path gameDir;

	public final String accessToken;

	public final int windowWidth;
	public final int windowHeight;

	public final String inferredSide;
	public final boolean nogui;

	public ParsedArguments(String[] args) {
		List<String> unparsedArguments = new ArrayList<>();
		boolean noguiTemp = false;
		for (int i = 0; i < args.length; i++) {
			if (i < (args.length - 1) && args[i].startsWith("--")) {
				arguments.put(args[i].substring(2), args[i + 1]);
				i++;
			} else if (args[i].equalsIgnoreCase("nogui")) {
				noguiTemp = true;
			} else {
				unparsedArguments.add(args[i]);
			}
		}
		nogui = noguiTemp;
		if (unparsedArguments.size() > 0) {
			LOGGER.warn("Found unparsed arguments: " + Arrays.toString(unparsedArguments.toArray()));
		}

		mcVersion = get("fml.mcVersion");
		gameDir = getPathOrDefault("gameDir", Paths.get("."));
		accessToken = get("accessToken");
		windowWidth = getIntOrDefault("width", 854);
		windowHeight = getIntOrDefault("height", 480);
		if (getOrDefault("launchTarget", "").contains("server")) {
			inferredSide = "server";
		} else {
			inferredSide = "client";
		}
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
				return def;
			}
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public String[] getArgsArray() {
		List<String> argsList = new ArrayList<>();
		if (nogui) {
			argsList.add("nogui");
		}
		for (Map.Entry<String, String> entry : arguments.entrySet()) {
			argsList.add("--" + entry.getKey());
			argsList.add(entry.getValue());
		}
		return argsList.toArray(new String[0]);
	}
}
