package link.infra.jumploader.resolution;

import link.infra.jumploader.resolution.download.PreDownloadCheck;
import link.infra.jumploader.resolution.download.verification.HashVerifierProvider;

import java.net.URL;
import java.nio.file.Path;

public class ResolvableJar {
	public final URL url;
	public final Path file;
	public final HashVerifierProvider hashVerifier;
	public final PreDownloadCheck downloadCheck;
	public final String friendlyName;

	public ResolvableJar(Path file, String friendlyName) {
		this.url = null;
		this.file = file;
		this.friendlyName = friendlyName;
		this.hashVerifier = null;
		this.downloadCheck = null;
	}

	public ResolvableJar(URL url, Path file, HashVerifierProvider hashVerifier, String friendlyName) {
		this.url = url;
		this.file = file;
		this.hashVerifier = hashVerifier;
		this.friendlyName = friendlyName;
		this.downloadCheck = null;
	}

	public ResolvableJar(URL url, Path file, HashVerifierProvider hashVerifier, PreDownloadCheck downloadCheck, String friendlyName) {
		this.url = url;
		this.file = file;
		this.hashVerifier = hashVerifier;
		this.downloadCheck = downloadCheck;
		this.friendlyName = friendlyName;
	}
}
