package link.infra.jumploader.ui;


import link.infra.jumploader.DownloadWorkerManager;

import java.util.Arrays;

/**
 * See Window, Component, and Layout for how this layout/component/gui system works. It's not a very good one, but I enjoyed
 * writing it, and it works.
 */
public class GUIManager {
	private boolean shouldClose = false;
	private final DownloadWorkerManager downloadWorkerManager;
	private final Window window;

	public GUIManager(DownloadWorkerManager downloadWorkerManager) {
		this.downloadWorkerManager = downloadWorkerManager;

		window = new Window(new AdaptiveWidthContainer(
			new DirectionLayout(Direction.VERTICAL).addChildren(
				new Image("assets/jumploader/splashlogo.png"),
				new AdaptiveWidthContainer(new ProgressBar(this::getProgress), 0.5f, 1500f)
			)
		));
		window.setIcons(Arrays.asList(
			"assets/jumploader/icon-16.png",
			"assets/jumploader/icon-32.png",
			"assets/jumploader/icon-48.png",
			"assets/jumploader/icon-128.png"
		));
	}

	public void run() {
		window.init();
		while (!window.shouldClose() && !shouldClose) {
			window.render();
		}
		window.free();
	}

	private float getProgress() {
		if (downloadWorkerManager.isDone()) {
			shouldClose = true;
		}
		return downloadWorkerManager.getWorkerProgress();
	}

}
















