package link.infra.jumploader.resolution.ui;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SortedIterable<E extends Comparable<? super E>, T extends Iterable<E>> implements Iterable<E> {
	private final T childIterable;

	public SortedIterable(T childIterable) {
		this.childIterable = childIterable;
	}

	@Nonnull
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int i = 0;
			List<E> cachedValues;

			private void updateCache() {
				if (cachedValues == null) {
					cachedValues = new ArrayList<>();
					for (E value : childIterable) {
						cachedValues.add(value);
					}
					Collections.sort(cachedValues);
				}
			}

			@Override
			public boolean hasNext() {
				updateCache();
				return cachedValues.size() - i > 0;
			}

			@Override
			public E next() {
				updateCache();
				return cachedValues.get(i++);
			}
		};
	}
}
