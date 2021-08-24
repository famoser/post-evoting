/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.impl;

import java.math.BigInteger;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomInteger;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;

/**
 * Utilities to generate random exponents.
 */
public class Exponents {

	private static final int SHORT_EXPONENT_BIT_LENGTH = 256;

	private Exponents() {
	}

	/**
	 * Generates a uniformly distributed random Exponent for the received mathematical group.
	 *
	 * <p>The value of the created exponent will be between {@code 0} and {@code q-1}.
	 *
	 * @param group               a mathematical group.
	 * @param cryptoRandomInteger the entropy source used to generate the value of the Exponent.
	 */
	public static Exponent random(final MathematicalGroup<?> group, final CryptoAPIRandomInteger cryptoRandomInteger)
			throws GeneralCryptoLibException {

		Validate.notNull(group, "Zp subgroup");

		BigInteger value = getRandomExponentValue(group.getQ().bitLength(), group.getQ(), cryptoRandomInteger);
		return new Exponent(group.getQ(), value);
	}

	/**
	 * Generates a short uniformly distributed random {@code Exponent} for the received mathematical group. Short exponents SHALL only be used for
	 * accelerating ElGamal encryptions. Short exponents MUST NOT be used with signature schemes.
	 *
	 * <p>The value of the created exponent will be between {@code 0} and {@code 2^}{@value
	 * #SHORT_EXPONENT_BIT_LENGTH}{@code -1} bit length.
	 *
	 * @param group               a mathematical group.
	 * @param cryptoRandomInteger a generator.
	 * @return a random Exponent.
	 * @throws GeneralCryptoLibException if a group is null.
	 * @throws CryptoLibException        if the bit length of the group parameter q is smaller than {@value #SHORT_EXPONENT_BIT_LENGTH}.
	 */
	public static Exponent shortRandom(final MathematicalGroup<?> group, final CryptoRandomInteger cryptoRandomInteger)
			throws GeneralCryptoLibException {

		Validate.notNull(group, "Zp subgroup");

		if (group.getQ().bitLength() < SHORT_EXPONENT_BIT_LENGTH) {
			throw new CryptoLibException(
					"Cannot generate a random value because the bit length of the group parameter q is smaller than " + SHORT_EXPONENT_BIT_LENGTH);
		}

		BigInteger value = getRandomExponentValue(SHORT_EXPONENT_BIT_LENGTH, group.getQ(), cryptoRandomInteger);

		return new Exponent(group.getQ(), value);
	}

	private static BigInteger getRandomExponentValue(final int bitLength, final BigInteger q, final CryptoAPIRandomInteger cryptoRandomInteger) {

		BigInteger random;

		do {
			random = cryptoRandomInteger.genRandomIntegerByBits(bitLength);
		} while (random.compareTo(q) >= 0);

		return random;
	}
}
