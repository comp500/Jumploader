package link.infra.jumploader.resources;

import link.infra.jumploader.DownloadWorkerManager;
import link.infra.jumploader.meta.MinecraftDownloadApi;
import link.infra.jumploader.util.SHA1HashingInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinecraftJar extends ResolvableJar {
	private static final Logger LOGGER = LogManager.getLogger();

	public String gameVersion;
	public String downloadType = "client";

	public MinecraftJar(EnvironmentDiscoverer.JarStorageLocation jarStorage) {
		super(jarStorage);
	}

	public MinecraftJar(EnvironmentDiscoverer.JarStorageLocation jarStorage, String gameVersion, String downloadType) {
		super(jarStorage);
		this.gameVersion = gameVersion;
		if (downloadType != null) {
			this.downloadType = downloadType;
		}
	}

	@Override
	public URL resolveLocal() throws FileNotFoundException {
		Path gameJarPath = jarStorage.getGameJar(gameVersion, downloadType);
		if (!Files.exists(gameJarPath)) {
			throw new FileNotFoundException();
		}
		return pathToURL(gameJarPath);
	}

	@Override
	public URL resolveRemote(DownloadWorkerManager.TaskStatus status, ParsedArguments args) throws Exception {
		// TODO: ensure special exception is handled correctly
		if (downloadType.contains("client")) {
			MinecraftDownloadApi.validate(args.accessToken);
		}
		URL versionMetaUrl = MinecraftDownloadApi.retrieveVersionMetaUrl(gameVersion);
		MinecraftDownloadApi.DownloadDetails details = MinecraftDownloadApi.retrieveDownloadDetails(versionMetaUrl, downloadType);
		Path gameJarPath = jarStorage.getGameJar(gameVersion, downloadType);
		try {
			downloadFile(status, details.url, gameJarPath, SHA1HashingInputStream.transformer(details.sha1));
		} catch (SHA1HashingInputStream.InvalidHashException e) {
			LOGGER.error("Minecraft JAR hash mismatch for " + details.url);
			LOGGER.error("Expected: " + details.sha1);
			LOGGER.error("Found:    " + e.hashFound);
			Files.deleteIfExists(gameJarPath);
			throw new RuntimeException("Failed to download Minecraft JAR!");
		}
		return pathToURL(gameJarPath);
	}
}
