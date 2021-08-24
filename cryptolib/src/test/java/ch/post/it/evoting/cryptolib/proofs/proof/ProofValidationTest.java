/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.proof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

class ProofValidationTest {

	static Stream<Arguments> createProof() throws GeneralCryptoLibException {

		BigInteger q = new BigInteger("11");

		Exponent hashValue = new Exponent(q, BigInteger.TEN);

		List<Exponent> exponentList = new ArrayList<>();
		exponentList.add(new Exponent(q, BigInteger.ONE));
		exponentList.add(new Exponent(q, new BigInteger("4")));

		List<Exponent> exponentListWithNullElement = new ArrayList<>(exponentList);
		exponentListWithNullElement.add(null);

		BigInteger differentQ = new BigInteger("12");
		List<Exponent> exponentListWithdifferentQExponent = new ArrayList<>(exponentList);
		exponentListWithdifferentQExponent.add(new Exponent(differentQ, BigInteger.TEN));

		return Stream
				.of(arguments(null, exponentList, "Hash value of proof is null."), arguments(hashValue, null, "List of proof exponents is null."),
						arguments(hashValue, new ArrayList<Exponent>(), "List of proof exponents is empty."),
						arguments(hashValue, exponentListWithNullElement, "List of proof exponents contains one or more null elements."),
						arguments(hashValue, exponentListWithdifferentQExponent,
								"Zp subgroup q parameter of proof exponent value must be equal to Zp subgroup q parameter of proof hash value: "
										+ hashValue.getQ() + "; Found " + differentQ));
	}

	@ParameterizedTest
	@MethodSource("createProof")
	void testProofCreationValidation(Exponent hashValue, List<Exponent> proofValues, String errorMsg) {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> new Proof(hashValue, proofValues));
		assertEquals(errorMsg, exception.getMessage());
	}
}
