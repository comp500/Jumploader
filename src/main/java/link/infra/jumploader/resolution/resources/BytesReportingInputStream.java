package link.infra.jumploader.resolution.resources;

import link.infra.jumploader.DownloadWorkerManager;

import javax.annotation.Nonnull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

class BytesReportingInputStream extends FilterInputStream {
	private final DownloadWorkerManager.TaskStatus status;
	private int bytesDownloaded;

	public BytesReportingInputStream(InputStream inputStream, DownloadWorkerManager.TaskStatus status, int contentLength) {
		super(inputStream);
		this.status = status;
		if (contentLength != -1) {
			status.setExpectedLength(contentLength);
		}
	}

	@Override
	public int read() throws IOException {
		bytesDownloaded++;
		if ((bytesDownloaded & 65535) == 0) {
			status.setDownloaded(bytesDownloaded);
		}
		return super.read();
	}

	private int readBulkCallCount = 0;

	@Override
	public int read(@Nonnull byte[] b, int off, int len) throws IOException {
		int bytesRead = super.read(b, off, len);
		bytesDownloaded += bytesRead;
		readBulkCallCount++;
		if (readBulkCallCount > 10) {
			status.setDownloaded(bytesDownloaded);
			readBulkCallCount = 0;
		}
		return bytesRead;
	}

	@Override
	public void reset() throws IOException {
		throw new IOException("BytesReportingInputStream doesn't support reset()");
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void mark(int readlimit) {
		// Do nothing
	}

	@Override
	public void close() throws IOException {
		super.close();
		status.setDownloaded(bytesDownloaded);
	}
}
