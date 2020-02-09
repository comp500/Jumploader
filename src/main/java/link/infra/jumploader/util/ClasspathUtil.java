package link.infra.jumploader.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClasspathUtil {
	private ClasspathUtil() {}

	public static Path jarFileFromUrl(URL jarUrl) throws URISyntaxException {
		String path = jarUrl.getFile();
		// Get the path to the jar from the jar path
		path = path.split("!")[0];
		return Paths.get(new URI(path));
	}

	public static void replaceAll(URL origUrl, URL newUrl) throws URISyntaxException, IOException {
		String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
		Path origFile = ClasspathUtil.jarFileFromUrl(origUrl);
		for (int i = 0; i < classPath.length; i++) {
			if (Files.isSameFile(Paths.get(classPath[i]), origFile)) {
				classPath[i] = ClasspathUtil.jarFileFromUrl(newUrl).toString();
				//LOGGER.debug("Replacing " + origFile.toString() + " with " + classPath[i] + " in java.class.path property");
			}
		}
		System.setProperty("java.class.path", String.join(File.pathSeparator, classPath));
	}

}
