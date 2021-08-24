/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.activity;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

/**
 * Provides functionality for compressing (adding together) a list of exponents.
 *
 * <p>The list of exponents must all belong to the same mathematical group. That mathematical group
 * must be passed as an argument to the constructor of this class.
 *
 * @param <G> the type of MathematicalGroup.
 */
public final class ExponentCompressor<G extends MathematicalGroup<?>> {

	private final G group;

	/**
	 * Construct an ExponentCompressor for exponents that belong to the received mathematical group.
	 *
	 * @param group the mathematical group for which this compressor operate on.
	 * @throws GeneralCryptoLibException if {@code group} is null.
	 */
	public ExponentCompressor(final G group) throws GeneralCryptoLibException {

		Validate.notNull(group, "Zp subgroup");

		this.group = group;
	}

	/**
	 * @param numExponentsRequiredInNewList
	 * @param inputList
	 * @throws GeneralCryptoLibException if numExponentsRequiredInNewList is less that one or bigger than the size of the given list.
	 */
	private static void validateOutputListLength(final int numExponentsRequiredInNewList, final List<Exponent> inputList)
			throws GeneralCryptoLibException {

		Validate.isPositive(numExponentsRequiredInNewList, "Number of exponents in output list");

		int numExponentsInExistingList = inputList.size();
		if (numExponentsRequiredInNewList > numExponentsInExistingList) {
			throw new GeneralCryptoLibException("Number of exponents in output list " + numExponentsRequiredInNewList
					+ " cannot be greater than number of exponents in input list " + numExponentsInExistingList);
		}
	}

	private static List<Exponent> buildFirstPartOfNewList(final int numExponentsRequiredInNewList, final List<Exponent> inputList) {

		List<Exponent> defensiveInputList = new ArrayList<>(inputList);

		return new ArrayList<>(defensiveInputList.subList(0, numExponentsRequiredInNewList - 1));
	}

	private static List<Exponent> extractListElementsToBeCompressed(final int numExponentsRequiredInNewList, final List<Exponent> inputList) {

		return new ArrayList<>(inputList.subList(numExponentsRequiredInNewList - 1, inputList.size()));
	}

	/**
	 * Compress a list of exponents into a single exponent.
	 *
	 * @param exponents the list of exponents to be compressed.
	 * @return the compressed exponent.
	 * @throws GeneralCryptoLibException if list of exponents is null or empty
	 * @throws GeneralCryptoLibException if exponents are invalid or any of the exponents do not belong to the group that this compressor supports.
	 */
	public Exponent compress(final List<Exponent> exponents) throws GeneralCryptoLibException {

		validateExponents(exponents);

		Exponent runningTotal = exponents.get(0);

		for (int i = 1; i < exponents.size(); i++) {
			runningTotal = runningTotal.add(exponents.get(i));
		}

		return runningTotal;
	}

	/**
	 * Builds a new list of elements from the input list, where the final elements from the input list are compressed into a simple element.
	 *
	 * @param numExponentsRequiredInNewList the number of elements that should be in the output list.
	 * @param inputList                     the input list.
	 * @return a new list of Exponents.
	 * @throws GeneralCryptoLibException if the inputList is null, empty or any of the exponents do not belong to the group that this compressor
	 *                                   supports.
	 * @throws GeneralCryptoLibException if numExponentsRequiredInNewList is less that one or bigger than inputList size.
	 */
	public List<Exponent> buildListWithCompressedFinalElement(final int numExponentsRequiredInNewList, final List<Exponent> inputList)
			throws GeneralCryptoLibException {

		validateExponents(inputList);
		validateOutputListLength(numExponentsRequiredInNewList, inputList);

		List<Exponent> listWithCompressedFinalElement = buildFirstPartOfNewList(numExponentsRequiredInNewList, inputList);

		listWithCompressedFinalElement.add(compress(extractListElementsToBeCompressed(numExponentsRequiredInNewList, inputList)));

		return listWithCompressedFinalElement;
	}

	/**
	 * @throws GeneralCryptoLibException if exponents are invalid or any of the exponents do not belong to the group that this compressor supports.
	 * @throws GeneralCryptoLibException if list of exponents is null or empty
	 */
	private void validateExponents(final List<Exponent> exponents) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(exponents, "List of exponents");

		for (Exponent exponent : exponents) {
			if (!exponent.getQ().equals(group.getQ())) {
				throw new GeneralCryptoLibException(
						"List of exponents contains exponent which is not of the expected Zp subgroup order. Exponent: " + exponent + ", Q: " + group
								.getQ());
			}
		}
	}
}
