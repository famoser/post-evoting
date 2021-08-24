/*
 *  (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.sdm.application.service;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffleGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProofGenerator;
import ch.post.it.evoting.domain.mixnet.VerifiablePlaintextDecryption;

public class MixDecryptResultGenerator {

	private final GqGroup group;

	MixDecryptResultGenerator(GqGroup group) {
		this.group = group;
	}

	MixDecryptService.Result genMixDecryptResult(int numCiphertexts, int ciphertextSize) {
		VerifiableShuffle shuffle = new VerifiableShuffleGenerator(group).genVerifiableShuffle(numCiphertexts, ciphertextSize);
		VerifiablePlaintextDecryption plaintextDecryption = genVerifiablePlaintextDecryption(numCiphertexts, ciphertextSize);
		return new MixDecryptService.Result(shuffle, plaintextDecryption.getDecryptedVotes(), plaintextDecryption.getDecryptionProofs());
	}

	VerifiablePlaintextDecryption genVerifiablePlaintextDecryption(int numMessages, int messageSize) {
		GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes = new ElGamalGenerator(group)
				.genRandomMessageVector(numMessages, messageSize);
		final GroupVector<DecryptionProof, ZqGroup> decryptionProofs = new DecryptionProofGenerator(ZqGroup.sameOrderAs(group))
				.genDecryptionProofVector(numMessages, messageSize);
		return new VerifiablePlaintextDecryption(decryptedVotes, decryptionProofs);
	}

}
