/*
 *  (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.domain.mixnet.generators;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Random;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffleGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptionGenerator;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.mixnet.MixnetShufflePayload;
import ch.post.it.evoting.domain.mixnet.SerializationUtils;

public class MixnetShufflePayloadGenerator {

	private static final Random secureRandom = new SecureRandom();
	private final GqGroup group;

	public MixnetShufflePayloadGenerator(GqGroup group) {
		this.group = group;
	}

	public MixnetShufflePayload genPayload(int numVotes, int voteSize, int nodeId) {

		final VerifiableShuffle verifiableShuffle =
				numVotes <= 1 ? null : new VerifiableShuffleGenerator(group).genVerifiableShuffle(numVotes, voteSize);

		ElGamalGenerator generator = new ElGamalGenerator(group);
		final ElGamalMultiRecipientPublicKey remainingElectionPublicKey = generator.genRandomPublicKey(voteSize);
		final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey = generator.genRandomPublicKey(voteSize);
		final ElGamalMultiRecipientPublicKey nodeElectionPublicKey = generator.genRandomPublicKey(voteSize);

		// Generate random bytes for signature content and create payload signature.
		final byte[] randomBytes = new byte[10];
		secureRandom.nextBytes(randomBytes);
		final X509Certificate certificate = SerializationUtils.generateTestCertificate();
		final CryptolibPayloadSignature signature = new CryptolibPayloadSignature(randomBytes, new X509Certificate[] { certificate });

		// VerifiableDecryptions.
		final VerifiableDecryptions verifiableDecryptions = new VerifiableDecryptionGenerator(group).genVerifiableDecryption(numVotes, voteSize);

		return new MixnetShufflePayload(group, verifiableDecryptions, verifiableShuffle, remainingElectionPublicKey,
				previousRemainingElectionPublicKey, nodeElectionPublicKey, nodeId, signature);
	}
}
