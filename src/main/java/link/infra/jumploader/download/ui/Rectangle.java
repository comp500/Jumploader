package link.infra.jumploader.download.ui;

import org.lwjgl.opengl.GL11;

public class Rectangle implements Component {
	private final int width;
	private final int height;
	private int parentWidth = 100;
	private int parentHeight = 100;
	private final float r;
	private final float g;
	private final float b;
	private final float a;

	public Rectangle(int width, int height, float r, float g, float b) {
		this(width, height, r, g, b, 0f);
	}

	public Rectangle(int width, int height, float r, float g, float b, float a) {
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
		GL11.glVertex2f(0, height == -1 ? parentHeight : height);
		GL11.glVertex2f(width == -1 ? parentWidth : width, height == -1 ? parentHeight : height);
		GL11.glVertex2f(width == -1 ? parentWidth : width, 0);

		GL11.glEnd();
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	@Override
	public void free() {}

	@Override
	public void updateSize(int width, int height) {
		parentWidth = width;
		parentHeight = height;
	}

	@Override
	public int getMinimumWidth() {
		return width == -1 ? parentWidth : width;
	}

	@Override
	public int getMinimumHeight() {
		return height == -1 ? parentHeight : height;
	}
}
