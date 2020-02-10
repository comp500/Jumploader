package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpecialCaseHandler {
	private final List<SpecialCase> allCases = Arrays.asList(
		new FabricLoaderReflectionHack(),
		new MixinHideModLauncherBlacklist(),
		new ServerSideRemoveFMLArgs(),
		new ClassRedefinerASM(),
		new ForgeJarClasspathModifier()
	);

	private final List<SpecialCase> appliedCases = new ArrayList<>();

	public void filterAppliedCases(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		for (SpecialCase spcase : allCases) {
			if (spcase.shouldApply(loadedJars, mainClass, gameArguments)) {
				appliedCases.add(spcase);
			}
		}
	}

	public <T extends SpecialCase> List<T> getImplementingCases(Class<T> clazz) {
		List<T> list = new ArrayList<>();
		for (SpecialCase spcase : appliedCases) {
			if (clazz.isInstance(spcase)) {
				list.add(clazz.cast(spcase));
			}
		}
		return list;
	}

}
