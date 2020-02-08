package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Required to fool MixinServiceModLauncherBootstrap into believing that ModLauncher doesn't exist
 */
public class MixinHideModLauncherBlacklist implements ClassBlacklist {
	@Override
	public boolean shouldBlacklistClass(String name) {
		return "cpw.mods.modlauncher.Launcher".equals(name);
	}

	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		return RegexUtil.patternMatchesJars(Pattern.compile("sponge-mixin-(.+)\\.jar$"), loadedJars);
	}
}
