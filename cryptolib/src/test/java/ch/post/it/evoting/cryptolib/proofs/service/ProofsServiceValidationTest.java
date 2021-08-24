/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.constants.SecureRandomConstants;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.test.tools.utils.CommonTestDataGenerator;

class ProofsServiceValidationTest {

	private static ProofsService proofsServiceForDefaultPolicy;
	private static String whiteSpaceString;

	@BeforeAll
	static void setUp() {

		proofsServiceForDefaultPolicy = new ProofsService();

		whiteSpaceString = CommonTestDataGenerator
				.getWhiteSpaceString(CommonTestDataGenerator.getInt(1, SecureRandomConstants.MAXIMUM_GENERATED_STRING_LENGTH));
	}

	static Stream<Arguments> createProofPreComputer() {
		return Stream.of(arguments(null, "Zp subgroup is null."));
	}

	static Stream<Arguments> createProofGenerator() {
		return Stream.of(arguments(null, "Zp subgroup is null."));
	}

	static Stream<Arguments> createProofVerifier() {
		return Stream.of(arguments(null, "Zp subgroup is null."));
	}

	static Stream<Arguments> deserializeProof() {

		String proofWithNullHashValue = "{\"zkProof\":{\"q\":\"Cw==\",\"hash\":null,\"values\":[\"AQ==\",\"BA==\"]}}";
		String proofWithNullExponentsList = "{\"zkProof\":{\"q\":\"Cw==\",\"hash\":\"Cg==\",\"values\":null}}";
		String proofWithEmptyExponentsList = "{\"zkProof\":{\"q\":\"Cw==\",\"hash\":\"Cg==\",\"values\":[]}}";
		String proofWithNullInExponentsList = "{\"zkProof\":{\"q\":\"Cw==\",\"hash\":\"Cg==\",\"values\":[\"AQ==\",null]}}";
		String proofWithNullQ = "{\"zkProof\":{\"q\":null,\"hash\":\"Cg==\",\"values\":[\"AQ==\",\"BA==\"]}}";

		return Stream.of(arguments(null, "Proof JSON string is null."), arguments("", "Proof JSON string is blank."),
				arguments(whiteSpaceString, "Proof JSON string is blank."), arguments(proofWithNullHashValue, "Exponent value is null."),
				arguments(proofWithNullExponentsList, "List of proof exponents is null."),
				arguments(proofWithEmptyExponentsList, "List of proof exponents is empty."),
				arguments(proofWithNullInExponentsList, "Exponent value is null."), arguments(proofWithNullQ, "Zp subgroup q parameter is null."));
	}

	@ParameterizedTest
	@MethodSource("createProofPreComputer")
	void testProofPreComputerCreationValidation(ZpSubgroup group, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofsServiceForDefaultPolicy.createProofPreComputerAPI(group));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createProofGenerator")
	void testProofGeneratorCreationValidation(ZpSubgroup group, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofsServiceForDefaultPolicy.createProofProverAPI(group));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createProofVerifier")
	void testProofVerifierCreationValidation(ZpSubgroup group, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofsServiceForDefaultPolicy.createProofVerifierAPI(group));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("deserializeProof")
	void testProofDeserializationValidation(String jsonStr, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> Proof.fromJson(jsonStr));
		assertEquals(errorMsg, exception.getMessage());
	}
}
