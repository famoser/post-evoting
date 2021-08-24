/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;
import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxIdImpl;

class VoteSetIdImplTest {

	@Test
	void considerEqualTwoInstanceWithTheSameValues() {
		BallotBoxId ballotBoxId = new BallotBoxIdImpl("1", "2", "3");
		VoteSetId one = new VoteSetIdImpl(ballotBoxId, 0);
		VoteSetId same = new VoteSetIdImpl(ballotBoxId, 0);
		VoteSetId different = new VoteSetIdImpl(ballotBoxId, 1);

		// Ensure the objects are different.
		assertNotSame(one, same);
		assertNotSame(one, different);
		// Ensure objects referring to the same data set are considered equal.
		assertEquals(one.hashCode(), same.hashCode());
		assertNotEquals(one.hashCode(), different.hashCode());
		assertEquals(one, same);
		assertNotEquals(one, different);
	}
}
