/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

/**
 * Conversion methods used during the serialization/deserialization of Mixnet payloads.
 */
class ConversionUtils {

	private static final String HEX_PREFIX = "0x";

	private ConversionUtils() {
		// Intentionally left blank.
	}

	/**
	 * Converts a {@link BigInteger} to its hexadecimal string representation. The string is prefixed with "0x".
	 *
	 * @param value the BigInteger to convert. Not null.
	 * @return the hexadecimal string representation of {@code value}, prefixed with "0x".
	 */
	static String bigIntegerToHex(final BigInteger value) {
		checkNotNull(value);

		return HEX_PREFIX + value.toString(16).toUpperCase();
	}

	/**
	 * Converts the hexadecimal string representation of a BigInteger to a BigInteger. The string must be prefixed with "0x".
	 *
	 * @param hexString the string to convert. Not null.
	 * @return a BigInteger.
	 */
	static BigInteger hexToBigInteger(final String hexString) {
		checkNotNull(hexString);
		checkArgument(HEX_PREFIX.equals(hexString.substring(0, 2)), String.format("The provided string must be prefixed with %s.", HEX_PREFIX));

		return new BigInteger(hexString.substring(2), 16);
	}

}
