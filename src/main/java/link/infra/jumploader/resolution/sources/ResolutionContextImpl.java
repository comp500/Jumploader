package link.infra.jumploader.resolution.sources;

import link.infra.jumploader.ConfigFile;
import link.infra.jumploader.launch.arguments.ParsedArguments;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;

public class ResolutionContextImpl implements ResolutionContext {
	private final ConfigFile config;
	private final EnvironmentDiscoverer environmentDiscoverer;
	private final ParsedArguments argsParsed;

	public ResolutionContextImpl(ConfigFile config, EnvironmentDiscoverer environmentDiscoverer, ParsedArguments argsParsed) {
		this.config = config;
		this.environmentDiscoverer = environmentDiscoverer;
		this.argsParsed = argsParsed;
	}

	@Override
	public ConfigFile getConfigFile() {
		return config;
	}

	@Override
	public EnvironmentDiscoverer getEnvironment() {
		return environmentDiscoverer;
	}

	@Override
	public ParsedArguments getArguments() {
		return argsParsed;
	}
}
