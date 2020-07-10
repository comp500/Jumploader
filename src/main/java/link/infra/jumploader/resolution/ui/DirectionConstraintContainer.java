package link.infra.jumploader.resolution.ui;

import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class DirectionConstraintContainer implements Component {
	private final Component containedComponent;
	private final Direction direction;
	private final Function<Float, Float> sizeConstrainer;

	private float currentWidth;
	private float currentHeight;

	public DirectionConstraintContainer(Component containedComponent, Direction direction, Function<Float, Float> sizeConstrainer) {
		this.containedComponent = containedComponent;
		this.direction = direction;
		this.sizeConstrainer = sizeConstrainer;
	}

	@Override
	public void init() {
		containedComponent.init();
	}

	@Override
	public void render() {
		GL11.glPushMatrix();
		if (direction == Direction.HORIZONTAL) {
			GL11.glTranslatef((currentWidth - containedComponent.getCurrentWidth()) / 2f, 0f, 0f);
			containedComponent.render();
		} else if (direction == Direction.VERTICAL) {
			GL11.glTranslatef((currentHeight - containedComponent.getCurrentHeight()) / 2f, 0f, 0f);
			containedComponent.render();
		} else {
			throw new Direction.InvalidDirectionException();
		}
		GL11.glPopMatrix();
	}

	@Override
	public void free() {
		containedComponent.free();
	}

	@Override
	public float getMinimumWidth() {
		return containedComponent.getMinimumWidth();
	}

	@Override
	public float getMinimumHeight() {
		return containedComponent.getMinimumHeight();
	}

	@Override
	public void updateSize(float maximumWidth, float maximumHeight) {
		if (direction == Direction.HORIZONTAL) {
			containedComponent.updateSize(sizeConstrainer.apply(maximumWidth), maximumHeight);
			currentWidth = maximumWidth;
			currentHeight = containedComponent.getCurrentHeight();
		} else if (direction == Direction.VERTICAL) {
			containedComponent.updateSize(maximumWidth, sizeConstrainer.apply(maximumHeight));
			currentWidth = containedComponent.getCurrentWidth();
			currentHeight = maximumHeight;
		} else {
			throw new Direction.InvalidDirectionException();
		}
	}

	@Override
	public float getCurrentWidth() {
		return currentWidth;
	}

	@Override
	public float getCurrentHeight() {
		return currentHeight;
	}
}
