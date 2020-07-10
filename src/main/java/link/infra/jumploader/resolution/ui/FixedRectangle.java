package link.infra.jumploader.resolution.ui;

import org.lwjgl.opengl.GL11;

public class FixedRectangle implements Component {
	private final int width;
	private final int height;
	private final float r;
	private final float g;
	private final float b;
	private final float a;

	public FixedRectangle(int width, int height, float r, float g, float b) {
		this(width, height, r, g, b, 0f);
	}

	public FixedRectangle(int width, int height, float r, float g, float b, float a) {
		this.width = width;
		this.height = height;
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	@Override
	public void init() {}

	@Override
	public void render() {
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_CURRENT_BIT);
		GL11.glBegin(GL11.GL_QUADS);

		GL11.glColor4f(r, g, b, a);
		GL11.glVertex2f(0, 0);
		GL11.glVertex2f(0, height);
		GL11.glVertex2f(width, height);
		GL11.glVertex2f(width, 0);

		GL11.glEnd();
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	@Override
	public void free() {}

	@Override
	public float getMinimumWidth() {
		return width;
	}

	@Override
	public float getMinimumHeight() {
		return height;
	}

	@Override
	public void updateSize(float maximumWidth, float maximumHeight) {}

	@Override
	public float getCurrentWidth() {
		return width;
	}

	@Override
	public float getCurrentHeight() {
		return height;
	}

	@Override
	public Grows getGrows() {
		return Grows.NEVER;
	}
}
