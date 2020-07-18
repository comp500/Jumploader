package link.infra.jumploader.resolution.download.verification;

import java.io.IOException;

public class InvalidHashException extends IOException {
	// TODO: handle at the ResolvableJar level
	public final String expectedHash;
	public final String hashFound;

	public InvalidHashException(String expectedHash, String hashFound) {
		this.expectedHash = expectedHash;
		this.hashFound = hashFound;
	}
}
