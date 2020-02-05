package link.infra.jumploader.ui;

public enum Alignment {
	START,
	CENTER,
	END;

	public static class InvalidAlignmentException extends RuntimeException {
		public InvalidAlignmentException() {
			super("Invalid state reached for Alignment!");
		}
	}
}
