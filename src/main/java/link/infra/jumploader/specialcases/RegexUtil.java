package link.infra.jumploader.specialcases;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

public class RegexUtil {
	private RegexUtil() {}

	public static boolean patternMatchesJars(Pattern pattern, List<URL> loadedJars) {
		for (URL jar : loadedJars) {
			if (pattern.matcher(jar.toString()).find()) {
				return true;
			}
		}
		return false;
	}
}
