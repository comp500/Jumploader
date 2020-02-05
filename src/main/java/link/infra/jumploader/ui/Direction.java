package link.infra.jumploader.ui;

public enum Direction {
	HORIZONTAL,
	VERTICAL;

	public static class InvalidDirectionException extends RuntimeException {
		public InvalidDirectionException() {
			super("Invalid state reached for Direction!");
		}
	}
}
