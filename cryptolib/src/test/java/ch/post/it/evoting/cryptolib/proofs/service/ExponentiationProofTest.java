/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

class ExponentiationProofTest {

	private static final int NUM_BASE_ELEMENTS = 5;

	private static ProofsService proofsServiceForDefaultPolicy;
	private static ZpSubgroup zpSubgroup;
	private static Witness witness;
	private static List<ZpGroupElement> baseElements;
	private static List<ZpGroupElement> exponentiatedElements;
	private static Proof proof;
	private static ProofPreComputedValues preComputedValues;
	private static ZpSubgroup differentZpSubgroup;
	private static List<ZpGroupElement> differentBaseElements;

	@BeforeAll
	static void setUp() throws Exception {

		proofsServiceForDefaultPolicy = new ProofsService();

		zpSubgroup = MathematicalTestDataGenerator.getQrSubgroup();

		witness = ElGamalTestDataGenerator.getWitness(zpSubgroup);
		baseElements = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_BASE_ELEMENTS);
		exponentiatedElements = MathematicalTestDataGenerator.exponentiateZpGroupElements(baseElements, witness.getExponent());

		proof = proofsServiceForDefaultPolicy.createProofProverAPI(zpSubgroup)
				.createExponentiationProof(exponentiatedElements, baseElements, witness);

		preComputedValues = proofsServiceForDefaultPolicy.createProofPreComputerAPI(zpSubgroup).preComputeExponentiationProof(baseElements);

		differentZpSubgroup = MathematicalTestDataGenerator.getOtherQrSubgroup();

		do {
			differentBaseElements = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_BASE_ELEMENTS);
		} while (differentBaseElements.equals(baseElements));
	}

	@Test
	final void whenGenerateAndVerifyProofThenOk() throws GeneralCryptoLibException {
		assertTrue(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
	}

	@Test
	final void whenGenerateProofUsingPreComputedValuesAndVerifyThenOk() throws GeneralCryptoLibException {
		Proof proof = proofsServiceForDefaultPolicy.createProofProverAPI(zpSubgroup)
				.createExponentiationProof(exponentiatedElements, baseElements, witness, preComputedValues);

		assertTrue(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
	}

	@Test
	final void whenSerializeAndDeserializeProofThenOk() throws GeneralCryptoLibException {
		Proof deserializedProof = Proof.fromJson(proof.toJson());

		assertTrue(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(exponentiatedElements, baseElements, deserializedProof));
	}

	@Test
	final void whenGenerateProofUsingInvalidPreComputedValuesThenVerificationIsFalse() throws GeneralCryptoLibException {
		ProofPreComputedValues differentPreComputedValues;
		do {
			differentPreComputedValues = proofsServiceForDefaultPolicy.createProofPreComputerAPI(zpSubgroup)
					.preComputeExponentiationProof(differentBaseElements);
		} while (differentPreComputedValues.getExponents().equals(preComputedValues.getExponents()));

		Proof proof = proofsServiceForDefaultPolicy.createProofProverAPI(zpSubgroup)
				.createExponentiationProof(exponentiatedElements, baseElements, witness, differentPreComputedValues);

		assertFalse(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidfBaseElementsThenVerificationIsFalse() throws GeneralCryptoLibException {
		Proof proof = proofsServiceForDefaultPolicy.createProofProverAPI(zpSubgroup)
				.createExponentiationProof(exponentiatedElements, differentBaseElements, witness);

		assertFalse(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(exponentiatedElements, differentBaseElements, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidWitnessThenVerificationIsFalse() throws GeneralCryptoLibException {
		Witness differentWitness;
		do {
			differentWitness = ElGamalTestDataGenerator.getWitness(zpSubgroup);
		} while (differentWitness.getExponent().equals(witness.getExponent()));

		Proof proof = proofsServiceForDefaultPolicy.createProofProverAPI(zpSubgroup)
				.createExponentiationProof(exponentiatedElements, baseElements, differentWitness);

		assertFalse(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
	}

	@Test
	final void whenVerifyProofUsingInvalidExponentiatedElementsThenVerificationIsFalse() throws GeneralCryptoLibException {
		List<ZpGroupElement> differentExponentiatedElements = MathematicalTestDataGenerator
				.exponentiateZpGroupElements(differentBaseElements, witness.getExponent());

		assertFalse(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(differentExponentiatedElements, baseElements, proof));
	}

	@Test
	final void whenVerifyProofUsingInvalidBaseElementsThenVerificationIsFalse() throws GeneralCryptoLibException {
		assertFalse(proofsServiceForDefaultPolicy.createProofVerifierAPI(zpSubgroup)
				.verifyExponentiationProof(exponentiatedElements, differentBaseElements, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidMathematicalGroupThenExceptionThrown() throws GeneralCryptoLibException {
		ProofProverAPI proofProverAPI = proofsServiceForDefaultPolicy.createProofProverAPI(differentZpSubgroup);
		assertThrows(GeneralCryptoLibException.class, () -> proofProverAPI.createExponentiationProof(exponentiatedElements, baseElements, witness));
	}

	@Test
	final void whenVerifyProofUsingInvalidMathematicalGroupThenExceptionThrown() throws GeneralCryptoLibException {
		ProofVerifierAPI proofVerifierAPI = proofsServiceForDefaultPolicy.createProofVerifierAPI(differentZpSubgroup);
		assertThrows(IllegalArgumentException.class, () -> proofVerifierAPI.verifyExponentiationProof(exponentiatedElements, baseElements, proof));
	}
}
