/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;

@SuppressWarnings("unused")
@JsonPropertyOrder({ "ciphertexts", "decryptionProofs" })
public abstract class VerifiableDecryptionsMixIn {

	@JsonProperty
	@JsonDeserialize(using = CiphertextGroupVectorDeserializer.class)
	GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts;

	@JsonProperty
	@JsonDeserialize(using = DecryptionProofGroupVectorDeserializer.class)
	GroupVector<DecryptionProof, ZqGroup> decryptionProofs;

	@JsonIgnore
	abstract GqGroup getGroup();

	@JsonIgnore
	abstract int get_N();

	@JsonIgnore
	abstract int get_l();

	@JsonCreator
	VerifiableDecryptionsMixIn(
			@JsonProperty(value = "ciphertexts", required = true)
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts,
			@JsonProperty(value = "decryptionProofs", required = true)
			final GroupVector<DecryptionProof, ZqGroup> decryptionProofs) {
	}

}
