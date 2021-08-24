/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.cryptoapi;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.CiphertextImpl;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Defines methods exposed by the output of ElGamal encryption.
 */
public interface Ciphertext {

	/**
	 * Returns the list of group elements which comprise this ciphertext. The first element of the list is gamma value and the rest are the phi
	 * values.
	 *
	 * @return the list of group elements.
	 */
	List<ZpGroupElement> getElements();

	/**
	 * Returns the gamma value.
	 *
	 * @return the gamma value.
	 */
	default ZpGroupElement getGamma() {
		return getElements().get(0);
	}

	/**
	 * Returns the phi values. The returned list is read-only.
	 *
	 * @return the phi values.
	 */
	default List<ZpGroupElement> getPhis() {
		List<ZpGroupElement> elements = getElements();
		return unmodifiableList(elements.subList(1, elements.size()));
	}

	/**
	 * Returns the size which is the number of elements.
	 *
	 * @return the size.
	 */
	default int size() {
		return getElements().size();
	}

	/**
	 * Multiply this ciphertext with another ciphertext to produce a new ciphertext. The first element in this ciphertext will be multiplied with the
	 * first element in the received ciphertext, etc. Therefore, this ciphertext and the other ciphertext must contain the same number of elements,
	 * otherwise an exception will be thrown.
	 *
	 * @param otherCiphertext the ciphertext to be multiplied with this ciphertext.
	 * @return the result of the multiplication.
	 * @throws GeneralCryptoLibException if the received ciphertext is not initialized or if it is not compatible with this ciphertext.
	 */
	default Ciphertext multiply(final Ciphertext otherCiphertext) throws GeneralCryptoLibException {

		if (otherCiphertext == null) {
			throw new GeneralCryptoLibException("The received ciphertext was null");
		}
		if (otherCiphertext.getPhis() == null) {
			throw new GeneralCryptoLibException("The received ciphertext does not contain an initialized list of phi values");
		}

		final List<ZpGroupElement> phis = getPhis();
		int numPhis = phis.size();
		final List<ZpGroupElement> otherPhis = otherCiphertext.getPhis();
		int numOtherPhis = otherPhis.size();
		if (numPhis != numOtherPhis) {
			throw new GeneralCryptoLibException(String.format("Ciphertexts of different lengths. This: %d other: %d", numPhis, numOtherPhis));
		}

		final ZpGroupElement resultGamma = getGamma().multiply(otherCiphertext.getGamma());

		final List<ZpGroupElement> resultPhis = new ArrayList<>();
		for (int i = 0; i < numPhis; i++) {
			resultPhis.add(phis.get(i).multiply(otherPhis.get(i)));
		}

		return new CiphertextImpl(resultGamma, resultPhis);
	}

	/**
	 * Exponentiate this ciphertext using the received exponent to create a new ciphertext. Every element in this ciphertext will be exponentiated
	 * using the exponent.
	 *
	 * @param exponent the exponent to which the ciphertext should be exponentiated.
	 * @return the result of the exponentiation.
	 * @throws GeneralCryptoLibException if the received exponent is not initialized or if it is not compatible with this ciphertext.
	 */
	default Ciphertext exponentiate(final Exponent exponent) throws GeneralCryptoLibException {

		if (exponent == null) {
			throw new GeneralCryptoLibException("The received exponent was null");
		}

		final ZpGroupElement gammaExponentiated = getGamma().exponentiate(exponent);

		final List<ZpGroupElement> phis = getPhis();
		final List<ZpGroupElement> phisExponentiated = new ArrayList<>();
		for (ZpGroupElement phi : phis) {
			phisExponentiated.add(phi.exponentiate(exponent));
		}

		return new CiphertextImpl(gammaExponentiated, phisExponentiated);
	}
}
