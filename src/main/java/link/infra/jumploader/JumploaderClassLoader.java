package link.infra.jumploader;

import link.infra.jumploader.specialcases.ClassBlacklist;
import link.infra.jumploader.specialcases.ClassRedefiner;
import link.infra.jumploader.specialcases.JarResourcePriorityModifier;
import link.infra.jumploader.specialcases.SpecialCaseHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

class JumploaderClassLoader extends URLClassLoader {
	private final List<ClassBlacklist> blacklists;
	private final List<JarResourcePriorityModifier> priorityModifiers;
	private final List<ClassRedefiner> classRedefiners;
	private final Map<String, Class<?>> redefinedClassCache = new HashMap<>();

	public JumploaderClassLoader(URL[] urls, SpecialCaseHandler specialCaseHandler) {
		super(urls);
		this.blacklists = specialCaseHandler.getImplementingCases(ClassBlacklist.class);
		this.priorityModifiers = specialCaseHandler.getImplementingCases(JarResourcePriorityModifier.class);
		this.classRedefiners = specialCaseHandler.getImplementingCases(ClassRedefiner.class);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		for (JarResourcePriorityModifier modifier : priorityModifiers) {
			if (modifier.shouldPrioritiseResource(name)) {
				List<URL> resList = Collections.list(super.getResources(name));
				modifier.modifyPriorities(resList);
				return Collections.enumeration(resList);
			}
		}
		return super.getResources(name);
	}

	@Override
	public URL getResource(String name) {
		for (JarResourcePriorityModifier modifier : priorityModifiers) {
			if (modifier.shouldPrioritiseResource(name)) {
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
		for (ClassRedefiner redefiner : classRedefiners) {
			if (redefiner.shouldRedefineClass(name)) {
				Class<?> cachedClass = redefinedClassCache.get(name);
				if (cachedClass != null) {
					return cachedClass;
				}

				InputStream classStream = getResourceAsStream(name.replace('.', '/') + ".class");
				if (classStream == null) {
					throw new ClassNotFoundException();
				}
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					byte[] buffer = new byte[8192];
					int length;
					while ((length = classStream.read(buffer)) != -1) {
						baos.write(buffer, 0, length);
					}
				} catch (IOException e) {
					throw new ClassNotFoundException();
				}
				byte[] classBytes = baos.toByteArray();

				Class<?> newClass = defineClass(name, classBytes, 0, classBytes.length, getClass().getProtectionDomain().getCodeSource());
				if (resolve) {
					resolveClass(newClass);
				}
				redefinedClassCache.put(name, newClass);
				return newClass;
			}
		}
		return super.loadClass(name, resolve);
	}
}
