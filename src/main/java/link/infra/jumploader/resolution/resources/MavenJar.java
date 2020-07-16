package link.infra.jumploader.resolution.resources;

import link.infra.jumploader.DownloadWorkerManager;
import link.infra.jumploader.launch.arguments.ParsedArguments;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;
import link.infra.jumploader.resolution.InvalidHashException;
import link.infra.jumploader.resolution.RequestUtils;
import link.infra.jumploader.resolution.SHA1HashingInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MavenJar extends ResolvableJar {
	public String mavenPath;
	public String repoUrl;

	private static final Logger LOGGER = LogManager.getLogger();

	public MavenJar(EnvironmentDiscoverer.JarStorageLocation jarStorage) {
		super(jarStorage);
	}

	public MavenJar(EnvironmentDiscoverer.JarStorageLocation jarStorage, String mavenPath, String repoUrl) {
		super(jarStorage);
		this.mavenPath = mavenPath;
		this.repoUrl = repoUrl;
	}

	@Override
	public URL resolveLocal() throws FileNotFoundException {
		Path jarPath = jarStorage.getMavenJar(mavenPath);
		if (Files.exists(jarPath)) {
			return pathToURL(jarPath);
		}
		throw new FileNotFoundException();
	}

	private static URL getSha1Url(URL downloadUrl) throws MalformedURLException {
		return new URL(downloadUrl.getProtocol(), downloadUrl.getHost(), downloadUrl.getPort(), downloadUrl.getFile() + ".sha1");
	}

	@Override
	public URL resolveRemote(DownloadWorkerManager.TaskStatus status, ParsedArguments args) throws URISyntaxException, IOException {
		URL downloadUrl = resolveMavenPath(new URI(repoUrl), mavenPath).toURL();
		Path jarPath = jarStorage.getMavenJar(mavenPath);

		String sha1Hash = RequestUtils.getString(getSha1Url(downloadUrl));

		try {
			downloadFile(status, downloadUrl, jarPath, SHA1HashingInputStream.transformer(sha1Hash));
		} catch (InvalidHashException e) {
			// TODO: better UI for this?
			LOGGER.error("Maven JAR hash mismatch for " + downloadUrl);
			LOGGER.error("Expected: " + sha1Hash);
			LOGGER.error("Found:    " + e.hashFound);
			throw new RuntimeException("Failed to download Maven JAR!");
		}
		return pathToURL(jarPath);
	}

	@Override
	public String toHumanString() {
		return "Maven library (" + repoUrl + ") " + mavenPath;
	}
}
