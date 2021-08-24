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
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

@SuppressWarnings("unused")
@JsonPropertyOrder({ "e", "z" })
public abstract class DecryptionProofMixIn {

	@JsonProperty
	ZqElement e;

	@JsonProperty
	@JsonDeserialize(using = ZqGroupVectorDeserializer.class)
	GroupVector<ZqElement, ZqGroup> z;

	@JsonCreator
	DecryptionProofMixIn(
			@JsonProperty(value = "e", required = true)
			final ZqElement e,
			@JsonProperty(value = "z", required = true)
			final GroupVector<ZqElement, ZqGroup> z) {
	}

	@JsonIgnore
	abstract ZqGroup getGroup();

}
