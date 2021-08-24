/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.destroy;

/**
 * Erases the information inside one field. After erasing the content must not reveal anything about the former content.
 *
 * @param <T> the type this instance can destroy.
 */
public interface FieldDestroyer<T> {

	/**
	 * Erases the information inside the field.
	 *
	 * @param fieldValue The value to erase.
	 */
	void destroy(T fieldValue);
}
