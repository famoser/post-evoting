/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.primitives.primes.utils.PrimitivesTestDataGenerator;
import ch.post.it.evoting.cryptolib.primitives.securerandom.constants.SecureRandomConstants;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.test.tools.utils.CommonTestDataGenerator;

class WrongProofVerificationInputTest {

	private static final int NUM_PLAINTEXT_ELEMENTS = 6;
	private static final int NUM_BASE_ELEMENTS = 5;

	private static ProofVerifierAPI verifier;
	private static String electionEventId;
	private static String voterId;
	private static ElGamalPublicKey publicKey;
	private static ElGamalPublicKey secondaryPublicKey;
	private static ElGamalPublicKey otherPublicKey;
	private static List<ZpGroupElement> plaintext;
	private static List<ZpGroupElement> otherPlaintext;
	private static Ciphertext ciphertext;
	private static Ciphertext secondaryCiphertext;
	private static Ciphertext otherCiphertext;
	private static ZpGroupElement otherExponentiatedGenerator;
	private static ZpGroupElement exponentiatedGenerator;
	private static List<ZpGroupElement> phis;
	private static List<ZpGroupElement> exponentiatedElements;
	private static List<ZpGroupElement> otherExponentiatedElements;
	private static List<ZpGroupElement> baseElements;
	private static List<ZpGroupElement> otherBaseElements;
	private static Proof exponentiationProof;
	private static Proof plaintextEqualityProof;

	@BeforeAll
	static void setUp() throws Exception {

		ProofsService proofsService = new ProofsService();

		ZpSubgroup zpSubgroup = MathematicalTestDataGenerator.getQrSubgroup();
		ZpSubgroup otherZpSubgroup = MathematicalTestDataGenerator.getOtherQrSubgroup();

		ProofProverAPI prover = proofsService.createProofProverAPI(zpSubgroup);
		verifier = proofsService.createProofVerifierAPI(zpSubgroup);

		int numChars = CommonTestDataGenerator.getInt(1, SecureRandomConstants.MAXIMUM_GENERATED_STRING_LENGTH);
		voterId = PrimitivesTestDataGenerator.getString64(numChars);
		electionEventId = PrimitivesTestDataGenerator.getString64(numChars);

		ElGamalKeyPair keyPair = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		ElGamalPrivateKey privateKey = keyPair.getPrivateKeys();
		publicKey = keyPair.getPublicKeys();

		ElGamalKeyPair secondaryKeyPair = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		secondaryPublicKey = secondaryKeyPair.getPublicKeys();

		ElGamalKeyPair otherKeyPair = ElGamalTestDataGenerator.getKeyPair(otherZpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		otherPublicKey = otherKeyPair.getPublicKeys();

		plaintext = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		otherPlaintext = MathematicalTestDataGenerator.getZpGroupElements(otherZpSubgroup, NUM_PLAINTEXT_ELEMENTS);

		ElGamalEncrypterValues encrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator.encryptGroupElements(publicKey, plaintext);
		ElGamalEncrypterValues secondaryEncrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator
				.encryptGroupElements(secondaryPublicKey, plaintext);
		ciphertext = encrypterValues;
		secondaryCiphertext = secondaryEncrypterValues;

		otherCiphertext = ElGamalTestDataGenerator.encryptGroupElements(otherPublicKey, otherPlaintext);
		Witness otherWitness = ElGamalTestDataGenerator.getWitness(otherZpSubgroup);

		ZpGroupElement generatorElement = new ZpGroupElement(zpSubgroup.getG(), zpSubgroup);
		exponentiatedGenerator = generatorElement.exponentiate(((Witness) encrypterValues).getExponent());
		ZpGroupElement otherGeneratorElement = new ZpGroupElement(zpSubgroup.getG(), otherZpSubgroup);
		otherExponentiatedGenerator = otherGeneratorElement.exponentiate(otherWitness.getExponent());

		phis = Collections.singletonList(new ZpGroupElement(ONE, zpSubgroup));

		baseElements = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_BASE_ELEMENTS);
		otherBaseElements = MathematicalTestDataGenerator.getZpGroupElements(otherZpSubgroup, NUM_BASE_ELEMENTS);

		exponentiatedElements = MathematicalTestDataGenerator.exponentiateZpGroupElements(baseElements, ((Witness) encrypterValues).getExponent());
		otherExponentiatedElements = MathematicalTestDataGenerator.exponentiateZpGroupElements(otherBaseElements, otherWitness.getExponent());

		exponentiationProof = prover.createExponentiationProof(exponentiatedElements, baseElements, encrypterValues);
		plaintextEqualityProof = prover.createPlaintextEqualityProof(ciphertext, publicKey, encrypterValues, secondaryCiphertext, secondaryPublicKey,
				secondaryEncrypterValues);
	}

	/**
	 * Breaks a proof by changing the group order of the last exponent.
	 *
	 * @param proof the proof to break
	 * @return a broken proof
	 */
	private static Proof getBadProof(Proof proof) throws GeneralCryptoLibException, NoSuchFieldException, IllegalAccessException {
		// Duplicate the 'good' proof.
		Proof badProof = new Proof(proof.getHashValue(), proof.getValuesList());
		// Create a tampered exponents list.
		List<Exponent> otherExponents = proof.getValuesList();
		Exponent lastExponent = otherExponents.remove(otherExponents.size() - 1);
		otherExponents.add(new Exponent(ONE, lastExponent.getValue()));
		// Inject the exponent in the proof.
		Field exponentsField = proof.getClass().getDeclaredField("values");
		exponentsField.setAccessible(true);
		exponentsField.set(badProof, otherExponents);

		return badProof;
	}

	@Test
	final void exponentiationProofOk() throws GeneralCryptoLibException {
		assertTrue(verifier.verifyExponentiationProof(exponentiatedElements, baseElements, exponentiationProof));
	}

	@Test
	final void exponentiationProofWithBadExponentiatedElements() {
		assertThrows(IllegalArgumentException.class,
				() -> verifier.verifyExponentiationProof(otherExponentiatedElements, baseElements, exponentiationProof));
	}

	@Test
	final void exponentiationProofWithBadBaseElements() {
		assertThrows(IllegalArgumentException.class,
				() -> verifier.verifyExponentiationProof(exponentiatedElements, otherBaseElements, exponentiationProof));
	}

	@Test
	final void exponentiationProofWithBadProof() {
		assertThrows(IllegalArgumentException.class,
				() -> verifier.verifyExponentiationProof(exponentiatedElements, baseElements, getBadProof(exponentiationProof)));
	}

	@Test
	final void plaintextEqualityProofOk() throws GeneralCryptoLibException {
		assertTrue(verifier.verifyPlaintextEqualityProof(ciphertext, publicKey, secondaryCiphertext, secondaryPublicKey, plaintextEqualityProof));
	}

	@Test
	final void plaintextEqualityProofWithBadPrimaryCiphertext() {
		assertThrows(IllegalArgumentException.class, () -> verifier
				.verifyPlaintextEqualityProof(otherCiphertext, publicKey, secondaryCiphertext, secondaryPublicKey, plaintextEqualityProof));
	}

	@Test
	final void plaintextEqualityProofWithBadPrimaryPublicKey() {
		assertThrows(IllegalArgumentException.class, () -> verifier
				.verifyPlaintextEqualityProof(ciphertext, otherPublicKey, secondaryCiphertext, secondaryPublicKey, plaintextEqualityProof));
	}

	@Test
	final void plaintextEqualityProofWithBadSecondaryCiphertext() {
		assertThrows(IllegalArgumentException.class,
				() -> verifier.verifyPlaintextEqualityProof(ciphertext, publicKey, otherCiphertext, secondaryPublicKey, plaintextEqualityProof));
	}

	@Test
	final void plaintextEqualityProofWithBadSecondaryPublicKey() {
		assertThrows(IllegalArgumentException.class,
				() -> verifier.verifyPlaintextEqualityProof(ciphertext, publicKey, secondaryCiphertext, otherPublicKey, plaintextEqualityProof));
	}

	@Test
	final void plaintextEqualityProofWithBadProof() {
		assertThrows(IllegalArgumentException.class, () -> verifier
				.verifyPlaintextEqualityProof(ciphertext, publicKey, secondaryCiphertext, secondaryPublicKey, getBadProof(plaintextEqualityProof)));
	}

}
