package link.infra.jumploader.download.ui;

public class GrowingSpacer implements Component {
	private final Direction direction;

	public GrowingSpacer(Direction direction) {
		this.direction = direction;
	}

	@Override
	public void init() {}

	@Override
	public void render() {}

	@Override
	public void free() {}

	@Override
	public float getMinimumWidth() {
		return 0;
	}

	@Override
	public float getMinimumHeight() {
		return 0;
	}

	@Override
	public float updateWidth(float maximumWidth, float maximumHeight) {
		if (direction == Direction.HORIZONTAL) {
			return maximumWidth;
		} else if (direction == Direction.VERTICAL) {
			return 0;
		}
		throw new Direction.InvalidDirectionException();
	}

	@Override
	public float updateHeight(float maximumWidth, float maximumHeight) {
		if (direction == Direction.HORIZONTAL) {
			return 0;
		} else if (direction == Direction.VERTICAL) {
			return maximumHeight;
		}
		throw new Direction.InvalidDirectionException();
	}

	@Override
	public Grows getGrows() {
		return Grows.ALWAYS;
	}
}
