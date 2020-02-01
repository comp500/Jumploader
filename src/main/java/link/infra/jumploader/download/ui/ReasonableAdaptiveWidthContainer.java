package link.infra.jumploader.download.ui;

public class ReasonableAdaptiveWidthContainer extends DirectionConstraintContainer {
	/**
	 * The fraction of the parent it should take up at infinite size - i.e. the smallest space it needs
	 */
	private static final float MIN_FRACTION = 0.5f;
	/**
	 * The width it should be halfway between full width and minFraction
	 */
	private static final float HALF_WIDTH = 1000f;

	public ReasonableAdaptiveWidthContainer(Component containedComponent) {
		super(containedComponent, Direction.HORIZONTAL, width -> {
			// @juliand665 came up with this fancy formula for getting the fraction it should be
			return width * (1f - (1f - MIN_FRACTION) * width / (width + HALF_WIDTH));
		});
	}
}
