/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ZeroArgument;

@SuppressWarnings({ "java:S100", "java:S116", "java:S117", "unused" })
@JsonPropertyOrder({ "c_A_0", "c_B_m", "c_d", "a_prime", "b_prime", "r_prime", "s_prime", "t_prime" })
@JsonDeserialize(builder = ZeroArgument.Builder.class)
public abstract class ZeroArgumentMixIn {

	@JsonProperty
	GqElement c_A_0;

	@JsonProperty
	GqElement c_B_m;

	@JsonProperty
	GroupVector<GqElement, GqGroup> c_d;

	@JsonProperty
	GroupVector<ZqElement, ZqGroup> a_prime;

	@JsonProperty
	GroupVector<ZqElement, ZqGroup> b_prime;

	@JsonProperty
	ZqElement r_prime;

	@JsonProperty
	ZqElement s_prime;

	@JsonProperty
	ZqElement t_prime;

	@JsonPOJOBuilder(withPrefix = "with_")
	public interface ZeroArgumentBuilderMixIn {

		@JsonProperty
		@JsonDeserialize(using = GqGroupVectorDeserializer.class)
		ZeroArgument.Builder with_c_d(final GroupVector<GqElement, GqGroup> c_d);

		@JsonProperty
		@JsonDeserialize(using = ZqGroupVectorDeserializer.class)
		ZeroArgument.Builder with_a_prime(final GroupVector<ZqElement, ZqGroup> a_prime);

		@JsonProperty
		@JsonDeserialize(using = ZqGroupVectorDeserializer.class)
		ZeroArgument.Builder with_b_prime(final GroupVector<ZqElement, ZqGroup> b_prime);

	}

}
