/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys.elgamal;

import java.security.KeyException;
import java.security.KeyPair;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.sdm.config.shares.keys.KeyPairGenerator;

public final class ElGamalKeyPairGenerator implements KeyPairGenerator {

	private final ElGamalServiceAPI elGamalServiceAPI;

	private final ElGamalEncryptionParameters encryptionParameters;

	private final int subkeyCount;

	public ElGamalKeyPairGenerator(final ElGamalEncryptionParameters encyptionParameters, int subkeyCount, ElGamalServiceAPI elGamalServiceAPI) {
		this.encryptionParameters = encyptionParameters;
		this.subkeyCount = subkeyCount;
		this.elGamalServiceAPI = elGamalServiceAPI;
	}

	@Override
	public KeyPair generate() throws KeyException {

		ElGamalKeyPair generatedKeyPair;
		try {
			generatedKeyPair = elGamalServiceAPI.generateKeyPair(encryptionParameters, subkeyCount);
		} catch (GeneralCryptoLibException e) {
			throw new KeyException("An error occurred while generating the ElGamal key pair", e);
		}
		return new KeyPair(new ElGamalPublicKeyAdapter(generatedKeyPair.getPublicKeys()),
				new ElGamalPrivateKeyAdapter(generatedKeyPair.getPrivateKeys()));
	}
}
