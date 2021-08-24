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
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

class PlaintextEqualityProofTest {

	private static final int NUM_PLAINTEXT_ELEMENTS = 6;

	private static ProofsService proofsService;
	private static ZpSubgroup zpSubgroup;
	private static ElGamalPublicKey primaryPublicKey;
	private static ElGamalPublicKey secondaryPublicKey;
	private static Ciphertext primaryCiphertext;
	private static Witness primaryWitness;
	private static Ciphertext secondaryCiphertext;
	private static Witness secondaryWitness;
	private static Proof proof;
	private static ProofPreComputedValues preComputedValues;
	private static ZpSubgroup differentZpSubgroup;
	private static ElGamalPublicKey differentPrimaryPublicKey;
	private static ElGamalPublicKey differentSecondaryPublicKey;
	private static Ciphertext differentPrimaryCiphertext;
	private static Witness differentPrimaryWitness;
	private static Ciphertext differentSecondaryCiphertext;
	private static Witness differentSecondaryWitness;

	@BeforeAll
	static void setUp() throws Exception {

		proofsService = new ProofsService();

		zpSubgroup = MathematicalTestDataGenerator.getQrSubgroup();

		primaryPublicKey = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS).getPublicKeys();
		secondaryPublicKey = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS).getPublicKeys();

		List<ZpGroupElement> plaintext = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);

		ElGamalEncrypterValues encrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator.encryptGroupElements(primaryPublicKey, plaintext);
		primaryCiphertext = encrypterValues;
		primaryWitness = encrypterValues;
		encrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator.encryptGroupElements(secondaryPublicKey, plaintext);
		secondaryCiphertext = encrypterValues;
		secondaryWitness = encrypterValues;

		proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness);

		preComputedValues = proofsService.createProofPreComputerAPI(zpSubgroup)
				.preComputePlaintextEqualityProof(primaryPublicKey, secondaryPublicKey);

		differentZpSubgroup = MathematicalTestDataGenerator.getOtherQrSubgroup();

		do {
			differentPrimaryPublicKey = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS).getPublicKeys();
		} while (differentPrimaryPublicKey.equals(primaryPublicKey));
		do {
			differentSecondaryPublicKey = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS).getPublicKeys();
		} while (differentSecondaryPublicKey.equals(secondaryPublicKey));

		List<ZpGroupElement> differentPlaintext;
		do {
			differentPlaintext = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		} while (differentPlaintext.equals(plaintext));

		do {
			encrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator.encryptGroupElements(primaryPublicKey, differentPlaintext);
			differentPrimaryCiphertext = encrypterValues;
			differentPrimaryWitness = encrypterValues;
		} while (differentPrimaryWitness.getExponent().equals(primaryWitness.getExponent()));
		do {
			encrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator.encryptGroupElements(secondaryPublicKey, differentPlaintext);
			differentSecondaryCiphertext = encrypterValues;
			differentSecondaryWitness = encrypterValues;
		} while (differentSecondaryWitness.getExponent().equals(secondaryWitness.getExponent()));
	}

	@Test
	final void whenGenerateAndVerifyProofThenOk() throws GeneralCryptoLibException {
		assertTrue(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingPreComputedValuesAndVerifyThenOk() throws GeneralCryptoLibException {
		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness, preComputedValues);

		assertTrue(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenSerializeAndDeserializeProofThenOk() throws GeneralCryptoLibException {
		Proof deserializedProof = Proof.fromJson(proof.toJson());

		assertTrue(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, deserializedProof));
	}

	@Test
	final void whenGenerateProofUsingInvalidPreComputedValuesThenVerificationIsFalse() throws GeneralCryptoLibException {
		ProofPreComputedValues differentPreComputedValues;
		do {
			differentPreComputedValues = proofsService.createProofPreComputerAPI(zpSubgroup)
					.preComputePlaintextEqualityProof(differentPrimaryPublicKey, differentSecondaryPublicKey);
		} while (differentPreComputedValues.getExponents().equals(preComputedValues.getExponents()));

		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness, differentPreComputedValues);

		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidPrimaryCiphertextThenVerificationIsFalse() throws GeneralCryptoLibException {
		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(differentPrimaryCiphertext, primaryPublicKey, differentPrimaryWitness, secondaryCiphertext,
						secondaryPublicKey, secondaryWitness);

		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(differentPrimaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidPrimaryPublicKeyThenVerificationIsFalse() throws GeneralCryptoLibException {
		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, differentPrimaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness);

		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, differentPrimaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidPrimaryWitnessThenVerificationIsFalse() throws GeneralCryptoLibException {
		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, differentPrimaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness);

		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidSecondaryCiphertextThenVerificationIsFalse() throws GeneralCryptoLibException {
		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, differentSecondaryCiphertext, secondaryPublicKey,
						differentSecondaryWitness);

		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, differentSecondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidSecondaryPublicKeyThenVerificationIsFalse() throws GeneralCryptoLibException {
		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, differentSecondaryPublicKey,
						secondaryWitness);

		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, differentSecondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidSecondaryWitnessThenVerificationIsFalse() throws GeneralCryptoLibException {
		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						differentSecondaryWitness);

		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenVerifyProofUsingInvalidPrimaryCiphertextThenVerificationIsFalse() throws GeneralCryptoLibException {
		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(differentPrimaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenVerifyProofUsingInvalidPrimaryPublicKeyThenVerificationIsFalse() throws GeneralCryptoLibException {
		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, differentPrimaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenVerifyProofUsingInvalidSecondaryCiphertextThenVerificationIsFalse() throws GeneralCryptoLibException {
		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, differentSecondaryCiphertext, secondaryPublicKey, proof));
	}

	@Test
	final void whenVerifyProofUsingInvalidSecondaryPublicKeyThenVerificationIsFalse() throws GeneralCryptoLibException {
		assertFalse(proofsService.createProofVerifierAPI(zpSubgroup)
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, differentSecondaryPublicKey, proof));
	}

	@Test
	final void whenGenerateProofUsingInvalidMathematicalGroupThenExceptionThrown() throws GeneralCryptoLibException {
		final ProofProverAPI proofProverAPI = proofsService.createProofProverAPI(differentZpSubgroup);
		// We explicitly expect an exception from createPlaintextEqualityProof() and not createProofProverAPI()
		assertThrows(GeneralCryptoLibException.class, () -> proofProverAPI
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness));
	}

	@Test
	final void whenVerifyProofUsingInvalidMathematicalGroupThenExceptionThrown() throws GeneralCryptoLibException {
		final ProofVerifierAPI proofVerifierAPI = proofsService.createProofVerifierAPI(differentZpSubgroup);
		// We explicitly expect an exception from verifyPlaintextEqualityProof() and not createProofVerifierAPI()
		assertThrows(IllegalArgumentException.class, () -> proofVerifierAPI
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
	}
}
