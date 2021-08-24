/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys.elgamal;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.sdm.config.shares.exception.ConfigSharesException;

public final class ElGamalPrivateKeyAdapter implements PrivateKey {

	private static final long serialVersionUID = 1392776796610868176L;

	private final ElGamalPrivateKey privateKey;

	/**
	 * @param privateKey the ElGamal private key to set in this adapter.
	 */
	public ElGamalPrivateKeyAdapter(final ElGamalPrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public ElGamalPrivateKey getPrivateKey() {
		return privateKey;
	}

	@Override
	public String getAlgorithm() {
		return "EL_GAMAL";
	}

	@Override
	public String getFormat() {
		return "EL_GAMAL";
	}

	@Override
	public byte[] getEncoded() {
		try {
			return privateKey.toJson().getBytes(StandardCharsets.UTF_8);
		} catch (GeneralCryptoLibException e) {
			throw new ConfigSharesException("Error while trying to get the encoding of the ElGamal private key", e);
		}
	}
}
