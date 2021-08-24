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
import ch.post.it.evoting.cryptoprimitives.mixnet.MultiExponentiationArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.ProductArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.ShuffleArgument;

@SuppressWarnings({ "java:S100", "java:S116", "java:S117", "unused" })
@JsonPropertyOrder({ "c_A", "c_B", "productArgument", "multiExponentiationArgument" })
@JsonDeserialize(builder = ShuffleArgument.Builder.class)
public abstract class ShuffleArgumentMixIn {

	@JsonProperty
	GroupVector<GqElement, GqGroup> c_A;

	@JsonProperty
	GroupVector<GqElement, GqGroup> c_B;

	@JsonProperty
	ProductArgument productArgument;

	@JsonProperty
	MultiExponentiationArgument multiExponentiationArgument;

	@JsonPOJOBuilder(withPrefix = "with_")
	public interface ShuffleArgumentBuilderMixin {

		@JsonProperty
		@JsonDeserialize(using = GqGroupVectorDeserializer.class)
		ShuffleArgument.Builder with_c_A(final GroupVector<GqElement, GqGroup> c_A);

		@JsonProperty
		@JsonDeserialize(using = GqGroupVectorDeserializer.class)
		ShuffleArgument.Builder with_c_B(final GroupVector<GqElement, GqGroup> c_B);

	}

}
