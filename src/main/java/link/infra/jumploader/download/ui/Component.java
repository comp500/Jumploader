package link.infra.jumploader.download.ui;

public interface Component extends Comparable<Component> {
	void init();
	void render();
	void free();
	float getMinimumWidth();
	float getMinimumHeight();
	float updateWidth(float maximumWidth, float maximumHeight);
	float updateHeight(float maximumWidth, float maximumHeight);

	enum Grows {
		ALWAYS,
		SOMETIMES,
		NEVER
	}

	default Grows getGrows() {
		return Grows.SOMETIMES;
	}

	// Default sorting order is ALWAYS -> SOMETIMES -> NEVER
	@Override
	default int compareTo(Component other) {
		int firstEquiv = getGrows().ordinal();
		int secondEquiv = other.getGrows().ordinal();
		return secondEquiv - firstEquiv;
	}
}
