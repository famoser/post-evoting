/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Converts a {@link BigInteger} into its hexadecimal string representation.
 */
class BigIntegerToHexConverter extends StdConverter<BigInteger, String> {

	@Override
	public String convert(final BigInteger value) {
		return ConversionUtils.bigIntegerToHex(value);
	}

}
