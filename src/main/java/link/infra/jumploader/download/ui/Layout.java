package link.infra.jumploader.download.ui;

import java.util.Arrays;
import java.util.Collection;

public interface Layout extends Collection<Component>, Component {
	enum Growth {
		ALWAYS,
		SOMETIMES,
		NEVER
	}

	@Override
	default void init() {
		for (Component component : this) {
			component.init();
		}
	}

	@Override
	default void free() {
		for (Component component : this) {
			component.free();
		}
	}

	default Layout addChildren(Component... components) {
		this.addAll(Arrays.asList(components));
		return this;
	}
}
