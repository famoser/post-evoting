/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.election.CombinedCorrectnessInformation;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;

@JsonPropertyOrder({ "tenantId", "electionEventId", "verificationCardSetId", "chunkId", "encryptionGroup", "returnCodeGenerationInputs",
		"combinedCorrectnessInformation", "signature" })
@JsonDeserialize(using = ReturnCodeGenerationRequestPayloadDeserializer.class)
public class ReturnCodeGenerationRequestPayload implements HashableList {

	@JsonProperty
	private final String tenantId;

	@JsonProperty
	private final String electionEventId;

	@JsonProperty
	private final String verificationCardSetId;

	@JsonProperty
	private final int chunkId;

	@JsonProperty
	private final GqGroup encryptionGroup;

	@JsonProperty
	private final List<ReturnCodeGenerationInput> returnCodeGenerationInputs;

	@JsonProperty
	private final CombinedCorrectnessInformation combinedCorrectnessInformation;

	@JsonProperty
	private CryptolibPayloadSignature signature;

	@JsonCreator
	public ReturnCodeGenerationRequestPayload(
			@JsonProperty("tenantId")
			final String tenantId,
			@JsonProperty("electionEventId")
			final String electionEventId,
			@JsonProperty("verificationCardSetId")
			final String verificationCardSetId,
			@JsonProperty("chunkId")
			final int chunkId,
			@JsonProperty("encryptionGroup")
			final GqGroup encryptionGroup,
			@JsonProperty("returnCodeGenerationInputs")
			final List<ReturnCodeGenerationInput> returnCodeGenerationInputs,
			@JsonProperty("combinedCorrectnessInformation")
			final CombinedCorrectnessInformation combinedCorrectnessInformation,
			@JsonProperty("signature")
			final CryptolibPayloadSignature signature) {

		this.tenantId = checkNotNull(tenantId);
		this.electionEventId = checkNotNull(electionEventId);
		this.verificationCardSetId = checkNotNull(verificationCardSetId);
		this.chunkId = chunkId;
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.returnCodeGenerationInputs = checkNotNull(returnCodeGenerationInputs);
		this.combinedCorrectnessInformation = checkNotNull(combinedCorrectnessInformation);
		this.signature = checkNotNull(signature);
	}

	/**
	 * Creates an unsigned payload.
	 */
	public ReturnCodeGenerationRequestPayload(final String tenantId, final String electionEventId, final String verificationCardSetId,
			final int chunkId, final GqGroup encryptionGroup, final List<ReturnCodeGenerationInput> returnCodeGenerationInputs,
			final CombinedCorrectnessInformation combinedCorrectnessInformation) {

		this.tenantId = checkNotNull(tenantId);
		this.electionEventId = checkNotNull(electionEventId);
		this.verificationCardSetId = checkNotNull(verificationCardSetId);
		this.chunkId = chunkId;
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.returnCodeGenerationInputs = checkNotNull(returnCodeGenerationInputs);
		this.combinedCorrectnessInformation = checkNotNull(combinedCorrectnessInformation);
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public int getChunkId() {
		return chunkId;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public List<ReturnCodeGenerationInput> getReturnCodeGenerationInputs() {
		return returnCodeGenerationInputs;
	}

	public CombinedCorrectnessInformation getCombinedCorrectnessInformation() {
		return combinedCorrectnessInformation;
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
		final ReturnCodeGenerationRequestPayload that = (ReturnCodeGenerationRequestPayload) o;
		return chunkId == that.chunkId && tenantId.equals(that.tenantId) && electionEventId.equals(that.electionEventId) && verificationCardSetId
				.equals(that.verificationCardSetId) && encryptionGroup.equals(that.encryptionGroup) && returnCodeGenerationInputs
				.equals(that.returnCodeGenerationInputs) && combinedCorrectnessInformation.equals(that.combinedCorrectnessInformation) && Objects
				.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tenantId, electionEventId, verificationCardSetId, chunkId, encryptionGroup, returnCodeGenerationInputs,
				combinedCorrectnessInformation, signature);
	}

	@Override
	public ImmutableList<? extends Hashable> toHashableForm() {
		return ImmutableList.of(HashableString.from(tenantId), HashableString.from(electionEventId), HashableString.from(verificationCardSetId),
				HashableBigInteger.from(BigInteger.valueOf(chunkId)), encryptionGroup, HashableList.from(returnCodeGenerationInputs),
				combinedCorrectnessInformation);
	}
}
