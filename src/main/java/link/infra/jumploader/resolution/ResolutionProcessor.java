package link.infra.jumploader.resolution;

import link.infra.jumploader.Jumploader;
import link.infra.jumploader.resolution.download.BytesReportingInputStream;
import link.infra.jumploader.resolution.download.DownloadWorkerManager;
import link.infra.jumploader.resolution.download.PreDownloadCheck;
import link.infra.jumploader.resolution.download.verification.InvalidHashException;
import link.infra.jumploader.resolution.sources.*;
import link.infra.jumploader.resolution.ui.GUIManager;
import link.infra.jumploader.resolution.ui.messages.ErrorMessages;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ResolutionProcessor {
	private static final Logger LOGGER = LogManager.getLogger();

	public static List<MetadataResolutionResult> processMetadata(ResolutionContext ctx) throws IOException {
		List<MetadataResolutionResult> results = new ArrayList<>();
		MetadataCacheHelper cacheHelper = new MetadataCacheHelper(ctx.getArguments());
		for (String sourceId : ctx.getConfigFile().sources) {
			ResolvableJarSource<? extends MetadataCacheHelper.InvalidationKey<?>> source = SourcesRegistry.getSource(sourceId);
			results.add(doMetaResolve(sourceId, source, cacheHelper, ctx));
		}
		return results;
	}

	private static <T extends MetadataCacheHelper.InvalidationKey<T>> MetadataResolutionResult doMetaResolve(String sourceId, ResolvableJarSource<T> source, MetadataCacheHelper cacheHelper, ResolutionContext ctx) throws IOException {
		MetadataCacheHelper.MetadataCacheView view = cacheHelper.viewCache(sourceId, source.getInvalidationKey(ctx));
		return source.resolve(view, ctx);
	}

	public static List<URL> resolveJars(List<MetadataResolutionResult> metadataResolutionResults, ResolutionContext ctx) throws IOException, PreDownloadCheck.PreDownloadCheckException {
		List<URL> urls = new ArrayList<>();
		List<ResolvableJar> downloadQueue = new ArrayList<>();

		for (MetadataResolutionResult meta : metadataResolutionResults) {
			for (ResolvableJar jar : meta.jars) {
				// Add the URL
				URL jarUrl = jar.path.toUri().toURL();
				urls.add(jarUrl);
				Path tmpPath = jar.path.resolveSibling(jar.path.getFileName() + ".tmp");
				// First, check if the file already exists
				if (Files.exists(jar.path)) {
					// If it has no download source, we have succeeded!
					if (jar.url == null) {
						LOGGER.info("Found JAR: " + jarUrl);
						continue;
					}

					// If the .tmp file exists, check the hash of the current file and check it is correct - if it is
					// incorrect, we need to redownload, but if it is correct just delete the .tmp file
					if (Files.exists(tmpPath)) {
						// If .tmp exists and there is no hash verifier, redownload
						if (jar.hashVerifier != null) {
							boolean hashIsValid = true;
							try (InputStream read = jar.hashVerifier.getVerifier(Files.newInputStream(jar.path))) {
								// Read the stream to completion - using hashVerifier to check the hash
								byte[] buf = new byte[2048];
								while (true) {
									// Skip doesn't work as it bypasses the hash verification
									if (read.read(buf) < 0) {
										break;
									}
								}
							} catch (InvalidHashException ignored) {
								hashIsValid = false;
							}
							if (hashIsValid) {
								Files.delete(tmpPath);
								LOGGER.info("Found JAR: " + jarUrl);
								continue;
							}
						}
					} else {
						LOGGER.info("Found JAR: " + jarUrl);
						continue;
					}
				}
				// Download file
				if (jar.url != null) {
					if (jar.downloadCheck != null) {
						jar.downloadCheck.check();
					}
					LOGGER.info("Queueing download: " + jar.friendlyName);
					downloadQueue.add(jar);
				} else {
					// Doesn't have a source, and we can't find it!
					throw new RuntimeException("Could not locate file " + jar.path);
				}
			}
		}

		if (!downloadQueue.isEmpty()) {
			DownloadWorkerManager<URL> workerManager = new DownloadWorkerManager<>();
			for (ResolvableJar jar : downloadQueue) {
				workerManager.queueWorker(status -> {
					Files.createDirectories(jar.path.getParent());

					URLConnection conn = jar.url.openConnection();
					conn.setRequestProperty("User-Agent", Jumploader.USER_AGENT);
					conn.setRequestProperty("Accept", "application/octet-stream");

					int contentLength = conn.getContentLength();
					Path tmpPath = jar.path.resolveSibling(jar.path.getFileName() + ".tmp");
					try (InputStream res = jar.hashVerifier != null ? jar.hashVerifier.getVerifier(conn.getInputStream()) : conn.getInputStream();
						 BytesReportingInputStream bris = new BytesReportingInputStream(res, status, contentLength)) {
						Files.copy(bris, tmpPath, StandardCopyOption.REPLACE_EXISTING);
						Files.move(tmpPath, jar.path);
					} catch (InvalidHashException e) {
						Files.deleteIfExists(jar.path);
						Files.deleteIfExists(tmpPath);
						throw e;
					}

					return jar.url;
				});
			}

			if (!ctx.useUI()) {
				while (!workerManager.isDone()) {
					LOGGER.info("Progress: " + (workerManager.getWorkerProgress() * 100) + "%");
					URL resolvedURL;
					try {
						resolvedURL = workerManager.pollResult(500);
					} catch (InvalidHashException e) {
						try {
							workerManager.shutdown();
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						ErrorMessages.showFatalMessage("Jumploader failed to load", "Hash mismatch for " +
							e.downloadUrl + "\r\nExpected " + e.expectedHash + " but found " + e.hashFound + ".\r\nIs your internet connection working?", LOGGER);
						throw new RuntimeException("Failed to download jar");
					} catch (IOException e) {
						try {
							workerManager.shutdown();
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						throw e;
					} catch (ExecutionException | InterruptedException e) {
						try {
							workerManager.shutdown();
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						throw new RuntimeException(e);
					}
					if (resolvedURL != null) {
						LOGGER.info("Downloaded successfully: " + resolvedURL);
					}
				}

				try {
					workerManager.shutdown();
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			} else {
				GUIManager guiManager = new GUIManager(workerManager, ctx.getArguments());
				guiManager.init();

				boolean closeTriggered = false;
				while (!workerManager.isDone()) {
					if (guiManager.wasCloseTriggered()) {
						LOGGER.warn("Download window closed! Shutting down...");
						try {
							workerManager.shutdown();
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						closeTriggered = true;
						break;
					}
					guiManager.render();
					URL resolvedURL;
					try {
						resolvedURL = workerManager.pollResult();
					} catch (InvalidHashException e) {
						try {
							workerManager.shutdown();
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						ErrorMessages.showFatalMessage("Jumploader failed to load", "Hash mismatch for " +
							e.downloadUrl + "\r\nExpected " + e.expectedHash + " but found " + e.hashFound + ".\r\nIs your internet connection working?", LOGGER);
						guiManager.cleanup();
						throw new RuntimeException("Failed to download jar");
					} catch (IOException e) {
						try {
							workerManager.shutdown();
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						guiManager.cleanup();
						throw e;
					} catch (ExecutionException | InterruptedException e) {
						try {
							workerManager.shutdown();
						} catch (InterruptedException ex) {
							throw new RuntimeException(ex);
						}
						guiManager.cleanup();
						throw new RuntimeException(e);
					}
					if (resolvedURL != null) {
						LOGGER.info("Downloaded successfully: " + resolvedURL);
					}
				}

				try {
					workerManager.shutdown();
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}

				guiManager.cleanup();
				if (closeTriggered) {
					System.exit(1);
				}
			}
		}

		return urls;
	}
}
