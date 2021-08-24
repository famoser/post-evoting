/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.cryptoapi;

import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

/**
 * Defines methods exposed by the output of ElGamal encryption, which are used during the generation of Zero Knowledge Proofs (ZKPs).
 */
public interface Witness {

	/**
	 * Returns the exponent that was used during the encryption process.
	 *
	 * @return the exponent.
	 */
	Exponent getExponent();
}
