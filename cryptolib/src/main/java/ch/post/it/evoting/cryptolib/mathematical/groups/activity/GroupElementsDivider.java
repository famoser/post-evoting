/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.activity;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;

/**
 * Provides functionality for dividing the elements in one list by the elements in another list.
 */
public final class GroupElementsDivider {

	private static <E extends GroupElement> void validateInput(final List<E> groupElementsList1, final List<E> groupElementsList2,
			final MathematicalGroup<E> group) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(groupElementsList1, "List of elements to be divided");
		Validate.notNullOrEmptyAndNoNulls(groupElementsList2, "List of elements to act as divisor");
		Validate.notNull(group, "Zp subgroup");
		Validate.isEqual(groupElementsList1.size(), groupElementsList2.size(), "Length of element list to be divided",
				"length of element list to act as divisor");
	}

	/**
	 * Given two lists of groups elements, this method produces a new list of group elements that represents the result of dividing {@code
	 * groupElementsList1} by {@code groupElementsList2}.
	 *
	 * <p>Note: it is assumed that all of the elements in {@code groupElementsList1} and {@code
	 * groupElementsList2} are members of {@code group}.
	 *
	 * @param <E>                the group element type
	 * @param groupElementsList1 a list of group elements.
	 * @param groupElementsList2 a list of group elements.
	 * @param group              a mathematical group, that both lists should belong to.
	 * @return a new list of group elements.
	 * @throws GeneralCryptoLibException if either list contains any value that is not a group element.
	 */
	@SuppressWarnings("unchecked")
	public <E extends GroupElement> List<E> divide(final List<E> groupElementsList1, final List<E> groupElementsList2,
			final MathematicalGroup<E> group) throws GeneralCryptoLibException {

		validateInput(groupElementsList1, groupElementsList2, group);

		List<E> newList = new ArrayList<>();

		for (int i = 0; i < groupElementsList1.size(); i++) {

			E newElement = (E) groupElementsList1.get(i).multiply(groupElementsList2.get(i).invert());

			newList.add(newElement);
		}

		return newList;
	}
}
