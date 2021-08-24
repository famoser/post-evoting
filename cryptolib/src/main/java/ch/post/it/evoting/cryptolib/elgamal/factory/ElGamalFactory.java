/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import ch.post.it.evoting.cryptolib.CryptolibFactory;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.SecureRandomFactory;

/**
 * Factory class for generators, encryptor and decryptor.
 */
public final class ElGamalFactory extends CryptolibFactory {

	private final CryptoRandomInteger cryptoRandomInteger;

	/**
	 * Constructs a CryptoElGamalFactory.
	 *
	 * @param elGamalPolicy the policy to set in this factory.
	 */
	public ElGamalFactory(final ElGamalPolicy elGamalPolicy) {

		cryptoRandomInteger = new SecureRandomFactory(elGamalPolicy).createIntegerRandom();
	}

	/**
	 * Creates a {@link CryptoElGamalKeyPairGenerator} which can be used to generate a {@link ElGamalKeyPair}.
	 *
	 * <p>Both keys of the pair (the public and the private keys) making up a CryptoElGamalKeyPair
	 * will themselves be composed of a number of parts.
	 *
	 * @return a new CryptoElGamalKeyPairGenerator.
	 */
	public CryptoElGamalKeyPairGenerator createCryptoElGamalKeyPairGenerator() {

		return new CryptoElGamalKeyPairGenerator(cryptoRandomInteger);
	}

	/**
	 * Creates a {@link CryptoElGamalEncrypter}.
	 *
	 * @param elGamalPublicKey the public key that should be set in the created CryptoElGamalEncrypter.
	 * @return a new {@link CryptoElGamalEncrypter}.
	 */
	public CryptoElGamalEncrypter createEncrypter(final ElGamalPublicKey elGamalPublicKey) {

		return new CryptoElGamalEncrypter(elGamalPublicKey, cryptoRandomInteger);
	}

	/**
	 * Creates a {@link CryptoElGamalDecrypter}.
	 *
	 * @param elGamalPrivateKey the private keys that should be set in the created CryptoElGamalDecrypter.
	 * @return a new CryptoElGamalDecrypter.
	 */
	public CryptoElGamalDecrypter createDecrypter(final ElGamalPrivateKey elGamalPrivateKey) {

		return new CryptoElGamalDecrypter(elGamalPrivateKey);
	}

}
