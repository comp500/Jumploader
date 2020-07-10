package link.infra.jumploader.launch.serviceloading;

import link.infra.jumploader.launch.PreLaunchDispatcher;
import link.infra.jumploader.launch.ReflectionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Hashtable;

public class JimfsURLHandlerAppender implements PreLaunchDispatcher.Handler {
	private final Logger LOGGER = LogManager.getLogger();

	@Override
	public void handlePreLaunch(ClassLoader loadingClassloader) {
		// Jimfs requires some funky hacks to load under a custom classloader, Java protocol handlers don't handle custom classloaders very well
		// See also FileSystemProviderAppender
		try {
			Class<?> handler = Class.forName("com.google.common.jimfs.Handler", true, loadingClassloader);
			// Add the jimfs handler to the URL handlers field, because Class.forName by default uses the classloader that loaded the calling class (in this case the system classloader, so we have to do it manually)
			Hashtable<String, URLStreamHandler> handlers = ReflectionUtil.reflectStaticField(URL.class, "handlers");
			handlers.putIfAbsent("jimfs", (URLStreamHandler) handler.getDeclaredConstructor().newInstance());
		} catch (ClassNotFoundException ignored) {
			// Ignore class not found - jimfs presumably isn't in the classpath
		} catch (NoSuchFieldException | IllegalAccessException | InstantiationException | ClassCastException | NoSuchMethodException | InvocationTargetException e) {
			LOGGER.warn("Failed to fix jimfs loading, jar-in-jar may not work", e);
		}
	}
}
