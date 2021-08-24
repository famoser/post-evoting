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

@JsonPropertyOrder({ "verificationCardId", "encryptedHashedSquaredConfirmationKey", "encryptedHashedSquaredPartialChoiceReturnCodes" })
public class ReturnCodeGenerationInput implements HashableList {

	@JsonProperty
	private final String verificationCardId;

	@JsonProperty
	private final ElGamalMultiRecipientCiphertext encryptedHashedSquaredConfirmationKey;

	@JsonProperty
	private final ElGamalMultiRecipientCiphertext encryptedHashedSquaredPartialChoiceReturnCodes;

	@JsonProperty
	private final ElGamalMultiRecipientPublicKey verificationCardPublicKey;

	/**
	 * Creates an object used as the input for return code (choice return codes and vote cast return codes) generation requests.
	 *
	 * @param verificationCardId                             the verification card identifier.
	 * @param encryptedHashedSquaredConfirmationKey          the encrypted hashed squared confirmation key.
	 * @param encryptedHashedSquaredPartialChoiceReturnCodes the encrypted hashed squared partial choice return codes.
	 * @param verificationCardPublicKey                      the verification card public key
	 */
	@JsonCreator
	public ReturnCodeGenerationInput(
			@JsonProperty("verificationCardId")
			final String verificationCardId,
			@JsonProperty("encryptedHashedSquaredConfirmationKey")
			final ElGamalMultiRecipientCiphertext encryptedHashedSquaredConfirmationKey,
			@JsonProperty("encryptedHashedSquaredPartialChoiceReturnCodes")
			final ElGamalMultiRecipientCiphertext encryptedHashedSquaredPartialChoiceReturnCodes,
			@JsonProperty("verificationCardPublicKey")
			final ElGamalMultiRecipientPublicKey verificationCardPublicKey) {

		checkNotNull(verificationCardId);
		checkNotNull(encryptedHashedSquaredConfirmationKey);
		checkNotNull(encryptedHashedSquaredPartialChoiceReturnCodes);
		checkNotNull(verificationCardPublicKey);

		this.verificationCardId = verificationCardId;
		this.encryptedHashedSquaredConfirmationKey = encryptedHashedSquaredConfirmationKey;
		this.encryptedHashedSquaredPartialChoiceReturnCodes = encryptedHashedSquaredPartialChoiceReturnCodes;
		this.verificationCardPublicKey = verificationCardPublicKey;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public ElGamalMultiRecipientCiphertext getEncryptedHashedSquaredConfirmationKey() {
		return encryptedHashedSquaredConfirmationKey;
	}

	public ElGamalMultiRecipientCiphertext getEncryptedHashedSquaredPartialChoiceReturnCodes() {
		return encryptedHashedSquaredPartialChoiceReturnCodes;
	}

	public ElGamalMultiRecipientPublicKey getVerificationCardPublicKey() {
		return verificationCardPublicKey;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ReturnCodeGenerationInput that = (ReturnCodeGenerationInput) o;
		return verificationCardId.equals(that.verificationCardId) && encryptedHashedSquaredConfirmationKey
				.equals(that.encryptedHashedSquaredConfirmationKey) && encryptedHashedSquaredPartialChoiceReturnCodes
				.equals(that.encryptedHashedSquaredPartialChoiceReturnCodes) && verificationCardPublicKey.equals(that.verificationCardPublicKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(verificationCardId, encryptedHashedSquaredConfirmationKey, encryptedHashedSquaredPartialChoiceReturnCodes,
				verificationCardPublicKey);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList
				.of(HashableString.from(verificationCardId), encryptedHashedSquaredConfirmationKey, encryptedHashedSquaredPartialChoiceReturnCodes);
	}

}
