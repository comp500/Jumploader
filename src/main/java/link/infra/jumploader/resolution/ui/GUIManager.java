package link.infra.jumploader.resolution.ui;


import link.infra.jumploader.DownloadWorkerManager;
import link.infra.jumploader.launch.arguments.ParsedArguments;

import java.util.Arrays;

/**
 * See Window, Component, and Layout for how this layout/component/gui system works. It's not a very good one, but I enjoyed
 * writing it, and it works.
 */
public class GUIManager {
	private final Window window;

	public GUIManager(DownloadWorkerManager<?> downloadWorkerManager, ParsedArguments args) {
		window = new Window(new AdaptiveWidthContainer(
			new DirectionLayout(Direction.VERTICAL).addChildren(
				new Image("assets/jumploader/splashlogo.png"),
				new AdaptiveWidthContainer(new ProgressBar(downloadWorkerManager::getWorkerProgress), 0.5f, 1500f)
			)
		), args.windowWidth, args.windowHeight);
		window.setIcons(Arrays.asList(
			"assets/jumploader/icon-16.png",
			"assets/jumploader/icon-32.png",
			"assets/jumploader/icon-48.png",
			"assets/jumploader/icon-128.png"
		));
	}

	public void init() {
		window.init();
	}

	public boolean wasCloseTriggered() {
		return window.shouldClose();
	}

	public void render() {
		window.render();
	}

	public void cleanup() {
		window.free();
	}

}
















