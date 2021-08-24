/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.cryptoapi;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;

/**
 * Defines the methods provided by ElGamal Key pair generators.
 */
public interface CryptoAPIElGamalKeyPairGenerator {

	/**
	 * Generates an {@link ElGamalKeyPair} with the specified Zp subgroup and number of keys.
	 *
	 * <p>The specified Zp subgroup must be an initialized and valid group.
	 *
	 * @param encryptionParameters The {@link ElGamalEncryptionParameters} that should should be used for generating the ElGamal key pair.
	 * @param length               The number of components that each key (the public key and the private key) should be composed of. This value must
	 *                             be 1 or more.
	 * @return The generated ElGamal key pair, encapsulated within a {@link ElGamalKeyPair} object.
	 * @throws GeneralCryptoLibException if the {@link ElGamalEncryptionParameters} object or the mathematical group that it contains is null, or if
	 *                                   the specified length of the key pair to generate is less than 1.
	 */
	ElGamalKeyPair generateKeys(final ElGamalEncryptionParameters encryptionParameters, final int length) throws GeneralCryptoLibException;
}
