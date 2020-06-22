package link.infra.jumploader.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClasspathUtil {
	private ClasspathUtil() {}

	public static Set<Path> removeMatchingClasses(List<String> classesToTest) {
		List<String> classPath = new ArrayList<>(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
		Set<Path> blacklistedJars = new HashSet<>();

		ClassLoader throwawayClassLoader = new ClassLoader() {};
		Set<Path> sources = new HashSet<>();
		for (String className : classesToTest) {
			try {
				Class<?> clazz = throwawayClassLoader.loadClass(className);
				URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
				sources.add(Paths.get(location.toURI()));
			} catch (Exception ignored) {}
		}

		Iterator<String> iter = classPath.iterator();
		while (iter.hasNext()) {
			String currentJar = iter.next();
			for (Path matchingSourcePath : sources) {
				blacklistedJars.add(matchingSourcePath);
				try {
					if (Files.isSameFile(Paths.get(currentJar), matchingSourcePath)) {
						iter.remove();
						break;
					}
				} catch (IOException ignored) {}
			}
		}
		System.setProperty("java.class.path", String.join(File.pathSeparator, classPath));
		return blacklistedJars;
	}
}
