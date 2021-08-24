/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.mixnet;

import static ch.post.it.evoting.domain.Validations.validateUUID;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Contains the ballot box information such as its id and the election event id.
 */
@JsonPropertyOrder({ "ballotBoxId", "electionEventId" })
public class BallotBoxDetails {

	@JsonProperty
	private final String ballotBoxId;

	@JsonProperty
	private final String electionEventId;

	@JsonCreator
	public BallotBoxDetails(
			@JsonProperty(value = "ballotBoxId", required = true)
			final String ballotBoxId,
			@JsonProperty(value = "electionEventId", required = true)
			final String electionEventId) {

		validateUUID(ballotBoxId);
		validateUUID(electionEventId);

		this.ballotBoxId = ballotBoxId;
		this.electionEventId = electionEventId;
	}

	public String getBallotBoxId() {
		return ballotBoxId;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	@Override
	public String toString() {
		return String.format("%s-%s", ballotBoxId, electionEventId);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final BallotBoxDetails that = (BallotBoxDetails) o;
		return Objects.equals(ballotBoxId, that.ballotBoxId) && Objects.equals(electionEventId, that.electionEventId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ballotBoxId, electionEventId);
	}
}
