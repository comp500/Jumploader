package link.infra.jumploader.download.ui;

public class FixedSpacer implements Component {
	private final int width;
	private final int height;

	public FixedSpacer(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public void init() {}

	@Override
	public void render() {}

	@Override
	public void free() {}

	@Override
	public float getMinimumWidth() {
		return width;
	}

	@Override
	public float getMinimumHeight() {
		return height;
	}

	@Override
	public float updateWidth(float maximumWidth, float maximumHeight) {
		return width;
	}

	@Override
	public float updateHeight(float maximumWidth, float maximumHeight) {
		return height;
	}

	@Override
	public Grows getGrows() {
		return Grows.NEVER;
	}
}
