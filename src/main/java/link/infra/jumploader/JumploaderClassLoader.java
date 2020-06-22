package link.infra.jumploader;

import link.infra.jumploader.specialcases.ClassBlacklist;
import link.infra.jumploader.specialcases.ClassRedefiner;
import link.infra.jumploader.specialcases.SpecialCaseHandler;
import link.infra.jumploader.specialcases.URLBlacklist;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

class JumploaderClassLoader extends URLClassLoader {
	private final List<ClassBlacklist> blacklists;
	private final List<ClassRedefiner> classRedefiners;
	private final List<URLBlacklist> urlBlacklists;
	private final ClassLoader parent = JumploaderClassLoader.class.getClassLoader();

	public JumploaderClassLoader(URL[] urls, SpecialCaseHandler specialCaseHandler) {
		super(urls, JumploaderClassLoader.class.getClassLoader());
		this.blacklists = specialCaseHandler.getImplementingCases(ClassBlacklist.class);
		this.classRedefiners = specialCaseHandler.getImplementingCases(ClassRedefiner.class);
		this.urlBlacklists = specialCaseHandler.getImplementingCases(URLBlacklist.class);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		// Prioritise self over parent classloader
		List<URL> urls = Collections.list(findResources(name));
		urls.addAll(Collections.list(parent.getResources(name)));
		// Hide blacklisted URLs, as fabric API looks at the back of the list anyway
		urls.removeIf(u -> urlBlacklists.stream().filter(b -> b.shouldBlacklistUrl(u)).limit(1).count() > 0);
		return Collections.enumeration(urls);
	}

	@Override
	public URL getResource(String name) {
		// Prioritise self over parent classloader
		URL url = findResource(name);
		if (url != null) {
			return url;
		}
		// TODO: use the blacklist in getResource as well?
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
		// Prioritise classes from self to parent classloader
		synchronized (getClassLoadingLock(name)) {
			Class<?> c = findLoadedClass(name);
			if (c == null) {
				try {
					// Try to find from self
					c = findClass(name);
				} catch (ClassNotFoundException e) {
					// If it failed, try the parent classloader
					return super.loadClass(name, resolve);
				}
			}
			if (resolve) {
				resolveClass(c);
			}
			return c;
		}
	}
}
