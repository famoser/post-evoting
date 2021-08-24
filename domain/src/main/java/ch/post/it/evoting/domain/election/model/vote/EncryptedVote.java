/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import java.math.BigInteger;
import java.util.List;

/**
 * Simplified representation of an encrypted vote, consisting of a big integer representing the gamma, and a set of big integers representing the
 * phis.
 */
public interface EncryptedVote {
	/**
	 * @return the encrypted vote's gamma
	 */
	BigInteger getGamma();

	/**
	 * @return the encrypted vote's phis
	 */
	List<BigInteger> getPhis();
}
