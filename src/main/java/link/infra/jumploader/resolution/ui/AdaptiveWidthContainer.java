package link.infra.jumploader.resolution.ui;

public class AdaptiveWidthContainer extends DirectionConstraintContainer {

	public AdaptiveWidthContainer(Component containedComponent) {
		this(containedComponent, 0.5f, 900f);
	}

	/**
	 * @param containedComponent The component to constrain the width of
	 * @param minFraction The fraction of the parent it should take up at infinite size - i.e. the smallest space it needs
	 * @param halfWidth The width it should be halfway between full width and minFraction
	 */
	public AdaptiveWidthContainer(Component containedComponent, float minFraction, float halfWidth) {
		super(containedComponent, Direction.HORIZONTAL, width -> {
			// @juliand665 came up with this fancy formula for getting the fraction it should be
			return width * (1f - (1f - minFraction) * width / (width + halfWidth));
		});
	}
}
