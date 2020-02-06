package link.infra.jumploader.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.annotation.Nonnull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Function;

public class SHA1HashingInputStream extends FilterInputStream {
	private final byte[] compareToHash;
	private final MessageDigest digest;

	public static class InvalidHashException extends IOException {
		public final String hashFound;

		public InvalidHashException(String hashFound) {
			this.hashFound = hashFound;
		}
	}

	public static Function<InputStream, InputStream> transformer(String compareToHash) {
		return inputStream -> new SHA1HashingInputStream(inputStream, compareToHash);
	}

	protected SHA1HashingInputStream(InputStream inputStream, String compareToHash) {
		super(inputStream);
		try {
			this.compareToHash = Hex.decodeHex(compareToHash.toCharArray());
			digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int read() throws IOException {
		int value = super.read();
		if (value == -1) {
			return value;
		}
		digest.update((byte) value);
		return value;
	}

	@Override
	public int read(@Nonnull byte[] b, int off, int len) throws IOException {
		int bytesRead = super.read(b, off, len);
		if (bytesRead > 0) {
			digest.update(b, off, bytesRead);
		}
		return bytesRead;
	}

	@Override
	public void reset() throws IOException {
		throw new IOException("SHA1HashingInputStream doesn't support reset()");
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void mark(int readlimit) {
		// Do nothing
	}

	private boolean alreadyClosed = false;

	@Override
	public void close() throws IOException {
		super.close();
		if (!alreadyClosed) {
			alreadyClosed = true;
			byte[] result = digest.digest();
			if (!Arrays.equals(result, compareToHash)) {
				throw new InvalidHashException(Hex.encodeHexString(result));
			}
		}
	}
}
