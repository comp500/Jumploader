package link.infra.jumploader;

import com.google.gson.JsonParseException;
import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import link.infra.jumploader.meta.AutoconfHandler;
import link.infra.jumploader.resources.EnvironmentDiscoverer;
import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.resources.ResolvableJar;
import link.infra.jumploader.specialcases.ClassBlacklist;
import link.infra.jumploader.specialcases.ReflectionHack;
import link.infra.jumploader.ui.GUIManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Jumploader implements ITransformationService {
	public static final String VERSION = "1.0.0";
	public static final String USER_AGENT = "Jumploader/" + VERSION;

	private final Logger LOGGER = LogManager.getLogger();

	private static class IgnoreForgeClassLoader extends URLClassLoader {
		private final Logger LOGGER = LogManager.getLogger();
		private final List<ClassBlacklist> blacklists;

		public IgnoreForgeClassLoader(URL[] urls, List<ClassBlacklist> blacklists) {
			super(urls);
			this.blacklists = blacklists;
		}

		private static Path jarFileFromUrl(URL jarUrl) throws URISyntaxException {
			String path = jarUrl.getFile();
			// Get the path to the jar from the jar path
			path = path.split("!")[0];
			return Paths.get(new URI(path));
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			ArrayList<URL> resList = Collections.list(super.getResources(name));
			// TODO: refactor this functionality into a specialcase
			if (name.contains("net/minecraft/client/main/Main") || name.contains("net/minecraft/server/MinecraftServer")) {
				LOGGER.debug("Found " + resList.size() + " Main classes, attempting to prioritize Jumploaded Vanilla JAR");

				// If this is the main class, reverse the order (so the child JARs are used first) and replace the system property
				Collections.reverse(resList);

				// Find the Forge JAR (this should be second in the list) and replace it with the Vanilla JAR (first in the list)
				try {
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
				} catch (URISyntaxException e) {
					LOGGER.error("Failed to replace java.class.path", e);
				}

				if (resList.size() > 2) {
					LOGGER.error("Found multiple Main classes, this is likely to be a problem. Have you used multiple Minecraft JARs?");
				}
			}
			return Collections.enumeration(resList);
		}

		@Override
		public URL getResource(String name) {
			if (name.contains("net/minecraft/client/main/Main") || name.contains("net/minecraft/server/MinecraftServer")) {
				try {
					return getResources(name).nextElement();
				} catch (IOException | NoSuchElementException e) {
					return null;
				}
			} else {
				return super.getResource(name);
			}
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			for (ClassBlacklist blacklist : blacklists) {
				if (blacklist.shouldBlacklistClass(name)) {
					throw new ClassNotFoundException();
				}
			}
			return super.loadClass(name, resolve);
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

		// Parse the arguments, detect the environment
		ParsedArguments argsParsed = new ParsedArguments(gameArgs);
		EnvironmentDiscoverer environmentDiscoverer = new EnvironmentDiscoverer(argsParsed);
		LOGGER.info("Detected environment " + environmentDiscoverer.jarStorage.getClass().getCanonicalName() + " [Minecraft version " + argsParsed.mcVersion + "]");

		ConfigFile config;
		try {
			config = ConfigFile.read(environmentDiscoverer);
			config.saveIfDirty();
		} catch (JsonParseException | IOException e) {
			throw new RuntimeException("Failed to read config file", e);
		}

		// Call the configured autoconfig handler
		if (config.autoconfig != null && config.autoconfig.enable) {
			LOGGER.info("Configuration successfully loaded! Updating configuration from handler: " + config.autoconfig.handler);
			AutoconfHandler autoconfHandler = AutoconfHandler.HANDLERS.get(config.autoconfig.handler);
			autoconfHandler.updateConfig(config, argsParsed, environmentDiscoverer);
			try {
				config.saveIfDirty();
			} catch (IOException e) {
				throw new RuntimeException("Failed to save the configuration", e);
			}
			LOGGER.info("Configuration up to date! Resolving JARs to jumpload...");
		} else {
			LOGGER.info("Configuration successfully loaded! Resolving JARs to jumpload...");
		}

		List<URL> loadUrls = resolveJars(config, argsParsed);

		for (URL jarUrl : loadUrls) {
			LOGGER.info("Found JAR: " + jarUrl);
		}

		List<ClassBlacklist> appliedBlacklists = ClassBlacklist.BLACKLISTS.stream().filter(bl -> bl.shouldApply(loadUrls, config.launch.mainClass)).collect(Collectors.toList());

		// Create the classloader with the found JARs
		URLClassLoader newLoader = new IgnoreForgeClassLoader(loadUrls.toArray(new URL[0]), appliedBlacklists);
		Thread.currentThread().setContextClassLoader(newLoader);

		// Apply the reflection hacks (mainly to get Java to recognize jimfs)
		for (ReflectionHack hack : ReflectionHack.HACKS) {
			if (hack.shouldApply(loadUrls, config.launch.mainClass)) {
				hack.applyHack(newLoader);
			}
		}

		LOGGER.info("Jumping to new loader, main class: " + config.launch.mainClass);
		// Attempt to launch mainClass with the classloader
		Class<?> mainClass;
		try {
			mainClass = newLoader.loadClass(config.launch.mainClass);
		} catch (ClassNotFoundException e) {
			if (config.launch.mainClass.contains("Knot")) {
				throw new RuntimeException("Failed to load " +
					config.launch.mainClass.substring(config.launch.mainClass.lastIndexOf(".") + 1) +
					" (Fabric) from " + config.launch.mainClass, e);
			}
			throw new RuntimeException("Failed to load " +
				config.launch.mainClass.substring(config.launch.mainClass.lastIndexOf(".") + 1) +
				" from " + config.launch.mainClass, e);
		}
		if (mainClass != null) {
			try {
				Method main = mainClass.getMethod("main", String[].class);
				main.invoke(null, new Object[] {gameArgs});
			} catch (NoSuchMethodException | IllegalAccessException e) {
				if (config.launch.mainClass.contains("Knot")) {
					throw new RuntimeException("Failed to invoke " +
						config.launch.mainClass.substring(config.launch.mainClass.lastIndexOf(".") + 1) +
						" (Fabric) from " + config.launch.mainClass, e);
				}
				throw new RuntimeException("Failed to invoke " +
					config.launch.mainClass.substring(config.launch.mainClass.lastIndexOf(".") + 1) +
					" from " + config.launch.mainClass, e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getTargetException());
			}
		}
	}

	private List<URL> resolveJars(ConfigFile config, ParsedArguments argsParsed) {
		List<ResolvableJar> configuredJars = config.getConfiguredJars();
		List<ResolvableJar> downloadRequiredJars = new ArrayList<>();
		// TODO: remove
		BlockingQueue<URL> loadUrlstemp = new LinkedBlockingQueue<>();
		List<URL> loadUrls = new ArrayList<>();

		// Resolve all the JARs locally
		for (ResolvableJar jar : configuredJars) {
			try {
				URL resolvedUrl = jar.resolveLocal();
				loadUrls.add(resolvedUrl);
			} catch (FileNotFoundException e) {
				downloadRequiredJars.add(jar);
			}
		}

		if (!downloadRequiredJars.isEmpty()) {
			if (config.downloadRequiredFiles) {
				// TODO: cli progress bar, file status

				DownloadWorkerManager workerManager = new DownloadWorkerManager();
				for (ResolvableJar jar : downloadRequiredJars) {
					workerManager.queueWorker(status -> {
						try {
							// TODO: get this value!! executorservice
							loadUrlstemp.put(jar.resolveRemote(status, argsParsed));
						} catch (Exception e) {
							status.markFailed(e);
							return;
						}
						status.markCompleted();
					});
				}

				// haha yes very good wait loop
//				while (!workerManager.isDone()) {
//					try {
//						Thread.sleep(500);
//						System.out.println(workerManager.getWorkerProgress());
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
				// TODO: check headless
				GUIManager guiManager = new GUIManager(workerManager);
				guiManager.run();

				workerManager.shutdown();

				// TODO: cleanup
				for (Exception ex : workerManager.getExceptions()) {
					throw new RuntimeException(ex);
				}
			} else {
				LOGGER.warn("downloadRequiredFiles is disabled, skipping downloading of JARs!");
			}
		}

		// TODO: remove
		while (!loadUrlstemp.isEmpty()) {
			loadUrls.add(loadUrlstemp.remove());
		}

		return loadUrls;
	}

	@SuppressWarnings("rawtypes")
	@Nonnull
	@Override
	public List<ITransformer> transformers() {
		return Collections.emptyList();
	}
}
