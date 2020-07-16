package link.infra.jumploader;

import com.google.gson.JsonParseException;
import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import link.infra.jumploader.launch.PreLaunchDispatcher;
import link.infra.jumploader.launch.ReflectionUtil;
import link.infra.jumploader.launch.arguments.ParsedArguments;
import link.infra.jumploader.launch.classpath.ClasspathReplacer;
import link.infra.jumploader.resolution.meta.AutoconfHandler;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;
import link.infra.jumploader.resolution.resources.ResolvableJar;
import link.infra.jumploader.resolution.ui.GUIManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Jumploader implements ITransformationService {
	public static final String VERSION = "2.0.0";
	public static final String USER_AGENT = "Jumploader/" + VERSION;

	private final Logger LOGGER = LogManager.getLogger();

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

	@Override
	public void onLoad(@Nonnull IEnvironment env, @Nonnull Set<String> set) {
		LOGGER.info("Jumploader " + VERSION + " initialising, discovering environment...");

		// Get the game arguments
		ParsedArguments argsParsedTemp;
		// Very bad reflection, don't try this at home!!
		try {
			ArgumentHandler handler = ReflectionUtil.reflectField(Launcher.INSTANCE, "argumentHandler");
			String[] gameArgs = ReflectionUtil.reflectField(handler, "args");
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

		// TODO: refactor autoconfig system
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

		// Replace the classpath URLs
		try {
			ClasspathReplacer.replaceClasspath(loadUrls);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to parse URL in replacement classpath", e);
		}

		// Create the classloader with the found JARs
		// Set the parent classloader to the parent of the AppClassLoader
		// This will be ExtClassLoader on Java 8 or older, PlatformClassLoader on Java 9 or newer - ensures extension classes will work
		URLClassLoader newLoader = new URLClassLoader(loadUrls.toArray(new URL[0]), ClassLoader.getSystemClassLoader().getParent());
		Thread.currentThread().setContextClassLoader(newLoader);

		// Dispatch prelaunch handlers
		PreLaunchDispatcher.dispatch(newLoader);

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
			Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
				// Do nothing!
			});
			throw new ThreadCloseException("Jumploader is closing the main thread, not an error!"){
				@Override
				public void printStackTrace(PrintStream s) {
					// Do nothing!
				}

				@Override
				public void printStackTrace(PrintWriter s) {
					// Do nothing!
				}
			};
		}
		System.exit(1);
	}

	private static class ThreadCloseException extends RuntimeException {
		public ThreadCloseException(String reason) {
			super(reason);
		}
	}

	// TODO: move to separate classes
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

				if (!lwjglAvailable || GraphicsEnvironment.isHeadless() || argsParsed.nogui || config.disableUI) {
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

					try {
						workerManager.shutdown();
					} catch (InterruptedException ex) {
						throw new RuntimeException(ex);
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
