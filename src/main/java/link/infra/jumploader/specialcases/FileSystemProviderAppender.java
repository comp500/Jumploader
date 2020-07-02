package link.infra.jumploader.specialcases;

import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.ReflectionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class FileSystemProviderAppender implements ReflectionHack {
	private final Logger LOGGER = LogManager.getLogger();

	private void loadProvidersFromClassLoader(ClassLoader classLoader, List<FileSystemProvider> list) {
		ServiceLoader<FileSystemProvider> loader = ServiceLoader.load(FileSystemProvider.class, classLoader);

		for (FileSystemProvider provider: loader) {
			String scheme = provider.getScheme();
			if (!scheme.equalsIgnoreCase("file")) {
				if (list.stream().noneMatch(p -> p.getScheme().equalsIgnoreCase(scheme))) {
					list.add(provider);
				}
			}
		}
	}

	@Override
	public void applyReflectionHack(ClassLoader loadingClassloader) {
		// Ensure the existing providers are loaded first
		FileSystemProvider.installedProviders();

		try {
			final Object lock = ReflectionUtil.reflectStaticField(FileSystemProvider.class, "lock");
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (lock) {
				// Load providers from the Jumploader classloader, and add them to the FileSystemProvider list
				ReflectionUtil.<List<FileSystemProvider>>transformStaticField(FileSystemProvider.class, "installedProviders", existingProviders -> {
					List<FileSystemProvider> newList = new ArrayList<>(existingProviders);
					AccessController.doPrivileged((PrivilegedAction<Void>) () -> {loadProvidersFromClassLoader(loadingClassloader, newList); return null;});
					return newList;
				});
			}
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			LOGGER.warn("Failed to fix FileSystemProvider loading, jar-in-jar may not work", e);
		}
	}

	@Override
	public boolean shouldApply(List<URL> loadedJars, String mainClass, ParsedArguments gameArguments) {
		return true;
	}
}
