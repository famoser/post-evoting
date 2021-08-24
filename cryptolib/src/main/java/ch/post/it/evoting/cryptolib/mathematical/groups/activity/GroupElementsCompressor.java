/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.activity;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;

/**
 * Provides functionality for compressing (using the multiplication operation) a list of group elements.
 *
 * @param <E> The type of elements that this compressor can handle.
 */
public final class GroupElementsCompressor<E extends GroupElement> {

	/**
	 * Compress a list of group elements.
	 *
	 * <p>
	 *
	 * @param elementsToBeCompressed The list of group elements to be compressed
	 * @return The result of the compression process.
	 * @throws GeneralCryptoLibException if {@code elementsToBeCompressed} is null or empty
	 * @throws GeneralCryptoLibException if {@code elementsToBeCompressed} contains any elements that are not elements of the mathematical group on
	 *                                   which this compressor operates.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public E compress(final List<E> elementsToBeCompressed) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(elementsToBeCompressed, "List of elements to be compressed");

		GroupElement runningTotal = elementsToBeCompressed.get(0);

		for (int i = 1; i < elementsToBeCompressed.size(); i++) {

			runningTotal = runningTotal.multiply(elementsToBeCompressed.get(i));
		}

		return (E) runningTotal;
	}

	/**
	 * Builds a new list of elements from the input list, where the final elements from the input list are compressed into a simple element.
	 *
	 * <p>Note: it is assumed that all of the elements in inputList are members of the mathematical
	 * group that was passed to the constructor of this GroupElementsCompressor.
	 *
	 * @param outputListSize the number of elements that should be in the output list.
	 * @param inputList      the input list.
	 * @return a new list of type {@code E}.
	 * @throws GeneralCryptoLibException if {@code numElementsRequiredInNewList} &le; 1 or {@code inputList} is empty or elements of {@code inputList}
	 *                                   are not belong to the compressor's group.
	 */
	public List<E> buildListWithCompressedFinalElement(final int outputListSize, final List<E> inputList) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(inputList, "Element input list");
		validateOutputListLength(outputListSize, inputList);

		List<E> listWithCompressedFinalElement = buildFirstPartOfNewList(outputListSize, inputList);

		listWithCompressedFinalElement.add(compress(getListElementsToBeCompressed(outputListSize, inputList)));

		return listWithCompressedFinalElement;
	}

	private void validateOutputListLength(final int outputListSize, final List<E> inputList) throws GeneralCryptoLibException {

		Validate.isPositive(outputListSize, "Number of elements in output list");
		Validate.notGreaterThan(outputListSize, inputList.size(), "Number of elements in output list", "number of elements in input list");
	}

	private List<E> buildFirstPartOfNewList(final int numElementsRequiredInNewList, final List<E> inputList) {

		List<E> defensiveInputList = new ArrayList<>(inputList);

		return new ArrayList<>(defensiveInputList.subList(0, numElementsRequiredInNewList - 1));
	}

	private List<E> getListElementsToBeCompressed(final int numElementsRequiredInNewList, final List<E> inputList) {

		List<E> defensiveInputList = new ArrayList<>(inputList);

		return new ArrayList<>(defensiveInputList.subList(numElementsRequiredInNewList - 1, defensiveInputList.size()));
	}
}
