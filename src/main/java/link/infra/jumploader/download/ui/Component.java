package link.infra.jumploader.download.ui;

public interface Component {
	void init();
	void render();
	void free();
	void updateSize(int width, int height);
	int getMinimumWidth();
	int getMinimumHeight();
	int getMaximumWidth();
	int getMaximumHeight();
}
