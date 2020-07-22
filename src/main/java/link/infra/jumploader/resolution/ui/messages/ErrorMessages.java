package link.infra.jumploader.resolution.ui.messages;

import link.infra.jumploader.resolution.ui.util.UIDetection;
import org.apache.logging.log4j.Logger;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class ErrorMessages {
	private interface ErrorMessageHandler {
		void showFatalMessage(String title, String description, Logger logger);
		void showErrorMessage(String title, String description, Logger logger);
		void showWarnMessage(String title, String description, Logger logger);
		void showInfoMessage(String title, String description, Logger logger);
	}

	private static ErrorMessageHandler handler = null;

	public static void disableGUI() {
		handler = new CLIErrorMessageHandler();
	}

	public static void showFatalMessage(String title, String description, Logger logger) {
		if (handler == null) {
			handler = UIDetection.uiAvailable ? new GUIErrorMessageHandler() : new CLIErrorMessageHandler();
		}
		handler.showFatalMessage(title, description, logger);
	}

	public static void showErrorMessage(String title, String description, Logger logger) {
		if (handler == null) {
			handler = UIDetection.uiAvailable ? new GUIErrorMessageHandler() : new CLIErrorMessageHandler();
		}
		handler.showErrorMessage(title, description, logger);
	}

	public static void showWarnMessage(String title, String description, Logger logger) {
		if (handler == null) {
			handler = UIDetection.uiAvailable ? new GUIErrorMessageHandler() : new CLIErrorMessageHandler();
		}
		handler.showWarnMessage(title, description, logger);
	}

	public static void showInfoMessage(String title, String description, Logger logger) {
		if (handler == null) {
			handler = UIDetection.uiAvailable ? new GUIErrorMessageHandler() : new CLIErrorMessageHandler();
		}
		handler.showInfoMessage(title, description, logger);
	}

	private static class CLIErrorMessageHandler implements ErrorMessageHandler {
		@Override
		public void showFatalMessage(String title, String description, Logger logger) {
			logger.fatal(description);
		}

		@Override
		public void showErrorMessage(String title, String description, Logger logger) {
			logger.error(description);
		}

		@Override
		public void showWarnMessage(String title, String description, Logger logger) {
			logger.warn(description);
		}

		@Override
		public void showInfoMessage(String title, String description, Logger logger) {
			logger.info(description);
		}
	}

	private static class GUIErrorMessageHandler extends CLIErrorMessageHandler {
		@Override
		public void showFatalMessage(String title, String description, Logger logger) {
			super.showFatalMessage(title, description, logger);
			TinyFileDialogs.tinyfd_messageBox(title, description, "ok", "error", true);
		}

		@Override
		public void showErrorMessage(String title, String description, Logger logger) {
			super.showErrorMessage(title, description, logger);
			TinyFileDialogs.tinyfd_messageBox(title, description, "ok", "error", true);
		}

		@Override
		public void showWarnMessage(String title, String description, Logger logger) {
			super.showWarnMessage(title, description, logger);
			TinyFileDialogs.tinyfd_messageBox(title, description, "ok", "warning", true);
		}

		@Override
		public void showInfoMessage(String title, String description, Logger logger) {
			super.showInfoMessage(title, description, logger);
			TinyFileDialogs.tinyfd_messageBox(title, description, "ok", "info", true);
		}
	}
}
