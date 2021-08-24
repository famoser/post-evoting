/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CiphertextEncryptedVoteTest {

	@Test
	void considerEqualTwoInstanceWithTheSameValues() {
		BigInteger gamma = BigInteger.valueOf(801);
		BigInteger gamma2 = BigInteger.valueOf(802);
		List<BigInteger> phis = new ArrayList<>();
		phis.add(BigInteger.valueOf(4431));
		phis.add(BigInteger.valueOf(1290));
		phis.add(BigInteger.valueOf(1921));

		CiphertextEncryptedVote ciphertextEncryptedVote = new CiphertextEncryptedVote(gamma, phis);
		CiphertextEncryptedVote ciphertextEncryptedVoteSame = new CiphertextEncryptedVote(gamma, phis);
		CiphertextEncryptedVote ciphertextEncryptedVoteDif = new CiphertextEncryptedVote(gamma, phis.subList(0, 2));
		CiphertextEncryptedVote ciphertextEncryptedVoteDif2 = new CiphertextEncryptedVote(gamma2, phis);

		// Ensure the objects are different.
		assertNotSame(ciphertextEncryptedVote, ciphertextEncryptedVoteSame);
		assertNotSame(ciphertextEncryptedVote, ciphertextEncryptedVoteDif);
		assertNotSame(ciphertextEncryptedVote, ciphertextEncryptedVoteDif2);

		// Ensure objects referring to the same data set are considered equal.
		assertEquals(ciphertextEncryptedVote.hashCode(), ciphertextEncryptedVoteSame.hashCode());
		assertNotEquals(ciphertextEncryptedVote.hashCode(), ciphertextEncryptedVoteDif.hashCode());
		assertNotEquals(ciphertextEncryptedVote.hashCode(), ciphertextEncryptedVoteDif2.hashCode());
		assertEquals(ciphertextEncryptedVote, ciphertextEncryptedVoteSame);
		assertNotEquals(ciphertextEncryptedVote, ciphertextEncryptedVoteDif);
		assertNotEquals(ciphertextEncryptedVote, ciphertextEncryptedVoteDif2);
	}
}
