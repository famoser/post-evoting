/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;

@JsonPropertyOrder({ "correctnessId", "numberOfSelections", "numberOfVotingOptions" })
public class CorrectnessInformation implements HashableList {

	@JsonProperty
	private final String correctnessId;

	@JsonProperty
	private final Integer numberOfSelections;

	@JsonProperty
	private final Integer numberOfVotingOptions;

	/**
	 * The constructor.
	 * <p>
	 * Must respect the following:
	 * <ul>
	 * 	<li>the number of selections must be at most the number of voting options.</li>
	 * </ul>
	 *
	 * @param correctnessId         The correctness id. Must be non-null.
	 * @param numberOfSelections    The number of selections. Must be non-null and strictly positive.
	 * @param numberOfVotingOptions The number of voting options. Must be non-null and strictly positive.
	 */
	@JsonCreator
	public CorrectnessInformation(
			@JsonProperty("correctnessId")
			final String correctnessId,
			@JsonProperty("numberOfSelections")
			final Integer numberOfSelections,
			@JsonProperty("numberOfVotingOptions")
			final Integer numberOfVotingOptions) {

		checkNotNull(correctnessId);

		checkNotNull(numberOfSelections);
		checkArgument(numberOfSelections > 0, "The number of selections must be strictly positive.");

		checkNotNull(numberOfVotingOptions);
		checkArgument(numberOfVotingOptions > 0, "The number of voting options must be strictly positive.");

		checkArgument(numberOfSelections <= numberOfVotingOptions, "The number of selections must be at most the number of voting options.");

		this.correctnessId = correctnessId;
		this.numberOfSelections = numberOfSelections;
		this.numberOfVotingOptions = numberOfVotingOptions;
	}

	public String getCorrectnessId() {
		return this.correctnessId;
	}

	public Integer getNumberOfSelections() {
		return this.numberOfSelections;
	}

	public Integer getNumberOfVotingOptions() {
		return this.numberOfVotingOptions;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CorrectnessInformation that = (CorrectnessInformation) o;
		return correctnessId.equals(that.correctnessId) && numberOfSelections.equals(that.numberOfSelections) && numberOfVotingOptions
				.equals(that.numberOfVotingOptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(correctnessId, numberOfSelections, numberOfVotingOptions);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(HashableString.from(correctnessId), HashableBigInteger.from(BigInteger.valueOf(numberOfSelections)),
				HashableBigInteger.from(BigInteger.valueOf(numberOfVotingOptions)));
	}
}
