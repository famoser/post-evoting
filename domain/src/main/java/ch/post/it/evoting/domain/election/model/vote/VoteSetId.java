/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import java.io.Serializable;

import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;

/**
 * A reference to a set of votes in a ballot box. As a side note, it may be worth noting that a vote set ID is not always a reflection of a set of
 * votes in a ballot box, but merely a pointer to where the votes originally came from.
 */
public interface VoteSetId extends Serializable {

	/**
	 * @return a reference to the ballot box the votes come from
	 */
	BallotBoxId getBallotBoxId();

	/**
	 * Which of the ballot box divisions this vote set represents. The size of such ballot box divisions is determined by {@code VoteSet.MAX_SIZE}.
	 *
	 * @return the index of the vote set
	 */
	int getIndex();
}
