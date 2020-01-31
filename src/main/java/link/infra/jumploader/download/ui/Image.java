package link.infra.jumploader.download.ui;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Image implements Component {
	private ByteBuffer imageData;
	private final int width;
	private final int height;
	private int winWidth = 100;
	private int winHeight = 100;
	private final int components;
	private int textureID;

	public Image(String resourcePath) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer imageCompressedData;
			try {
				imageCompressedData = ResourceLoader.loadResource(resourcePath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load image", e);
			}

			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);
			imageData = STBImage.stbi_load_from_memory(imageCompressedData, w, h, comp, 0);
			MemoryUtil.memFree(imageCompressedData);
			if (imageData == null) {
				throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
			}
			width = w.get(0);
			height = h.get(0);
			components = comp.get(0);
		}
	}

	@Override
	public void init() {
		textureID = GL11.glGenTextures();

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

		int format;
		if (components == 3) {
			if ((width & 3) != 0) {
				GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 2 - (width & 1));
			}
			format = GL11.GL_RGB;
		} else {
			int stride = width * 4;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int i = y * stride + x * 4;

					float alpha = (imageData.get(i + 3) & 0xFF) / 255.0f;
					imageData.put(i, (byte)Math.round(((imageData.get(i) & 0xFF) * alpha)));
					imageData.put(i + 1, (byte)Math.round(((imageData.get(i + 1) & 0xFF) * alpha)));
					imageData.put(i + 2, (byte)Math.round(((imageData.get(i + 2) & 0xFF) * alpha)));
				}
			}

			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

			format = GL11.GL_RGBA;
		}

		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, imageData);
		STBImage.stbi_image_free(imageData);
	}

	@Override
	public void render() {
		GL11.glPushMatrix();

		float scaleFactor = (float)winWidth / width;
		GL11.glTranslatef(winWidth * 0.5f,  winHeight * 0.5f, 0.0f);
		GL11.glScalef(scaleFactor, scaleFactor, 1f);
		GL11.glTranslatef(-width * 0.5f, -height * 0.5f, 0.0f);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0f, 0.0f);
		GL11.glVertex2f(0.0f, 0.0f);

		GL11.glTexCoord2f(1.0f, 0.0f);
		GL11.glVertex2f(width, 0.0f);

		GL11.glTexCoord2f(1.0f, 1.0f);
		GL11.glVertex2f(width, height);

		GL11.glTexCoord2f(0.0f, 1.0f);
		GL11.glVertex2f(0.0f, height);
		GL11.glEnd();

		GL11.glPopMatrix();
	}

	@Override
	public void free() {
		GL11.glDeleteTextures(textureID);
	}

	@Override
	public void updateSize(int width, int height) {
		this.winWidth = width;
		this.winHeight = height;
	}
}
