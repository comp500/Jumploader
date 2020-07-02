package link.infra.jumploader.util;

import java.lang.reflect.Field;
import java.util.function.Function;

public class ReflectionUtil {
	@SuppressWarnings("unchecked")
	public static <T> T reflectField(Object destObj, String name) throws NoSuchFieldException, IllegalAccessException, ClassCastException {
		Field field = destObj.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(destObj);
	}

	@SuppressWarnings("unchecked")
	public static <T> T reflectStaticField(Class<?> destClass, String name) throws NoSuchFieldException, IllegalAccessException, ClassCastException {
		Field field = destClass.getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(null);
	}

	@SuppressWarnings({"unchecked", "UnusedReturnValue"})
	public static <T> T transformStaticField(Class<?> destClass, String name, Function<T, T> transformer) throws NoSuchFieldException, IllegalAccessException, ClassCastException {
		Field field = destClass.getDeclaredField(name);
		field.setAccessible(true);
		T value = transformer.apply((T) field.get(null));
		field.set(null, value);
		return value;
	}
}
