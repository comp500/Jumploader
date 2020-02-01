package link.infra.jumploader.download.ui;









































/**
 * See Window, Component, and Layout for how this layout/component/gui system works.
 */
public class GUIManager {
	// TODO: move into some reasonable API
	public GUIManager() {
		Window window = new Window(new ReasonableAdaptiveWidthContainer(
			new DirectionLayout(Direction.VERTICAL, Alignment.CENTER, true).addChildren(
				new EmptyComponent(),
				new Image("splashlogo.png", false, true),
				new FixedRectangle(200, 30, 0f, 0f, 0f),
				new EmptyComponent()
			)
		));
		window.init();
		while (!window.shouldClose()) {
			window.render();
		}
		window.free();
	}

}
















