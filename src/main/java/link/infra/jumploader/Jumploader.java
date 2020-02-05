package link.infra.jumploader;

import com.google.gson.JsonParseException;
import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import link.infra.jumploader.reflectionhacks.ReflectionHack;
import link.infra.jumploader.resources.EnvironmentDiscoverer;
import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.resources.ResolvableJar;
import link.infra.jumploader.ui.GUIManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Jumploader implements ITransformationService {
	public static final String VERSION = "1.0.0";
	public static final String USER_AGENT = "Jumploader/" + VERSION;

	private final Logger LOGGER = LogManager.getLogger();

	private static class IgnoreForgeClassLoader extends URLClassLoader {
		private final Logger LOGGER = LogManager.getLogger();

		public IgnoreForgeClassLoader(URL[] urls) {
			super(urls);
		}

		private static Path jarFileFromUrl(URL jarUrl) {
			String path = jarUrl.getFile();
			// Get the path to the jar from the jar path
			path = path.replace("file:", "").split("!")[0];
			return Paths.get(path);
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
					Path origFile = jarFileFromUrl(resList.get(1));
					for (int i = 0; i < classPath.length; i++) {
						if (Files.isSameFile(Paths.get(classPath[i]), origFile)) {
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

	@Override
	public void onLoad(@Nonnull IEnvironment env, @Nonnull Set<String> set) {
		LOGGER.info("Jumploader " + VERSION + " initialising, discovering environment...");

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

		ParsedArguments argsParsed = new ParsedArguments(gameArgs);
		EnvironmentDiscoverer environmentDiscoverer = new EnvironmentDiscoverer(argsParsed);

		LOGGER.info("Detected environment " + environmentDiscoverer.jarStorage.getClass().getCanonicalName() + " [Minecraft version " + argsParsed.mcVersion + "]");

		ConfigFile config;
		try {
			config = ConfigFile.read(environmentDiscoverer.configFile);
			config.saveIfDirty();
		} catch (JsonParseException | IOException e) {
			throw new RuntimeException("Failed to read config file", e);
		}

		LOGGER.info("Configuration successfully loaded!");

		// TODO: check if minecraft version is valid, and if there are any configured jars (get fabric config otherwise!)
		List<ResolvableJar> configuredJars = config.getConfiguredJars();

		GUIManager guiManager = new GUIManager();
		//DownloadManager manager = new DownloadManager(argsParsed, config, environmentDiscoverer);
		//manager.download();

		LOGGER.info("Loading JARs...");
		// TODO: get list from configuration / resource loading magic
		//File[] jarList = mainDir.listFiles((file, name) -> !("jumploader.txt".equals(name)));
		File[] jarList = new File[0];
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

		// Apply the reflection hacks (mainly to get Java to recognize jimfs)
		for (ReflectionHack hack : ReflectionHack.HACKS) {
			if (hack.hackApplies(loadURLs.toArray(new URL[0]))) {
				hack.applyHack(newLoader);
			}
		}

		LOGGER.info("Jumping to new loader, main class: " + config.launch.mainClass);
		// Attempt to launch KnotClient with the classloader
		Class<?> knotClient;
		try {
			knotClient = newLoader.loadClass(config.launch.mainClass);
		} catch (ClassNotFoundException e) {
			// TODO: make this exception depend on mainClass text
			throw new RuntimeException("Failed to load KnotClient (Fabric) from " + config.launch.mainClass, e);
		}
		if (knotClient != null) {
			try {
				Method main = knotClient.getMethod("main", String[].class);
				main.invoke(null, new Object[] {gameArgs});
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new RuntimeException("Failed to invoke KnotClient (Fabric) from " + config.launch.mainClass, e);
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
