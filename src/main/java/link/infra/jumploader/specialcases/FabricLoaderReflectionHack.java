package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.RegexUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.file.spi.FileSystemProvider;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

public class FabricLoaderReflectionHack implements ReflectionHack {
	private final Logger LOGGER = LogManager.getLogger();
	private static final double JAVA_VERSION = Double.parseDouble(System.getProperty("java.specification.version", "0"));

	private interface ReflectionVersionHandler {
		void setClassLoader(ClassLoader loader) throws NoSuchFieldException, IllegalAccessException;
		void resetInstalledProviders() throws NoSuchFieldException, IllegalAccessException;
	}

	private static class ReflectionVersionHandler8 implements ReflectionVersionHandler {
		@Override
		public void setClassLoader(ClassLoader loader) throws NoSuchFieldException, IllegalAccessException {
			Field scl = ClassLoader.class.getDeclaredField("scl");
			scl.setAccessible(true);
			scl.set(null, loader);
		}

		@Override
		public void resetInstalledProviders() throws NoSuchFieldException, IllegalAccessException {
			Field installedProviders = FileSystemProvider.class.getDeclaredField("installedProviders");
			installedProviders.setAccessible(true);
			installedProviders.set(null, null);
			Field loadingProviders = FileSystemProvider.class.getDeclaredField("loadingProviders");
			loadingProviders.setAccessible(true);
			loadingProviders.set(null, false);
		}
	}

	@SuppressWarnings("Since15")
	private static class ReflectionVersionHandler9 implements ReflectionVersionHandler {
		@Override
		public void setClassLoader(ClassLoader loader) throws NoSuchFieldException, IllegalAccessException {
			VarHandle handle = MethodHandles.privateLookupIn(ClassLoader.class, MethodHandles.lookup()).findStaticVarHandle(ClassLoader.class, "scl", ClassLoader.class);
			handle.set(loader);
		}

		@Override
		public void resetInstalledProviders() throws NoSuchFieldException, IllegalAccessException {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(FileSystemProvider.class, MethodHandles.lookup());
			VarHandle installedProviders = lookup.findStaticVarHandle(FileSystemProvider.class, "installedProviders", List.class);
			installedProviders.set((Object) null);
			VarHandle loadingProviders = lookup.findStaticVarHandle(FileSystemProvider.class, "loadingProviders", boolean.class);
			loadingProviders.set(false);
		}
	}

	private static void reloadFSHandlers(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
		// Attempt to load the jimfs protocol handler (required for jar-in-jar) by hacking around the system classloader
		ClassLoader existingLoader = ClassLoader.getSystemClassLoader();
		ReflectionVersionHandler handler;
		if (JAVA_VERSION > 8) {
			handler = new ReflectionVersionHandler9();
		} else {
			handler = new ReflectionVersionHandler8();
		}
		handler.setClassLoader(classLoader);

		// Force FileSystemProvider to re-enumerate installed providers
		handler.resetInstalledProviders();
		FileSystemProvider.installedProviders();

		// Set the system classloader back to the actual system classloader
		handler.setClassLoader(existingLoader);
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
	public void applyReflectionHack(ClassLoader loadingClassloader) {
		// Jimfs requires some funky hacks to load under a custom classloader, Java protocol handlers don't handle custom classloaders very well
		try {
			FabricLoaderReflectionHack.reloadFSHandlers(loadingClassloader);
			FabricLoaderReflectionHack.injectJimfsHandler(loadingClassloader);
		} catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException | InstantiationException | ClassCastException e) {
			LOGGER.warn("Failed to fix jimfs loading, jar-in-jar may not work", e);
		}
	}

	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		return RegexUtil.patternMatchesJars(Pattern.compile("fabric-loader-(.+)\\.jar$"), loadedJars);
	}
}
