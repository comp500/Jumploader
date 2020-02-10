package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.ClasspathUtil;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ForgeJarClasspathModifier implements ClasspathModifier {
	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		// Always applies - if you're having problems this you should probably load the
		// class through Jumploader rather than in the classpath
		return true;
	}

	@Override
	public void modifyClasspath() {
		// If a JAR contains the Minecraft Main classes, remove it from the classpath - so Fabric doesn't see it and think the remapper needs to use it
		ClasspathUtil.removeMatchingClasses(Arrays.asList("net.minecraft.client.main.Main", "net.minecraft.server.MinecraftServer"));
	}
}
