/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.factory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunction;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

/**
 * Class which acts as a verifier in a Zero-Knowledge Proof of Knowledge (ZK-PoK) exchange using Maurer's unified PHI function.
 *
 * @param <E> the type of the group elements of the MathematicalGroup which will be used during this exchange. E must be a type which extends
 *            GroupElement.
 */
public final class Verifier<E extends GroupElement> {

	private final MathematicalGroup<E> group;

	private final PhiFunction phiFunction;

	private final HashBuilder hashBuilder;

	/**
	 * Creates an instance of a Verifier and initializes it by the provided arguments.
	 *
	 * @param group       the mathematical group to be used during this exchange.
	 * @param phiFunction the PHI function to be used by the prover during the exchange.
	 * @param hashBuilder A helper that calculates hashes for proofs.
	 * @throws GeneralCryptoLibException if {@code group} or {@code phiFunction} is null.
	 */
	public Verifier(final MathematicalGroup<E> group, final PhiFunction phiFunction, final HashBuilder hashBuilder) throws GeneralCryptoLibException {

		Validate.notNull(group, "Mathematical group");
		Validate.notNull(phiFunction, "Phi function.");

		this.group = group;

		this.phiFunction = phiFunction;

		this.hashBuilder = hashBuilder;
	}

	private static void validateInputString(final String data) throws GeneralCryptoLibException {

		Validate.notNullOrBlank(data, "Data string");
	}

	/**
	 * Verifies the received proof as being true or false.
	 *
	 * @param publicValues a list of group elements.
	 * @param proof        the {@link Proof} that needs to be verified.
	 * @param data         some data encoded as a String.
	 * @return true if proof has been successfully verified, false otherwise.
	 * @throws GeneralCryptoLibException if {@code Verifier} was initialized by invalid values.
	 */
	public boolean verify(final List<E> publicValues, final Proof proof, final String data) throws GeneralCryptoLibException {

		validateVerifyInputs(publicValues, proof, data);

		Exponent proofHashValue = proof.getHashValue();

		List<ZpGroupElement> phiOutputs = phiFunction.calculatePhi(proof.getValuesList());

		List<GroupElement> computedValues = calculateComputedValues(publicValues, phiOutputs, proofHashValue);

		Exponent calculatedHash = hashBuilder.generateHash(group.getQ(), publicValues, computedValues, data);

		return proofHashValue.equals(calculatedHash);
	}

	@SuppressWarnings("unchecked")
	private List<GroupElement> calculateComputedValues(final List<E> publicValues, final List<ZpGroupElement> phiOutputs, final Exponent c)
			throws GeneralCryptoLibException {

		List<GroupElement> computedValues = new ArrayList<>();

		new ArrayList<GroupElement>();
		int m = phiFunction.getNumberOfOutputs();

		for (int i = 0; i < m; i++) {
			computedValues.add(publicValues.get(i).exponentiate(c.negate()).multiply(phiOutputs.get(i)));
		}

		return computedValues;
	}

	private void validateVerifyInputs(final List<E> publicValues, final Proof proof, final String data) throws GeneralCryptoLibException {

		validatePublicValues(publicValues);

		validateProof(proof);

		validateInputString(data);
	}

	private void validatePublicValues(final List<E> publicValues) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(publicValues, "List of public values");

		checkThatPublicValuesAreGroupMembers(publicValues);
	}

	private void checkThatPublicValuesAreGroupMembers(final List<E> publicValues) throws GeneralCryptoLibException {
		for (E element : publicValues) {
			if (!group.isGroupMember(element)) {
				throw new GeneralCryptoLibException(
						"All elements of list of public values must be elements of mathematical group associated with proof.");
			}
		}
	}

	private void validateProof(final Proof proof) throws GeneralCryptoLibException {

		Validate.notNull(proof, "Proof");

		Validate.isEqual(proof.getValuesList().size(), phiFunction.getNumberOfSecrets(), "Number of proof values", "number of phi function secrets");

		BigInteger groupQ = group.getQ();

		validateExponent(proof.getHashValue(), groupQ);

		for (Exponent exponentFromProof : proof.getValuesList()) {
			validateExponent(exponentFromProof, groupQ);
		}
	}

	private void validateExponent(final Exponent e, final BigInteger groupQ) throws GeneralCryptoLibException {

		if (!(e.getQ().equals(groupQ)) || !exponentValueWithinRange(e)) {
			throw new GeneralCryptoLibException(
					"Invalid exponent found while validating proof; q must match q (order) of group, value must be less than q");
		}
	}

	private boolean exponentValueWithinRange(final Exponent e) {
		return (group.getQ().compareTo(e.getValue()) > 0) && (BigInteger.ZERO.compareTo(e.getValue()) < 1);
	}
}
