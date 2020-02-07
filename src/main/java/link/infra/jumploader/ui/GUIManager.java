package link.infra.jumploader.ui;


import link.infra.jumploader.DownloadWorkerManager;

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
				new Image("splashlogo.png"),
				new AdaptiveWidthContainer(new ProgressBar(this::getProgress), 0.5f, 1500f)
			)
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
















