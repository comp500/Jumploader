package link.infra.jumploader.download.ui;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class DirectionLayout extends ArrayList<Component> implements Layout {
	private final Direction direction;
	private final Alignment alignment;
	private final boolean spaceBetween;

	public enum Direction {
		HORIZONTAL,
		VERTICAL
	}

	public enum Alignment {
		START,
		CENTER,
		END
	}

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

	private int parentWidth;
	private int parentHeight;
	private int minimumWidth;
	private int minimumHeight;
	private int maximumWidth;
	private int maximumHeight;
	private int[] cachedAlignmentOffsets;
	private int[] cachedComponentWidths;
	private int[] cachedComponentHeights;

	/**
	 * updateSize() must be called after this DirectionLayout has been modified!
	 * TODO: should this changed to a decorator, so updateSize is called on modification?
	 */
	@Override
	public void render() {
		float spacing = 0f;
		if (direction == Direction.HORIZONTAL) {
			spacing = ((float)(parentWidth - minimumWidth)) / (size() + 1);
		} else if (direction == Direction.VERTICAL) {
			spacing = ((float)(parentHeight - minimumHeight)) / (size() + 1);
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
				GL11.glTranslatef(component.getMinimumWidth(), 0f, 0f);
				GL11.glTranslatef(spacing, 0f, 0f);
			} else if (direction == Direction.VERTICAL) {
				component.render();
				GL11.glTranslatef(0f, component.getMinimumHeight(), 0f);
				GL11.glTranslatef(0f, spacing, 0f);
			}
		}
		GL11.glPopMatrix();
	}

	private int calculateAlignmentOffset(int componentSize, int containerSize) {
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
		return 0;
	}

	@Override
	public void updateSize(int width, int height) {
		this.parentWidth = width;
		this.parentHeight = height;
		if (direction == Direction.HORIZONTAL) {
			minimumWidth = 0;
			minimumHeight = 0;
			maximumWidth = 0;
			maximumHeight = 0;
			for (Component component : this) {
				minimumWidth += component.getMinimumWidth();
				maximumWidth += component.getMaximumWidth();
				if (component.getMinimumHeight() > minimumHeight) {
					minimumHeight = component.getMinimumHeight();
					maximumHeight = component.getMaximumHeight();
				}
				// TODO: calculate component sizes
			}

			// TODO: WAT oh no
			// TODO: if a component's maximum width depends on it's height (ratio), if the max is set to Infinity it'll just take all space (i.e. no left over for padding)
			if (parentWidth <= minimumWidth) {
				// If there is just enough or not enough space, give all components their minimum width
				for (int i = 0; i < size(); i++) {
					Component component = get(i);
					cachedComponentHeights[i] = Math.max(Math.max(component.getMaximumHeight(), maximumHeight), parentHeight);
					cachedAlignmentOffsets[i] = calculateAlignmentOffset(cachedComponentHeights[i], maximumHeight);
					cachedComponentWidths[i] = component.getMinimumWidth();
					component.updateSize(cachedComponentWidths[i], cachedComponentHeights[i]);
				}
			} else {
				// Equally distribute maximum width among children, up to each of their maximum widths
				for (int i = 0; i < size(); i++) {
					Component component = get(i);
					cachedComponentHeights[i] = Math.max(Math.max(component.getMaximumHeight(), maximumHeight), parentHeight);
					cachedAlignmentOffsets[i] = calculateAlignmentOffset(cachedComponentHeights[i], maximumHeight);
					cachedComponentWidths[i] = component.getMinimumWidth();
					component.updateSize(cachedComponentWidths[i], cachedComponentHeights[i]);
				}
			}
		} else if (direction == Direction.VERTICAL) {
			minimumWidth = 0;
			minimumHeight = 0;
			maximumWidth = 0;
			maximumHeight = 0;
			for (Component component : this) {
				minimumHeight += component.getMinimumHeight();
				maximumHeight += component.getMaximumHeight();
				if (component.getMinimumWidth() > minimumWidth) {
					minimumWidth = component.getMinimumWidth();
					maximumWidth = component.getMaximumWidth();
				}
				// TODO: calculate component sizes
				//component.updateSize(width, component.getMinimumHeight());
			}
			for (int i = 0; i < size(); i++) {
				Component component = get(i);
				cachedAlignmentOffsets[i] = calculateAlignmentOffset(component.getMaximumWidth(), maximumWidth);
			}
		}
	}

	@Override
	public int getMinimumWidth() {
		return minimumWidth;
	}

	@Override
	public int getMinimumHeight() {
		return minimumHeight;
	}

	@Override
	public int getMaximumWidth() {
		return maximumWidth;
	}

	@Override
	public int getMaximumHeight() {
		return maximumHeight;
	}
}
