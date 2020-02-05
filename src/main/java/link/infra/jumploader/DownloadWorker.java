package link.infra.jumploader;

/**
 * A DownloadWorker downloads one file, reporting its progress to the TaskStatus given
 */
public interface DownloadWorker {
	void start(DownloadWorkerManager.TaskStatus status);
}
