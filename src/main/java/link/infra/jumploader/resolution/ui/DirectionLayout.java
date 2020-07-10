package link.infra.jumploader.resolution.ui;

import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Layout that lays out components in order, in the direction given. An Alignment can also be given to specify how the components should be aligned
 * in the non-leading direction. It can also be specified whether to put remaining space between components, or space around them.
 * DirectionLayout takes up all the space it can in the leading direction, and the sum of the size of its components in the non-leading direction.
 */
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

	private static class AlignedComponent implements Comparable<AlignedComponent> {
		final Component c;
		float alignmentOffset;

		private AlignedComponent(Component c) {
			this.c = c;
		}

		@Override
		public int compareTo(AlignedComponent other) {
			return c.compareTo(other.c);
		}
	}

	private final ArrayList<AlignedComponent> components = new ArrayList<>();
	private float remainingSpace;
	private float minimumWidth;
	private float minimumHeight;
	private float currentWidth;
	private float currentHeight;
	private float cachedParentWidth;
	private float cachedParentHeight;

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
		for (AlignedComponent compPos : components) {
			if (direction == Direction.HORIZONTAL) {
				// Offset into the alignment offset of this component
				GL11.glTranslatef(0f, compPos.alignmentOffset, 0f);
				compPos.c.render();
				// Reset the offset, translate past the width of this component
				GL11.glTranslatef(compPos.c.getCurrentWidth(), -compPos.alignmentOffset, 0f);
				if (spaceBetween) {
					// It doesn't matter if we translate at the end, that'll get reset when we popMatrix
					GL11.glTranslatef(individualSpacing, 0f, 0f);
				}
			} else if (direction == Direction.VERTICAL) {
				GL11.glTranslatef(compPos.alignmentOffset, 0f, 0f);
				compPos.c.render();
				GL11.glTranslatef(-compPos.alignmentOffset, compPos.c.getCurrentHeight(), 0f);
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

	@Override
	public void updateSize(float maximumWidth, float maximumHeight) {
		cachedParentWidth = maximumWidth;
		cachedParentHeight = maximumHeight;
		if (direction == Direction.HORIZONTAL) {
			if (maximumWidth <= minimumWidth) {
				// If there is just enough or not enough space, give all components their minimum width
				currentHeight = 0;
				remainingSpace = 0;
				for (AlignedComponent compPos : components) {
					compPos.c.updateSize(compPos.c.getMinimumWidth(), maximumHeight);
					if (compPos.c.getCurrentHeight() > currentHeight) {
						currentHeight = compPos.c.getCurrentHeight();
					}
				}
				currentWidth = minimumWidth;
				// Calculate alignment offsets such that this Layout only takes currentHeight rather than maximumHeight
				for (AlignedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.c.getCurrentHeight(), Math.min(currentHeight, maximumHeight));
				}
			} else {
				// If there is more than enough space, give components space according to their order
				currentHeight = 0;
				remainingSpace = maximumWidth - minimumWidth;
				for (AlignedComponent compPos : new SortedIterable<>(components)) {
					if (compPos.c.getGrows() == Grows.NEVER) {
						compPos.c.updateSize(compPos.c.getMinimumWidth(), maximumHeight);
					} else {
						compPos.c.updateSize(compPos.c.getMinimumWidth() + remainingSpace, maximumHeight);
						remainingSpace -= (compPos.c.getCurrentWidth() - compPos.c.getMinimumWidth());
					}
					if (compPos.c.getCurrentHeight() > currentHeight) {
						currentHeight = compPos.c.getCurrentHeight();
					}
				}
				currentWidth = minimumWidth + (remainingSpace > 0 ? remainingSpace : 0);
				// Calculate alignment offsets such that this Layout only takes currentHeight rather than maximumHeight
				for (AlignedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.c.getCurrentHeight(), Math.min(currentHeight, maximumHeight));
				}
			}
		} else if (direction == Direction.VERTICAL) {
			if (maximumHeight <= minimumHeight) {
				// If there is just enough or not enough space, give all components their minimum height
				currentWidth = 0;
				remainingSpace = 0;
				for (AlignedComponent compPos : components) {
					compPos.c.updateSize(maximumWidth, compPos.c.getMinimumHeight());
					if (compPos.c.getCurrentWidth() > currentWidth) {
						currentWidth = compPos.c.getCurrentWidth();
					}
				}
				currentHeight = minimumHeight;
				// Calculate alignment offsets such that this Layout only takes currentWidth rather than maximumWidth
				for (AlignedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.c.getCurrentWidth(), Math.min(currentWidth, maximumWidth));
				}
			} else {
				// If there is more than enough space, give components space according to their order
				currentWidth = 0;
				remainingSpace = maximumHeight - minimumHeight;
				for (AlignedComponent compPos : new SortedIterable<>(components)) {
					if (compPos.c.getGrows() == Grows.NEVER) {
						compPos.c.updateSize(maximumWidth, compPos.c.getMinimumHeight());
					} else {
						compPos.c.updateSize(maximumWidth, compPos.c.getMinimumHeight() + remainingSpace);
						remainingSpace -= (compPos.c.getCurrentHeight() - compPos.c.getMinimumHeight());
					}
					if (compPos.c.getCurrentWidth() > currentWidth) {
						currentWidth = compPos.c.getCurrentWidth();
					}
				}
				currentHeight = minimumHeight + (remainingSpace > 0 ? remainingSpace : 0);
				// Calculate alignment offsets such that this Layout only takes currentWidth rather than maximumWidth
				for (AlignedComponent compPos : components) {
					compPos.alignmentOffset = calculateAlignmentOffset(compPos.c.getCurrentWidth(), Math.min(currentWidth, maximumWidth));
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
		components.add(new AlignedComponent(component));
		updateMinimumSizes();
		updateSize(cachedParentWidth, cachedParentHeight);
		return this;
	}

	@Override
	public Layout addChildren(Component... components) {
		this.components.ensureCapacity(this.components.size() + components.length);
		for (Component component : components) {
			this.components.add(new AlignedComponent(component));
		}
		updateMinimumSizes();
		updateSize(cachedParentWidth, cachedParentHeight);
		return this;
	}

	@Override
	@Nonnull
	public Iterator<Component> iterator() {
		Iterator<AlignedComponent> subIterator = components.iterator();
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
