package link.infra.jumploader.download.ui;

import link.infra.jumploader.util.SortedIterable;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class DirectionLayout implements Layout {
	private final Direction direction;
	private final Alignment alignment;
	private final boolean spaceBetween;

	/**
	 * A Layout that lays out components in order, in the direction given
	 * @param direction The leading direction to lay out the components in
	 */
	public DirectionLayout(Direction direction) {
		this(direction, Alignment.CENTER, false);
	}

	/**
	 * A Layout that lays out components in order, in the direction given
	 * @param direction The leading direction to lay out the components in
	 * @param alignment The alignment to align components in, in the non-leading direction
	 * @param spaceBetween Whether to put space between components, or space around them
	 */
	public DirectionLayout(Direction direction, Alignment alignment, boolean spaceBetween) {
		this.direction = direction;
		this.spaceBetween = spaceBetween;
		this.alignment = alignment;
	}

	private final ArrayList<Component> components = new ArrayList<>();
	private float remainingSpace;
	private float minimumWidth;
	private float minimumHeight;
	private float currentWidth;
	private float currentHeight;
	private float[] cachedAlignmentOffsets;
	private float[] cachedComponentWidths;
	private float[] cachedComponentHeights;

	/**
	 * updateSize() must be called after this DirectionLayout has been modified!
	 * TODO: call updateSize after modifying
	 */
	@Override
	public void render() {
		GL11.glPushMatrix();
		if (!spaceBetween) {
			if (direction == Direction.HORIZONTAL) {
				GL11.glTranslatef(remainingSpace / 2, 0f, 0f);
			} else if (direction == Direction.VERTICAL) {
				GL11.glTranslatef(0f, remainingSpace / 2, 0f);
			}
		}

		int i = 0;
		float individualSpacing = remainingSpace / cachedAlignmentOffsets.length;
		for (Component component : this) {
			if (direction == Direction.HORIZONTAL) {
				GL11.glTranslatef(0f, cachedAlignmentOffsets[i], 0f);
				component.render();
				GL11.glTranslatef(component.getMinimumWidth(), -cachedAlignmentOffsets[i], 0f);
				if (spaceBetween) {
					GL11.glTranslatef(individualSpacing, 0f, 0f);
				}
			} else if (direction == Direction.VERTICAL) {
				GL11.glTranslatef(cachedAlignmentOffsets[i], 0f, 0f);
				component.render();
				GL11.glTranslatef(-cachedAlignmentOffsets[i], component.getMinimumHeight(), 0f);
				if (spaceBetween) {
					GL11.glTranslatef(0f, individualSpacing, 0f);
				}
			}
			i++;
		}
		GL11.glPopMatrix();
	}

	private float calculateAlignmentOffset(float componentSize, float containerSize) {
		if (componentSize > containerSize) {
			return 0;
		}
		if (alignment == Alignment.START) {
			return 0;
		} else if (alignment == Alignment.CENTER) {
			return (containerSize - componentSize) / 2;
		} else if (alignment == Alignment.END) {
			return containerSize - componentSize;
		}
		throw new Alignment.InvalidAlignmentException();
	}

	@Override
	public float getMinimumWidth() {
		return minimumWidth;
	}

	@Override
	public float getMinimumHeight() {
		return minimumHeight;
	}

	private void updateMinimumSizes() {
		minimumWidth = 0;
		minimumHeight = 0;
		if (direction == Direction.HORIZONTAL) {
			for (Component component : this) {
				minimumWidth += component.getMinimumWidth();
				if (component.getMinimumHeight() > minimumHeight) {
					minimumHeight = component.getMinimumHeight();
				}
			}
		} else if (direction == Direction.VERTICAL) {
			for (Component component : this) {
				if (component.getMinimumWidth() > minimumWidth) {
					minimumWidth = component.getMinimumWidth();
				}
				minimumHeight += component.getMinimumHeight();
			}
		} else {
			throw new Direction.InvalidDirectionException();
		}
	}

	private void resetCacheArrays() {
		if (cachedComponentWidths == null || cachedComponentWidths.length != components.size()) {
			cachedComponentWidths = new float[components.size()];
			cachedComponentHeights = new float[components.size()];
			cachedAlignmentOffsets = new float[components.size()];
		}
	}

	// DirectionLayout takes up all the space it can in it's leading direction, and the sum of the widths of it's components in the non-leading direction

	@Override
	public float updateWidth(float maximumWidth, float maximumHeight) {
		if (direction == Direction.HORIZONTAL) {
			if (minimumWidth <= maximumWidth) {
				// If there is just enough or not enough space, give all components their minimum width
				int i = 0;
				currentHeight = 0;
				for (Component component : components) {
					cachedComponentWidths[i] = component.getMinimumWidth();
					cachedComponentHeights[i] = component.updateHeight(cachedComponentWidths[i], maximumHeight);
					if (cachedComponentHeights[i] > currentHeight) {
						currentHeight = cachedComponentHeights[i];
					}
					cachedAlignmentOffsets[i] = calculateAlignmentOffset(cachedComponentHeights[i], maximumHeight);
					i++;
				}
				currentWidth = minimumWidth;
			} else {
				// If there is more than enough space, give components space according to their order
				int i = 0;
				currentHeight = 0;
				remainingSpace = maximumWidth - minimumWidth;
				for (Component component : new SortedIterable<>(components)) {
					if (component.getGrows() == Grows.NEVER) {
						cachedComponentWidths[i] = component.getMinimumWidth();
					} else {
						cachedComponentWidths[i] = component.updateWidth(component.getMinimumWidth() + remainingSpace, maximumHeight);
						remainingSpace -= cachedComponentWidths[i];
					}
					cachedComponentHeights[i] = component.updateHeight(cachedComponentWidths[i], maximumHeight);
					if (cachedComponentHeights[i] > currentHeight) {
						currentHeight = cachedComponentHeights[i];
					}
					cachedAlignmentOffsets[i] = calculateAlignmentOffset(cachedComponentHeights[i], maximumHeight);
					i++;
				}
				currentWidth = minimumWidth + (remainingSpace > 0 ? remainingSpace : 0);
			}
			return maximumWidth;
		} else if (direction == Direction.VERTICAL) {
			return currentWidth;
		}
		throw new Direction.InvalidDirectionException();
	}

	@Override
	public float updateHeight(float maximumWidth, float maximumHeight) {
		if (direction == Direction.HORIZONTAL) {
			return currentHeight;
		} else if (direction == Direction.VERTICAL) {
			if (minimumHeight <= maximumHeight) {
				// If there is just enough or not enough space, give all components their minimum height
				int i = 0;
				currentWidth = 0;
				for (Component component : components) {
					cachedComponentHeights[i] = component.getMinimumHeight();
					cachedComponentWidths[i] = component.updateWidth(maximumWidth, cachedComponentHeights[i]);
					if (cachedComponentWidths[i] > currentWidth) {
						currentWidth = cachedComponentWidths[i];
					}
					cachedAlignmentOffsets[i] = calculateAlignmentOffset(cachedComponentWidths[i], maximumWidth);
					i++;
				}
				currentHeight = minimumHeight;
			} else {
				// If there is more than enough space, give components space according to their order
				int i = 0;
				currentWidth = 0;
				remainingSpace = maximumHeight - minimumHeight;
				for (Component component : new SortedIterable<>(components)) {
					if (component.getGrows() == Grows.NEVER) {
						cachedComponentHeights[i] = component.getMinimumHeight();
					} else {
						cachedComponentHeights[i] = component.updateHeight(maximumWidth, component.getMinimumHeight() + remainingSpace);
						remainingSpace -= cachedComponentHeights[i];
					}
					cachedComponentWidths[i] = component.updateWidth(maximumWidth, cachedComponentHeights[i]);
					if (cachedComponentWidths[i] > currentWidth) {
						currentWidth = cachedComponentWidths[i];
					}
					cachedAlignmentOffsets[i] = calculateAlignmentOffset(cachedComponentWidths[i], maximumWidth);
					i++;
				}
				currentHeight = minimumHeight + (remainingSpace > 0 ? remainingSpace : 0);
			}
			return maximumHeight;
		}
		throw new Direction.InvalidDirectionException();
	}

	@Override
	public Layout addChild(Component component) {
		components.add(component);
		resetCacheArrays();
		updateMinimumSizes();
		return this;
	}

	@Override
	public Layout addChildren(Component... components) {
		this.components.addAll(Arrays.asList(components));
		resetCacheArrays();
		updateMinimumSizes();
		return this;
	}

	@Override
	@Nonnull
	public Iterator<Component> iterator() {
		return components.iterator();
	}
}
