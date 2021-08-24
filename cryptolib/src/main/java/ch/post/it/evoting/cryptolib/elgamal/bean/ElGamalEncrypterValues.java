/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Class which encapsulates an 'r' value (random exponent) and a set of ElGamal encryption values (a gamma value and a list of phi values).
 */
public final class ElGamalEncrypterValues implements Witness, Ciphertext {

	private final Exponent r;

	private final ElGamalCiphertext elGamalCiphertext;

	/**
	 * Creates an ElGamalEncrypterValues, setting the received {@link Exponent} and {@link ElGamalCiphertext}.
	 *
	 * @param exponent the {@link Exponent}
	 * @param values   the {@link ElGamalCiphertext}
	 * @throws GeneralCryptoLibException if the {@link Exponent} or the {@link ElGamalCiphertext} is null.
	 */
	public ElGamalEncrypterValues(final Exponent exponent, final ElGamalCiphertext values) throws GeneralCryptoLibException {

		Validate.notNull(exponent, "Random exponent");
		Validate.notNull(values, "ElGamal ciphertext");

		r = exponent;
		elGamalCiphertext = values;
	}

	public Exponent getR() {
		return r;
	}

	public ElGamalCiphertext getElGamalCiphertext() {
		return elGamalCiphertext;
	}

	@Override
	public List<ZpGroupElement> getElements() {
		return elGamalCiphertext.getValues();
	}

	@Override
	public Exponent getExponent() {
		return r;
	}
}
