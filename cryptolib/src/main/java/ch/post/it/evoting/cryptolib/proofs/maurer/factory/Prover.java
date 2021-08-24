/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomInteger;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.bigintegers.BigIntegers;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponents;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunction;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Class which acts as a prover in a Zero-Knowledge Proof of Knowledge (ZK-PoK) exchange using Maurer's unified PHI function.
 *
 * @param <E> The type of the group elements of the {@link MathematicalGroup} that will be used during this exchange. E must be a type which extends
 *            GroupElement.
 */
public final class Prover<E extends GroupElement> {

	private final MathematicalGroup<E> group;

	private final PhiFunction phiFunction;

	private final HashBuilder hashBuilder;

	/**
	 * Creates a proofs prover, that can be used for constructing proofs.
	 *
	 * @param group       the mathematical group to be used during this exchange.
	 * @param phiFunction the PHI function to be used by the prover during the exchange.
	 * @param hashBuilder A helper that calculates hashes for proofs.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	public Prover(final MathematicalGroup<E> group, final PhiFunction phiFunction, final HashBuilder hashBuilder) throws GeneralCryptoLibException {

		Validate.notNull(group, "Mathematical group");
		Validate.notNull(phiFunction, "Phi function.");

		this.group = group;

		this.phiFunction = phiFunction;

		this.hashBuilder = hashBuilder;
	}

	/**
	 * Performs prover steps in a ZK-PoK exchange using Maurer's unified PHI function. This involves generating a {@link Proof}, using the received
	 * inputs and this Prover's internal fields. This method starts with pre-computed values for the proof generation.
	 *
	 * @param publicValues      a list of group elements.
	 * @param privateValues     a list of exponents, this list is used as the secret values in the exchange.
	 * @param data              some data encoded as a String.
	 * @param preComputedValues the pre-computed values of the proof generation.
	 * @return the generated proof.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	public Proof prove(final List<E> publicValues, final List<Exponent> privateValues, final String data,
			final ProofPreComputedValues preComputedValues) throws GeneralCryptoLibException {

		validateInput(publicValues, privateValues, data);

		Exponent hash = hashBuilder.generateHash(group.getQ(), publicValues, preComputedValues.getPhiOutputs(), data);

		List<Exponent> proofValues = generateValuesList(privateValues, hash, preComputedValues.getExponents());

		return new Proof(hash, proofValues);
	}

	/**
	 * Pre-computes the values needed for proof generation.
	 *
	 * @param cryptoRandomInteger the random number generator used to generate the proof secrets.
	 * @return the pre-computed values.
	 * @throws GeneralCryptoLibException if the pre-computation process fails.
	 */
	public ProofPreComputedValues preCompute(final CryptoAPIRandomInteger cryptoRandomInteger) throws GeneralCryptoLibException {

		List<Exponent> randomExponents = calculateRandomExponents(phiFunction.getNumberOfSecrets(), cryptoRandomInteger);

		List<ZpGroupElement> phiOutputs = phiFunction.calculatePhi(randomExponents);

		return new ProofPreComputedValues(randomExponents, phiOutputs);
	}

	private List<Exponent> generateValuesList(final List<Exponent> privateValues, final Exponent hashAsExponent, final List<Exponent> randomExponents)
			throws GeneralCryptoLibException {

		int numInputs = privateValues.size();

		List<Exponent> proofValues = new ArrayList<>();
		BigInteger exponentValue;

		for (int i = 0; i < numInputs; i++) {

			exponentValue = BigIntegers.multiply(hashAsExponent.getValue(), privateValues.get(i).getValue()).add(randomExponents.get(i).getValue());

			proofValues.add(new Exponent(group.getQ(), exponentValue));
		}

		return proofValues;
	}

	private List<Exponent> calculateRandomExponents(final int numInputs, final CryptoAPIRandomInteger cryptoRandomInteger)
			throws GeneralCryptoLibException {

		List<Exponent> randomExponents = new ArrayList<>();

		for (int i = 0; i < numInputs; i++) {
			randomExponents.add(Exponents.random(group, cryptoRandomInteger));
		}

		return randomExponents;
	}

	private void validateInput(final List<E> publicValues, final List<Exponent> privateValues, final String data) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(publicValues, "List of public values");

		Validate.notNullOrEmptyAndNoNulls(privateValues, "List of private values");
		Validate.isEqual(privateValues.size(), phiFunction.getNumberOfSecrets(), "Number of private values", "number of phi function secrets");

		Validate.notNullOrBlank(data, "Data string");
	}
}
