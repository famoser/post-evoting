/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.hashing.TestHashService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomService;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetService;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofService;
import ch.post.it.evoting.domain.mixnet.exceptions.FailedValidationException;

class MixDecryptServiceTest extends TestGroupSetup {

	private static String ee;
	private static String bb;
	private static MixDecryptService mixingDecryptionService;
	private static ElGamalGenerator elGamalGenerator;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts;
	private static ElGamalMultiRecipientPublicKey remainingElectionPublicKey;
	private static ElGamalMultiRecipientKeyPair electoralBoardKeyPair;
	private static int numCiphertexts;
	private static int ciphertextsSize;
	private static int keySize;
	private static MixnetService mixnet;
	private static ZeroKnowledgeProofService zeroKnowledgeProof;

	@BeforeAll
	static void setUpAll() {
		elGamalGenerator = new ElGamalGenerator(gqGroup);
		keySize = secureRandom.nextInt(10) + 3;
		numCiphertexts = secureRandom.nextInt(10) + 1;
		ciphertextsSize = 1;
		ciphertexts = elGamalGenerator.genRandomCiphertextVector(numCiphertexts, ciphertextsSize);
		ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, keySize, new RandomService());
		remainingElectionPublicKey = keyPair.getPublicKey();
		electoralBoardKeyPair = keyPair;
		ee = "0d31a1148f95488fae6827391425dc08";
		bb = "f8ba3dd3844a4815af39c63570c12006";

		HashService hashService = TestHashService.create(gqGroup.getQ());
		mixnet = new MixnetService(hashService);
		zeroKnowledgeProof = new ZeroKnowledgeProofService(new RandomService(), hashService);
		mixingDecryptionService = new MixDecryptService(mixnet, zeroKnowledgeProof);
	}

	@Test
	void testThatYouCantCreateAMixDecryptServiceWithNulls() {
		assertAll(() -> assertThrows(NullPointerException.class, () -> new MixDecryptService(null, zeroKnowledgeProof)),
				() -> assertThrows(NullPointerException.class, () -> new MixDecryptService(mixnet, null)));
	}

	@Test
	void testThatYouCantMixDecryptWithNulls() {
		assertAll(() -> assertThrows(NullPointerException.class,
				() -> mixingDecryptionService.mixDecryptOffline(null, remainingElectionPublicKey, electoralBoardKeyPair, ee, bb)),
				() -> assertThrows(NullPointerException.class,
						() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, null, electoralBoardKeyPair, ee, bb)),
				() -> assertThrows(NullPointerException.class,
						() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, remainingElectionPublicKey, null, ee, bb)),
				() -> assertThrows(NullPointerException.class,
						() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, remainingElectionPublicKey, electoralBoardKeyPair, null, bb)),
				() -> assertThrows(NullPointerException.class,
						() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, remainingElectionPublicKey, electoralBoardKeyPair, ee, null)));
	}

	@Test
	void testThatDifferentGroupsThrow() {
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> otherCiphertexts = new ElGamalGenerator(otherGqGroup)
				.genRandomCiphertextVector(numCiphertexts, ciphertextsSize);
		assertThrows(IllegalArgumentException.class,
				() -> mixingDecryptionService.mixDecryptOffline(otherCiphertexts, remainingElectionPublicKey, electoralBoardKeyPair, ee, bb));

		ElGamalMultiRecipientKeyPair otherKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(otherGqGroup, keySize, new RandomService());
		ElGamalMultiRecipientPublicKey otherElectionPk = otherKeyPair.getPublicKey();
		assertThrows(IllegalArgumentException.class,
				() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, otherElectionPk, electoralBoardKeyPair, ee, bb));

		assertThrows(IllegalArgumentException.class,
				() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, remainingElectionPublicKey, otherKeyPair, ee, bb));
	}

	@Test
	void testThatDifferentSizesKeysThrows() {
		ElGamalMultiRecipientPublicKey otherElectionPk = elGamalGenerator.genRandomPublicKey(keySize + 1);
		assertThrows(IllegalArgumentException.class,
				() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, otherElectionPk, electoralBoardKeyPair, ee, bb));
	}

	@Test
	void testThatEmptyCiphertextsThrows() {
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> empty = elGamalGenerator.genRandomCiphertextVector(0, ciphertextsSize);
		assertThrows(IllegalArgumentException.class,
				() -> mixingDecryptionService.mixDecryptOffline(empty, remainingElectionPublicKey, electoralBoardKeyPair, ee, bb));
	}

	@Test
	void testThatElectionPublicKeyAndElectoralBoardPublicKeyAreEqual() {
		ElGamalMultiRecipientPublicKey otherElectionPk = elGamalGenerator.genRandomPublicKey(keySize);
		assertThrows(IllegalArgumentException.class,
				() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, otherElectionPk, electoralBoardKeyPair, ee, bb));
	}

	@Test
	void testThatResultContainsNoVerifiableShuffleIfOnlyOneCiphertext() {
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> oneCiphertext = elGamalGenerator.genRandomCiphertextVector(1, ciphertextsSize);
		MixDecryptService.Result result = mixingDecryptionService
				.mixDecryptOffline(oneCiphertext, remainingElectionPublicKey, electoralBoardKeyPair, ee, bb);
		assertFalse(result.getVerifiableShuffle().isPresent());
		assertNotNull(result.getVerifiablePlaintextDecryption());
		assertNotNull(result.getVerifiablePlaintextDecryption());
	}

	@Test
	void testThatResultContainsVerifiableShuffleForMoreThanOneCiphertext() {
		int numCiphertexts = secureRandom.nextInt(gqGroup.getQ().intValueExact() - 4) + 2;
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> moreThanOneCiphertexts = elGamalGenerator
				.genRandomCiphertextVector(numCiphertexts, ciphertextsSize);
		MixDecryptService.Result result = mixingDecryptionService
				.mixDecryptOffline(moreThanOneCiphertexts, remainingElectionPublicKey, electoralBoardKeyPair, ee, bb);
		assertTrue(result.getVerifiableShuffle().isPresent());
		assertNotNull(result.getVerifiablePlaintextDecryption());
		assertNotNull(result.getVerifiablePlaintextDecryption());
	}

	@Test
	void testThatInvalidUUIDThrows() {
		String invalidUUID = "0d31a1148f95488fae6827391425dc0X";
		assertThrows(FailedValidationException.class,
				() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, remainingElectionPublicKey, electoralBoardKeyPair, invalidUUID, bb));
		assertThrows(FailedValidationException.class,
				() -> mixingDecryptionService.mixDecryptOffline(ciphertexts, remainingElectionPublicKey, electoralBoardKeyPair, ee, invalidUUID));
	}
}
