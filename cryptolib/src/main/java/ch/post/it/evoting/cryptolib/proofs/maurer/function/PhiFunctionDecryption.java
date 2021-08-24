/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.function;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;

/**
 * Implementation of Maurer's PHI function for providing a Decryption proof.
 */
public class PhiFunctionDecryption extends PhiFunction {

	/**
	 * Creates an instance of {@code PhiFunctionDecryption}.
	 *
	 * <p>Note: the number of outputs and the computation rules are derived from numInputs.
	 *
	 * @param group        the mathematical group on which this PHI function operates.
	 * @param numInputs    the number of secret inputs that the calculatePhi method of this class should accept.
	 * @param baseElements a list of base elements which are members of the received mathematical group.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	public <E extends GroupElement> PhiFunctionDecryption(final MathematicalGroup<E> group, final int numInputs, final List<E> baseElements)
			throws GeneralCryptoLibException {

		super(group, numInputs, calculateNumOutputs(numInputs), baseElements, buildComputationRules(calculateNumOutputs(numInputs)));
	}

	private static int[][][] buildComputationRules(final int numOutputs) {

		int[][][] rules = new int[numOutputs][][];
		int pairSecondValue = 1;

		for (int i = 0; i < numOutputs; i += 2) {

			rules[i] = new int[][] { { 1, pairSecondValue } };
			rules[i + 1] = new int[][] { { 2, pairSecondValue } };
			pairSecondValue++;
		}

		return rules;
	}

	private static int calculateNumOutputs(final int numberInputs) {
		return 2 * numberInputs;
	}
}
