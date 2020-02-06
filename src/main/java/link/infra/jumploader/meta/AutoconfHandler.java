package link.infra.jumploader.meta;

import link.infra.jumploader.ConfigFile;
import link.infra.jumploader.resources.EnvironmentDiscoverer;
import link.infra.jumploader.resources.MinecraftJar;
import link.infra.jumploader.resources.ParsedArguments;

import java.util.HashMap;
import java.util.Map;

public interface AutoconfHandler {
	void updateConfig(ConfigFile configFile, ParsedArguments args, EnvironmentDiscoverer env);

	Map<String, AutoconfHandler> HANDLERS = new HashMap<String, AutoconfHandler>() {{
		put("fabric", new FabricAutoconfHandler());
	}};

	static boolean doesConfigContainGameVersion(String gameVersion, ConfigFile configFile) {
		if (configFile.jars == null || configFile.jars.minecraft == null) {
			return false;
		}
		for (MinecraftJar jar : configFile.jars.minecraft) {
			if (jar.gameVersion.equals(gameVersion)) {
				return true;
			}
		}
		return false;
	}
}
