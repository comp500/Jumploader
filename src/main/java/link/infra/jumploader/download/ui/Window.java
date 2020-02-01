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
import java.util.ArrayList;
import java.util.Objects;

public class Window implements Component {
	private final ArrayList<Component> components = new ArrayList<>();
	private final long windowPtr;
	// TODO: implement getter
	private boolean shouldClose;

	public Window() {
		// Populate components
		components.add(new DirectionLayout(DirectionLayout.Direction.VERTICAL).addChildren(
			new DirectionLayout(DirectionLayout.Direction.HORIZONTAL).addChildren(
				// TODO: make DirectionLayout make space only on outside, not between components
				// TODO: also fix stuff being broken on first paint
				// TODO: also figure out adaptive width magic
				new DirectionLayout(DirectionLayout.Direction.VERTICAL).addChildren(
					new DirectionLayout(DirectionLayout.Direction.HORIZONTAL).addChildren(new Image("splashlogo.png")),
					new Spacer(0, 30),
					new Rectangle(500, 30, 1f, 0f, 0f),
					new Rectangle(200, 30, 0f, 0f, 0f)
				)
			)
		));

		// Initialise GLFW
		GLFWErrorCallback.createPrint(System.err).set();
		if (!GLFW.glfwInit()) {
			throw new IllegalStateException("Failed to initialise GLFW for Jumploader status");
		}

		// Configure GLFW
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

		// Create the window
		windowPtr = GLFW.glfwCreateWindow(854, 480, "Jumploader", MemoryUtil.NULL, MemoryUtil.NULL);
		if (windowPtr == MemoryUtil.NULL) {
			throw new RuntimeException("Failed to create a window for Jumploader status");
		}

		// TODO: use params?
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

		for (Component component : components) {
			component.init();
		}

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

	// TODO: window should rescale/translate matrix itself? so all components are on a 0-1f frame?
	private void redraw() {
		// Render a frame
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		for (Component component : components) {
			component.render();
		}

		GLFW.glfwSwapBuffers(windowPtr);
	}

	@Override
	public void free() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		for (Component component : components) {
			component.free();
		}

		GL.setCapabilities(null);

		Callbacks.glfwFreeCallbacks(windowPtr);
		GLFW.glfwDestroyWindow(windowPtr);

		GLFW.glfwTerminate();
		Objects.requireNonNull(GLFW.glfwSetErrorCallback(null)).free();
	}

	@Override
	public void updateSize(int width, int height) {
		for (Component component : components) {
			component.updateSize(width, height);
		}
	}

	@Override
	public int getPreferredWidth() {
		return 0;
	}

	@Override
	public int getPreferredHeight() {
		return 0;
	}
}
