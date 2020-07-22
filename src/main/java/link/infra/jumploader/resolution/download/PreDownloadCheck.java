package link.infra.jumploader.resolution.download;

public interface PreDownloadCheck {
	void check() throws PreDownloadCheckException;

	class PreDownloadCheckException extends Exception {
		public PreDownloadCheckException(String reason) {
			super(reason);
		}

		public PreDownloadCheckException(String reason, Throwable cause) {
			super(reason, cause);
		}
	}
}
