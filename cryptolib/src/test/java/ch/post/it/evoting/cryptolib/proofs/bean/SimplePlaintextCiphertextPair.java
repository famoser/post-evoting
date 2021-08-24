/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.bean;

import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;

/**
 * Container class for a pair of ElGamal ciphertexts suitable for use with simple plaintext equality zero knowledge proofs of knowledge.
 */
public class SimplePlaintextCiphertextPair {

	private final Ciphertext _primaryCiphertext;

	private final Ciphertext _secondaryCiphertext;

	private final Witness _witness;

	/**
	 * @param primaryCiphertext   the primary ElGamal ciphertext for a simple plaintext zero knowledge proof of knowledge.
	 * @param secondaryCiphertext the secondary ElGamal ciphertext for a simple plaintext zero knowledge proof of knowledge.
	 * @param witness             the witness wrapping the randomly generated exponent used to create the ciphertext pair.
	 */
	public SimplePlaintextCiphertextPair(final Ciphertext primaryCiphertext, final Ciphertext secondaryCiphertext, final Witness witness) {

		_primaryCiphertext = primaryCiphertext;
		_secondaryCiphertext = secondaryCiphertext;
		_witness = witness;
	}

	public Ciphertext getPrimaryCiphertext() {

		return _primaryCiphertext;
	}

	public Ciphertext getSecondaryCiphertext() {

		return _secondaryCiphertext;
	}

	public Witness getWitness() {

		return _witness;
	}
}
