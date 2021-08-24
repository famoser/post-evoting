/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;

/**
 * This class encapsulated a control component's output when generating return codes (both choice return codes and vote cast return codes) in the
 * configuration phase - namely in the algorithm GenEncLongCodeShares.
 */
@JsonPropertyOrder({ "verificationCardId", "voterChoiceReturnCodeGenerationPublicKey", "voterVoteCastReturnCodeGenerationPublicKey",
		"exponentiatedEncryptedPartialChoiceReturnCodes", "encryptedPartialChoiceReturnCodeExponentiationProof",
		"exponentiatedEncryptedConfirmationKey", "encryptedConfirmationKeyExponentiationProof", })
public class ReturnCodeGenerationOutput implements HashableList {

	@JsonProperty
	private final String verificationCardId;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey voterChoiceReturnCodeGenerationPublicKey;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey voterVoteCastReturnCodeGenerationPublicKey;

	/* The squared, hashed partial Choice Return Codes that were
	 * exponentiated to the Voter Choice Return Code Generation Private key and encrypted with the setup public key.
	 */
	@JsonProperty
	private final ElGamalMultiRecipientCiphertext exponentiatedEncryptedPartialChoiceReturnCodes;

	/* Proof that the partial choice return code - hashed, squared, and encrypted with the setup public key - was exponentiated to the
	 * Voter Choice Return Code generation secret key.
	 */
	@JsonProperty
	private final ExponentiationProof encryptedPartialChoiceReturnCodeExponentiationProof;

	/* This is the squared, hashed confirmation key that was exponentiated to the Voter Vote Cast Return Code Generation private key - encrypted
	 * with the setup public key.
	 */
	@JsonProperty
	private final ElGamalMultiRecipientCiphertext exponentiatedEncryptedConfirmationKey;

	/* Proof that the confirmation key - hashed, squared, and encrypted with the setup public key - was exponentiated to the Voter Vote
	 * Cast Return Code generation private key.
	 */
	@JsonProperty
	private final ExponentiationProof encryptedConfirmationKeyExponentiationProof;

	@JsonCreator
	public ReturnCodeGenerationOutput(
			@JsonProperty("verificationCardId")
			final String verificationCardId,
			@JsonProperty("voterChoiceReturnCodeGenerationPublicKey")
			final ElGamalMultiRecipientPublicKey voterChoiceReturnCodeGenerationPublicKey,
			@JsonProperty("voterVoteCastReturnCodeGenerationPublicKey")
			final ElGamalMultiRecipientPublicKey voterVoteCastReturnCodeGenerationPublicKey,
			@JsonProperty("exponentiatedEncryptedPartialChoiceReturnCodes")
			final ElGamalMultiRecipientCiphertext exponentiatedEncryptedPartialChoiceReturnCodes,
			@JsonProperty("encryptedPartialChoiceReturnCodeExponentiationProof")
			final ExponentiationProof encryptedPartialChoiceReturnCodeExponentiationProof,
			@JsonProperty("exponentiatedEncryptedConfirmationKey")
			final ElGamalMultiRecipientCiphertext exponentiatedEncryptedConfirmationKey,
			@JsonProperty("encryptedConfirmationKeyExponentiationProof")
			final ExponentiationProof encryptedConfirmationKeyExponentiationProof) {

		checkNotNull(verificationCardId);
		checkNotNull(voterChoiceReturnCodeGenerationPublicKey);
		checkNotNull(voterVoteCastReturnCodeGenerationPublicKey);
		checkNotNull(exponentiatedEncryptedPartialChoiceReturnCodes);
		checkNotNull(encryptedPartialChoiceReturnCodeExponentiationProof);
		checkNotNull(exponentiatedEncryptedConfirmationKey);
		checkNotNull(encryptedConfirmationKeyExponentiationProof);

		this.verificationCardId = verificationCardId;
		this.voterChoiceReturnCodeGenerationPublicKey = voterChoiceReturnCodeGenerationPublicKey;
		this.voterVoteCastReturnCodeGenerationPublicKey = voterVoteCastReturnCodeGenerationPublicKey;
		this.exponentiatedEncryptedPartialChoiceReturnCodes = exponentiatedEncryptedPartialChoiceReturnCodes;
		this.encryptedPartialChoiceReturnCodeExponentiationProof = encryptedPartialChoiceReturnCodeExponentiationProof;
		this.exponentiatedEncryptedConfirmationKey = exponentiatedEncryptedConfirmationKey;
		this.encryptedConfirmationKeyExponentiationProof = encryptedConfirmationKeyExponentiationProof;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public ElGamalMultiRecipientPublicKey getVoterChoiceReturnCodeGenerationPublicKey() {
		return voterChoiceReturnCodeGenerationPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getVoterVoteCastReturnCodeGenerationPublicKey() {
		return voterVoteCastReturnCodeGenerationPublicKey;
	}

	public ElGamalMultiRecipientCiphertext getExponentiatedEncryptedPartialChoiceReturnCodes() {
		return exponentiatedEncryptedPartialChoiceReturnCodes;
	}

	public ExponentiationProof getEncryptedPartialChoiceReturnCodeExponentiationProof() {
		return encryptedPartialChoiceReturnCodeExponentiationProof;
	}

	public ElGamalMultiRecipientCiphertext getExponentiatedEncryptedConfirmationKey() {
		return exponentiatedEncryptedConfirmationKey;
	}

	public ExponentiationProof getEncryptedConfirmationKeyExponentiationProof() {
		return encryptedConfirmationKeyExponentiationProof;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ReturnCodeGenerationOutput that = (ReturnCodeGenerationOutput) o;
		return verificationCardId.equals(that.verificationCardId) && voterChoiceReturnCodeGenerationPublicKey
				.equals(that.voterChoiceReturnCodeGenerationPublicKey) && voterVoteCastReturnCodeGenerationPublicKey
				.equals(that.voterVoteCastReturnCodeGenerationPublicKey) && exponentiatedEncryptedPartialChoiceReturnCodes
				.equals(that.exponentiatedEncryptedPartialChoiceReturnCodes) && encryptedPartialChoiceReturnCodeExponentiationProof
				.equals(that.encryptedPartialChoiceReturnCodeExponentiationProof) && exponentiatedEncryptedConfirmationKey
				.equals(that.exponentiatedEncryptedConfirmationKey) && encryptedConfirmationKeyExponentiationProof
				.equals(that.encryptedConfirmationKeyExponentiationProof);
	}

	@Override
	public int hashCode() {
		return Objects.hash(verificationCardId, voterChoiceReturnCodeGenerationPublicKey, voterVoteCastReturnCodeGenerationPublicKey,
				exponentiatedEncryptedPartialChoiceReturnCodes, encryptedPartialChoiceReturnCodeExponentiationProof,
				exponentiatedEncryptedConfirmationKey, encryptedConfirmationKeyExponentiationProof);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList
				.of(HashableString.from(verificationCardId), voterChoiceReturnCodeGenerationPublicKey, voterVoteCastReturnCodeGenerationPublicKey,
						exponentiatedEncryptedPartialChoiceReturnCodes, encryptedPartialChoiceReturnCodeExponentiationProof,
						exponentiatedEncryptedConfirmationKey, encryptedConfirmationKeyExponentiationProof);
	}

}
