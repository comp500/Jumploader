package link.infra.jumploader.resolution.ui;

/**
 * Just in case you want spacing to work better. I guess... this is a badly designed GUI system. But I had fun!
 */
public class EmptyComponent implements Component {
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
	public void updateSize(float maximumWidth, float maximumHeight) {}

	@Override
	public float getCurrentWidth() {
		return 0;
	}

	@Override
	public float getCurrentHeight() {
		return 0;
	}

	@Override
	public Grows getGrows() {
		return Grows.NEVER;
	}
}
