/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.stream.Collectors;

/**
 * An encrypted vote that can be serialised (to allow it to move across components) and compared (to allow sets thereof to be sorted for predictable
 * signatures).
 */
public abstract class SerializableEncryptedVote implements EncryptedVote, Serializable {
	static final String DELIMITER = ";";

	private static final long serialVersionUID = 8046142652960758759L;

	/**
	 * Provides a standard representation for encrypted votes.
	 *
	 * @return the gamma and the phis, semi-colon separated.
	 */
	@Override
	public String toString() {
		return String.join(DELIMITER, getGamma().toString(), getPhis().stream().map(BigInteger::toString).collect(Collectors.joining(DELIMITER)));
	}

	@Override
	public abstract boolean equals(Object object);

	@Override
	public abstract int hashCode();

}
