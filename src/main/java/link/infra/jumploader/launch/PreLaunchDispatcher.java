package link.infra.jumploader.launch;

import link.infra.jumploader.launch.serviceloading.FileSystemProviderAppender;
import link.infra.jumploader.launch.serviceloading.JimfsURLHandlerAppender;

import java.util.Arrays;
import java.util.List;

public class PreLaunchDispatcher {
	public interface Handler {
		void handlePreLaunch(ClassLoader loadingClassloader);
	}

	private static final List<Handler> HANDLERS = Arrays.asList(
		new FileSystemProviderAppender(),
		new JimfsURLHandlerAppender()
	);

	public static void dispatch(ClassLoader loadingClassloader) {
		for (Handler handler : HANDLERS) {
			handler.handlePreLaunch(loadingClassloader);
		}
	}
}
