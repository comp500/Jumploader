package link.infra.jumploader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadWorkerManager<T> {
	private final ExecutorService threadPool = Executors.newFixedThreadPool(5);
	private final ExecutorCompletionService<TaskResult> completionService = new ExecutorCompletionService<>(threadPool);

	private int queuedTasks = 0;
	private final List<TaskStatus> statusValues = new ArrayList<>();
	private final AtomicInteger completedTasks = new AtomicInteger();

	/**
	 * A DownloadWorker downloads one file, reporting its progress to the TaskStatus given
	 */
	public interface DownloadWorker<T> {
		T start(TaskStatus status) throws Exception;
	}

	public static class TaskStatus {
		private int downloaded = 0;
		private int expectedLength = -1;
		private boolean done = false;

		private TaskStatus() {}

		public synchronized void setDownloaded(int downloaded) {
			this.downloaded = downloaded;
		}

		public synchronized void setExpectedLength(int expectedLength) {
			this.expectedLength = expectedLength;
		}

		private synchronized void markCompleted() {
			done = true;
		}

		private synchronized int getDownloaded() {
			if (done) {
				if (downloaded != expectedLength && expectedLength != -1) {
					return expectedLength;
				}
			}
			return downloaded;
		}

		private synchronized int getExpectedLength() {
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

		public synchronized boolean isComplete() {
			return done;
		}
	}

	private class TaskResult {
		private final Exception failure;
		private final T result;

		private TaskResult(T result) {
			this.result = result;
			this.failure = null;
		}

		private TaskResult(Exception failure) {
			this.result = null;
			this.failure = failure;
		}

		public Exception getFailure() {
			return failure;
		}

		public T getResult() {
			return result;
		}
	}

	/**
	 * Queues a new worker. Must not be called at the same time as isDone or getWorkerProgress
	 */
	public void queueWorker(DownloadWorker<T> worker) {
		TaskStatus status = new TaskStatus();
		completionService.submit(() -> {
			try {
				T result = worker.start(status);
				status.markCompleted();
				return new TaskResult(result);
			} catch (Exception e) {
				status.markCompleted();
				return new TaskResult(e);
			}
 		});
		statusValues.add(status);
		queuedTasks++;
	}

	public T pollResult() throws Exception {
		Future<TaskResult> fRes = completionService.poll();
		if (fRes == null) {
			return null;
		}
		TaskResult res = fRes.get();
		completedTasks.incrementAndGet();

		if (res.getFailure() != null) {
			throw res.getFailure();
		}
		return res.getResult();
	}

	public T pollResult(long millisecondsToWait) throws Exception {
		Future<TaskResult> fRes = completionService.poll(millisecondsToWait, TimeUnit.MILLISECONDS);
		if (fRes == null) {
			return null;
		}
		TaskResult res = fRes.get();
		completedTasks.incrementAndGet();

		if (res.getFailure() != null) {
			throw res.getFailure();
		}
		return res.getResult();
	}

	public void shutdown() throws InterruptedException {
		threadPool.shutdown();
		threadPool.awaitTermination(10, TimeUnit.SECONDS);
		threadPool.shutdownNow();
	}

	public float getWorkerProgress() {
		float completedCount = 0;
		float sumExpected = 0;
		float sumDownloaded = 0;
		for (TaskStatus status : statusValues) {
			if (status.isComplete()) {
				completedCount++;
				continue;
			}
			sumDownloaded += status.getDownloaded();
			sumExpected += status.getExpectedLength();
		}
		if (sumExpected == 0) {
			return completedCount / queuedTasks;
		}
		return (completedCount + (sumDownloaded / sumExpected)) / queuedTasks;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isDone() {
		return statusValues.size() == completedTasks.get();
	}
}
