package link.infra.jumploader.ui;


import java.util.function.Supplier;

/**
 * See Window, Component, and Layout for how this layout/component/gui system works. It's not a very good one, but I enjoyed
 * writing it, and it works.
 */
public class GUIManager {
	// TODO: move into some reasonable API
	public GUIManager() {
		Window window = new Window(new AdaptiveWidthContainer(
			new DirectionLayout(Direction.VERTICAL).addChildren(
				new Image("splashlogo.png"),
				new AdaptiveWidthContainer(new ProgressBar(new Supplier<Float>() {
					private double ohno = 0;

					@Override
					public Float get() {
						ohno += 0.05f;
						if (ohno > Math.PI) {
							ohno = 0;
						}
						return (float) Math.sin(ohno);
					}
				}), 0.5f, 1500f)
			)
		));
		window.init();
		while (!window.shouldClose()) {
			window.render();
		}
		window.free();
	}

}
















