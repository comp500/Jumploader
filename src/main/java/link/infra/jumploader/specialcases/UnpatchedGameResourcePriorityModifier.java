package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.ClasspathUtil;
import link.infra.jumploader.util.RegexUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class UnpatchedGameResourcePriorityModifier implements JarResourcePriorityModifier {
	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public boolean shouldPrioritiseResource(String resourceName) {
		return resourceName.contains("net/minecraft/client/main/Main") || resourceName.contains("net/minecraft/server/MinecraftServer");
	}

	@Override
	public void modifyPriorities(List<URL> resList) throws IOException {
		LOGGER.debug("Found " + resList.size() + " Main classes, attempting to prioritize Jumploaded Vanilla JAR");

		// If this is the main class, reverse the order (so the child JARs are used first) and replace the system property
		Collections.reverse(resList);

		// Find the Forge JAR (this should be second in the list) and replace it with the Vanilla JAR (first in the list)
		try {
			if (resList.size() >= 2) {
				ClasspathUtil.replaceAll(resList.get(1), resList.get(0));
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
