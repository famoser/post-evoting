/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.serialization.AbstractJsonSerializable;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;

/**
 * Class which encapsulates a gamma and a set of phi values that are of generic type.
 *
 * <p>This class is used to represent the result of ElGamal computations. These computations include
 * pre-computations and as well as encryptions. Instances of this class can therefore act as both an input or as an output, depending on the operation
 * being performed.
 *
 * @param <E> type of the group.
 */
public class BaseElGamalCiphertext<E extends GroupElement> extends AbstractJsonSerializable {

	/**
	 * Gamma to be used by subclasses.
	 */
	protected final E gamma;

	/**
	 * Phis to be used by subclasses.
	 */
	protected final List<E> phis;

	/**
	 * Creates a BaseElGamalCiphertext using the specified gamma and phi values.
	 *
	 * @param gamma the gamma (first element) of the ciphertext.
	 * @param phis  the phis of the ciphertext.
	 * @throws GeneralCryptoLibException if the gamma element is null or the list of phi elements is null, empty or contains one or more null values.
	 */
	public BaseElGamalCiphertext(final E gamma, final List<E> phis) throws GeneralCryptoLibException {
		Validate.notNull(gamma, "ElGamal gamma element");
		Validate.notNullOrEmptyAndNoNulls(phis, "List of ElGamal phi elements");

		this.gamma = gamma;
		this.phis = new ArrayList<>(phis);
	}

	/**
	 * Creates a {@code BaseElGamalCiphertext} using the specified list of elements. This list must fulfill this contract:
	 *
	 * <ul>
	 *   <li>The first element has to be the {@code gamma}.
	 *   <li>The other elements have to be the {@code phis} in the correct order.
	 *   <li>All elements should be members of the same mathematical group.
	 * </ul>
	 *
	 * @param ciphertext the sequence of the elements, first the {@code gamma}, then the phis with the correct order.
	 * @throws GeneralCryptoLibException if the ciphertext is null, empty or contains one or more null values.
	 */
	public BaseElGamalCiphertext(final List<E> ciphertext) throws GeneralCryptoLibException {
		Validate.notNullOrEmptyAndNoNulls(ciphertext, "ElGamal ciphertext");
		Validate.notLessThan(ciphertext.size(), 2, "ElGamal ciphertext length", "");

		gamma = ciphertext.get(0);
		phis = new ArrayList<>(ciphertext.subList(1, ciphertext.size()));
	}

	public final E getGamma() {
		return gamma;
	}

	public final List<E> getPhis() {
		return new ArrayList<>(phis);
	}

	/**
	 * Get a single list containing all the values of this {@code BaseElGamalCiphertext}. First the {@code gamma} , followed by all of the {@code phi}
	 * values in order.
	 *
	 * @return a single list containing all of the values of this {@code BaseElGamalCiphertext} (gamma and phis).
	 */
	public final List<E> getValues() {

		List<E> result = new ArrayList<>(phis.size() + 1);

		result.add(gamma);
		result.addAll(phis);

		return result;
	}
}
