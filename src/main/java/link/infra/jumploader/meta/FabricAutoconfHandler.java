package link.infra.jumploader.meta;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import link.infra.jumploader.ConfigFile;
import link.infra.jumploader.resources.EnvironmentDiscoverer;
import link.infra.jumploader.resources.MavenJar;
import link.infra.jumploader.resources.MinecraftJar;
import link.infra.jumploader.resources.ParsedArguments;
import link.infra.jumploader.util.RequestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class FabricAutoconfHandler implements AutoconfHandler {
	public FabricAutoconfHandler() {}

	private static final String FABRIC_MAVEN = "https://maven.fabricmc.net/";

	private static boolean shouldUpdate(ConfigFile configFile, ParsedArguments args, EnvironmentDiscoverer env) {
		if (configFile.autoconfig.forceUpdate || configFile.jars == null) {
			return true;
		}
		return !AutoconfHandler.doesConfigContainGame(args.mcVersion, args.inferredSide, configFile);
	}

	@Override
	public void updateConfig(ConfigFile configFile, ParsedArguments args, EnvironmentDiscoverer env) {
		if (shouldUpdate(configFile, args, env)) {
			// Default to the inferred side
			String side = configFile.autoconfig.side != null ? configFile.autoconfig.side : args.inferredSide;
			try {
				URL loaderJsonUrl = new URI("https", "meta.fabricmc.net", "/v2/versions/loader/" + args.mcVersion, null).toURL();
				JsonArray manifestData = RequestUtils.getJson(loaderJsonUrl).getAsJsonArray();
				if (manifestData.size() == 0) {
					throw new RuntimeException("Failed to update configuration: no Fabric versions available!");
				}

				configFile.resetConfiguredJars();
				JsonObject latestLoaderData = manifestData.get(0).getAsJsonObject();

				configFile.jars.minecraft.add(new MinecraftJar(env.jarStorage, args.mcVersion, side));

				String loaderMaven = latestLoaderData.getAsJsonObject("loader").get("maven").getAsString();
				configFile.jars.maven.add(new MavenJar(env.jarStorage, loaderMaven, FABRIC_MAVEN));
				String intermediaryMaven = latestLoaderData.getAsJsonObject("intermediary").get("maven").getAsString();
				configFile.jars.maven.add(new MavenJar(env.jarStorage, intermediaryMaven, FABRIC_MAVEN));

				JsonObject launcherMeta = latestLoaderData.getAsJsonObject("launcherMeta");

				JsonObject mainClass = launcherMeta.getAsJsonObject("mainClass");
				configFile.launch.mainClass = mainClass.get(side).getAsString();

				JsonObject libraries = launcherMeta.getAsJsonObject("libraries");
				for (JsonElement library : libraries.getAsJsonArray("common")) {
					JsonObject libraryObj = library.getAsJsonObject();
					configFile.jars.maven.add(new MavenJar(
						env.jarStorage,
						libraryObj.get("name").getAsString(),
						libraryObj.get("url").getAsString()
					));
				}
				for (JsonElement library : libraries.getAsJsonArray(side)) {
					JsonObject libraryObj = library.getAsJsonObject();
					configFile.jars.maven.add(new MavenJar(
						env.jarStorage,
						libraryObj.get("name").getAsString(),
						libraryObj.get("url").getAsString()
					));
				}
			} catch (IOException | URISyntaxException e) {
				throw new RuntimeException("Failed to update configuration to latest Fabric version", e);
			}
		}
	}
}
