package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.ConfigFile;
import link.infra.jumploader.launch.arguments.ParsedArguments;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;

public interface ResolutionContext {
	ConfigFile getConfigFile();
	EnvironmentDiscoverer getEnvironment();
	ParsedArguments getArguments();

	default String getLoadingSide() {
		ConfigFile configFile = getConfigFile();
		if (configFile.autoconfig.side != null) {
			return configFile.autoconfig.side;
		}
		return getArguments().inferredSide;
	}

	default String getLoadingVersion() {
		ConfigFile configFile = getConfigFile();
		if (configFile.autoconfig.gameVersion != null) {
			return configFile.autoconfig.gameVersion;
		}
		return getArguments().mcVersion;
	}
}
