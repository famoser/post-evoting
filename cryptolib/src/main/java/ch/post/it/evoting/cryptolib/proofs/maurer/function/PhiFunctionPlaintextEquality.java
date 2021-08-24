/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.function;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;

/**
 * The implementation of Maurer's PHI function used for generating the Zero Knowledge Proof of Knowledge of the plaintext equality of two ciphertexts
 * that were each generated with a different key pair and random exponent.
 */
public class PhiFunctionPlaintextEquality extends PhiFunction {

	/**
	 * Creates an instance of {@code PhiFunctionPlaintextEquality} and initializes it by provided values.
	 *
	 * <p>Note: the number of inputs is set to two, while the number of outputs and the computation
	 * rules are derived from the length (size) of baseElements.
	 *
	 * @param group        the mathematical group.
	 * @param baseElements the mathematical group elements used as base elements for the phi computation.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	public <E extends GroupElement> PhiFunctionPlaintextEquality(final MathematicalGroup<E> group, final List<E> baseElements)
			throws GeneralCryptoLibException {

		super(group, 2, calculateNumOutputs(baseElements.size()), baseElements, buildComputationRules(calculateNumOutputs(baseElements.size())));
	}

	private static int[][][] buildComputationRules(final int numOutputs) {

		int numKeyElements = numOutputs - 2;

		int[][][] rules = new int[numOutputs][][];

		rules[0] = new int[][] { { 1, 1 } };
		rules[1] = new int[][] { { 1, 2 } };

		for (int i = 2; i < numOutputs; i++) {
			rules[i] = new int[][] { { i, 1 }, { i + numKeyElements, 2 } };
		}

		return rules;
	}

	private static int calculateNumOutputs(final int numBaseElements) {
		return ((numBaseElements - 1) / 2) + 2;
	}
}
