/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;
import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxIdImpl;

/**
 * Default implementation of the vote set identifier.
 */
public class VoteSetIdImpl implements VoteSetId {

	private static final long serialVersionUID = 975163881474928750L;

	private final BallotBoxId ballotBoxId;

	private final int index;

	@JsonCreator
	public VoteSetIdImpl(
			@JsonProperty("ballotBoxId")
					BallotBoxId ballotBoxId,
			@JsonProperty("index")
					int index) {
		this.ballotBoxId = ballotBoxId;
		this.index = index;
	}

	@Override
	public String toString() {
		return String.format("%s-%d", ballotBoxId, index);
	}

	@Override
	@JsonDeserialize(as = BallotBoxIdImpl.class)
	public BallotBoxId getBallotBoxId() {
		return ballotBoxId;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj) {
			return false;
		}

		if (!(obj instanceof VoteSetId)) {
			return false;
		}

		return obj.hashCode() == hashCode();
	}

	@Override
	public int hashCode() {
		return Objects.hash(ballotBoxId, index);
	}
}
