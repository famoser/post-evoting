/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ZeroArgument;

@SuppressWarnings({ "java:S116", "java:S117", "unused" })
@JsonPropertyOrder({ "c_b", "zeroArgument" })
public abstract class HadamardArgumentMixIn {

	@JsonProperty
	@JsonDeserialize(using = GqGroupVectorDeserializer.class)
	GroupVector<GqElement, GqGroup> c_b;

	@JsonProperty
	ZeroArgument zeroArgument;

	@JsonCreator
	HadamardArgumentMixIn(
			@JsonProperty(value = "c_b", required = true)
			final GroupVector<GqElement, GqGroup> c_b,
			@JsonProperty(value = "zeroArgument", required = true)
			final ZeroArgument zeroArgument) {
	}

}
