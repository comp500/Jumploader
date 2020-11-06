package link.infra.jumploader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import link.infra.jumploader.resolution.EnvironmentDiscoverer;
import link.infra.jumploader.resolution.sources.SourcesRegistry;
import link.infra.jumploader.resolution.ui.messages.ErrorMessages;
import link.infra.jumploader.util.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("CanBeFinal")
public class ConfigFile {
	private transient final Path destFile;
	private transient boolean dirty = false;
	private transient static final Logger LOGGER = LogManager.getLogger();

	@Expose
	public int configVersion = 2;

	// A list of sources to load JARs from. The order matters - sources specified later can override
	// settings specified by earlier sources.
	@Expose
	public List<String> sources = SourcesRegistry.getDefaultSources();
	// Overrides the game version launched by Forge
	@Expose
	public String gameVersion = "current";
	// Specifies the side to launch, defaults to the side Forge is launching
	@Expose
	public Side gameSide = null;
	// Disable the user interface - temporary fix for crashes on Linux!
	// ... not sure if these crashes still happen, please report them if they do!
	@Expose
	public boolean disableUI = false;
	// Load JARs from a folder, requires adding "folder" to the sources list
	@Expose
	public String loadJarsFromFolder = null;
	// Call this main class instead of the class determined by the source list
	@Expose
	public String overrideMainClass = null;
	// TODO: Rewrite everything according to https://medium.com/@sdboyer/so-you-want-to-write-a-package-manager-4ae9c17d9527
	// TODO: - all the metadata can be supplied with modpacks - as long as it's ensured that the files are downloaded from fabricmc/mojang
	// Use this Fabric loader version, if possible, instead of the latest one
	// This should be automatically set on first load to the version that is loaded, if it does not already exist
	// Then, updating the Fabric loader version in a modpack is an explicit action - and is set by the modpack
	@Expose
	public String pinFabricLoaderVersion = null;

	// Legacy config file detection
	@Expose(serialize = false)
	public AutoconfOptions autoconfig = null;

	private static class AutoconfOptions {
		@Expose
		public boolean enable = true;
		@Expose
		public String handler = "fabric";
		@Expose
		public boolean forceUpdate = false;
		@Expose
		public String side = null;
		@Expose
		public String gameVersion = null;
	}

	private ConfigFile(Path destFile) {
		this.destFile = destFile;
	}

	public static ConfigFile read(EnvironmentDiscoverer environmentDiscoverer) throws JsonParseException, IOException {
		if (Files.exists(environmentDiscoverer.configFile)) {
			Gson gson = new GsonBuilder()
				.registerTypeAdapter(ConfigFile.class, (InstanceCreator<ConfigFile>)(type) -> new ConfigFile(environmentDiscoverer.configFile))
				.excludeFieldsWithoutExposeAnnotation()
				.create();
			try (InputStreamReader isr = new InputStreamReader(Files.newInputStream(environmentDiscoverer.configFile))) {
				ConfigFile loadedFile = gson.fromJson(isr, ConfigFile.class);
				if (loadedFile != null) {
					// Check if the loaded configuration is version 1.0
					if (loadedFile.autoconfig != null || loadedFile.configVersion < 2) {
						// Warn the user, if they've changed any settings, that these settings will be reset
						if (loadedFile.autoconfig != null) {
							if (!loadedFile.autoconfig.enable || !loadedFile.autoconfig.handler.equals("fabric")
								|| loadedFile.autoconfig.forceUpdate || loadedFile.autoconfig.side != null || loadedFile.autoconfig.gameVersion != null) {
								ErrorMessages.showInfoMessage("Jumploader 2.0 update", "Please note: Configuration changes from Jumploader 1.0.x will be reset due to changes in Jumploader 2.0", LOGGER);
							}
						}
						LOGGER.info("Jumploader 1.0.x config file has been detected, your settings have been reset!");
						ConfigFile newFile = new ConfigFile(environmentDiscoverer.configFile);
						newFile.dirty = true;
						return newFile;
					}
					if (loadedFile.gameSide != null) {
						environmentDiscoverer.updateForSide(loadedFile.gameSide);
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
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls()
			// Normal JsonAdapter annot doesn't serialise nulls properly
			.registerTypeAdapter(Side.class, new Side.Adapter())
			.excludeFieldsWithoutExposeAnnotation().create();
		try (OutputStreamWriter osw = new OutputStreamWriter(Files.newOutputStream(destFile))) {
			gson.toJson(this, osw);
		}
		dirty = false;
	}
}
