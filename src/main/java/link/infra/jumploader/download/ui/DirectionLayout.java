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
			for (Component component : this) {
				component.updateSize(component.getPreferredWidth(), height);
			}
			// TODO: optimise this?
			preferredWidth = stream().mapToInt(Component::getPreferredWidth).sum();
			preferredHeight = stream().mapToInt(Component::getPreferredHeight).max().orElse(preferredHeight);
		} else if (direction == Direction.VERTICAL) {
			for (Component component : this) {
				component.updateSize(width, component.getPreferredHeight());
			}
			// TODO: optimise this?
			preferredWidth = stream().mapToInt(Component::getPreferredWidth).max().orElse(preferredWidth);
			preferredHeight = stream().mapToInt(Component::getPreferredHeight).sum();
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
