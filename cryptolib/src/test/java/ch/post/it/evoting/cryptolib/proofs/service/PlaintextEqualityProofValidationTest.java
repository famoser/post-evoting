/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofPreComputerAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

class PlaintextEqualityProofValidationTest {

	private static final int NUM_PLAINTEXT_ELEMENTS = 6;

	private static ProofsService proofsService;
	private static ZpSubgroup zpSubgroup;
	private static ElGamalPublicKey primaryPublicKey;
	private static ElGamalPublicKey secondaryPublicKey;
	private static Ciphertext primaryCiphertext;
	private static Witness primaryWitness;
	private static Ciphertext secondaryCiphertext;
	private static Witness secondaryWitness;
	private static ElGamalPublicKey shorterPrimaryPublicKey;
	private static ElGamalPublicKey shorterSecondaryPublicKey;
	private static Ciphertext shorterPrimaryCiphertext;
	private static int publicKeyLength;
	private static int ciphertextLength;
	private static int shorterPublicKeyLength;
	private static int shorterCiphertextLength;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

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

		shorterPrimaryPublicKey = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, (NUM_PLAINTEXT_ELEMENTS - 1)).getPublicKeys();
		shorterSecondaryPublicKey = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, (NUM_PLAINTEXT_ELEMENTS - 1)).getPublicKeys();

		List<ZpGroupElement> shorterPlaintext = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, (NUM_PLAINTEXT_ELEMENTS - 1));
		encrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator.encryptGroupElements(shorterPrimaryPublicKey, shorterPlaintext);
		shorterPrimaryCiphertext = encrypterValues;

		publicKeyLength = primaryPublicKey.getKeys().size();
		ciphertextLength = primaryCiphertext.size();
		shorterPublicKeyLength = shorterPrimaryPublicKey.getKeys().size();
		shorterCiphertextLength = shorterPrimaryCiphertext.size();
	}

	static Stream<Arguments> createPlaintextEqualityProof() {
		return Stream.of(arguments(zpSubgroup, null, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey, secondaryWitness,
				"Primary ciphertext is null."),
				arguments(zpSubgroup, primaryCiphertext, null, primaryWitness, secondaryCiphertext, secondaryPublicKey, secondaryWitness,
						"Primary ElGamal public key is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, null, secondaryCiphertext, secondaryPublicKey, secondaryWitness,
						"Primary witness is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, primaryWitness, null, secondaryPublicKey, secondaryWitness,
						"Secondary ciphertext is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, null, secondaryWitness,
						"Secondary ElGamal public key is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey, null,
						"Secondary witness is null."),
				arguments(zpSubgroup, shorterPrimaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness, "Primary ciphertext length must be equal to secondary ciphertext length: " + ciphertextLength + "; Found "
								+ shorterCiphertextLength),
				arguments(zpSubgroup, primaryCiphertext, shorterPrimaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness,
						"Primary ElGamal public key length must be equal to secondary ElGamal public key length: " + publicKeyLength + "; Found "
								+ shorterPublicKeyLength),
				arguments(zpSubgroup, primaryCiphertext, shorterPrimaryPublicKey, primaryWitness, secondaryCiphertext, shorterSecondaryPublicKey,
						secondaryWitness,
						"Ciphertext length must be equal to ElGamal public key length plus 1: " + shorterCiphertextLength + "; Found "
								+ ciphertextLength));
	}

	static Stream<Arguments> preComputePlaintextEqualityProofProof() {
		return Stream.of(arguments(zpSubgroup, null, secondaryPublicKey, "Primary ElGamal public key is null."),
				arguments(zpSubgroup, primaryPublicKey, null, "Secondary ElGamal public key is null."),
				arguments(zpSubgroup, shorterPrimaryPublicKey, secondaryPublicKey,
						"Primary ElGamal public key length must be equal to secondary ElGamal public key length: " + publicKeyLength + "; Found "
								+ shorterPublicKeyLength));
	}

	static Stream<Arguments> createPlaintextEqualityProofUsingPreComputedValues() throws GeneralCryptoLibException {

		ProofPreComputedValues preComputedValues = proofsService.createProofPreComputerAPI(zpSubgroup)
				.preComputePlaintextEqualityProof(primaryPublicKey, secondaryPublicKey);

		return Stream.of(arguments(zpSubgroup, null, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey, secondaryWitness,
				preComputedValues, "Primary ciphertext is null."),
				arguments(zpSubgroup, primaryCiphertext, null, primaryWitness, secondaryCiphertext, secondaryPublicKey, secondaryWitness,
						preComputedValues, "Primary ElGamal public key is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, null, secondaryCiphertext, secondaryPublicKey, secondaryWitness,
						preComputedValues, "Primary witness is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, primaryWitness, null, secondaryPublicKey, secondaryWitness,
						preComputedValues, "Secondary ciphertext is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, null, secondaryWitness,
						preComputedValues, "Secondary ElGamal public key is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey, null,
						preComputedValues, "Secondary witness is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey, secondaryWitness,
						null, "Pre-computed values object is null."),
				arguments(zpSubgroup, shorterPrimaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness, preComputedValues,
						"Primary ciphertext length must be equal to secondary ciphertext length: " + ciphertextLength + "; Found "
								+ shorterCiphertextLength),
				arguments(zpSubgroup, primaryCiphertext, shorterPrimaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness, preComputedValues,
						"Primary ElGamal public key length must be equal to secondary ElGamal public key length: " + publicKeyLength + "; Found "
								+ shorterPublicKeyLength),
				arguments(zpSubgroup, primaryCiphertext, shorterPrimaryPublicKey, primaryWitness, secondaryCiphertext, shorterSecondaryPublicKey,
						secondaryWitness, preComputedValues,
						"Ciphertext length must be equal to ElGamal public key length plus 1: " + shorterCiphertextLength + "; Found "
								+ ciphertextLength));
	}

	static Stream<Arguments> verifyPlaintextEqualityProof() throws GeneralCryptoLibException {

		Proof proof = proofsService.createProofProverAPI(zpSubgroup)
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness);

		return Stream.of(arguments(zpSubgroup, null, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof, "Primary ciphertext is null."),
				arguments(zpSubgroup, primaryCiphertext, null, secondaryCiphertext, secondaryPublicKey, proof,
						"Primary ElGamal public key is null" + "."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, null, secondaryPublicKey, proof, "Secondary ciphertext is null."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, secondaryCiphertext, null, proof,
						"Secondary ElGamal public key is null" + "."),
				arguments(zpSubgroup, primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, null,
						"Plaintext equality proof is null."),
				arguments(zpSubgroup, shorterPrimaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof,
						"Primary ciphertext length must be equal to secondary ciphertext length: " + ciphertextLength + "; Found "
								+ shorterCiphertextLength),
				arguments(zpSubgroup, primaryCiphertext, shorterPrimaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof,
						"Primary ElGamal public key length must be equal to secondary ElGamal public key length: " + publicKeyLength + "; Found "
								+ shorterPublicKeyLength),
				arguments(zpSubgroup, primaryCiphertext, shorterPrimaryPublicKey, secondaryCiphertext, shorterSecondaryPublicKey, proof,
						"Ciphertext length must be equal to ElGamal public key length plus 1: " + shorterCiphertextLength + "; Found "
								+ ciphertextLength));
	}

	@Test
	void testProofProverAPICreationGroupValidation() {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> proofsService.createProofProverAPI(null));
		assertTrue(exception.getMessage().contains("Zp subgroup is null."));
	}

	@ParameterizedTest
	@MethodSource("createPlaintextEqualityProof")
	void testPlaintextEqualityProofCreationValidation(ZpSubgroup group, Ciphertext primaryCiphertext, ElGamalPublicKey primaryPublicKey,
			Witness primaryWitness, Ciphertext secondaryCiphertext, ElGamalPublicKey secondaryPublicKey, Witness secondaryWitness, String errorMsg)
			throws GeneralCryptoLibException {
		final ProofProverAPI proofProverAPI = proofsService.createProofProverAPI(group);
		// We explicitly expect an exception from createPlaintextEqualityProof() and not createProofProverAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> proofProverAPI
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness));
		assertTrue(exception.getMessage().contains(errorMsg));
	}

	@Test
	void testProofPreComputerAPICreationGroupValidation() {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofsService.createProofPreComputerAPI(null));
		assertTrue(exception.getMessage().contains("Zp subgroup is null."));
	}

	@ParameterizedTest
	@MethodSource("preComputePlaintextEqualityProofProof")
	void testPlaintextEqualityProofPreComputationValidation(ZpSubgroup group, ElGamalPublicKey primaryPublicKey, ElGamalPublicKey secondaryPublicKey,
			String errorMsg) throws GeneralCryptoLibException {
		final ProofPreComputerAPI proofPreComputerAPI = proofsService.createProofPreComputerAPI(group);
		// We explicitly expect an exception from preComputePlaintextEqualityProof() and not createProofPreComputerAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class,
				() -> proofPreComputerAPI.preComputePlaintextEqualityProof(primaryPublicKey, secondaryPublicKey));
		assertTrue(exception.getMessage().contains(errorMsg));
	}

	@ParameterizedTest
	@MethodSource("createPlaintextEqualityProofUsingPreComputedValues")
	void testPlaintextEqualityProofCreationUsingPreComputedValuesValidation(ZpSubgroup group, Ciphertext primaryCiphertext,
			ElGamalPublicKey primaryPublicKey, Witness primaryWitness, Ciphertext secondaryCiphertext, ElGamalPublicKey secondaryPublicKey,
			Witness secondaryWitness, ProofPreComputedValues preComputedValues, String errorMsg) throws GeneralCryptoLibException {
		final ProofProverAPI proofProverAPI = proofsService.createProofProverAPI(group);
		// We explicitly expect an exception from createPlaintextEqualityProof() and not createProofProverAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> proofProverAPI
				.createPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, primaryWitness, secondaryCiphertext, secondaryPublicKey,
						secondaryWitness, preComputedValues));
		assertTrue(exception.getMessage().contains(errorMsg));
	}

	@Test
	void testProofVerifierAPICreationGroupValidation() {
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> proofsService.createProofVerifierAPI(null));
		assertTrue(exception.getMessage().contains("Zp subgroup is null."));
	}

	@ParameterizedTest
	@MethodSource("verifyPlaintextEqualityProof")
	void testPlaintextEqualityProofVerificationValidation(ZpSubgroup group, Ciphertext primaryCiphertext, ElGamalPublicKey primaryPublicKey,
			Ciphertext secondaryCiphertext, ElGamalPublicKey secondaryPublicKey, Proof proof, String errorMsg) throws GeneralCryptoLibException {
		final ProofVerifierAPI proofVerifierAPI = proofsService.createProofVerifierAPI(group);
		// We explicitly expect an exception from verifyPlaintextEqualityProof() and not createProofVerifierAPI()
		final GeneralCryptoLibException exception = assertThrows(GeneralCryptoLibException.class, () -> proofVerifierAPI
				.verifyPlaintextEqualityProof(primaryCiphertext, primaryPublicKey, secondaryCiphertext, secondaryPublicKey, proof));
		assertTrue(exception.getMessage().contains(errorMsg));
	}
}
