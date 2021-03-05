package link.infra.jumploader.resolution.ui;

import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ResourceLoader {
	private ResourceLoader() {}

	public static ByteBuffer loadResource(String resourcePath) throws IOException {
		InputStream stream = ResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath);
		if (stream == null) {
			throw new IOException("Resource not found!");
		}
		return loadStream(stream);
	}

	public static ByteBuffer loadStream(InputStream stream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int numRead;
		byte[] copyBuffer = new byte[16384];
		while ((numRead = stream.read(copyBuffer, 0, copyBuffer.length)) != -1) {
			buffer.write(copyBuffer, 0, numRead);
		}
		// Horribly inefficient but it works I guess
		ByteBuffer bufferOut = MemoryUtil.memAlloc(buffer.size());
		bufferOut.put(buffer.toByteArray());
		bufferOut.flip();
		return bufferOut;
	}
}
