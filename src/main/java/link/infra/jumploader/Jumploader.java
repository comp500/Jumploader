package link.infra.jumploader;

import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import link.infra.jumploader.download.ui.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.stream.Collectors;

public class Jumploader implements ITransformationService {
	private Logger LOGGER = LogManager.getLogger();

	private static class IgnoreForgeClassLoader extends URLClassLoader {
		private Logger LOGGER = LogManager.getLogger();

		public IgnoreForgeClassLoader(URL[] urls) {
			super(urls);
		}

		private static File jarFileFromUrl(URL jarUrl) {
			String path = jarUrl.getFile();
			// Get the path to the jar from the jar path
			path = path.replace("file:", "").split("!")[0];
			return new File(path);
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			ArrayList<URL> resList = Collections.list(super.getResources(name));
			if (name.contains("net/minecraft/client/main/Main")) {
				LOGGER.debug("Found " + resList.size() + " Main classes, attempting to prioritize Jumploaded Vanilla JAR");

				// If this is the main class, reverse the order (so the child JARs are used first) and replace the system property
				Collections.reverse(resList);

				// Find the Forge JAR (this should be second in the list) and replace it with the Vanilla JAR (first in the list)
				if (resList.size() >= 2) {
					String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
					File origFile = jarFileFromUrl(resList.get(1));
					for (int i = 0; i < classPath.length; i++) {
						if (new File(classPath[i]).equals(origFile)) {
							classPath[i] = jarFileFromUrl(resList.get(0)).toString();
							LOGGER.debug("Replacing " + origFile.toString() + " with " + classPath[i] + " in java.class.path property");
						}
					}
					System.setProperty("java.class.path", String.join(File.pathSeparator, classPath));
				}

				if (resList.size() > 2) {
					LOGGER.error("Found multiple Main classes, this is likely to be a problem. Have you used multiple Minecraft JARs?");
				}
			}
			return Collections.enumeration(resList);
		}

		@Override
		public URL getResource(String name) {
			if (name.contains("net/minecraft/client/main/Main")) {
				try {
					return getResources(name).nextElement();
				} catch (IOException | NoSuchElementException e) {
					return null;
				}
			} else {
				return super.getResource(name);
			}
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "jumploader";
	}

	@Override
	public void initialize(@Nonnull IEnvironment env) {
		throw new RuntimeException("Jumploader shouldn't get to this stage!");
	}

	@Override
	public void beginScanning(@Nonnull IEnvironment env) {
		throw new RuntimeException("Jumploader shouldn't get to this stage!");
	}

	@SuppressWarnings("unchecked")
	private static <T> T reflectField(Object destObj, String name) throws NoSuchFieldException, IllegalAccessException, ClassCastException {
		Field field = destObj.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(destObj);
	}

	// TODO: refactor to a separate class?
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
	public void onLoad(@Nonnull IEnvironment env, @Nonnull Set<String> set) {
		Window window = new Window();
		window.init();
		while (!window.shouldClose()) {
			window.render();
		}
		window.free();

		// Get the game arguments
		String[] gameArgs;
		// Very bad reflection, don't try this at home!!
		try {
			ArgumentHandler handler = reflectField(Launcher.INSTANCE, "argumentHandler");
			gameArgs = reflectField(handler, "args");
			if (gameArgs == null) {
				LOGGER.warn("Game arguments are null, have they been parsed yet?");
				gameArgs = new String[0];
			}
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			gameArgs = new String[0];
			LOGGER.warn("Failed to retrieve game arguments, has modlauncher changed?", e);
		}

		File libsDir = new File("mods/jumploader");
		if (!libsDir.exists()) {
			if (!libsDir.mkdir()) {
				throw new RuntimeException("Failed to create the jumploader directory");
			}
		}

		LOGGER.info("Reading configuration...");
		String mainClass = "net.fabricmc.loader.launch.knot.KnotClient";
		File configFile = new File(libsDir, "jumploader.txt");
		if (configFile.exists()) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)))) {
				List<String> configLines = in.lines().filter(s -> s.trim().length() > 0).collect(Collectors.toList());
				for (String line : configLines) {
					String[] parts = line.split("=");
					if (parts.length != 2) {
						LOGGER.warn("Invalid config line: " + line);
						continue;
					}
					if (parts[0].trim().equalsIgnoreCase("mainClass")) {
						mainClass = parts[1].trim();
					} else {
						LOGGER.warn("Invalid config line: " + line);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to read config file", e);
			}
		} else {
			try (PrintStream out = new PrintStream(new FileOutputStream(configFile))) {
				out.print("mainClass = " + mainClass);
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Failed to write config file", e);
			}
		}

		LOGGER.info("Loading JARs from mods/jumploader/");
		File[] jarList = libsDir.listFiles((file, name) -> !("jumploader.txt".equals(name)));
		List<URL> loadURLs = new ArrayList<>();
		if (jarList == null) {
			throw new RuntimeException("No JARs found to jumpload!");
		}
		for (File jar : jarList) {
			URL loadableURL;
			try {
				loadableURL = jar.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException("Failed to load JAR, malformed URL", e);
			}
			LOGGER.info("Found JAR: " + loadableURL);
			loadURLs.add(loadableURL);
		}

		// Create the classloader with the found JARs
		URLClassLoader newLoader = new IgnoreForgeClassLoader(loadURLs.toArray(new URL[0]));
		Thread.currentThread().setContextClassLoader(newLoader);

		// Jimfs requires some funky hacks to load under a custom classloader, Java protocol handlers don't handle custom classloaders very well
		try {
			reloadFSHandlers(newLoader);
			injectJimfsHandler(newLoader);
		} catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException | InstantiationException | ClassCastException e) {
			LOGGER.warn("Failed to fix jimfs loading, jar-in-jar may not work", e);
		}

		LOGGER.info("Jumping to new loader, main class: " + mainClass);
		// Attempt to launch KnotClient with the classloader
		Class<?> knotClient;
		try {
			knotClient = newLoader.loadClass(mainClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Failed to load KnotClient (Fabric) from " + mainClass, e);
		}
		if (knotClient != null) {
			try {
				Method main = knotClient.getMethod("main", String[].class);
				main.invoke(null, new Object[] {gameArgs});
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new RuntimeException("Failed to invoke KnotClient (Fabric) from " + mainClass, e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getTargetException());
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Nonnull
	@Override
	public List<ITransformer> transformers() {
		return Collections.emptyList();
	}
}
