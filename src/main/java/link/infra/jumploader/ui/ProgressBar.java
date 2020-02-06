package link.infra.jumploader.ui;

import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class ProgressBar implements Component {
	private final Supplier<Float> progressRetriever;
	private static final float height = 35f;
	private static final float inset = 3f;
	private static final float colorInset = 2 * inset;

	public ProgressBar(Supplier<Float> progressRetriever) {
		this.progressRetriever = progressRetriever;
	}

	@Override
	public void init() {}

	@Override
	public void render() {
		float progress = progressRetriever.get();
		if (progress < 0f) progress = 0f;
		if (progress > 1f) progress = 1f;

		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_CURRENT_BIT);
		GL11.glBegin(GL11.GL_QUADS);

		GL11.glColor4f(0, 0, 0, 0);
		GL11.glVertex2f(0, 0);
		GL11.glVertex2f(0, height);
		GL11.glVertex2f(currentWidth, height);
		GL11.glVertex2f(currentWidth, 0);

		GL11.glColor4f(1, 1, 1, 0);
		GL11.glVertex2f(inset, inset);
		GL11.glVertex2f(inset, height - inset);
		GL11.glVertex2f(currentWidth - inset, height - inset);
		GL11.glVertex2f(currentWidth - inset, inset);

		GL11.glColor4f(50f/255f, 162f/255f, 208f/255f, 0);
		GL11.glVertex2f(colorInset, colorInset);
		GL11.glVertex2f(colorInset, height - colorInset);
		GL11.glColor4f(92f/255f, 255f/255f, 180f/255f, 0);
		GL11.glVertex2f(currentWidth - colorInset, height - colorInset);
		GL11.glVertex2f(currentWidth - colorInset, colorInset);

		// Because I don't know how to clip a quad, I'm just going to paint another white quad over the gradient quad !!!!
		GL11.glColor4f(1, 1, 1, 0);
		GL11.glVertex2f(((currentWidth - (2*inset)) * progress) + inset, inset);
		GL11.glVertex2f(((currentWidth - (2*inset)) * progress) + inset, height - inset);
		GL11.glVertex2f(currentWidth - inset, height - inset);
		GL11.glVertex2f(currentWidth - inset, inset);

		GL11.glEnd();
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	@Override
	public void free() {}

	@Override
	public float getMinimumWidth() {
		return 0;
	}

	@Override
	public float getMinimumHeight() {
		return height;
	}

	private float currentWidth;

	@Override
	public void updateSize(float maximumWidth, float maximumHeight) {
		this.currentWidth = maximumWidth;
	}

	@Override
	public float getCurrentWidth() {
		return currentWidth;
	}

	@Override
	public float getCurrentHeight() {
		return height;
	}
}
