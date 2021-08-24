/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

/**
 * Value class representing the final result of a mixnet.
 */
@JsonPropertyOrder({ "encryptionGroup", "verifiableShuffle", "verifiablePlaintextDecryption", "previousRemainingElectionPublicKey" })
@JsonDeserialize(using = MixnetFinalPayloadDeserializer.class)
public class MixnetFinalPayload {

	@JsonProperty
	private final GqGroup encryptionGroup;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	private final VerifiableShuffle verifiableShuffle;

	@JsonProperty
	private final VerifiablePlaintextDecryption verifiablePlaintextDecryption;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey;

	@JsonProperty
	private CryptolibPayloadSignature signature;

	@JsonCreator
	public MixnetFinalPayload(
			@JsonProperty(value = "encryptionGroup", required = true)
			final GqGroup encryptionGroup,
			@JsonProperty("verifiableShuffle")
			final VerifiableShuffle verifiableShuffle,
			@JsonProperty(value = "verifiablePlaintextDecryption", required = true)
			final VerifiablePlaintextDecryption verifiablePlaintextDecryption,
			@JsonProperty(value = "previousRemainingElectionPublicKey", required = true)
			final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey,
			@JsonProperty(value = "signature", required = true)
			final CryptolibPayloadSignature signature) {

		checkNotNull(encryptionGroup);
		checkNotNull(verifiablePlaintextDecryption);
		checkNotNull(previousRemainingElectionPublicKey);

		this.encryptionGroup = encryptionGroup;
		this.verifiableShuffle = verifiableShuffle;
		this.verifiablePlaintextDecryption = verifiablePlaintextDecryption;
		this.previousRemainingElectionPublicKey = previousRemainingElectionPublicKey;
		this.signature = signature;
	}

	/**
	 * Constructs an unsigned payload.
	 */
	public MixnetFinalPayload(final GqGroup encryptionGroup, final VerifiableShuffle verifiableShuffle,
			final VerifiablePlaintextDecryption verifiablePlaintextDecryption,
			final ElGamalMultiRecipientPublicKey previousRemainingElectionPublicKey) {

		checkNotNull(encryptionGroup);
		checkNotNull(verifiablePlaintextDecryption);
		checkNotNull(previousRemainingElectionPublicKey);

		this.encryptionGroup = encryptionGroup;
		this.verifiableShuffle = verifiableShuffle;
		this.verifiablePlaintextDecryption = verifiablePlaintextDecryption;
		this.previousRemainingElectionPublicKey = previousRemainingElectionPublicKey;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	@JsonIgnore
	public Optional<VerifiableShuffle> getVerifiableShuffle() {
		return Optional.ofNullable(verifiableShuffle);
	}

	public VerifiablePlaintextDecryption getVerifiablePlaintextDecryption() {
		return verifiablePlaintextDecryption;
	}

	public ElGamalMultiRecipientPublicKey getPreviousRemainingElectionPublicKey() {
		return previousRemainingElectionPublicKey;
	}

	public CryptolibPayloadSignature getSignature() {
		return signature;
	}

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
		final MixnetFinalPayload that = (MixnetFinalPayload) o;
		return encryptionGroup.equals(that.encryptionGroup) && Objects.equals(verifiableShuffle, that.verifiableShuffle)
				&& verifiablePlaintextDecryption.equals(that.verifiablePlaintextDecryption) && previousRemainingElectionPublicKey
				.equals(that.previousRemainingElectionPublicKey) && signature.equals(that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, verifiableShuffle, verifiablePlaintextDecryption, previousRemainingElectionPublicKey, signature);
	}
}
