/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

/**
 * The payload sent to the first mixing control component.
 */
@JsonPropertyOrder({ "encryptionGroup", "ciphertexts", "electionPublicKey", "signature", "signingPublicKey" })
@JsonDeserialize(as = MixnetInitialPayload.class, using = MixnetInitialPayloadDeserializer.class)
public class MixnetInitialPayload implements MixnetPayload {

	@JsonProperty
	private final GqGroup encryptionGroup;

	@JsonProperty("ciphertexts")
	private final List<ElGamalMultiRecipientCiphertext> encryptedVotes;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey electionPublicKey;

	@JsonProperty
	private CryptolibPayloadSignature signature;

	@JsonCreator
	public MixnetInitialPayload(
			@JsonProperty(value = "encryptionGroup", required = true)
			final GqGroup encryptionGroup,
			@JsonProperty(value = "ciphertexts", required = true)
			final List<ElGamalMultiRecipientCiphertext> encryptedVotes,
			@JsonProperty(value = "electionPublicKey", required = true)
			final ElGamalMultiRecipientPublicKey electionPublicKey,
			@JsonProperty(value = "signature", required = true)
			final CryptolibPayloadSignature signature) {

		this.encryptionGroup = encryptionGroup;
		this.encryptedVotes = encryptedVotes;
		this.electionPublicKey = electionPublicKey;
		this.signature = signature;
	}

	/**
	 * Constructs an unsigned payload.
	 */
	public MixnetInitialPayload(final GqGroup encryptionGroup, final List<ElGamalMultiRecipientCiphertext> encryptedVotes,
			final ElGamalMultiRecipientPublicKey electionPublicKey) {

		checkNotNull(encryptionGroup);
		checkNotNull(encryptedVotes);
		checkNotNull(electionPublicKey);

		this.encryptionGroup = encryptionGroup;
		this.encryptedVotes = encryptedVotes;
		this.electionPublicKey = electionPublicKey;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	@Override
	public List<ElGamalMultiRecipientCiphertext> getEncryptedVotes() {
		return encryptedVotes;
	}

	@Override
	@JsonIgnore
	public ElGamalMultiRecipientPublicKey getRemainingElectionPublicKey() {
		return getElectionPublicKey();
	}

	public ElGamalMultiRecipientPublicKey getElectionPublicKey() {
		return electionPublicKey;
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
		final MixnetInitialPayload that = (MixnetInitialPayload) o;
		return Objects.equals(encryptionGroup, that.encryptionGroup) && Objects.equals(encryptedVotes, that.encryptedVotes) && Objects
				.equals(electionPublicKey, that.electionPublicKey) && Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, encryptedVotes, electionPublicKey, signature);
	}

}
