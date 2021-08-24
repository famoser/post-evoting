/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.service;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.primitives.primes.utils.PrimitivesTestDataGenerator;
import ch.post.it.evoting.cryptolib.primitives.securerandom.constants.SecureRandomConstants;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofProverAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.test.tools.utils.CommonTestDataGenerator;

class WrongProofCreationInputTest {

	private static final int NUM_PLAINTEXT_ELEMENTS = 6;
	private static final int NUM_BASE_ELEMENTS = 5;

	private static ProofProverAPI prover;
	private static String electionEventId;
	private static String voterId;
	private static ElGamalPublicKey publicKey;
	private static ElGamalPublicKey secondaryPublicKey;
	private static ElGamalPublicKey otherPublicKey;
	private static ElGamalPrivateKey privateKey;
	private static ElGamalPrivateKey otherPrivateKey;
	private static List<ZpGroupElement> plaintext;
	private static List<ZpGroupElement> otherPlaintext;
	private static Ciphertext ciphertext;
	private static Ciphertext secondaryCiphertext;
	private static Ciphertext otherCiphertext;
	private static Witness witness;
	private static Witness secondaryWitness;
	private static Witness otherWitness;
	private static ZpGroupElement otherExponentiatedGenerator;
	private static ZpGroupElement exponentiatedGenerator;
	private static List<ZpGroupElement> phis;
	private static List<ZpGroupElement> otherPhis;
	private static List<ZpGroupElement> exponentiatedElements;
	private static List<ZpGroupElement> otherExponentiatedElements;
	private static List<ZpGroupElement> baseElements;
	private static List<ZpGroupElement> otherBaseElements;

	@BeforeAll
	static void setUp() throws Exception {

		ProofsService proofsService = new ProofsService();

		ZpSubgroup zpSubgroup = MathematicalTestDataGenerator.getQrSubgroup();
		ZpSubgroup otherZpSubgroup = MathematicalTestDataGenerator.getOtherQrSubgroup();

		prover = proofsService.createProofProverAPI(zpSubgroup);

		int numChars = CommonTestDataGenerator.getInt(1, SecureRandomConstants.MAXIMUM_GENERATED_STRING_LENGTH);
		voterId = PrimitivesTestDataGenerator.getString64(numChars);
		electionEventId = PrimitivesTestDataGenerator.getString64(numChars);

		ElGamalKeyPair keyPair = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		privateKey = keyPair.getPrivateKeys();
		publicKey = keyPair.getPublicKeys();

		ElGamalKeyPair secondaryKeyPair = ElGamalTestDataGenerator.getKeyPair(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		secondaryPublicKey = secondaryKeyPair.getPublicKeys();

		ElGamalKeyPair otherKeyPair = ElGamalTestDataGenerator.getKeyPair(otherZpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		otherPrivateKey = otherKeyPair.getPrivateKeys();
		otherPublicKey = otherKeyPair.getPublicKeys();

		plaintext = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_PLAINTEXT_ELEMENTS);
		otherPlaintext = MathematicalTestDataGenerator.getZpGroupElements(otherZpSubgroup, NUM_PLAINTEXT_ELEMENTS);

		ciphertext = ElGamalTestDataGenerator.encryptGroupElements(publicKey, plaintext);
		secondaryCiphertext = ElGamalTestDataGenerator.encryptGroupElements(secondaryPublicKey, plaintext);
		otherCiphertext = ElGamalTestDataGenerator.encryptGroupElements(otherPublicKey, otherPlaintext);

		witness = ElGamalTestDataGenerator.getWitness(zpSubgroup);
		secondaryWitness = ElGamalTestDataGenerator.getWitness(zpSubgroup);
		otherWitness = ElGamalTestDataGenerator.getWitness(otherZpSubgroup);

		ZpGroupElement generatorElement = new ZpGroupElement(zpSubgroup.getG(), zpSubgroup);
		exponentiatedGenerator = generatorElement.exponentiate(witness.getExponent());
		ZpGroupElement otherGeneratorElement = new ZpGroupElement(zpSubgroup.getG(), otherZpSubgroup);
		otherExponentiatedGenerator = otherGeneratorElement.exponentiate(otherWitness.getExponent());

		phis = Collections.singletonList(new ZpGroupElement(BigInteger.ONE, zpSubgroup));
		otherPhis = Collections.singletonList(new ZpGroupElement(BigInteger.ONE, zpSubgroup));

		baseElements = MathematicalTestDataGenerator.getZpGroupElements(zpSubgroup, NUM_BASE_ELEMENTS);
		otherBaseElements = MathematicalTestDataGenerator.getZpGroupElements(otherZpSubgroup, NUM_BASE_ELEMENTS);

		exponentiatedElements = MathematicalTestDataGenerator.exponentiateZpGroupElements(baseElements, witness.getExponent());
		otherExponentiatedElements = MathematicalTestDataGenerator.exponentiateZpGroupElements(otherBaseElements, otherWitness.getExponent());
	}

	@Test
	final void exponentiationProofOk() throws GeneralCryptoLibException {
		MatcherAssert.assertThat(prover.createExponentiationProof(exponentiatedElements, baseElements, witness), instanceOf(Proof.class));
	}

	@Test
	final void exponentiationProofWithBadExponentiatedElements() {
		assertThrows(IllegalArgumentException.class, () -> prover.createExponentiationProof(otherExponentiatedElements, baseElements, witness));
	}

	@Test
	final void exponentiationProofWithBadBaseElements() {
		assertThrows(GeneralCryptoLibException.class, () -> prover.createExponentiationProof(exponentiatedElements, otherBaseElements, witness));
	}

	@Test
	final void exponentiationProofWithBadWitness() {
		assertThrows(IllegalArgumentException.class, () -> prover.createExponentiationProof(exponentiatedElements, baseElements, otherWitness));
	}

	@Test
	final void plaintextEqualityProofOk() throws GeneralCryptoLibException {
		MatcherAssert.assertThat(
				prover.createPlaintextEqualityProof(ciphertext, publicKey, witness, secondaryCiphertext, secondaryPublicKey, secondaryWitness),
				instanceOf(Proof.class));
	}

	@Test
	final void plaintextEqualityProofWithBadPrimaryCiphertext() {
		assertThrows(IllegalArgumentException.class,
				() -> prover.createPlaintextEqualityProof(otherCiphertext, publicKey, witness, ciphertext, publicKey, witness));
	}

	@Test
	final void plaintextEqualityProofWithBadPrimaryPublicKey() {
		assertThrows(GeneralCryptoLibException.class,
				() -> prover.createPlaintextEqualityProof(ciphertext, otherPublicKey, witness, ciphertext, publicKey, witness));
	}

	@Test
	final void plaintextEqualityProofWithBadPrimaryWitness() {
		assertThrows(IllegalArgumentException.class,
				() -> prover.createPlaintextEqualityProof(ciphertext, publicKey, otherWitness, ciphertext, publicKey, witness));
	}

	@Test
	final void plaintextEqualityProofWithBadSecondaryCiphertext() {
		assertThrows(IllegalArgumentException.class,
				() -> prover.createPlaintextEqualityProof(ciphertext, publicKey, witness, otherCiphertext, publicKey, witness));
	}

	@Test
	final void plaintextEqualityProofWithBadSecondaryPublicKey() {
		assertThrows(GeneralCryptoLibException.class,
				() -> prover.createPlaintextEqualityProof(ciphertext, publicKey, witness, ciphertext, otherPublicKey, witness));
	}

	@Test
	final void plaintextEqualityProofWithBadSecondaryWitness() {
		assertThrows(IllegalArgumentException.class,
				() -> prover.createPlaintextEqualityProof(ciphertext, publicKey, witness, ciphertext, publicKey, otherWitness));
	}
}
