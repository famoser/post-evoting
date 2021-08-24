/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.function;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;

/**
 * Definition of the phi function used for generating the Zero Knowledge Proof of Knowledge of an exponent used to perform the exponentiation
 * operation on a collection of mathematical group elements.
 */
public class PhiFunctionExponentiation extends PhiFunction {

	/**
	 * Creates an instance of {@code PhiFunctionExponentiation}.
	 *
	 * <p>Note: the number of inputs is set to one, while the number of outputs and the computation
	 * rules are derived from the length (size) of baseElements.
	 *
	 * @param group        the mathematical group.
	 * @param baseElements the mathematical group elements used as base elements for the phi computation.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	public <E extends GroupElement> PhiFunctionExponentiation(final MathematicalGroup<E> group, final List<E> baseElements)
			throws GeneralCryptoLibException {

		super(group, 1, baseElements.size(), baseElements, buildComputationRules(baseElements.size()));
	}

	private static int[][][] buildComputationRules(final int numOutputs) {

		int[][][] rules = new int[numOutputs][][];

		for (int i = 0; i < numOutputs; i++) {

			rules[i] = new int[][] { { i + 1, 1 } };
		}

		return rules;
	}
}
