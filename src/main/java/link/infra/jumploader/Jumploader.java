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
import link.infra.jumploader.resolution.EnvironmentDiscoverer;
import link.infra.jumploader.resolution.ResolutionProcessor;
import link.infra.jumploader.resolution.download.PreDownloadCheck;
import link.infra.jumploader.resolution.sources.MetadataResolutionResult;
import link.infra.jumploader.resolution.sources.ResolutionContext;
import link.infra.jumploader.resolution.sources.ResolutionContextImpl;
import link.infra.jumploader.resolution.ui.messages.ErrorMessages;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Jumploader implements ITransformationService {
	public static final String VERSION = "2.1.2";
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

		ConfigFile config;
		try {
			config = ConfigFile.read(environmentDiscoverer);
			config.saveIfDirty();
		} catch (JsonParseException | IOException e) {
			ErrorMessages.showFatalMessage("Jumploader failed to load", "Failed to read config file: " + e.getClass().getTypeName() + ": " + e.getLocalizedMessage(), LOGGER);
			throw new RuntimeException("Failed to read config file", e);
		}

		if (config.disableUI) {
			ErrorMessages.disableGUI();
		}
		LOGGER.info("Configuration successfully loaded with sources: [" + String.join(", ", config.sources) + "] Resolving JARs to jumpload...");

		ResolutionContext resCtx = new ResolutionContextImpl(config, environmentDiscoverer, argsParsed);
		// Resolve metadata for jars to download
		List<MetadataResolutionResult> metadataResolutionResults;
		try {
			metadataResolutionResults = ResolutionProcessor.processMetadata(resCtx);
		} catch (IOException e) {
			ErrorMessages.showFatalMessage("Jumploader failed to load", "Failed to resolve metadata: " + e.getClass().getTypeName() + ": " + e.getLocalizedMessage(), LOGGER);
			throw new RuntimeException("Failed to resolve metadata", e);
		}
		// Download and resolve URLs for jars
		List<URL> loadUrls;
		try {
			loadUrls = ResolutionProcessor.resolveJars(metadataResolutionResults, resCtx);
		} catch (IOException e) {
			ErrorMessages.showFatalMessage("Jumploader failed to load", "Failed to resolve jars: " + e.getClass().getTypeName() + ": " + e.getLocalizedMessage(), LOGGER);
			throw new RuntimeException("Failed to resolve jars", e);
		} catch (PreDownloadCheck.PreDownloadCheckException e) {
			ErrorMessages.showFatalMessage("Jumploader failed to load", "Failed to download jar: " + e.getMessage(), LOGGER);
			throw new RuntimeException("Failed to download jar", e);
		}

		// Replace the classpath URLs
		try {
			ClasspathReplacer.replaceClasspath(loadUrls);
		} catch (URISyntaxException e) {
			ErrorMessages.showFatalMessage("Jumploader failed to load", "Failed to parse URL in replacement classpath: " + e.getClass().getTypeName() + ": " + e.getLocalizedMessage(), LOGGER);
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

		String mainClassName = null;
		boolean isFabric = false;
		if (config.overrideMainClass != null) {
			mainClassName = config.overrideMainClass;
		} else {
			// Read main class from jar sources - later sources override earlier sources
			for (int i = 0; i < metadataResolutionResults.size(); i++) {
				String resMainClass = metadataResolutionResults.get(i).mainClass;
				if (resMainClass != null) {
					mainClassName = resMainClass;
					isFabric = config.sources.get(i).equals("fabric");
				}
			}
		}
		if (mainClassName == null) {
			ErrorMessages.showFatalMessage("Jumploader failed to load", "Sources [" + String.join(", ", config.sources) + "] did not provide a main class!", LOGGER);
			throw new RuntimeException("Sources [" + String.join(", ", config.sources) + "] did not provide a main class!");
		}

		LOGGER.info("Jumping to new loader, main class: " + mainClassName);
		// Attempt to launch mainClass with the classloader
		Class<?> mainClass;
		try {
			mainClass = newLoader.loadClass(mainClassName);
		} catch (ClassNotFoundException e) {
			String clsName = mainClassName.substring(mainClassName.lastIndexOf(".") + 1);
			String msg = isFabric ? "Failed to load " + clsName + " (Fabric) from " + mainClassName :
				"Failed to load " + clsName + " from " + mainClassName;

			ErrorMessages.showFatalMessage("Jumploader failed to load", msg, LOGGER);
			throw new RuntimeException(msg, e);
		}
		if (mainClass != null) {
			try {
				Method main = mainClass.getMethod("main", String[].class);
				main.invoke(null, new Object[] {argsParsed.getArgsArray()});
			} catch (NoSuchMethodException | IllegalAccessException e) {
				String clsName = mainClassName.substring(mainClassName.lastIndexOf(".") + 1);
				String msg = isFabric ? "Failed to invoke " + clsName + " (Fabric) from " + mainClassName :
					"Failed to invoke " + clsName + " from " + mainClassName;

				ErrorMessages.showFatalMessage("Jumploader failed to load", msg, LOGGER);
				throw new RuntimeException(msg, e);
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
			throw new ThreadCloseException("Jumploader is closing the main thread, not an error!");
		}
		System.exit(1);
	}

	private static class ThreadCloseException extends RuntimeException {
		public ThreadCloseException(String reason) {
			super(reason);
		}

		@Override
		public void printStackTrace(PrintStream s) {
			// Do nothing!
		}

		@Override
		public void printStackTrace(PrintWriter s) {
			// Do nothing!
		}
	}

	@SuppressWarnings("rawtypes")
	@Nonnull
	@Override
	public List<ITransformer> transformers() {
		return Collections.emptyList();
	}

}
