/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.keys.elgamal;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.sdm.config.shares.exception.ConfigSharesException;

public final class ElGamalPublicKeyAdapter implements PublicKey {

	private static final long serialVersionUID = 2142776796684568176L;

	private final ElGamalPublicKey publicKey;

	/**
	 * @param publicKey the ElGamal public key to set in this adapter.
	 */
	public ElGamalPublicKeyAdapter(final ElGamalPublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public ElGamalPublicKey getPublicKey() {
		return publicKey;
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
			return publicKey.toJson().getBytes(StandardCharsets.UTF_8);
		} catch (GeneralCryptoLibException e) {
			throw new ConfigSharesException("Error while trying to get the encoding of the ElGamal public key", e);
		}
	}
}
