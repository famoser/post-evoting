/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.destroy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BigIntegerDestroyerTest {
	private static final BigIntegerDestroyer destroyer = new BigIntegerDestroyer();

	@Test
	void testDestroyer() {
		BigInteger bigInteger = new BigInteger(String.valueOf(Integer.MAX_VALUE));

		destroyer.destroyInstances(bigInteger);

		assertArrayEquals(new byte[] { 0 }, bigInteger.toByteArray());
	}

	@Test
	void testMultipleBigIntegers() {
		List<BigInteger> bigIntegers = new ArrayList<>();

		for (int i = 0; i < 100; i++) {
			bigIntegers.add(new BigInteger(String.valueOf(i)));
		}

		destroyer.destroyInstances(bigIntegers.toArray(new BigInteger[0]));

		for (BigInteger bigInteger : bigIntegers) {
			assertArrayEquals(new byte[] { 0 }, bigInteger.toByteArray());
		}
	}
}
