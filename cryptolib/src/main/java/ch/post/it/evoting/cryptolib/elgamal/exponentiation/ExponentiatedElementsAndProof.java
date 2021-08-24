/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.cryptolib.elgamal.exponentiation;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

/**
 * Encapsulates exponentiated group elements and their corresponding exponentiationProof of correct exponentiating.
 *
 * @param <T> the type of the bases.
 */
@Immutable
public final class ExponentiatedElementsAndProof<T> {
	private final List<T> exponentiatedElements;

	private final Proof exponentiationProof;

	/**
	 * Constructor. For internal use only.
	 *
	 * @param exponentiatedElements the group elements exponentiated to a secret element.
	 * @param exponentiationProof   the zero-knowledge exponentiationProof of correct exponentiation.
	 */
	ExponentiatedElementsAndProof(List<T> exponentiatedElements, Proof exponentiationProof) {
		this.exponentiatedElements = unmodifiableList(exponentiatedElements);
		this.exponentiationProof = exponentiationProof;
	}

	/**
	 * Creates a new instance for given exponentiated elements and corresponding exponentiation exponentiationProof. This method is provided for
	 * testing purposes.
	 *
	 * @param exponentiatedElements the group elements exponentiated to a secret element.
	 * @param exponentiationProof   the zero-knowledge exponentiationProof of correct exponentiation.
	 * @return a new instance.
	 */
	public static <T> ExponentiatedElementsAndProof<T> newInstance(List<T> exponentiatedElements, Proof exponentiationProof) {
		return new ExponentiatedElementsAndProof<>(new ArrayList<>(exponentiatedElements), exponentiationProof);
	}

	/**
	 * Returns the exponentiatedElements. In the returned list is read-only.
	 *
	 * @return the exponentiated elements.
	 */
	public List<T> exponentiatedElements() {
		return exponentiatedElements;
	}

	/**
	 * Returns the proof of correct exponentiation.
	 *
	 * @return the exponentiation proof.
	 */
	public Proof exponentiationProof() {
		return exponentiationProof;
	}
}
