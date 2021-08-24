/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofPreComputerAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

class ExponentiationProofValidationTest {

	private static final int NUM_BASE_ELEMENTS = 5;

	private static ProofsService proofsServiceForDefaultPolicy;
	private static ZpSubgroup zpSubgroup;
	private static Witness witness;
	private static List<ZpGroupElement> baseElementList;
	private static List<ZpGroupElement> exponentiatedElementList;
	private static List<ZpGroupElement> shorterBaseElementList;
	private static List<ZpGroupElement> emptyElementList;
	private static List<ZpGroupElement> elementListWithNullValue;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		proofsServiceForDefaultPolicy = new ProofsService();

		zpSubgroup = MathematicalTestDataGenerator.getQrSubgroup();

		witness = ElGamalTestDataGenerator.getWitness(zpSubgroup);
		baseElementList = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_BASE_ELEMENTS);
		exponentiatedElementList = MathematicalTestDataGenerator.exponentiateZpGroupElements(baseElementList, witness.getExponent());

		shorterBaseElementList = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, (NUM_BASE_ELEMENTS - 1));

		emptyElementList = new ArrayList<>();

		elementListWithNullValue = new ArrayList<>(baseElementList);
		elementListWithNullValue.set(0, null);
	}

	static Stream<Arguments> createExponentiationProofValidation() {
		return Stream.of(arguments(zpSubgroup, null, baseElementList, witness, "List of exponentiated elements is null."),
				arguments(zpSubgroup, emptyElementList, baseElementList, witness, "List of exponentiated elements is empty."),
				arguments(zpSubgroup, elementListWithNullValue, baseElementList, witness,
						"List of exponentiated elements contains one or more null elements."),
				arguments(zpSubgroup, exponentiatedElementList, null, witness, "List of base elements is null."),
				arguments(zpSubgroup, exponentiatedElementList, emptyElementList, witness, "List of base elements is empty."),
				arguments(zpSubgroup, exponentiatedElementList, elementListWithNullValue, witness,
						"List of base elements contains one or more null elements."),
				arguments(zpSubgroup, exponentiatedElementList, baseElementList, null, "Witness is null."),
				arguments(zpSubgroup, exponentiatedElementList, shorterBaseElementList, witness,
						"Number of exponentiated elements must be equal to number of base elements: " + shorterBaseElementList.size() + "; Found "
								+ exponentiatedElementList.size()));
	}

	static Stream<Arguments> preComputeExponentiationProofValidation() {
		return Stream.of(arguments(zpSubgroup, null, "List of base elements is null."),
				arguments(zpSubgroup, emptyElementList, "List of base elements is empty."),
				arguments(zpSubgroup, elementListWithNullValue, "List of base elements contains one or more null elements."));
	}

	static Stream<Arguments> createExponentiationProofUsingPreComputedValues() throws GeneralCryptoLibException {

		ProofPreComputedValues preComputedValues = proofsServiceForDefaultPolicy.createProofPreComputerAPI(zpSubgroup)
				.preComputeExponentiationProof(baseElementList);

		return Stream.of(arguments(zpSubgroup, null, baseElementList, witness, preComputedValues, "List of exponentiated elements is null."),
				arguments(zpSubgroup, emptyElementList, baseElementList, witness, preComputedValues, "List of exponentiated elements is empty."),
				arguments(zpSubgroup, elementListWithNullValue, baseElementList, witness, preComputedValues,
						"List of exponentiated elements contains one or more null elements."),
				arguments(zpSubgroup, exponentiatedElementList, null, witness, preComputedValues, "List of base elements is null."),
				arguments(zpSubgroup, exponentiatedElementList, emptyElementList, witness, preComputedValues, "List of base elements is empty."),
				arguments(zpSubgroup, exponentiatedElementList, elementListWithNullValue, witness, preComputedValues,
						"List of base elements contains one or more null elements."),
				arguments(zpSubgroup, exponentiatedElementList, baseElementList, null, preComputedValues, "Witness is null."),
				arguments(zpSubgroup, exponentiatedElementList, shorterBaseElementList, witness, preComputedValues,
						"Number of exponentiated elements must be equal to number of base elements: " + shorterBaseElementList.size() + "; Found "
								+ exponentiatedElementList.size()),
				arguments(zpSubgroup, exponentiatedElementList, baseElementList, witness, null, "Pre-computed values object is null."));
	}

	static Stream<Arguments> verifyExponentiationProof() throws GeneralCryptoLibException {
		Proof proof = proofsServiceForDefaultPolicy.createProofProverAPI(zpSubgroup)
				.createExponentiationProof(exponentiatedElementList, baseElementList, witness);

		return Stream.of(arguments(zpSubgroup, null, baseElementList, proof, "List of exponentiated elements is null."),
				arguments(zpSubgroup, emptyElementList, baseElementList, proof, "List of exponentiated elements is empty."),
				arguments(zpSubgroup, elementListWithNullValue, baseElementList, proof,
						"List of exponentiated elements contains one or more null elements."),
				arguments(zpSubgroup, exponentiatedElementList, null, proof, "List of base elements is null."),
				arguments(zpSubgroup, exponentiatedElementList, emptyElementList, proof, "List of base elements is empty."),
				arguments(zpSubgroup, exponentiatedElementList, elementListWithNullValue, proof,
						"List of base elements contains one or more null elements."),
				arguments(zpSubgroup, exponentiatedElementList, baseElementList, null, "Exponentiation proof is null."),
				arguments(zpSubgroup, exponentiatedElementList, shorterBaseElementList, proof,
						"Number of exponentiated elements must be equal to number of base elements: " + shorterBaseElementList.size() + "; Found "
								+ exponentiatedElementList.size()));
	}

	@Test
	void testProofProverAPICreationGroupValidation() {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofsServiceForDefaultPolicy.createProofProverAPI(null));
		assertEquals("Zp subgroup is null.", exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createExponentiationProofValidation")
	void testExponentiationProofCreationValidation(ZpSubgroup group, List<ZpGroupElement> exponentiatedElements, List<ZpGroupElement> baseElements,
			Witness witness, String errorMsg) throws GeneralCryptoLibException {
		final ProofProverAPI proofProverAPI = proofsServiceForDefaultPolicy.createProofProverAPI(group);
		// We explicitly expect an exception from createExponentiationProof() and not createProofProverAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofProverAPI.createExponentiationProof(exponentiatedElements, baseElements, witness));
		assertEquals(errorMsg, exception.getMessage());
	}

	@Test
	void testExponentiationProofPreComputationGroupValidation() {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofsServiceForDefaultPolicy.createProofPreComputerAPI(null));
		assertEquals("Zp subgroup is null.", exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("preComputeExponentiationProofValidation")
	void testExponentiationProofPreComputationValidation(ZpSubgroup group, List<ZpGroupElement> baseElements, String errorMsg)
			throws GeneralCryptoLibException {
		final ProofPreComputerAPI proofPreComputerAPI = proofsServiceForDefaultPolicy.createProofPreComputerAPI(group);
		// We explicitly expect an exception from preComputeExponentiationProof() and not createProofPreComputerAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofPreComputerAPI.preComputeExponentiationProof(baseElements));
		assertEquals(errorMsg, exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("createExponentiationProofUsingPreComputedValues")
	void testExponentiationProofCreationUsingPreComputedValuesValidation(ZpSubgroup group, List<ZpGroupElement> exponentiatedElements,
			List<ZpGroupElement> baseElements, Witness witness, ProofPreComputedValues preComputedValues, String errorMsg)
			throws GeneralCryptoLibException {
		final ProofProverAPI proofProverAPI = proofsServiceForDefaultPolicy.createProofProverAPI(group);
		// We explicitly expect an exception from createExponentiationProof() and not createProofProverAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofProverAPI.createExponentiationProof(exponentiatedElements, baseElements, witness, preComputedValues));
		assertEquals(errorMsg, exception.getMessage());
	}

	@Test
	void testProofVerifierAPIVerificationGroupValidation() {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofsServiceForDefaultPolicy.createProofVerifierAPI(null));
		assertEquals("Zp subgroup is null.", exception.getMessage());
	}

	@ParameterizedTest
	@MethodSource("verifyExponentiationProof")
	void testExponentiationProofVerificationValidation(ZpSubgroup group, List<ZpGroupElement> exponentiatedElements,
			List<ZpGroupElement> baseElements, Proof proof, String errorMsg) throws GeneralCryptoLibException {
		final ProofVerifierAPI proofVerifierAPI = proofsServiceForDefaultPolicy.createProofVerifierAPI(group);
		// We explicitly expect an exception from verifyExponentiationProof() and not createProofVerifierAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofVerifierAPI.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
		assertEquals(errorMsg, exception.getMessage());
	}
}
