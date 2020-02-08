package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.RegexUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class UnpatchedGameResourcePriorityModifier implements JarResourcePriorityModifier {
	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public boolean shouldApplyClass(String className) {
		return className.contains("net/minecraft/client/main/Main") || className.contains("net/minecraft/server/MinecraftServer");
	}

	private static Path jarFileFromUrl(URL jarUrl) throws URISyntaxException {
		String path = jarUrl.getFile();
		// Get the path to the jar from the jar path
		path = path.split("!")[0];
		return Paths.get(new URI(path));
	}

	@Override
	public void apply(List<URL> resList) throws IOException {
		LOGGER.debug("Found " + resList.size() + " Main classes, attempting to prioritize Jumploaded Vanilla JAR");

		// If this is the main class, reverse the order (so the child JARs are used first) and replace the system property
		Collections.reverse(resList);

		// Find the Forge JAR (this should be second in the list) and replace it with the Vanilla JAR (first in the list)
		try {
			if (resList.size() >= 2) {
				String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
				Path origFile = jarFileFromUrl(resList.get(1));
				for (int i = 0; i < classPath.length; i++) {
					if (Files.isSameFile(Paths.get(classPath[i]), origFile)) {
						classPath[i] = jarFileFromUrl(resList.get(0)).toString();
						LOGGER.debug("Replacing " + origFile.toString() + " with " + classPath[i] + " in java.class.path property");
					}
				}
				System.setProperty("java.class.path", String.join(File.pathSeparator, classPath));
			}
		} catch (URISyntaxException e) {
			LOGGER.error("Failed to replace java.class.path", e);
		}

		if (resList.size() > 2) {
			LOGGER.error("Found multiple Main classes, this is likely to be a problem. Have you used multiple Minecraft JARs?");
		}
	}

	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		// Applies when Fabric is loaded, won't work for non-fabric alternative loaders!
		return RegexUtil.patternMatchesJars(Pattern.compile("fabric-loader-(.+)\\.jar$"), loadedJars);
	}
}
