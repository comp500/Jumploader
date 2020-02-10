package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.RegexUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 * If the version of ASM to be loaded isn't the same as the version already loaded (e.g. for ModLauncher), redefine ASM classes
 */
public class ClassRedefinerASM implements ClassRedefiner {
	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		// Attempt to determine the loaded version of ASM from the loaded manifests
		try {
			Enumeration<URL> manifests = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (manifests.hasMoreElements()) {
				try {
					Manifest manifest = new Manifest(manifests.nextElement().openStream());
					if ("org.objectweb.asm".equals(manifest.getMainAttributes().getValue("Bundle-Name"))) {
						String asmVersionLoaded = manifest.getMainAttributes().getValue("Bundle-Version");
						// Determine the version to load from the loadedJars list
						String asmVersionToLoad = RegexUtil.groupResultJars(Pattern.compile("/org/ow2/asm/asm/([\\d.]+)/asm-[\\d.]+\\.jar$"), loadedJars);
						if (asmVersionLoaded != null && asmVersionToLoad != null && !asmVersionLoaded.equals(asmVersionToLoad)) {
							// If both use different versions, apply this redefinition
							return true;
						}
					}
				} catch (IOException ignored) {}
			}
		} catch (IOException e) {
			return false;
		}
		return false;
	}

	@Override
	public boolean shouldRedefineClass(String name) {
		return name.startsWith("org.objectweb.asm") || name.startsWith("org.ow2.asm");
	}
}
