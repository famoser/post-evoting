/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

@SuppressWarnings({ "unused", "java:S100", "java:S1610" })
@JsonPropertyOrder({ "e", "z" })
public abstract class ExponentiationProofMixIn {

	@JsonCreator
	ExponentiationProofMixIn(
			@JsonProperty(value = "e", required = true)
			final ZqElement e,
			@JsonProperty(value = "z", required = true)
			final ZqElement z) {
	}

	@JsonProperty("e")
	abstract ZqElement get_e();

	@JsonProperty("z")
	abstract ZqElement get_z();

	@JsonIgnore
	abstract ZqGroup getGroup();

}
