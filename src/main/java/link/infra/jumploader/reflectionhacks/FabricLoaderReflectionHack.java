package link.infra.jumploader.reflectionhacks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.file.spi.FileSystemProvider;
import java.util.Hashtable;
import java.util.regex.Pattern;

public class FabricLoaderReflectionHack implements ReflectionHack {
	private final Logger LOGGER = LogManager.getLogger();

	private static void reloadFSHandlers(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
		// Attempt to load the jimfs protocol handler (required for jar-in-jar) by hacking around the system classloader
		Field scl = ClassLoader.class.getDeclaredField("scl");
		scl.setAccessible(true);
		ClassLoader existingLoader = (ClassLoader) scl.get(null);
		scl.set(null, classLoader);

		// Force FileSystemProvider to re-enumerate installed providers
		Field installedProviders = FileSystemProvider.class.getDeclaredField("installedProviders");
		installedProviders.setAccessible(true);
		installedProviders.set(null, null);
		Field loadingProviders = FileSystemProvider.class.getDeclaredField("loadingProviders");
		loadingProviders.setAccessible(true);
		loadingProviders.set(null, false);
		FileSystemProvider.installedProviders();

		// Set the system classloader back to the actual system classloader
		scl.set(null, existingLoader);
	}

	@SuppressWarnings("unchecked")
	private static void injectJimfsHandler(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException, ClassCastException, ClassNotFoundException, InstantiationException {
		// Add the jimfs handler to the URL handlers field, because Class.forName by default uses the classloader that loaded the calling class (in this case the system classloader, so we have to do it manually)
		Field handlersField = URL.class.getDeclaredField("handlers");
		handlersField.setAccessible(true);
		Hashtable<String, URLStreamHandler> handlers = (Hashtable<String, URLStreamHandler>) handlersField.get(null);
		handlers.putIfAbsent("jimfs", (URLStreamHandler) Class.forName("com.google.common.jimfs.Handler", true, classLoader).newInstance());
	}

	@Override
	public void applyHack(ClassLoader loadingClassloader) {
		// Jimfs requires some funky hacks to load under a custom classloader, Java protocol handlers don't handle custom classloaders very well
		try {
			FabricLoaderReflectionHack.reloadFSHandlers(loadingClassloader);
			FabricLoaderReflectionHack.injectJimfsHandler(loadingClassloader);
		} catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException | InstantiationException | ClassCastException e) {
			LOGGER.warn("Failed to fix jimfs loading, jar-in-jar may not work", e);
		}
	}

	@Override
	public boolean hackApplies(URL[] loadedJars) {
		Pattern hackTest = Pattern.compile("fabric-loader-(.+).jar$");
		for (URL jar : loadedJars) {
			if (hackTest.matcher(jar.toString()).matches()) {
				return true;
			}
		}
		return false;
	}
}
