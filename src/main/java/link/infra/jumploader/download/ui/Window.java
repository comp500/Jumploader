package link.infra.jumploader.download.ui;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.Objects;

public class Window implements Component {
	private final Component rootComponent;
	private final long windowPtr;
	private boolean shouldClose;

	public Window(Component rootComponent) {
		this.rootComponent = rootComponent;

		// Initialise GLFW
		GLFWErrorCallback.createPrint(System.err).set();
		if (!GLFW.glfwInit()) {
			throw new IllegalStateException("Failed to initialise GLFW for Jumploader status");
		}

		// Configure GLFW
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

		// TODO: use params?
		// Create the window
		windowPtr = GLFW.glfwCreateWindow(854, 480, "Jumploader", MemoryUtil.NULL, MemoryUtil.NULL);
		if (windowPtr == MemoryUtil.NULL) {
			throw new RuntimeException("Failed to create a window for Jumploader status");
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);

			GLFW.glfwGetWindowSize(windowPtr, width, height);

			updateSize(width.get(0), height.get(0));

			GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
			if (vidmode != null) {
				int xPos = (vidmode.width() - width.get(0)) / 2;
				int yPos = (vidmode.height() - height.get(0)) / 2;
				GLFW.glfwSetWindowPos(windowPtr, xPos, yPos);
			}

			GLFW.glfwSetWindowRefreshCallback(windowPtr, window -> redraw());
			GLFW.glfwSetWindowSizeCallback(windowPtr, this::windowSizeChanged);
			GLFW.glfwSetFramebufferSizeCallback(windowPtr, this::framebufferSizeChanged);
		}
	}

	@Override
	public void init() {
		// Set up the GL context
		GLFW.glfwMakeContextCurrent(windowPtr);
		GL.createCapabilities();

		GLFW.glfwSwapInterval(1);
		GLFW.glfwShowWindow(windowPtr);

		rootComponent.init();

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
	}

	private void windowSizeChanged(long window, int width, int height) {
		updateSize(width, height);

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0, width, height, 0.0, -1.0, 1.0);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}

	private void framebufferSizeChanged(long window, int newWidth, int newHeight) {
		GL11.glViewport(0, 0, newWidth, newHeight);
	}

	@Override
	public void render() {
		if (shouldClose || GLFW.glfwWindowShouldClose(windowPtr)) {
			shouldClose = true;
			return;
		}

		// Poll for input events
		GLFW.glfwPollEvents();

		redraw();
	}

	public boolean shouldClose() {
		return shouldClose;
	}

	private void redraw() {
		// Render a frame
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		rootComponent.render();
		GLFW.glfwSwapBuffers(windowPtr);
	}

	@Override
	public void free() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		rootComponent.free();

		GL.setCapabilities(null);

		Callbacks.glfwFreeCallbacks(windowPtr);
		GLFW.glfwDestroyWindow(windowPtr);

		GLFW.glfwTerminate();
		Objects.requireNonNull(GLFW.glfwSetErrorCallback(null)).free();
	}

	@Override
	public float getMinimumWidth() {
		return 0;
	}

	@Override
	public float getMinimumHeight() {
		return 0;
	}

	@Override
	public void updateSize(float maximumWidth, float maximumHeight) {
		rootComponent.updateSize(maximumWidth, maximumHeight);
	}

	@Override
	public float getCurrentWidth() {
		return rootComponent.getCurrentWidth();
	}

	@Override
	public float getCurrentHeight() {
		return rootComponent.getCurrentHeight();
	}

}
