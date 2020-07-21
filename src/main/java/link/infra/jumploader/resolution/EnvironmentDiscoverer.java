package link.infra.jumploader.resolution;

import link.infra.jumploader.launch.arguments.ParsedArguments;
import link.infra.jumploader.util.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class EnvironmentDiscoverer {
	public final Path configFile;
	public JarStorageLocation jarStorage;
	private final Path gameDir;
	public final String os;

	private final Logger LOGGER = LogManager.getLogger();

	public static class UnsupportedLocationException extends Exception {}

	public interface JarStorageLocation {
		Path getLibraryMaven(String mavenPath);
		Path getGameJar(String gameVersion, Side side);
		boolean compatibleWithSide(Side side);
	}

	private static Path resolveMavenPathOnDisk(Path baseDir, String mavenPath) {
		String[] mavenPathSplit = mavenPath.split(":");
		if (mavenPathSplit.length != 3 && mavenPathSplit.length != 4) {
			throw new RuntimeException("Invalid maven path: " + mavenPath);
		}
		String classifierPart = mavenPathSplit.length == 3 ? "" : "-" + mavenPathSplit[3];
		return baseDir
			.resolve(Paths.get(".", mavenPathSplit[0].split("\\."))) // Group ID
			.resolve(mavenPathSplit[1]) // Artifact ID
			.resolve(mavenPathSplit[2]) // Version
			.resolve(mavenPathSplit[1] + "-" + mavenPathSplit[2] + classifierPart + ".jar");
	}

	private static class FallbackJarStorage implements JarStorageLocation {
		private final Path gameVersionsDir;
		private final Path librariesDir;

		public FallbackJarStorage(Path gameDir) {
			Path localDir = gameDir.resolve(".jumploader");
			try {
				Files.createDirectories(localDir);
				gameVersionsDir = localDir.resolve("versions");
				Files.createDirectories(gameVersionsDir);
				librariesDir = localDir.resolve("libraries");
				Files.createDirectories(librariesDir);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create a folder for Jumploader", e);
			}
		}

		@Override
		public Path getLibraryMaven(String mavenPath) {
			return resolveMavenPathOnDisk(librariesDir, mavenPath);
		}

		@Override
		public Path getGameJar(String gameVersion, Side side) {
			return gameVersionsDir.resolve(side.name).resolve(gameVersion + ".jar");
		}

		@Override
		public boolean compatibleWithSide(Side side) {
			return true;
		}
	}

	private static class VanillaJarStorage implements JarStorageLocation {
		private final Path gameVersionsDir;
		private final Path librariesDir;

		public VanillaJarStorage(Path gameDir) throws UnsupportedLocationException {
			gameVersionsDir = gameDir.resolve("versions");
			librariesDir = gameDir.resolve("libraries");
			if (!Files.exists(gameVersionsDir) || !Files.exists(librariesDir)) {
				throw new UnsupportedLocationException();
			}
		}

		@Override
		public Path getLibraryMaven(String mavenPath) {
			return resolveMavenPathOnDisk(librariesDir, mavenPath);
		}

		@Override
		public Path getGameJar(String gameVersion, Side side) {
			if (side != Side.CLIENT) {
				throw new RuntimeException("VanillaJarStorage doesn't support non-clientside, something has gone wrong!");
			}
			return gameVersionsDir.resolve(Paths.get(gameVersion, gameVersion + ".jar"));
		}

		@Override
		public boolean compatibleWithSide(Side side) {
			return side == Side.CLIENT;
		}
	}

	private static class TwitchJarStorage implements JarStorageLocation {
		private final Path gameVersionsDir;
		private final Path librariesDir;

		public TwitchJarStorage(Path gameDir) throws UnsupportedLocationException {
			Path installDir = gameDir.resolve(Paths.get("..", "..", "Install"));
			if (Files.exists(installDir)) {
				gameVersionsDir = installDir.resolve("versions");
				librariesDir = installDir.resolve("libraries");
				if (!Files.exists(gameVersionsDir) || !Files.exists(librariesDir)) {
					throw new UnsupportedLocationException();
				}
			} else {
				throw new UnsupportedLocationException();
			}
		}

		@Override
		public Path getLibraryMaven(String mavenPath) {
			return resolveMavenPathOnDisk(librariesDir, mavenPath);
		}

		@Override
		public Path getGameJar(String gameVersion, Side side) {
			if (side != Side.CLIENT) {
				throw new RuntimeException("TwitchJarStorage doesn't support non-clientside, something has gone wrong!");
			}
			return gameVersionsDir.resolve(Paths.get(gameVersion, gameVersion + ".jar"));
		}

		@Override
		public boolean compatibleWithSide(Side side) {
			return side == Side.CLIENT;
		}
	}

	public interface JarStorageLocationConstructor<T extends JarStorageLocation> {
		T construct(Path gameDir) throws UnsupportedLocationException;
	}

	private static final List<JarStorageLocationConstructor<?>> locations = Arrays.asList(
		TwitchJarStorage::new,
		VanillaJarStorage::new,
		FallbackJarStorage::new
	);

	public EnvironmentDiscoverer(ParsedArguments args) {
		Path configDir = args.gameDir.resolve("config");
		try {
			Files.createDirectories(configDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create the config directory");
		}
		configFile = configDir.resolve("jumploader.json");

		this.gameDir = args.gameDir;

		for (JarStorageLocationConstructor<?> constructor : locations) {
			try {
				jarStorage = constructor.construct(args.gameDir);
				break;
			} catch (UnsupportedLocationException ignored) {}
		}
		if (jarStorage == null) {
			throw new RuntimeException("Failed to find a matching environment!");
		}

		String osProp = System.getProperty("os.name").toLowerCase();
		if (osProp.contains("win")) {
			os = "windows";
		} else if (osProp.contains("mac")) {
			os = "osx";
		} else {
			os = "linux";
		}

		LOGGER.info("Detected environment " + jarStorage.getClass().getCanonicalName() + " [Minecraft version " + args.mcVersion + "]");
	}

	public void updateForSide(Side side) {
		if (!jarStorage.compatibleWithSide(side)) {
			JarStorageLocation oldJarStorage = jarStorage;
			for (JarStorageLocationConstructor<?> constructor : locations) {
				try {
					jarStorage = constructor.construct(gameDir);
					break;
				} catch (UnsupportedLocationException ignored) {}
			}
			if (jarStorage == null) {
				throw new RuntimeException("Failed to find a matching environment after updating for side!");
			}
			LOGGER.info("Environment " + oldJarStorage.getClass().getCanonicalName() + " was incompatible with side " + side + ", switched to " + jarStorage.getClass().getCanonicalName());
		}
	}
}
