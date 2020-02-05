package link.infra.jumploader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonParseException;
import link.infra.jumploader.resources.MavenJar;
import link.infra.jumploader.resources.MinecraftJar;
import link.infra.jumploader.resources.ResolvableJar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("CanBeFinal")
public class ConfigFile {
	private transient final Path destFile;
	private transient boolean dirty = false;

	// TODO: implement
	public boolean downloadRequiredFiles = true;
	// TODO: implement
	public boolean updateFabricIfLoadedGameIsOld = true;
	public LaunchOptions launch = new LaunchOptions();
	public JarOptions jars = new JarOptions();

	public static class LaunchOptions {
		public String mainClass = "net.fabricmc.loader.launch.knot.KnotClient";
	}

	public static class JarOptions {
		public MinecraftJar[] minecraft;
		public MavenJar[] maven;
	}

	private ConfigFile(Path destFile) {
		this.destFile = destFile;
	}

	public static ConfigFile read(Path configFile) throws JsonParseException, IOException {
		if (Files.exists(configFile)) {
			Gson gson = new GsonBuilder().registerTypeAdapter(ConfigFile.class,
				(InstanceCreator<ConfigFile>)(type) -> new ConfigFile(configFile)).create();
			try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(configFile))) {
				ConfigFile loadedFile = gson.fromJson(isr, ConfigFile.class);
				if (loadedFile != null) {
					return loadedFile;
				}
			}
		}
		ConfigFile newFile = new ConfigFile(configFile);
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
				jarList.addAll(Arrays.asList(jars.minecraft));
			}
			if (jars.maven != null) {
				jarList.addAll(Arrays.asList(jars.maven));
			}
		}
		return jarList;
	}
}
