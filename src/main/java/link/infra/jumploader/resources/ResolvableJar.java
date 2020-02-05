package link.infra.jumploader.resources;

import link.infra.jumploader.DownloadWorkerManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Function;

public abstract class ResolvableJar {
	protected transient final EnvironmentDiscoverer.JarStorageLocation jarStorage;
	public ResolvableJar(EnvironmentDiscoverer.JarStorageLocation jarStorage) {
		this.jarStorage = jarStorage;
	}

	public static URL pathToURL(Path path) {
		try {
			return path.toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Failed to load JAR, malformed URL", e);
		}
	}

	public static URI resolveMavenPath(URI baseURL, String mavenPath) {
		String[] mavenPathSplit = mavenPath.split(":");
		if (mavenPathSplit.length != 3) {
			throw new RuntimeException("Invalid maven path: " + mavenPath);
		}
		return baseURL.resolve(
			String.join("/", mavenPathSplit[0].split(".")) + "/" + // Group ID
			mavenPathSplit[1] + "/" + // Artifact ID
			mavenPathSplit[2] + "/" + // Version
			mavenPathSplit[1] + "-" + mavenPathSplit[2] + ".jar"
		);
	}

	protected static void downloadFile(DownloadWorkerManager.TaskStatus status, URL downloadURI, Path destPath, Function<InputStream, InputStream> bytesTransformer) throws IOException {
		// TODO: implement file downloading
	}

	abstract URL resolveLocal() throws FileNotFoundException;
	abstract URL resolveRemote(DownloadWorkerManager.TaskStatus status, ParsedArguments args) throws Exception;
}
