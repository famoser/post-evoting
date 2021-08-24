/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encrypted vote created from a string representation.
 */
public class StringEncryptedVote extends SerializableEncryptedVote {

	private static final long serialVersionUID = 3622103125746798126L;

	private static final String NO_VOTE_ERROR_MESSAGE = "There is no vote representation";

	private final BigInteger gamma;

	private final List<BigInteger> phis;

	/**
	 * Create an encrypted vote from a semicolon-separated set of numbers.
	 *
	 * @param representation a semicolon-separated set of numbers
	 */
	public StringEncryptedVote(String representation) {
		Objects.requireNonNull(representation, NO_VOTE_ERROR_MESSAGE);
		if (representation.isEmpty()) {
			throw new IllegalArgumentException(NO_VOTE_ERROR_MESSAGE);
		}

		String[] elements = representation.split(DELIMITER);

		int size = elements.length;
		if (size < 2) {
			throw new IllegalArgumentException("An encrypted vote must have at least two elements");
		}

		gamma = new BigInteger(elements[0]);

		phis = new ArrayList<>(size - 1);
		for (int i = 1; i < size; i++) {
			phis.add(new BigInteger(elements[i]));
		}
	}

	/**
	 * Create an encrypted vote from its json representation.
	 *
	 * @param gamma the encrypted vote gamma
	 * @param phis  the encrypted vote phis
	 */
	@JsonCreator
	StringEncryptedVote(
			@JsonProperty("gamma")
					BigInteger gamma,
			@JsonProperty("phis")
					List<BigInteger> phis) {
		this.gamma = Objects.requireNonNull(gamma);
		this.phis = Objects.requireNonNull(phis);
	}

	@Override
	public BigInteger getGamma() {
		return gamma;
	}

	@Override
	public List<BigInteger> getPhis() {
		return phis;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		StringEncryptedVote other = (StringEncryptedVote) obj;
		return Objects.equals(gamma, other.gamma) && Objects.equals(phis, other.phis);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gamma, phis);
	}

}
