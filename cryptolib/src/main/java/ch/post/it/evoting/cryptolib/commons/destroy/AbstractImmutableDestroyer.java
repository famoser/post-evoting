/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.destroy;

import java.lang.reflect.Field;

/**
 * This class groups the common logic to remove sensitive information inside immutable classes. When one class is immutable its content is not freed
 * from memory until it is dereferenced and garbage collected. Additionally the memory content may still have the sensitive bytes left without
 * rewriting. In order to avoid memory dumps from revealing this kind of data, the user must call an appropriate subclass of {@link
 * AbstractImmutableDestroyer} before dereferencing this kind of objects.
 *
 * @param <T> the type of immutable objects this instance can deal with.
 * @param <S> the type of the sensitive information this instance can destroy.
 */
public abstract class AbstractImmutableDestroyer<T, S> {

	private static String errorMessage(final Class<?> clazz, final String fieldName) {
		return "The " + clazz.getCanonicalName() + " field " + fieldName;
	}

	/**
	 * Destroys the sensitive information inside the provided instances of T.
	 *
	 * @param objects the instances to destroy.
	 */
	public abstract void destroyInstances(final T... objects);

	protected void destroyFieldValue(final Class<T> clazz, final String fieldName, final FieldDestroyer<S> destroyer, final T... objects) {
		Field field;
		try {
			field = clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException nsfe) {
			throw new IllegalStateException(errorMessage(clazz, fieldName) + " does not exist.", nsfe);
		}
		field.setAccessible(true);
		for (T object : objects) {
			if (object == null) {
				continue;
			}
			destroyer.destroy(obtainValue(field, object));
		}
	}

	@SuppressWarnings("unchecked")
	private S obtainValue(final Field field, final Object instance) {
		try {
			return (S) field.get(instance);
		} catch (IllegalArgumentException iae) {
			throw new IllegalStateException(errorMessage(field.getClass(), field.getName()) + " has been accessed with an illegal argument.", iae);
		} catch (IllegalAccessException iae) {
			throw new IllegalStateException(errorMessage(field.getClass(), field.getName()) + " is not accessible.", iae);
		}
	}
}
