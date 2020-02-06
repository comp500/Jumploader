package link.infra.jumploader.resources;

import link.infra.jumploader.DownloadWorkerManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MavenJar extends ResolvableJar {
	public String mavenPath;
	public String repoUrl;

	public MavenJar(EnvironmentDiscoverer.JarStorageLocation jarStorage) {
		super(jarStorage);
	}

	@Override
	public URL resolveLocal() throws FileNotFoundException {
		Path jarPath = jarStorage.getMavenJar(mavenPath);
		if (Files.exists(jarPath)) {
			return pathToURL(jarPath);
		}
		throw new FileNotFoundException();
	}

	@Override
	public URL resolveRemote(DownloadWorkerManager.TaskStatus status, ParsedArguments args) throws URISyntaxException, IOException {
		URL downloadUrl = resolveMavenPath(new URI(repoUrl), mavenPath).toURL();
		Path jarPath = jarStorage.getMavenJar(mavenPath);
		downloadFile(status, downloadUrl, jarPath, is -> is);
		return pathToURL(jarPath);
	}
}
