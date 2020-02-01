package link.infra.jumploader.download.ui;

public interface Component {
	void init();
	void render();
	void free();
	void updateSize(int width, int height);
	int getPreferredWidth();
	int getPreferredHeight();
}
