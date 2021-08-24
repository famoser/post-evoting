/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys.elgamal;

import java.security.KeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.sdm.config.shares.keys.PrivateKeySerializer;

public final class ElGamalPrivateKeySerializer implements PrivateKeySerializer {

	private final ElGamalUtils elGamalUtils;

	public ElGamalPrivateKeySerializer() {
		this.elGamalUtils = new ElGamalUtils();
	}

	/**
	 * Serializes an ElGamal private key, using the utils class from the projectslib
	 *
	 * @throws IllegalArgumentException If the argument passed is not an ElGamal key
	 * @see ch.post.it.evoting.config.shares.keys.nsw.shares.applet.crypto.PrivateKeySerializer#serialize(java.security.PrivateKey)
	 */
	@Override
	public byte[] serialize(final PrivateKey privateKeyAdapter) {
		if (!(privateKeyAdapter instanceof ElGamalPrivateKeyAdapter)) {
			throw new IllegalArgumentException("The private key must be an El Gamal private key");
		}

		return elGamalUtils.serialize(((ElGamalPrivateKeyAdapter) privateKeyAdapter).getPrivateKey());
	}

	/**
	 * Reconstructs an ElGamal private key, using the utils class from the projectslib
	 *
	 * @see ch.post.it.evoting.config.shares.keys.nsw.shares.applet.crypto.PrivateKeySerializer#reconstruct(byte[], java.security.PublicKey)
	 */
	@Override
	public PrivateKey reconstruct(final byte[] recovered, final PublicKey publicKeyAdapter) throws KeyException {
		if (!(publicKeyAdapter instanceof ElGamalPublicKeyAdapter)) {
			throw new IllegalArgumentException("The public key must be an El Gamal public key");
		}

		try {
			ElGamalPrivateKey elGamalPrivateKey = elGamalUtils.reconstruct(((ElGamalPublicKeyAdapter) publicKeyAdapter).getPublicKey(), recovered);
			return new ElGamalPrivateKeyAdapter(elGamalPrivateKey);
		} catch (GeneralCryptoLibException e) {
			throw new KeyException("Reconstruction of El Gamal private key failed", e);
		}
	}
}
