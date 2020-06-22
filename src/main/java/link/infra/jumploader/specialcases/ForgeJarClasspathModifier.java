package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.ClasspathUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ForgeJarClasspathModifier implements ClasspathModifier, URLBlacklist {
	private Set<Path> blacklistedPaths;

	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		// Always applies - if you're having problems this you should probably load the
		// class through Jumploader rather than in the classpath
		return true;
	}

	@Override
	public void modifyClasspath() {
		// If a JAR contains the Minecraft Main classes, remove it from the classpath - so Fabric doesn't see it and think the remapper needs to use it
		blacklistedPaths = ClasspathUtil.removeMatchingClasses(Arrays.asList("net.minecraft.client.main.Main", "net.minecraft.server.MinecraftServer"));
	}

	@Override
	public boolean shouldBlacklistUrl(URL url) {
		String[] splitUrl = url.getPath().split("!");
		if (splitUrl.length != 2) {
			return false;
		}
		Path resPath;
		try {
			resPath = Paths.get(new URI(splitUrl[0]));
		} catch (URISyntaxException e) {
			return false;
		}
		for (Path blacklistedPath : blacklistedPaths) {
			try {
				if (Files.isSameFile(resPath, blacklistedPath)) {
					return true;
				}
			} catch (IOException ignored) {}
		}
		return false;
	}
}
