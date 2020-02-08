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
import link.infra.jumploader.specialcases.ArgumentsModifier;
import link.infra.jumploader.specialcases.ClassBlacklist;
import link.infra.jumploader.specialcases.JarResourcePriorityModifier;
import link.infra.jumploader.specialcases.ReflectionHack;
import link.infra.jumploader.ui.GUIManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Jumploader implements ITransformationService {
	public static final String VERSION = "1.0.0";
	public static final String USER_AGENT = "Jumploader/" + VERSION;

	private final Logger LOGGER = LogManager.getLogger();

	private static class JumploaderClassLoader extends URLClassLoader {
		private final List<ClassBlacklist> blacklists;
		private final List<JarResourcePriorityModifier> priorityModifiers;

		public JumploaderClassLoader(URL[] urls, List<ClassBlacklist> blacklists, List<JarResourcePriorityModifier> priorityModifiers) {
			super(urls);
			this.blacklists = blacklists;
			this.priorityModifiers = priorityModifiers;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			for (JarResourcePriorityModifier modifier : priorityModifiers) {
				if (modifier.shouldApplyClass(name)) {
					ArrayList<URL> resList = Collections.list(super.getResources(name));
					modifier.apply(resList);
					return Collections.enumeration(resList);
				}
			}
			return super.getResources(name);
		}

		@Override
		public URL getResource(String name) {
			for (JarResourcePriorityModifier modifier : priorityModifiers) {
				if (modifier.shouldApplyClass(name)) {
					try {
						return getResources(name).nextElement();
					} catch (IOException | NoSuchElementException e) {
						return null;
					}
				}
			}
			return super.getResource(name);
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
		ParsedArguments argsParsedTemp;
		// Very bad reflection, don't try this at home!!
		try {
			ArgumentHandler handler = reflectField(Launcher.INSTANCE, "argumentHandler");
			String[] gameArgs = reflectField(handler, "args");
			if (gameArgs == null) {
				LOGGER.warn("Game arguments are null, have they been parsed yet?");
				gameArgs = new String[0];
			}
			argsParsedTemp = new ParsedArguments(gameArgs);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			argsParsedTemp = new ParsedArguments(new String[0]);
			LOGGER.warn("Failed to retrieve game arguments, has modlauncher changed?", e);
		}
		ParsedArguments argsParsed = argsParsedTemp;

		// Detect the environment
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

		List<ClassBlacklist> appliedBlacklists = ClassBlacklist.BLACKLISTS.stream().filter(bl -> bl.shouldApply(loadUrls, config.launch.mainClass, argsParsed)).collect(Collectors.toList());
		List<JarResourcePriorityModifier> priorityModifiers = JarResourcePriorityModifier.MODIFIERS.stream().filter(bl -> bl.shouldApply(loadUrls, config.launch.mainClass, argsParsed)).collect(Collectors.toList());

		// Create the classloader with the found JARs
		URLClassLoader newLoader = new JumploaderClassLoader(loadUrls.toArray(new URL[0]), appliedBlacklists, priorityModifiers);
		Thread.currentThread().setContextClassLoader(newLoader);

		// Apply the argument modifiers
		for (ArgumentsModifier modifier : ArgumentsModifier.MODIFIERS) {
			if (modifier.shouldApply(loadUrls, config.launch.mainClass, argsParsed)) {
				modifier.apply(loadUrls, config.launch.mainClass, argsParsed);
			}
		}

		// Apply the reflection hacks (mainly to get Java to recognize jimfs)
		for (ReflectionHack hack : ReflectionHack.HACKS) {
			if (hack.shouldApply(loadUrls, config.launch.mainClass, argsParsed)) {
				hack.applyHack(newLoader);
			}
		}

		int preLaunchRunningThreads = Thread.currentThread().getThreadGroup().activeCount();

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
				main.invoke(null, new Object[] {argsParsed.getArgsArray()});
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

		// Attempt to determine if returning from invoke() was intentional
		if (Thread.currentThread().getThreadGroup().activeCount() - preLaunchRunningThreads <= 0) {
			LOGGER.warn("Minecraft shouldn't return from invoke() without spawning threads, loading is likely to have failed!");
		} else {
			LOGGER.info("Game returned from invoke(), attempting to stop ModLauncher init");
			// Kill the current thread (the Server starts it's own thread)
			Thread.currentThread().interrupt();
			LOGGER.info("Failed to stop, starting infinite wait to stop ModLauncher init");
			try {
				Object obj = new Object();
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (obj) {
					obj.wait();
				}
			} catch (InterruptedException ignored) {
				// We'll be interrupted at least once thanks to interrupt(), so try again
				try {
					Object obj = new Object();
					//noinspection SynchronizationOnLocalVariableOrMethodParameter
					synchronized (obj) {
						obj.wait();
					}
				} catch (InterruptedException ignored2) {
					Thread.currentThread().interrupt();
				}
			}
			LOGGER.error("Something funky is going on, ModLauncher seems to not care that I'm trying to break it!!!!!");
			LOGGER.error("As this should never be reached normally, I'm going to System.exit(1)");
		}
		System.exit(1);
	}

	private List<URL> resolveJars(ConfigFile config, ParsedArguments argsParsed) {
		List<ResolvableJar> configuredJars = config.getConfiguredJars();
		List<ResolvableJar> downloadRequiredJars = new ArrayList<>();
		List<URL> loadUrls = new ArrayList<>();

		// Resolve all the JARs locally
		for (ResolvableJar jar : configuredJars) {
			try {
				URL resolvedUrl = jar.resolveLocal();
				loadUrls.add(resolvedUrl);
				LOGGER.info("Found JAR: " + resolvedUrl);
			} catch (FileNotFoundException e) {
				downloadRequiredJars.add(jar);
			}
		}

		if (!downloadRequiredJars.isEmpty()) {
			if (config.downloadRequiredFiles) {
				DownloadWorkerManager<URL> workerManager = new DownloadWorkerManager<>();
				for (ResolvableJar jar : downloadRequiredJars) {
					LOGGER.info("Queueing download: " + jar.toHumanString());
					workerManager.queueWorker(status -> jar.resolveRemote(status, argsParsed));
				}

				boolean lwjglAvailable = false;
				try {
					Class.forName("org.lwjgl.system.MemoryStack");
					lwjglAvailable = true;
				} catch (ClassNotFoundException ignored) {}

				if (!lwjglAvailable || GraphicsEnvironment.isHeadless() || argsParsed.nogui) {
					while (!workerManager.isDone()) {
						LOGGER.info("Progress: " + (workerManager.getWorkerProgress() * 100) + "%");
						URL resolvedURL;
						try {
							resolvedURL = workerManager.pollResult(500);
						} catch (Exception e) {
							// TODO: implement a better way of showing download exceptions?
							try {
								workerManager.shutdown();
							} catch (InterruptedException ex) {
								throw new RuntimeException(ex);
							}
							throw new RuntimeException(e);
						}
						if (resolvedURL != null) {
							LOGGER.info("Downloaded successfully: " + resolvedURL);
							loadUrls.add(resolvedURL);
						}
					}
				} else {
					GUIManager guiManager = new GUIManager(workerManager, argsParsed);
					guiManager.init();

					boolean closeTriggered = false;
					while (!workerManager.isDone()) {
						if (guiManager.wasCloseTriggered()) {
							LOGGER.warn("Download window closed! Shutting down...");
							try {
								workerManager.shutdown();
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							closeTriggered = true;
							break;
						}
						guiManager.render();
						URL resolvedURL;
						try {
							resolvedURL = workerManager.pollResult();
						} catch (Exception e) {
							// TODO: implement a better way of showing download exceptions?
							try {
								workerManager.shutdown();
							} catch (InterruptedException ex) {
								throw new RuntimeException(ex);
							}
							guiManager.cleanup();
							throw new RuntimeException(e);
						}
						if (resolvedURL != null) {
							LOGGER.info("Downloaded successfully: " + resolvedURL);
							loadUrls.add(resolvedURL);
						}
					}

					try {
						workerManager.shutdown();
					} catch (InterruptedException ex) {
						throw new RuntimeException(ex);
					}

					guiManager.cleanup();
					if (closeTriggered) {
						System.exit(1);
					}
				}
			} else {
				LOGGER.warn("downloadRequiredFiles is disabled, skipping downloading of JARs!");
			}
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
