/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.impl;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.SecureRandomFactory;

/**
 * Tests for {@link Exponents}.
 */
class ExponentsTest extends ExponentTestBase {

	@Test
	void givenNullGroupAndCryptoSecureRandomWhenAttemptToCreateExponentThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> Exponents.random(null, _cryptoRandomInteger));
	}

	@Test
	void testWhenRandomExponentCreatedThenValueIsInRange() throws GeneralCryptoLibException {

		boolean notLessThanZero = false;
		boolean lessThanQ = false;

		Exponent randomExponent = Exponents.random(_smallGroup, _cryptoRandomInteger);

		if (BigInteger.ZERO.compareTo(randomExponent.getValue()) < 1) {
			notLessThanZero = true;
		}
		if (randomExponent.getValue().compareTo(_smallQ) < 0) {
			lessThanQ = true;
		}

		assertTrue(notLessThanZero, "The random exponent should be equal or greater than zero");
		assertTrue(lessThanQ, "The random exponent should be less than q");
	}

	@Test
	void testRandomExponents() throws GeneralCryptoLibException {
		String errorMessage = "The random exponents should be different";

		Exponent exponent1 = Exponents.random(_largeGroup, _cryptoRandomInteger);
		Exponent exponent2 = Exponents.random(_largeGroup, _cryptoRandomInteger);
		Exponent exponent3 = Exponents.random(_largeGroup, _cryptoRandomInteger);

		assertNotEquals(exponent1.getValue(), exponent2.getValue(), errorMessage);
		assertNotEquals(exponent1.getValue(), exponent3.getValue(), errorMessage);
		assertNotEquals(exponent2.getValue(), exponent3.getValue(), errorMessage);
	}

	@Test
	void givenTooShortGroupQWhenGenerateShortExponentThenException() throws GeneralCryptoLibException {
		BigInteger p = new BigInteger("23");
		BigInteger q = new BigInteger("11");
		BigInteger g = new BigInteger("2");
		ZpSubgroup subgroup = new ZpSubgroup(g, p, q);
		CryptoRandomInteger cryptoRandomInteger = new SecureRandomFactory(getSecureRandomPolicy()).createIntegerRandom();

		assertThrows(CryptoLibException.class, () -> Exponents.shortRandom(subgroup, cryptoRandomInteger));
	}
}
