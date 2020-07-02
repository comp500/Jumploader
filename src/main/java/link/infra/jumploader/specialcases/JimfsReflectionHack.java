package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.ReflectionUtil;
import link.infra.jumploader.util.RegexUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

public class JimfsReflectionHack implements ReflectionHack {
	private final Logger LOGGER = LogManager.getLogger();

	private static void injectJimfsHandler(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException, ClassCastException, ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// Add the jimfs handler to the URL handlers field, because Class.forName by default uses the classloader that loaded the calling class (in this case the system classloader, so we have to do it manually)
		Hashtable<String, URLStreamHandler> handlers = ReflectionUtil.reflectStaticField(URL.class, "handlers");
		handlers.putIfAbsent("jimfs", (URLStreamHandler) Class.forName("com.google.common.jimfs.Handler", true, classLoader).getDeclaredConstructor().newInstance());
	}

	@Override
	public void applyReflectionHack(ClassLoader loadingClassloader) {
		// Jimfs requires some funky hacks to load under a custom classloader, Java protocol handlers don't handle custom classloaders very well
		// See also FileSystemProviderAppender
		try {
			JimfsReflectionHack.injectJimfsHandler(loadingClassloader);
		} catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException | InstantiationException | ClassCastException | NoSuchMethodException | InvocationTargetException e) {
			LOGGER.warn("Failed to fix jimfs loading, jar-in-jar may not work", e);
		}
	}

	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		return RegexUtil.patternMatchesJars(Pattern.compile("jimfs-(.+)\\.jar$"), loadedJars);
	}
}
