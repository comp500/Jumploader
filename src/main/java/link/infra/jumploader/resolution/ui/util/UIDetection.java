package link.infra.jumploader.resolution.ui.util;

import java.awt.*;

public class UIDetection {
	public static final boolean lwjglAvailable;
	public static final boolean uiAvailable;

	static {
		boolean lwjglAvailableTemp = false;
		try {
			Class.forName("org.lwjgl.system.MemoryStack");
			lwjglAvailableTemp = true;
		} catch (ClassNotFoundException ignored) {}
		lwjglAvailable = lwjglAvailableTemp;
		uiAvailable = lwjglAvailable && !GraphicsEnvironment.isHeadless();
	}
}
