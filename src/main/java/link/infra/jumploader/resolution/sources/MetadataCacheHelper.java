package link.infra.jumploader.resolution.sources;

import com.google.gson.*;
import link.infra.jumploader.launch.arguments.ParsedArguments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Horrifically overengineered cache invalidation and storage system, storing files based on the hash
 * of the source location.
 */
public class MetadataCacheHelper {
	private final Path cacheFolderPath;
	private final Path cacheIndexPath;
	private final Map<String, InvalidationKey<?>> indexValues = new HashMap<>();

	private final Logger LOGGER = LogManager.getLogger();

	public interface InvalidationKey<T extends InvalidationKey<T>> {
		boolean isValid(T key);
	}

	// TODO: forceUpdate?

	public interface InvalidationUpdateSourcer<T, E extends Throwable> {
		T get() throws IOException, E;
	}

	public interface MetadataCacheView {
		boolean isValid(String name);
		Path resolve(String name);
		void completeUpdate() throws IOException;

		default <E extends Throwable> byte[] getAsBytes(String name, InvalidationUpdateSourcer<byte[], E> updater) throws IOException, E {
			if (isValid(name)) {
				return Files.readAllBytes(resolve(name));
			} else {
				byte[] bytes = updater.get();
				Files.write(resolve(name), bytes);
				return bytes;
			}
		}

		default <E extends Throwable> String getAsString(String name, InvalidationUpdateSourcer<String, E> updater) throws IOException, E {
			return new String(getAsBytes(name, () -> updater.get().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		}
	}

	private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

	public interface GsonMetadataCacheView extends MetadataCacheView {
		default <T, E extends Throwable> T getObject(String name, Class<T> type, InvalidationUpdateSourcer<T, E> updater) throws IOException, E {
			return PRETTY_GSON.fromJson(getAsString(name, () -> PRETTY_GSON.toJson(updater.get(), type)), type);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends InvalidationKey<T>> MetadataCacheView viewCache(String sourceId, T invalidationKey) {
		T oldInvKey;
		if (indexValues.containsKey(sourceId)) {
			oldInvKey = (T) indexValues.get(sourceId);
		} else {
			oldInvKey = null;
			indexValues.put(sourceId, invalidationKey);
		}
		return new MetadataCacheView() {
			@Override
			public boolean isValid(String name) {
				// Check the invalidation key
				if (oldInvKey == null || !invalidationKey.isValid(oldInvKey)) {
					return false;
				}
				// Check for existence of the file
				return Files.exists(cacheFolderPath.resolve(name));
			}

			@Override
			public Path resolve(String name) {
				return cacheFolderPath.resolve(name);
			}

			@Override
			public void completeUpdate() throws IOException {
				// Note that it's still important to update the key even if the old one is valid
				// - it could include a "date last updated" field
				indexValues.put(sourceId, invalidationKey);
				MetadataCacheHelper.this.save();
			}
		};
	}

	public MetadataCacheHelper(ParsedArguments args) throws IOException {
		cacheFolderPath = args.gameDir.resolve(".jumploader").resolve("metacache");
		Files.createDirectories(cacheFolderPath);
		cacheIndexPath = cacheFolderPath.resolve("index.json");

		JsonParser parser = new JsonParser();
		Gson gson = new Gson();
		try (InputStreamReader rdr = new InputStreamReader(Files.newInputStream(cacheIndexPath))) {
			JsonElement el = parser.parse(rdr);
			try {
				JsonObject obj = el.getAsJsonObject();
				obj.entrySet().forEach(entry -> {
					ResolvableJarSource<?> src = SourcesRegistry.getSource(entry.getKey());
					if (src != null) {
						InvalidationKey<?> storedSrcEntry = gson.fromJson(entry.getValue(), src.getInvalidationKeyType());
						indexValues.put(entry.getKey(), storedSrcEntry);
					} else {
						LOGGER.warn("Couldn't find jar source " + entry.getKey());
					}
				});
			} catch (IllegalStateException e) {
				LOGGER.warn("Failed to load cache file", e);
			}
		} catch (NoSuchFileException ignored) {
			// Ignore if there are no cached values
		}
	}

	private void save() throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (OutputStreamWriter osw = new OutputStreamWriter(Files.newOutputStream(cacheIndexPath))) {
			gson.toJson(indexValues, osw);
		}
	}
}
