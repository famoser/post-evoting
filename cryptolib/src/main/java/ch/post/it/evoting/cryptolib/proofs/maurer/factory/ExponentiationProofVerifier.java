/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import static ch.post.it.evoting.cryptolib.proofs.maurer.configuration.Constants.EXPONENTIATION_PROOF_AUXILIARY_DATA;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunctionExponentiation;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Verifier of the zero knowledge proof of an exponent used to perform the exponentiation operation on a collection of mathematical group elements.
 *
 * @param <E> the type of the group elements of the {@link MathematicalGroup} that will be used during this exchange. E must be a type which extends
 *            GroupElement.
 */
public class ExponentiationProofVerifier<E extends GroupElement> {

	private final List<E> exponentiatedElements;

	private final List<E> baseElements;

	private final Proof proof;

	private final MathematicalGroup<E> group;

	private final HashBuilder hashBuilder;

	/**
	 * Default constructor.
	 *
	 * @param exponentiatedElements the exponentiated base elements.
	 * @param baseElements          the base elements.
	 * @param proof                 the proof to be verified.
	 * @param group                 the Zp subgroup used for the exponentiation.
	 * @param hashBuilder           A helper that calculates hashes for proofs.
	 */
	ExponentiationProofVerifier(final List<E> exponentiatedElements, final List<E> baseElements, final Proof proof, final MathematicalGroup<E> group,
			final HashBuilder hashBuilder) {

		this.exponentiatedElements = new ArrayList<>(exponentiatedElements);
		this.baseElements = new ArrayList<>(baseElements);
		this.proof = proof;
		this.group = group;

		this.hashBuilder = hashBuilder;
	}

	/**
	 * Verifies the exponentiation zero knowledge proof.
	 *
	 * @return true if the proof is verified as true, false otherwise.
	 * @throws GeneralCryptoLibException if {@code ExponentiationProofVerifier} was instantiated by invalid arguments.
	 */
	public boolean verify() throws GeneralCryptoLibException {

		PhiFunctionExponentiation phiFunctionExponentiation = new PhiFunctionExponentiation(group, baseElements);

		Verifier<E> verifier = new Verifier<>(group, phiFunctionExponentiation, hashBuilder);

		List<E> publicValues = new ArrayList<>();
		publicValues.addAll(exponentiatedElements);
		publicValues.addAll(baseElements);

		return verifier.verify(publicValues, proof, EXPONENTIATION_PROOF_AUXILIARY_DATA);
	}
}
