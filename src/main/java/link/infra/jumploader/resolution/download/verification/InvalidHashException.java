package link.infra.jumploader.resolution.download.verification;

import java.io.IOException;

public class InvalidHashException extends IOException {
	public final String downloadUrl;
	public final String expectedHash;
	public final String hashFound;

	public InvalidHashException(String expectedHash, String hashFound, String downloadUrl) {
		this.expectedHash = expectedHash;
		this.hashFound = hashFound;
		this.downloadUrl = downloadUrl;
	}
}
