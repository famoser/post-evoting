/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalKeyPairGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponents;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;

/**
 * Class for generating ElGamal key pairs (which are instances of {@link ElGamalKeyPair}).
 */
public final class CryptoElGamalKeyPairGenerator implements CryptoAPIElGamalKeyPairGenerator {

	private final CryptoRandomInteger cryptoRandomInteger;

	/**
	 * Creates a CryptoElGamalKeyGenerator, setting the specified {@link CryptoRandomInteger} as a source of random integers.
	 *
	 * @param cryptoRandomInteger a generator of random integers.
	 */
	CryptoElGamalKeyPairGenerator(final CryptoRandomInteger cryptoRandomInteger) {

		this.cryptoRandomInteger = cryptoRandomInteger;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is equivalent to the crypto-primitives' GenKeyPair method.
	 */
	@Override
	public ElGamalKeyPair generateKeys(final ElGamalEncryptionParameters encryptionParameters, final int length) throws GeneralCryptoLibException {

		Validate.notNull(encryptionParameters, "Mathematical group parameters object");
		Validate.isPositive(length, "Length of ElGamal key pair to generate");

		final ZpSubgroup group = (ZpSubgroup) encryptionParameters.getGroup();
		final ZpGroupElement generator = group.getGenerator();

		// Lists to store private and public keys.
		final List<Exponent> privateKeys = new ArrayList<>();
		final List<ZpGroupElement> publicKeys = new ArrayList<>();

		// Generate the public and the private keys (generate one of each of them during each iteration of the loop).
		for (int i = 0; i < length; i++) {

			// Construct a new random exponent between 0 and q-1, this value will be used as a private key.
			final Exponent randomExponent = Exponents.random(group, cryptoRandomInteger);

			// Add the newly generated private key to the list of private keys.
			privateKeys.add(randomExponent);

			// Generate the corresponding public key by raising the generator to the power of the private key.
			final ZpGroupElement groupElement = generator.exponentiate(randomExponent);

			// Add the newly generated public key to the list of public keys.
			publicKeys.add(groupElement);
		}

		return new ElGamalKeyPair(new ElGamalPrivateKey(privateKeys, group), new ElGamalPublicKey(publicKeys, group));
	}
}
