/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.bean;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;

/**
 * Container class for a pair of ElGamal public keys suitable for use with simple plaintext equality zero knowledge proofs of knowledge.
 */
public class SimplePlaintextPublicKeyPair {

	private final ElGamalPublicKey _primaryPublicKey;

	private final ElGamalPublicKey _secondaryPublicKey;

	/**
	 * @param primaryPublicKey   the primary ElGamal public key for a simple plaintext zero knowledge proof of knowledge.
	 * @param secondaryPublicKey the secondary ElGamal public key for a simple plaintext zero knowledge proof of knowledge.
	 */
	public SimplePlaintextPublicKeyPair(final ElGamalPublicKey primaryPublicKey, final ElGamalPublicKey secondaryPublicKey) {

		_primaryPublicKey = primaryPublicKey;
		_secondaryPublicKey = secondaryPublicKey;
	}

	public ElGamalPublicKey getPrimaryPublicKey() {

		return _primaryPublicKey;
	}

	public ElGamalPublicKey getSecondaryPublicKey() {

		return _secondaryPublicKey;
	}
}
