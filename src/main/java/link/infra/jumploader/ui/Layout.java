package link.infra.jumploader.ui;

public interface Layout extends Iterable<Component>, Component {
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

	Layout addChild(Component component);
	Layout addChildren(Component... components);
}
