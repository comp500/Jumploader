package link.infra.jumploader.download.ui;

public class Spacer implements Component {
	private final int width;
	private final int height;

	public Spacer(int width, int height) {
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
	public void updateSize(int width, int height) {}

	@Override
	public int getPreferredWidth() {
		return width;
	}

	@Override
	public int getPreferredHeight() {
		return height;
	}
}
