package link.infra.jumploader;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DownloadWorkerManager {
	private final HashMap<Thread, TaskStatus> threadMap = new HashMap<>();

	public static class TaskStatus {
		private int downloaded = 0;
		private int expectedLength = -1;
		private boolean done = false;
		private Exception failureException = null;

		private TaskStatus() {}

		private static class UnknownFailureReasonException extends Exception {}

		public synchronized void setDownloaded(int downloaded) {
			this.downloaded = downloaded;
		}

		public synchronized void setExpectedLength(int expectedLength) {
			this.expectedLength = expectedLength;
		}

		public synchronized void markCompleted() {
			done = true;
		}

		public synchronized void markFailed(Exception failureException) {
			done = true;
			if (failureException == null) {
				this.failureException = new UnknownFailureReasonException();
			} else {
				this.failureException = failureException;
			}
		}

		public synchronized int getDownloaded() {
			if (done) {
				if (downloaded != expectedLength && expectedLength != -1) {
					return expectedLength;
				}
			}
			return downloaded;
		}

		public synchronized int getExpectedLength() {
			if (done) {
				if (downloaded != expectedLength) {
					return downloaded;
				}
			}
			if (expectedLength == -1) {
				return done ? downloaded : downloaded + 1;
			}
			return expectedLength;
		}

		public synchronized Exception getFailureException() {
			return failureException;
		}

		public synchronized boolean isIncomplete() {
			return !done;
		}
	}

	public void queueWorker(DownloadWorker worker) {
		TaskStatus status = new TaskStatus();
		Thread workerThread = new Thread(() -> worker.start(status));
		threadMap.put(workerThread, status);
		workerThread.start();
	}

	public void shutdown() {
		threadMap.forEach((t, ts) -> {
			t.interrupt();
			if (ts.getFailureException() == null && ts.isIncomplete()) {
				ts.markFailed(new InterruptedException());
			}
		});
		threadMap.clear();
	}

	public float getWorkerProgress() {
		int sumExpected = 0;
		int sumDownloaded = 0;
		for (TaskStatus status : threadMap.values()) {
			sumDownloaded += status.getDownloaded();
			sumExpected += status.getExpectedLength();
		}
		if (sumExpected == 0) {
			return 0f;
		}
		return (float)sumDownloaded / (float)sumExpected;
	}

	public boolean isDone() {
		for (TaskStatus status : threadMap.values()) {
			if (status.isIncomplete()) {
				return false;
			}
		}
		return true;
	}

	public List<Exception> getExceptions() {
		return threadMap.values().stream().map(TaskStatus::getFailureException).filter(Objects::nonNull).collect(Collectors.toList());
	}

}
