/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.api.elgamal;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalDecrypter;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;

/**
 * Defines the API offered by the ElGamal service.
 */
public interface ElGamalServiceAPI {

	/**
	 * Generates an ElGamal key pair for the provided encryption parameters.
	 *
	 * @param encryptionParameters information about the group the key pair should be generated for
	 * @param keyCount             how many keys should be generated
	 * @return an ElGamal key pair
	 * @throws GeneralCryptoLibException if the key pair creation did not succeed
	 */
	ElGamalKeyPair generateKeyPair(ElGamalEncryptionParameters encryptionParameters, int keyCount) throws GeneralCryptoLibException;

	/**
	 * Creates a {@link CryptoAPIElGamalEncrypter} that can be used for encrypting data. The created CryptoAPIElGamalEncrypter will have the specified
	 * public key set, and this key will always be used for encrypting data using that CryptoAPIElGamalEncrypter.
	 *
	 * @param publicKey the public key to be set in the created CryptoAPIElGamalEncrypter.
	 * @return a newly created {@link CryptoAPIElGamalEncrypter}.
	 * @throws GeneralCryptoLibException if the public key is null or if any of its elements belong to a different group.
	 */
	CryptoAPIElGamalEncrypter createEncrypter(final ElGamalPublicKey publicKey) throws GeneralCryptoLibException;

	/**
	 * Creates a {@link CryptoAPIElGamalDecrypter} that can be used for decrypting data. The created {@link CryptoAPIElGamalDecrypter} will have the
	 * specified private key set, and this key will always be used for decrypting data using that CryptoAPIElGamalDecrypter.
	 *
	 * @param privateKey the private key to be set in the created CryptoElGamalDecrypter.
	 * @return a newly created {@link CryptoAPIElGamalDecrypter}.
	 * @throws GeneralCryptoLibException if the private key is null or if any of its exponents belong to a different group.
	 * @see #createEncrypter(ElGamalPublicKey)
	 */
	CryptoAPIElGamalDecrypter createDecrypter(final ElGamalPrivateKey privateKey) throws GeneralCryptoLibException;

	/**
	 * Checks whether the parameters of the provided group match the current policy.
	 *
	 * @param group the group to check
	 * @throws IllegalArgumentException if the group does not match the current policy
	 */
	void checkGroupPolicy(MathematicalGroup<?> group);
}
