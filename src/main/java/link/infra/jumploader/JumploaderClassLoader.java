package link.infra.jumploader;

import link.infra.jumploader.specialcases.ClassBlacklist;
import link.infra.jumploader.specialcases.ClassRedefiner;
import link.infra.jumploader.specialcases.SpecialCaseHandler;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

class JumploaderClassLoader extends URLClassLoader {
	private final List<ClassBlacklist> blacklists;
	private final List<ClassRedefiner> classRedefiners;
	private final ClassLoader parent = JumploaderClassLoader.class.getClassLoader();

	public JumploaderClassLoader(URL[] urls, SpecialCaseHandler specialCaseHandler) {
		super(urls, JumploaderClassLoader.class.getClassLoader());
		this.blacklists = specialCaseHandler.getImplementingCases(ClassBlacklist.class);
		this.classRedefiners = specialCaseHandler.getImplementingCases(ClassRedefiner.class);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		// Prioritise self over parent classloader
		List<URL> urls = Collections.list(findResources(name));
		urls.addAll(Collections.list(parent.getResources(name)));
		return Collections.enumeration(urls);
	}

	@Override
	public URL getResource(String name) {
		// Prioritise self over parent classloader
		URL url = findResource(name);
		if (url != null) {
			return url;
		}
		return parent.getResource(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		for (ClassBlacklist blacklist : blacklists) {
			if (blacklist.shouldBlacklistClass(name)) {
				throw new ClassNotFoundException();
			}
		}
		for (ClassRedefiner redefiner : classRedefiners) {
			if (redefiner.shouldRedefineClass(name)) {
				synchronized (getClassLoadingLock(name)) {
					// findLoadedClass doesn't check the parent classloader - so we know that this is the right one
					Class<?> existingClass = findLoadedClass(name);
					if (existingClass != null) {
						return existingClass;
					}

					Class<?> newClass = findClass(name);
					if (resolve) {
						resolveClass(newClass);
					}
					return newClass;
				}
			}
		}
		return super.loadClass(name, resolve);
	}
}
