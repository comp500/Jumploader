package link.infra.jumploader.download.ui;

public class ReasonableAdaptiveWidthContainer extends DirectionConstraintContainer {
	public ReasonableAdaptiveWidthContainer(Component containedComponent) {
		super(containedComponent, Direction.HORIZONTAL, width -> {
			if (width < 500) {
				return width;
			} else if (width < 900) {
				return width * 0.8f;
			} else {
				return width * 0.6f;
			}
		});
	}
}
