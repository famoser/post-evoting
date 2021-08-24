/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalDecrypter;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.ExponentCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Implementation of an ElGamal decrypter.
 */
public final class CryptoElGamalDecrypter implements CryptoAPIElGamalDecrypter {

	private final ElGamalPrivateKey elGamalPrivateKey;

	private final int elGamalPrivateKeyLength;

	/**
	 * Creates an ElGamal decrypter.
	 *
	 * @param elGamalPrivateKey the ElGamal private key.
	 */
	CryptoElGamalDecrypter(final ElGamalPrivateKey elGamalPrivateKey) {

		super();

		this.elGamalPrivateKey = elGamalPrivateKey;
		elGamalPrivateKeyLength = this.elGamalPrivateKey.getKeys().size();
	}

	@Override
	public List<ZpGroupElement> decrypt(final ElGamalCiphertext ciphertext, final boolean validateGroupMembership) throws GeneralCryptoLibException {

		Validate.notNull(ciphertext, "ElGamal ciphertext");
		Validate.notGreaterThan(ciphertext.getPhis().size(), elGamalPrivateKeyLength, "ElGamal ciphertext length", "decrypter private key length");
		if (validateGroupMembership) {
			validateGroupMembership(ciphertext);
		}

		ElGamalPrivateKey keysAfterCompression = compressKeysIfNecessary(elGamalPrivateKey, ciphertext.getPhis().size());

		List<ZpGroupElement> plaintext = new ArrayList<>(keysAfterCompression.getKeys().size());

		// Perform decryption
		Exponent negatedExponent;
		for (int i = 0; i < ciphertext.getPhis().size(); i++) {

			// Compute the e = negate (-) of privKey[i]
			negatedExponent = keysAfterCompression.getKeys().get(i).negate();

			// Compute dm[i]= gamma^(e) * phi[i]
			plaintext.add(ciphertext.getGamma().exponentiate(negatedExponent).multiply(ciphertext.getPhis().get(i)));
		}

		return plaintext;
	}

	private ElGamalPrivateKey compressKeysIfNecessary(final ElGamalPrivateKey elGamalPrivateKey, final int numMessages)
			throws GeneralCryptoLibException {

		List<Exponent> keys = elGamalPrivateKey.getKeys();

		if (keys.size() <= numMessages) {
			return elGamalPrivateKey;
		}

		ExponentCompressor<ZpSubgroup> compressor = new ExponentCompressor<>(this.elGamalPrivateKey.getGroup());

		List<Exponent> listWithCompressedFinalElement = compressor.buildListWithCompressedFinalElement(numMessages, keys);

		return new ElGamalPrivateKey(listWithCompressedFinalElement, this.elGamalPrivateKey.getGroup());
	}

	private void validateGroupMembership(final ElGamalCiphertext ciphertext) throws GeneralCryptoLibException {

		final boolean containsNotMember = ciphertext.getValues().stream()
				.anyMatch(zpGroupElement -> !elGamalPrivateKey.getGroup().isGroupMember(zpGroupElement));

		if (containsNotMember) {
			throw new GeneralCryptoLibException(
					"ElGamal ciphertext contains one or more elements that do not belong to mathematical group of decrypter private key.");
		}
	}
}
