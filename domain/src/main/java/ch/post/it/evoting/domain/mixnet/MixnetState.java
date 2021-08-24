/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Object for exchanging MixnetPayloads between the voting-server and the control components. Contains information about the state of the mixing
 * process of the payloads.
 */
@JsonPropertyOrder({ "ballotBoxDetails", "nodeToVisit", "payload", "retryCount", "mixnetError" })
public class MixnetState {

	@JsonProperty
	private final BallotBoxDetails ballotBoxDetails;
	@JsonProperty
	private final MixnetPayload payload;
	@JsonProperty
	private int nodeToVisit;
	@JsonProperty
	private int retryCount;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String mixnetError;

	@JsonCreator
	public MixnetState(
			@JsonProperty(value = "ballotBoxDetails", required = true)
			final BallotBoxDetails ballotBoxDetails,
			@JsonProperty(value = "nodeToVisit", required = true)
			final int nodeToVisit,
			@JsonProperty(value = "payload", required = true)
			final MixnetPayload payload,
			@JsonProperty(value = "retryCount", required = true)
			final int retryCount,
			@JsonProperty("mixnetError")
			final String mixnetError) {

		this.ballotBoxDetails = ballotBoxDetails;
		this.nodeToVisit = nodeToVisit;
		this.payload = payload;
		this.retryCount = retryCount;
		this.mixnetError = mixnetError;
	}

	public MixnetState(final BallotBoxDetails ballotBoxDetails, final MixnetPayload payload) {
		this(ballotBoxDetails, 0, payload, 5, null);
	}

	public BallotBoxDetails getBallotBoxDetails() {
		return ballotBoxDetails;
	}

	public MixnetPayload getPayload() {
		return payload;
	}

	public int getNodeToVisit() {
		return nodeToVisit;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public String getMixnetError() {
		return mixnetError;
	}

	public void setMixnetError(final String mixnetError) {
		this.mixnetError = mixnetError;
	}

	public int incrementNodeToVisit() {
		this.nodeToVisit++;
		return this.nodeToVisit;
	}

	public void decrementRetryCount() {
		checkArgument(retryCount >= 1, "The retry count can not be further decremented because it is 0.");
		this.retryCount -= 1;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final MixnetState that = (MixnetState) o;
		return nodeToVisit == that.nodeToVisit && retryCount == that.retryCount && Objects.equals(ballotBoxDetails, that.ballotBoxDetails) && Objects
				.equals(payload, that.payload) && Objects.equals(mixnetError, that.mixnetError);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ballotBoxDetails, nodeToVisit, payload, retryCount, mixnetError);
	}
}
