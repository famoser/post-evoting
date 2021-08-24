/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomInteger;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunctionExponentiation;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Exponentiation proof pre-computer.
 *
 * @param <E> the type of the group elements of the {@link MathematicalGroup} that will be used during this exchange. E must be a type which extends
 *            GroupElement.
 */
public class ExponentiationProofPreComputer<E extends GroupElement> {

	private final MathematicalGroup<E> group;

	private final HashBuilder hashBuilder;

	private final PhiFunctionExponentiation phiFunctionExponentiation;

	/**
	 * Default constructor.
	 *
	 * @param baseElements the base elements.
	 * @param group        the Zp subgroup used for the exponentiation.
	 * @param hashBuilder  A helper that calculates hashes for proofs.
	 * @throws GeneralCryptoLibException if the phi function cannot be created.
	 */
	ExponentiationProofPreComputer(final List<E> baseElements, final MathematicalGroup<E> group, final HashBuilder hashBuilder)
			throws GeneralCryptoLibException {

		this.group = group;

		this.hashBuilder = hashBuilder;

		phiFunctionExponentiation = new PhiFunctionExponentiation(this.group, new ArrayList<>(baseElements));
	}

	/**
	 * Pre-computes the values needed for proof generation.
	 *
	 * @param cryptoRandomInteger the random number generator used to create the proof secrets.
	 * @return the pre-computed values.
	 * @throws GeneralCryptoLibException if an error occurs during the pre-computation process.
	 */
	public ProofPreComputedValues preCompute(final CryptoAPIRandomInteger cryptoRandomInteger) throws GeneralCryptoLibException {

		Prover<E> prover = new Prover<>(group, phiFunctionExponentiation, hashBuilder);

		return prover.preCompute(cryptoRandomInteger);
	}
}
