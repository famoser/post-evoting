/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.destroy;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * A destroyer for the magnitude of a {@link BigInteger}. Since {@link BigInteger} is immutable, in order to clean up any information stored inside,
 * the user must call the {@link #destroyInstances(BigInteger...)} method on whatever {@link BigInteger} need to be destroyed before dereferencing
 * them.
 */
public class BigIntegerDestroyer extends AbstractImmutableDestroyer<BigInteger, int[]> {

	@Override
	public void destroyInstances(final BigInteger... objects) {
		destroyFieldValue(BigInteger.class, "mag", fieldValue -> Arrays.fill(fieldValue, 0), objects);
	}
}
