/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.primitives.primes.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.mathematical.primes.PrimesUtils;

class PrimesUtilsTest {

	@Test
	void whenGetAllPrimesListFromFile() {
		List<BigInteger> listBigInteger = PrimesUtils.getPrimesList();

		assertEquals(10000, listBigInteger.size());
	}

}
