/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

@SuppressWarnings("unused")
@JsonPropertyOrder({ "gamma", "phis" })
public abstract class ElGamalMultiRecipientCiphertextMixIn {

	@JsonProperty
	GqElement gamma;

	@JsonProperty
	GroupVector<GqElement, GqGroup> phis;

	@JsonIgnore
	GqGroup group;

	@JsonCreator
	static ElGamalMultiRecipientCiphertext create(
			@JsonProperty(value = "gamma", required = true)
			final GqElement gamma,
			@JsonProperty(value = "phis", required = true)
			final List<GqElement> phis) {
		return null;
	}

	@JsonIgnore
	abstract GroupVector<GqElement, GqGroup> getPhi();

}
