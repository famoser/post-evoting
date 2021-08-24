/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

/**
 * Implementation of {@link Witness}, instances of this class are used during the generation of certain Zero Knowledge Proofs (ZKPs).
 */
public class WitnessImpl implements Witness {

	private final Exponent exponent;

	/**
	 * Constructor.
	 *
	 * @param exponent used to set in this Witness.
	 * @throws GeneralCryptoLibException failed to create instance.
	 */
	public WitnessImpl(final Exponent exponent) throws GeneralCryptoLibException {
		Validate.notNull(exponent, "Exponent");

		this.exponent = exponent;
	}

	@Override
	public Exponent getExponent() {
		return exponent;
	}
}
