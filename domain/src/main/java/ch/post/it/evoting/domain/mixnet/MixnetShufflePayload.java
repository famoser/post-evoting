/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

/**
 * Encapsulates the output of a mixing / decryption operation. If the ballot box contained only one vote, the verifiableShuffle is null, since
 * shuffling only works with at least two votes.
 */
@JsonPropertyOrder({ "encryptionGroup", "verifiableDecryptions", "verifiableShuffle", "remainingElectionPublicKey",
		"previousRemainingElectionPublicKey", "nodeElectionPublicKey", "nodeId", "signature", "signingPublicKey" })
@JsonDeserialize(as = MixnetShufflePayload.class, using = MixnetShufflePayloadDeserializer.class)
public class MixnetShufflePayload implements MixnetPayload {

	@JsonProperty
	private final GqGroup encryptionGroup;

	@JsonProperty
	private final VerifiableDecryptions verifiableDecryptions;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final VerifiableShuffle verifiableShuffle;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey remainingElectionPublicKey;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey nodeElectionPublicKey;

	@JsonProperty
	private final int nodeId;

	@JsonProperty
	private CryptolibPayloadSignature signature;

	@JsonCreator
	public MixnetShufflePayload(
			@JsonProperty(value = "encryptionGroup", required = true)
			final GqGroup encryptionGroup,
			@JsonProperty(value = "verifiableDecryptions", required = true)
			final VerifiableDecryptions verifiableDecryptions,
			@JsonProperty("verifiableShuffle")
			final VerifiableShuffle verifiableShuffle,
			@JsonProperty(value = "remainingElectionPublicKey", required = true)
			final ElGamalMultiRecipientPublicKey remainingElectionPublicKey,
			@JsonProperty(value = "previousRemainingElectionPublicKey", required = true)
			final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey,
			@JsonProperty(value = "nodeElectionPublicKey", required = true)
			final ElGamalMultiRecipientPublicKey nodeElectionPublicKey,
			@JsonProperty(value = "nodeId", required = true)
			final int nodeId,
			@JsonProperty(value = "signature", required = true)
			final CryptolibPayloadSignature signature) {

		this.encryptionGroup = encryptionGroup;
		this.verifiableDecryptions = verifiableDecryptions;
		this.verifiableShuffle = verifiableShuffle;
		this.remainingElectionPublicKey = remainingElectionPublicKey;
		this.previousRemainingElectionPublicKey = previousRemainingElectionPublicKey;
		this.nodeElectionPublicKey = nodeElectionPublicKey;
		this.nodeId = nodeId;
		this.signature = signature;
	}

	/**
	 * Constructs an unsigned payload.
	 */
	public MixnetShufflePayload(final GqGroup encryptionGroup, final VerifiableDecryptions verifiableDecryptions,
			final VerifiableShuffle verifiableShuffle, final ElGamalMultiRecipientPublicKey remainingElectionPublicKey,
			final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey, final ElGamalMultiRecipientPublicKey nodeElectionPublicKey,
			final int nodeId) {

		checkNotNull(encryptionGroup);
		checkNotNull(verifiableDecryptions);
		checkNotNull(remainingElectionPublicKey);
		checkNotNull(previousRemainingElectionPublicKey);
		checkNotNull(nodeElectionPublicKey);

		this.encryptionGroup = encryptionGroup;
		this.verifiableDecryptions = verifiableDecryptions;
		this.verifiableShuffle = verifiableShuffle;
		this.remainingElectionPublicKey = remainingElectionPublicKey;
		this.previousRemainingElectionPublicKey = previousRemainingElectionPublicKey;
		this.nodeElectionPublicKey = nodeElectionPublicKey;
		this.nodeId = nodeId;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public VerifiableDecryptions getVerifiableDecryptions() {
		return verifiableDecryptions;
	}

	public VerifiableShuffle getVerifiableShuffle() {
		return verifiableShuffle;
	}

	@Override
	@JsonIgnore
	public List<ElGamalMultiRecipientCiphertext> getEncryptedVotes() {
		return verifiableDecryptions.getCiphertexts();
	}

	@Override
	public ElGamalMultiRecipientPublicKey getRemainingElectionPublicKey() {
		return remainingElectionPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getPreviousRemainingElectionPublicKey() {
		return previousRemainingElectionPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getNodeElectionPublicKey() {
		return nodeElectionPublicKey;
	}

	public int getNodeId() {
		return nodeId;
	}

	@Override
	public CryptolibPayloadSignature getSignature() {
		return signature;
	}

	@Override
	public void setSignature(final CryptolibPayloadSignature signature) {
		this.signature = signature;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final MixnetShufflePayload that = (MixnetShufflePayload) o;
		return nodeId == that.nodeId && Objects.equals(encryptionGroup, that.encryptionGroup) &&
				Objects.equals(verifiableDecryptions, that.verifiableDecryptions) && Objects.equals(verifiableShuffle, that.verifiableShuffle) &&
				Objects.equals(remainingElectionPublicKey, that.remainingElectionPublicKey) &&
				Objects.equals(previousRemainingElectionPublicKey, that.previousRemainingElectionPublicKey) &&
				Objects.equals(nodeElectionPublicKey, that.nodeElectionPublicKey) && Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, verifiableDecryptions, verifiableShuffle, remainingElectionPublicKey, previousRemainingElectionPublicKey,
				nodeElectionPublicKey, nodeId, signature);
	}
}
