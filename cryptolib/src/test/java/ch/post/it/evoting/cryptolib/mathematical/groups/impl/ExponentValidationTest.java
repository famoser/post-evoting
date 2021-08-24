/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.test.tools.configuration.GroupLoader;

class ExponentValidationTest {

	static Stream<Arguments> createExponent() {

		BigInteger q = new GroupLoader().getQ();
		BigInteger exponentValue = BigInteger.TEN;

		return Stream.of(arguments(null, exponentValue, "Zp subgroup q parameter is null."), arguments(q, null, "Exponent value is null."),
				arguments(BigInteger.ZERO, exponentValue, "Zp subgroup q parameter must be greater than or equal to : 1; Found 0"));
	}

	static Stream<Arguments> deserializeExponent() {

		String nullQParamJsonStr = "{\"exponent\":{\"q\":null,\"value\":\"Cg==\"}}";
		String nullExponentValueJsonStr = "{\"exponent\":{\"q\":\"Cw==\",\"value\":null}}";
		String zeroQParamJsonStr = "{\"exponent\":{\"q\":\"AA==\",\"value\":\"Cg==\"}}";

		return Stream.of(arguments(null, "Exponent JSON string is null."), arguments("", "Exponent JSON string is blank."),
				arguments("   ", "Exponent JSON string is blank."), arguments(nullQParamJsonStr, "Zp subgroup q parameter is null."),
				arguments(nullExponentValueJsonStr, "Exponent value is null."),
				arguments(zeroQParamJsonStr, "Zp subgroup q parameter must be greater than or equal to : 1; Found 0"));
	}

	@ParameterizedTest
	@MethodSource("createExponent")
	void testExponentCreationValidation(BigInteger q, BigInteger value, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new Exponent(q, value));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("deserializeExponent")
	void testExponentDeserializationValidation(String jsonStr, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> Exponent.fromJson(jsonStr));
		assertEquals(errorMsg, exception.getMessage());
	}
}
