/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.vote;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * An encrypted vote created from a ciphertext.
 */
public class CiphertextEncryptedVote extends SerializableEncryptedVote {

	private static final long serialVersionUID = -4076480117841308514L;

	private final BigInteger gamma;

	private final List<BigInteger> phis;

	public CiphertextEncryptedVote(Ciphertext ciphertext) {
		gamma = ciphertext.getGamma().getValue();

		phis = ciphertext.getPhis().stream().map(ZpGroupElement::getValue).collect(Collectors.toList());
	}

	public CiphertextEncryptedVote(BigInteger gamma, List<BigInteger> phis) {
		this.gamma = gamma;
		this.phis = phis;
	}

	@Override
	public BigInteger getGamma() {
		return gamma;
	}

	@Override
	public List<BigInteger> getPhis() {
		return Collections.unmodifiableList(phis);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		CiphertextEncryptedVote other = (CiphertextEncryptedVote) obj;
		return Objects.equals(gamma, other.gamma) && Objects.equals(phis, other.phis);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gamma, phis);
	}

}
