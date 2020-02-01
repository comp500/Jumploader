package link.infra.jumploader.download.ui;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class DirectionLayout extends ArrayList<Component> implements Layout {
	private int parentWidth = 100;
	private int parentHeight = 100;
	private int preferredWidth = 100;
	private int preferredHeight = 100;
	private final Direction direction;

	public enum Direction {
		HORIZONTAL,
		VERTICAL
	}

	public DirectionLayout(Direction direction) {
		this.direction = direction;
	}

	@Override
	public void render() {
		float spacing = 0f;
		if (direction == Direction.HORIZONTAL) {
			spacing = ((float)(parentWidth - preferredWidth)) / (size() + 1);
		} else if (direction == Direction.VERTICAL) {
			spacing = ((float)(parentHeight - preferredHeight)) / (size() + 1);
		}
		GL11.glPushMatrix();
		if (direction == Direction.HORIZONTAL) {
			GL11.glTranslatef(spacing, 0f, 0f);
		} else if (direction == Direction.VERTICAL) {
			GL11.glTranslatef(0f, spacing, 0f);
		}
		for (Component component : this) {
			if (direction == Direction.HORIZONTAL) {
				component.render();
				GL11.glTranslatef(component.getPreferredWidth(), 0f, 0f);
				GL11.glTranslatef(spacing, 0f, 0f);
			} else if (direction == Direction.VERTICAL) {
				component.render();
				GL11.glTranslatef(0f, component.getPreferredHeight(), 0f);
				GL11.glTranslatef(0f, spacing, 0f);
			}
		}
		GL11.glPopMatrix();
	}

	@Override
	public void updateSize(int width, int height) {
		this.parentWidth = width;
		this.parentHeight = height;
		if (direction == Direction.HORIZONTAL) {
			preferredWidth = 0;
			preferredHeight = 0;
			for (Component component : this) {
				preferredWidth += component.getPreferredWidth();
				if (component.getPreferredHeight() > preferredHeight) {
					preferredHeight = component.getPreferredHeight();
				}
				component.updateSize(component.getPreferredWidth(), height);
			}
		} else if (direction == Direction.VERTICAL) {
			preferredWidth = 0;
			preferredHeight = 0;
			for (Component component : this) {
				preferredHeight += component.getPreferredHeight();
				if (component.getPreferredWidth() > preferredWidth) {
					preferredWidth = component.getPreferredWidth();
				}
				component.updateSize(width, component.getPreferredHeight());
			}
		}
	}

	@Override
	public int getPreferredWidth() {
		return preferredWidth;
	}

	@Override
	public int getPreferredHeight() {
		return preferredHeight;
	}
}
