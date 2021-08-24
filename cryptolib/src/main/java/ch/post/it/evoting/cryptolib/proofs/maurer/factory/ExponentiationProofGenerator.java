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
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunctionExponentiation;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Generator of the zero knowledge proof of the exponent used to perform the exponentiation operation on a collection of mathematical group elements.
 *
 * @param <E> the type of the group elements of the {@link MathematicalGroup} that will be used during this exchange. E must be a type which extends
 *            GroupElement.
 */
public class ExponentiationProofGenerator<E extends GroupElement> {

	private final List<E> exponentiatedElements;

	private final List<E> baseElements;

	private final Exponent exponent;

	private final MathematicalGroup<E> group;

	private final HashBuilder hashBuilder;

	private final ProofPreComputedValues proofPreComputedValues;

	/**
	 * Default constructor.
	 *
	 * @param exponentiatedElements the exponentiated base elements.
	 * @param baseElements          the base elements.
	 * @param exponent              the exponent, which acts as the witness for this proof.
	 * @param group                 the Zp subgroup used for the exponentiation.
	 * @param hashBuilder           A helper that calculates hashes for proofs.
	 * @param preComputedValues     the pre-computed values used to generate the proof.
	 */
	ExponentiationProofGenerator(final List<E> exponentiatedElements, final List<E> baseElements, final Exponent exponent,
			final MathematicalGroup<E> group, final HashBuilder hashBuilder, final ProofPreComputedValues preComputedValues) {

		this.exponentiatedElements = new ArrayList<>(exponentiatedElements);
		this.baseElements = new ArrayList<>(baseElements);
		this.exponent = exponent;
		this.group = group;

		this.hashBuilder = hashBuilder;

		proofPreComputedValues = preComputedValues;
	}

	/**
	 * Generates the exponentiation zero knowledge proof.
	 *
	 * @return the exponentiation zero knowledge proof, as an object of type {@link Proof}.
	 * @throws GeneralCryptoLibException if the @{ExponentiationProofGenerator} was instantiated by invalid arguments.
	 */
	public Proof generate() throws GeneralCryptoLibException {

		PhiFunctionExponentiation phiFunctionExponentiation = new PhiFunctionExponentiation(group, baseElements);

		Prover<E> prover = new Prover<>(group, phiFunctionExponentiation, hashBuilder);

		List<E> publicValues = new ArrayList<>();
		publicValues.addAll(exponentiatedElements);
		publicValues.addAll(baseElements);

		return prover.prove(publicValues, buildListExponents(), EXPONENTIATION_PROOF_AUXILIARY_DATA, proofPreComputedValues);
	}

	private List<Exponent> buildListExponents() {

		List<Exponent> exponentList = new ArrayList<>();
		exponentList.add(exponent);

		return exponentList;
	}
}
