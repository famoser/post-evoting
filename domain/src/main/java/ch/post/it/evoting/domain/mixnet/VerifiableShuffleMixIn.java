/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ShuffleArgument;

@SuppressWarnings("unused")
@JsonPropertyOrder({ "shuffledCiphertexts", "shuffleArgument" })
public abstract class VerifiableShuffleMixIn {

	@JsonProperty
	@JsonDeserialize(using = CiphertextGroupVectorDeserializer.class)
	GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	ShuffleArgument shuffleArgument;

	@JsonCreator
	VerifiableShuffleMixIn(
			@JsonProperty(value = "shuffledCiphertexts", required = true)
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts,
			@JsonProperty(value = "shuffleArgument", required = true)
			final ShuffleArgument shuffleArgument) {
	}

}
