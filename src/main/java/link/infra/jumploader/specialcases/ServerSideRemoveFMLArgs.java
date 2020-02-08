package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;

import java.net.URL;
import java.util.List;

/**
 * Server sided Minecraft doesn't like excess arguments. So we remove them!
 */
public class ServerSideRemoveFMLArgs implements ArgumentsModifier {
	@Override
	public void apply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		gameArguments.arguments.entrySet().removeIf(entry ->
			entry.getKey().startsWith("fml") || entry.getKey().equals("launchTarget"));
	}

	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		return gameArguments.inferredSide.equals("server");
	}
}
