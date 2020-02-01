package link.infra.jumploader.download.ui;

import link.infra.jumploader.util.SortedIterable;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
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

	private static class PositionedComponent implements Comparable<PositionedComponent> {
		final Component c;
		float alignmentOffset;
		// TODO: remove width/height?
		float width;
		float height;

		private PositionedComponent(Component c) {
			this.c = c;
		}

		@Override
		public int compareTo(PositionedComponent other) {
			return c.compareTo(other.c);
		}
	}

	private final ArrayList<PositionedComponent> components = new ArrayList<>();
	private float remainingSpace;
	private float minimumWidth;
	private float minimumHeight;
	private float currentWidth;
	private float currentHeight;
	private float cachedParentWidth;
	private float cachedParentHeight;

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

		float individualSpacing = components.size() > 0 ? remainingSpace / (components.size() - 1) : 0;
		for (PositionedComponent compPos : components) {
			if (direction == Direction.HORIZONTAL) {
				// Offset into the alignment offset of this component
				GL11.glTranslatef(0f, compPos.alignmentOffset, 0f);
				compPos.c.render();
				// Reset the offset, translate past the width of this component
				GL11.glTranslatef(compPos.width, -compPos.alignmentOffset, 0f);
				if (spaceBetween) {
					// It doesn't matter if we translate at the end, that'll get reset when we popMatrix
					GL11.glTranslatef(individualSpacing, 0f, 0f);
				}
			} else if (direction == Direction.VERTICAL) {
				GL11.glTranslatef(compPos.alignmentOffset, 0f, 0f);
				compPos.c.render();
				GL11.glTranslatef(-compPos.alignmentOffset, compPos.height, 0f);
				if (spaceBetween) {
					GL11.glTranslatef(0f, individualSpacing, 0f);
				}
			}
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
			return (containerSize / 2) - (componentSize / 2);
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

	// DirectionLayout takes up all the space it can in it's leading direction, and the sum of the widths of it's components in the non-leading direction

	@Override
	public void updateSize(float maximumWidth, float maximumHeight) {
		cachedParentWidth = maximumWidth;
		cachedParentHeight = maximumHeight;
		if (direction == Direction.HORIZONTAL) {
			if (maximumWidth <= minimumWidth) {
				// If there is just enough or not enough space, give all components their minimum width
				currentHeight = 0;
				remainingSpace = 0;
				for (PositionedComponent compPos : components) {
					compPos.width = compPos.c.getMinimumWidth();
					compPos.c.updateSize(compPos.width, maximumHeight);
					compPos.height = compPos.c.getCurrentHeight();
					if (compPos.height > currentHeight) {
						currentHeight = compPos.height;
					}
				}
				currentWidth = minimumWidth;
				// Calculate alignment offsets such that this Layout only takes currentHeight rather than maximumHeight
				for (PositionedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.height, currentHeight);
				}
			} else {
				// If there is more than enough space, give components space according to their order
				currentHeight = 0;
				remainingSpace = maximumWidth - minimumWidth;
				for (PositionedComponent compPos : new SortedIterable<>(components)) {
					if (compPos.c.getGrows() == Grows.NEVER) {
						compPos.width = compPos.c.getMinimumWidth();
					} else {
						compPos.c.updateSize(compPos.c.getMinimumWidth() + remainingSpace, maximumHeight);
						compPos.width = compPos.c.getCurrentWidth();
						remainingSpace -= (compPos.width - compPos.c.getMinimumWidth());
					}
					compPos.height = compPos.c.getCurrentHeight();
					if (compPos.height > currentHeight) {
						currentHeight = compPos.height;
					}
				}
				currentWidth = minimumWidth + (remainingSpace > 0 ? remainingSpace : 0);
				// Calculate alignment offsets such that this Layout only takes currentHeight rather than maximumHeight
				for (PositionedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.height, currentHeight);
				}
			}
		} else if (direction == Direction.VERTICAL) {
			if (maximumHeight <= minimumHeight) {
				// If there is just enough or not enough space, give all components their minimum height
				currentWidth = 0;
				remainingSpace = 0;
				for (PositionedComponent compPos : components) {
					compPos.height = compPos.c.getMinimumHeight();
					compPos.c.updateSize(maximumWidth, compPos.height);
					compPos.width = compPos.c.getCurrentWidth();
					if (compPos.width > currentWidth) {
						currentWidth = compPos.width;
					}
				}
				currentHeight = minimumHeight;
				// Calculate alignment offsets such that this Layout only takes currentWidth rather than maximumWidth
				for (PositionedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.width, currentWidth);
				}
			} else {
				// If there is more than enough space, give components space according to their order
				currentWidth = 0;
				remainingSpace = maximumHeight - minimumHeight;
				for (PositionedComponent compPos : new SortedIterable<>(components)) {
					if (compPos.c.getGrows() == Grows.NEVER) {
						compPos.height = compPos.c.getMinimumHeight();
					} else {
						compPos.c.updateSize(maximumWidth, compPos.c.getMinimumHeight() + remainingSpace);
						compPos.height = compPos.c.getCurrentHeight();
						remainingSpace -= (compPos.height - compPos.c.getMinimumHeight());
					}
					compPos.width = compPos.c.getCurrentWidth();
					if (compPos.width > currentWidth) {
						currentWidth = compPos.width;
					}
				}
				currentHeight = minimumHeight + (remainingSpace > 0 ? remainingSpace : 0);
				// Calculate alignment offsets such that this Layout only takes currentWidth rather than maximumWidth
				for (PositionedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.width, currentWidth);
				}
			}
		} else {
			throw new Direction.InvalidDirectionException();
		}
	}

	@Override
	public float getCurrentWidth() {
		if (direction == Direction.HORIZONTAL) {
			return cachedParentWidth;
		} else if (direction == Direction.VERTICAL) {
			return currentWidth;
		}
		throw new Direction.InvalidDirectionException();
	}

	@Override
	public float getCurrentHeight() {
		if (direction == Direction.HORIZONTAL) {
			return currentHeight;
		} else if (direction == Direction.VERTICAL) {
			return cachedParentHeight;
		}
		throw new Direction.InvalidDirectionException();
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

	@Override
	public Layout addChild(Component component) {
		components.add(new PositionedComponent(component));
		updateMinimumSizes();
		updateSize(cachedParentWidth, cachedParentHeight);
		return this;
	}

	@Override
	public Layout addChildren(Component... components) {
		this.components.ensureCapacity(this.components.size() + components.length);
		for (Component component : components) {
			this.components.add(new PositionedComponent(component));
		}
		updateMinimumSizes();
		updateSize(cachedParentWidth, cachedParentHeight);
		return this;
	}

	@Override
	@Nonnull
	public Iterator<Component> iterator() {
		Iterator<PositionedComponent> subIterator = components.iterator();
		return new Iterator<Component>() {
			@Override
			public boolean hasNext() {
				return subIterator.hasNext();
			}

			@Override
			public Component next() {
				return subIterator.next().c;
			}
		};
	}
}
