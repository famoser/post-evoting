/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

/**
 * Represents a mixnet payload. This payload is the input or the output of a mixing / decryption operation.
 */
@JsonDeserialize(using = MixnetPayloadDeserializer.class)
public interface MixnetPayload {

	List<ElGamalMultiRecipientCiphertext> getEncryptedVotes();

	ElGamalMultiRecipientPublicKey getRemainingElectionPublicKey();

	CryptolibPayloadSignature getSignature();

	void setSignature(final CryptolibPayloadSignature signature);
}
