package link.infra.jumploader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonParseException;
import link.infra.jumploader.resources.EnvironmentDiscoverer;
import link.infra.jumploader.resources.MavenJar;
import link.infra.jumploader.resources.MinecraftJar;
import link.infra.jumploader.resources.ResolvableJar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("CanBeFinal")
public class ConfigFile {
	private transient final Path destFile;
	private transient boolean dirty = false;

	// Download the files required to start the game. May cause problems if a download is needed!
	public boolean downloadRequiredFiles = true;
	// Force EnvironmentDiscoverer to use FallbackJarStorage, e.g. for running server installs
	public boolean forceFallbackStorage = false;
	// Don't update configuration if the side to be downloaded is not the same as the current side
	public boolean overrideInferredSide = false;
	public LaunchOptions launch = new LaunchOptions();
	public JarOptions jars = new JarOptions();
	public AutoconfOptions autoconfig = new AutoconfOptions();

	public static class LaunchOptions {
		public String mainClass = null;
	}

	public static class JarOptions {
		public List<MinecraftJar> minecraft = new ArrayList<>();
		public List<MavenJar> maven = new ArrayList<>();
	}

	public static class AutoconfOptions {
		public boolean enable = true;
		public String handler = "fabric";
		public boolean forceUpdate = false;
		public String side = null;
	}

	private ConfigFile(Path destFile) {
		this.destFile = destFile;
	}

	public static ConfigFile read(EnvironmentDiscoverer environmentDiscoverer) throws JsonParseException, IOException {
		if (Files.exists(environmentDiscoverer.configFile)) {
			Gson gson = new GsonBuilder()
				.registerTypeAdapter(ConfigFile.class, (InstanceCreator<ConfigFile>)(type) -> new ConfigFile(environmentDiscoverer.configFile))
				.registerTypeAdapter(MinecraftJar.class, (InstanceCreator<MinecraftJar>)(type) -> new MinecraftJar(environmentDiscoverer.jarStorage))
				.registerTypeAdapter(MavenJar.class, (InstanceCreator<MavenJar>)(type) -> new MavenJar(environmentDiscoverer.jarStorage))
				.create();
			try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(environmentDiscoverer.configFile))) {
				ConfigFile loadedFile = gson.fromJson(isr, ConfigFile.class);
				if (loadedFile != null) {
					if (loadedFile.forceFallbackStorage) {
						environmentDiscoverer.forceFallbackStorage();
					}
					return loadedFile;
				}
			}
		}
		ConfigFile newFile = new ConfigFile(environmentDiscoverer.configFile);
		newFile.dirty = true;
		return newFile;
	}

	public void saveIfDirty() throws IOException {
		if (dirty) {
			save();
		}
	}

	public void save() throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (OutputStreamWriter osw = new OutputStreamWriter(Files.newOutputStream(destFile))) {
			gson.toJson(this, osw);
		}
		dirty = false;
	}

	public List<ResolvableJar> getConfiguredJars() {
		List<ResolvableJar> jarList = new ArrayList<>();
		if (jars != null) {
			if (jars.minecraft != null) {
				jarList.addAll(jars.minecraft);
			}
			if (jars.maven != null) {
				jarList.addAll(jars.maven);
			}
		}
		return jarList;
	}

	public void resetConfiguredJars() {
		dirty = true;
		if (jars != null) {
			if (jars.minecraft != null) {
				jars.minecraft.clear();
			} else {
				jars.minecraft = new ArrayList<>();
			}
			if (jars.maven != null) {
				jars.maven.clear();
			} else {
				jars.maven = new ArrayList<>();
			}
		} else {
			jars = new JarOptions();
			jars.minecraft = new ArrayList<>();
			jars.maven = new ArrayList<>();
		}
	}
}
