package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.ConfigFile;
import link.infra.jumploader.launch.arguments.ParsedArguments;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;
import link.infra.jumploader.resolution.ui.util.UIDetection;
import link.infra.jumploader.util.Side;

public interface ResolutionContext {
	ConfigFile getConfigFile();
	EnvironmentDiscoverer getEnvironment();
	ParsedArguments getArguments();

	default Side getLoadingSide() {
		ConfigFile configFile = getConfigFile();
		if (configFile.gameSide != null) {
			return configFile.gameSide;
		}
		return getArguments().inferredSide;
	}

	default String getLoadingVersion() {
		ConfigFile configFile = getConfigFile();
		if (configFile.gameVersion != null && !configFile.gameVersion.equals("current")) {
			return configFile.gameVersion;
		}
		return getArguments().mcVersion;
	}

	default boolean useUI() {
		return UIDetection.uiAvailable && !getConfigFile().disableUI && !getArguments().nogui;
	}
}
