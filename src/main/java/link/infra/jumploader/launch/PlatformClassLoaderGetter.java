package link.infra.jumploader.launch;

public class PlatformClassLoaderGetter {
	private static final double JAVA_VERSION = Double.parseDouble(System.getProperty("java.specification.version", "0"));

	/**
	 * Getter for a ClassLoader that is above the bootstrap classloader in the hierarchy
	 * @return null on Java 8 or older, the platform classloader on Java 9 or newer
	 */
	public static ClassLoader get() {
		if (JAVA_VERSION > 8) {
			return Getter9.get();
		} else {
			return Getter8.get();
		}
	}

	private static class Getter8 {
		public static ClassLoader get() {
			return null;
		}
	}

	@SuppressWarnings("Since15")
	private static class Getter9 {
		public static ClassLoader get() {
			return ClassLoader.getPlatformClassLoader();
		}
	}
}
