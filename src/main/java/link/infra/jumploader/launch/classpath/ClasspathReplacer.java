package link.infra.jumploader.launch.classpath;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClasspathReplacer {
	private ClasspathReplacer() { }

	public static void replaceClasspath(List<URL> newUrlList) throws URISyntaxException {
		List<String> newClasspath = new ArrayList<>();
		for (URL url : newUrlList) {
			newClasspath.add(Paths.get(url.toURI()).toString());
		}
		System.setProperty("java.class.path", String.join(File.pathSeparator, newClasspath));
	}
}
