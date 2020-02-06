package link.infra.jumploader.specialcases;

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
	public boolean shouldApply(List<URL> loadedJars, String mainClass) {
		Pattern urlTest = Pattern.compile("sponge-mixin-(.+)\\.jar$");
		return RegexUtil.patternMatchesJars(urlTest, loadedJars);
	}
}
