/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.test.tools.configuration.GroupLoader;

/**
 * Tests of the Zp subgroup input validation.
 */

class ZpSubgroupValidationTest {

	private static BigInteger p;
	private static BigInteger q;
	private static BigInteger g;
	private static BigInteger pMinusOne;

	@BeforeAll
	static void setUp() {
		GroupLoader zpGroupLoader = new GroupLoader();

		p = zpGroupLoader.getP();
		q = zpGroupLoader.getQ();
		g = zpGroupLoader.getG();

		pMinusOne = p.subtract(BigInteger.ONE);
	}

	static Stream<Arguments> createZpSubgroup() {

		return Stream.of(arguments(null, q, g, "Zp subgroup generator is null."), arguments(g, null, q, "Zp subgroup p parameter is null."),
				arguments(g, p, null, "Zp subgroup q parameter is null."),
				arguments(g, p, BigInteger.ZERO, "Zp subgroup q parameter must be greater than or equal to : 1; Found 0"), arguments(g, p, p,
						"Zp subgroup q parameter must be less than or equal to Zp subgroup p parameter minus 1: " + pMinusOne + "; Found " + p),
				arguments(BigInteger.ONE, p, q, "Zp subgroup generator must be greater than or equal to : 2; Found 1"), arguments(p, p, q,
						"Zp subgroup generator must be less than or equal to Zp subgroup p parameter minus 1: " + pMinusOne + "; Found " + p));
	}

	static Stream<Arguments> deserializeZpSubgroup() {

		String nullPParamJsonStr = "{\"zpSubgroup\":{\"q\":\"Cw==\",\"p\":null,\"g\":\"Ag==\"}}";
		String nullQParamJsonStr = "{\"zpSubgroup\":{\"q\":null,\"p\":\"Fw==\",\"g\":\"Ag==\"}}";
		String nullGeneratorJsonStr = "{\"zpSubgroup\":{\"q\":\"Cw==\",\"p\":\"Fw==\",\"g\":null}}";
		String zeroQParamJsonStr = "{\"zpSubgroup\":{\"q\":\"AA==\",\"p\":\"Fw==\",\"g\":\"Ag==\"}}";
		String tooLargeQParamJsonStr = "{\"zpSubgroup\":{\"q\":\"Fw==\",\"p\":\"Fw==\",\"g\":\"Ag==\"}}";
		String oneGeneratorJsonStr = "{\"zpSubgroup\":{\"q\":\"Cw==\",\"p\":\"Fw==\",\"g\":\"AQ==\"}}";
		String tooLargeGeneratorJsonStr = "{\"zpSubgroup\":{\"q\":\"Cw==\",\"p\":\"Fw==\",\"g\":\"Fw==\"}}";

		return Stream.of(arguments(null, "ZpSubgroup JSON string is null."), arguments("", "ZpSubgroup JSON string is blank."),
				arguments("   ", "ZpSubgroup JSON string is blank."), arguments(nullPParamJsonStr, "Zp subgroup p parameter is null."),
				arguments(nullQParamJsonStr, "Zp subgroup q parameter is null."), arguments(nullGeneratorJsonStr, "Zp subgroup generator is null."),
				arguments(zeroQParamJsonStr, "Zp subgroup q parameter must be greater than or equal to : 1; Found 0"),
				arguments(tooLargeQParamJsonStr, "Zp subgroup q parameter must be less than or equal to Zp subgroup p parameter minus 1: "),
				arguments(oneGeneratorJsonStr, "Zp subgroup generator must be greater than or equal to : 2; Found 1"),
				arguments(tooLargeGeneratorJsonStr, "Zp subgroup generator must be less than or equal to Zp subgroup p parameter minus 1: "));
	}

	@ParameterizedTest
	@MethodSource("createZpSubgroup")
	void testZpSubgroupCreationValidation(BigInteger g, BigInteger p, BigInteger q, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new ZpSubgroup(g, p, q));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("deserializeZpSubgroup")
	void testZpSubgroupDeserializationValidation(String jsonStr, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> ZpSubgroup.fromJson(jsonStr));
		assertTrue(exception.getMessage().contains(errorMsg));
	}
}
