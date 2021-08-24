/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;

/**
 * Represents the result of a decryption that produced plaintext messages and their associated decryption proofs.
 * <p>
 * This is an immutable value class.
 */
@JsonPropertyOrder({ "decryptedVotes", "decryptionProofs" })
public class VerifiablePlaintextDecryption {

	@JsonProperty
	@JsonDeserialize(using = MessageGroupVectorDeserializer.class)
	private final GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes;

	@JsonProperty
	@JsonDeserialize(using = DecryptionProofGroupVectorDeserializer.class)
	private final GroupVector<DecryptionProof, ZqGroup> decryptionProofs;

	private final GqGroup group;

	@JsonCreator
	public VerifiablePlaintextDecryption(
			@JsonProperty(value = "decryptedVotes", required = true)
			final GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes,
			@JsonProperty(value = "decryptionProofs", required = true)
			final GroupVector<DecryptionProof, ZqGroup> decryptionProofs) {

		checkNotNull(decryptedVotes);
		checkNotNull(decryptionProofs);
		checkArgument(decryptedVotes.getGroup().hasSameOrderAs(decryptionProofs.getGroup()));
		checkArgument(decryptedVotes.size() == decryptionProofs.size());
		checkArgument(decryptedVotes.getElementSize() == decryptionProofs.getElementSize());

		this.decryptedVotes = decryptedVotes;
		this.decryptionProofs = decryptionProofs;
		this.group = decryptedVotes.getGroup();
	}

	@JsonIgnore
	public GqGroup getGroup() {
		return group;
	}

	public GroupVector<ElGamalMultiRecipientMessage, GqGroup> getDecryptedVotes() {
		return decryptedVotes;
	}

	public GroupVector<DecryptionProof, ZqGroup> getDecryptionProofs() {
		return decryptionProofs;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final VerifiablePlaintextDecryption that = (VerifiablePlaintextDecryption) o;
		return decryptedVotes.equals(that.decryptedVotes) && decryptionProofs.equals(that.decryptionProofs) && group.equals(that.group);
	}

	@Override
	public int hashCode() {
		return Objects.hash(decryptedVotes, decryptionProofs, group);
	}
}
