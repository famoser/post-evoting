/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Converts a hexadecimal string representation of BigInteger to a BigInteger.
 */
class HexToBigIntegerConverter extends StdConverter<String, BigInteger> {

	@Override
	public BigInteger convert(final String value) {
		return ConversionUtils.hexToBigInteger(value);
	}

}
