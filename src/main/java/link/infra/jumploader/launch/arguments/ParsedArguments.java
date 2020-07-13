package link.infra.jumploader.launch.arguments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class ParsedArguments {
	private final Map<String, String> argumentsParsed = new HashMap<>();
	private final List<String> argumentsList;
	private static final Logger LOGGER = LogManager.getLogger();

	public final String mcVersion;
	public final Path gameDir;

	public final String accessToken;

	public final int windowWidth;
	public final int windowHeight;

	public final String inferredSide;
	public final boolean nogui;

	// TODO: use proxy arguments?

	public ParsedArguments(String[] args) {
		argumentsList = new ArrayList<>(Arrays.asList(args));
		List<String> unparsedArguments = new ArrayList<>();
		boolean noguiTemp = false;
		for (int i = 0; i < args.length; i++) {
			if (i < (args.length - 1) && args[i].startsWith("--")) {
				argumentsParsed.put(args[i].substring(2), args[i + 1]);
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

		removeFMLArgs(argumentsList);
		replaceTwitchBrand(getOrDefault("version", ""), argumentsList, gameDir);
	}

	private static void removeFMLArgs(List<String> argsList) {
		Iterator<String> iter = argsList.iterator();
		String peek = null;
		while (iter.hasNext() || peek != null) {
			String arg = peek != null ? peek : iter.next();
			if (arg.startsWith("--fml") || arg.equals("--launchTarget")) {
				iter.remove();
				peek = iter.next();
				// If the next argument starts with -- we should parse it as a separate argument,
				// otherwise remove it as the value for this argument
				if (!peek.startsWith("--")) {
					iter.remove();
					peek = null;
				}
			}
		}
	}

	/**
	 * In a Twitch environment, the game version is usually forge-... which looks confusing to users, as
	 * it shows up as forge-.../fabric/Fabric in the F3 menu. Here, we replace it with Jumploader.
	 */
	private static void replaceTwitchBrand(String version, List<String> argsList, Path gameDir) {
		// Detect if the version name is a Forge version
		if (!Pattern.matches("forge-[.\\d]+", version)) {
			return;
		}
		// Detect if we are in a Twitch environment
		Path installDir = gameDir.resolve(Paths.get("..", "..", "Install"));
		if (Files.exists(installDir)) {
			Path gameVersionsDir = installDir.resolve("versions");
			Path librariesDir = installDir.resolve("libraries");
			if (!Files.exists(gameVersionsDir) || !Files.exists(librariesDir)) {
				return;
			}
		} else {
			return;
		}
		// Replace the version with Jumploader
		for (int i = 0; i < argsList.size(); i++) {
			if (argsList.get(i).equals("--version")) {
				i++;
				if (i < argsList.size()) {
					argsList.set(i, "Jumploader");
				}
			}
		}
	}

	public String get(String key) {
		return argumentsParsed.get(key);
	}

	public String getOrDefault(String key, String def) {
		return argumentsParsed.getOrDefault(key, def);
	}

	public Path getPath(String key) {
		return getPathOrDefault(key, null);
	}

	public Path getPathOrDefault(String key, Path def) {
		String value = argumentsParsed.get(key);
		if (value == null) {
			return def;
		}
		return Paths.get(value);
	}

	public int getInt(String key) {
		return getIntOrDefault(key, 0);
	}

	public int getIntOrDefault(String key, int def) {
		try {
			String value = argumentsParsed.get(key);
			if (value == null) {
				return def;
			}
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public String[] getArgsArray() {
		return argumentsList.toArray(new String[0]);
	}
}
