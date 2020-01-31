package link.infra.jumploader.download.ui;

import org.lwjgl.opengl.GL11;

public class Container implements Component {
	private Component component;
	private int winWidth = 100;
	private int winHeight = 100;

	public Container(Component containedComponent) {
		component = containedComponent;
	}

	@Override
	public void init() {
		component.init();
	}

	@Override
	public void render() {
		GL11.glPushMatrix();

		//GL11.glScalef(0.5f, 1f, 1f);

		//float scaleFactor = (float)winWidth / width;
		//GL11.glTranslatef(winWidth * 0.5f,  winHeight * 0.5f, 0.0f);
		//GL11.glScalef(scaleFactor, scaleFactor, 1f);
		//GL11.glTranslatef(-width * 0.5f, -height * 0.5f, 0.0f);
		if (winWidth > 300) {
			GL11.glTranslatef((float)winWidth / 4, 1f, 1f);
		}

		component.render();

		GL11.glPopMatrix();
	}

	@Override
	public void free() {
		component.free();
	}

	@Override
	public void updateSize(int width, int height) {
		this.winWidth = width;
		this.winHeight = height;
		if (winWidth > 300) {
			component.updateSize(width / 2, height);
		} else {
			component.updateSize(width, height);
		}
	}
}
