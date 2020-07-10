package link.infra.jumploader.resolution;

import java.io.IOException;

public class InvalidHashException extends IOException {
	public final String hashFound;

	public InvalidHashException(String hashFound) {
		this.hashFound = hashFound;
	}
}
