package link.infra.jumploader.resolution.sources;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import link.infra.jumploader.launch.arguments.ParsedArguments;
import link.infra.jumploader.resolution.HashUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Horrifically overengineered cache invalidation and storage system, storing files based on the hash
 * of the source location.
 */
public class MetadataCacheHelper {
	private final Path cacheFolderPath;
	private final Path cacheIndexPath;
	private final Map<String, SourceEntry<?>> indexValues = new HashMap<>();

	private final Logger LOGGER = LogManager.getLogger();

	public interface InvalidationKey<T extends InvalidationKey<T>> {
		boolean isValid(T key);
	}

	private class SourceEntry<T extends InvalidationKey<T>> {
		public T invalidationKey;
		private final List<FileEntry> files = new ArrayList<>();

		private class FileEntry {
			public final String source;
			public final Path path;

			public FileEntry(String source, Path path) {
				this.source = source;
				this.path = path;
			}
		}

		public SourceEntry(T invalidationKey) {
			this.invalidationKey = invalidationKey;
		}

		public FileEntry getOrAdd(String source) {
			for (FileEntry file : files) {
				if (file.source.equals(source)) {
					return file;
				}
			}
			String extension = ".json";
			int index = source.lastIndexOf('.');
			if (index > -1) {
				extension = source.substring(index);
			}
			FileEntry newFile = new FileEntry(source, cacheFolderPath.resolve(HashUtils.computeSHA256(source) + extension));
			files.add(newFile);
			return newFile;
		}

		public boolean hasSource(String source) {
			for (FileEntry file : files) {
				if (file.source.equals(source)) {
					return true;
				}
			}
			return false;
		}
	}

	// TODO: forceUpdate

	public interface MetadataCacheView {
		boolean isValid(String source);
		Path get(String source);
		void completeUpdate() throws IOException;
	}

	@SuppressWarnings("unchecked")
	public <T extends InvalidationKey<T>> MetadataCacheView viewCache(String sourceId, T invalidationKey) {
		SourceEntry<T> val;
		if (indexValues.containsKey(sourceId)) {
			val = (SourceEntry<T>) indexValues.get(sourceId);
		} else {
			val = new SourceEntry<>(invalidationKey);
			indexValues.put(sourceId, val);
		}
		return new MetadataCacheView() {
			@Override
			public boolean isValid(String source) {
				// Check the invalidation key
				if (!invalidationKey.isValid(val.invalidationKey)) {
					return false;
				}
				// Check for existence of the file
				if (!val.hasSource(source)) {
					return false;
				}
				return Files.exists(val.getOrAdd(source).path);
			}

			@Override
			public Path get(String source) {
				return val.getOrAdd(source).path;
			}

			@Override
			public void completeUpdate() throws IOException {
				val.invalidationKey = invalidationKey;
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
						Type type = TypeToken.getParameterized(SourceEntry.class, src.getInvalidationKeyType()).getType();
						SourceEntry<?> storedSrcEntry = gson.fromJson(entry.getValue(), type);
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
