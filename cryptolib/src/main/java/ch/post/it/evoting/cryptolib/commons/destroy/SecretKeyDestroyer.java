/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.destroy;

import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

/**
 * A destroyer for the key bytes of a {@link SecretKeySpec}. Since {@link SecretKeySpec} is immutable, in order to clean up any information stored
 * inside, the user must call the {@link #destroyInstances(SecretKeySpec...)} method on whatever {@link SecretKeySpec} need to be destroyed before
 * dereferencing them.
 */
public class SecretKeyDestroyer extends AbstractImmutableDestroyer<SecretKeySpec, byte[]> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroyInstances(final SecretKeySpec... objects) {
		destroyFieldValue(SecretKeySpec.class, "key", fieldValue -> Arrays.fill(fieldValue, (byte) 0x00), objects);
	}

}
