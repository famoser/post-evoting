/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;

/**
 * Container for the CCM_j election keys
 */
class CcmjElectionKeys {

	private final ElGamalPrivateKey ccmjElectionSecretKey;
	private final ElGamalPublicKey ccmjElectionPublicKey;
	private final byte[] ccmjElectionPublicKeySignature;

	public CcmjElectionKeys(final ElGamalPrivateKey ccmjElectionSecretKey, final ElGamalPublicKey ccmjElectionPublicKey,
			final byte[] ccmjElectionPublicKeySignature) {

		this.ccmjElectionSecretKey = ccmjElectionSecretKey;
		this.ccmjElectionPublicKey = ccmjElectionPublicKey;
		this.ccmjElectionPublicKeySignature = ccmjElectionPublicKeySignature;
	}

	public ElGamalPrivateKey getCcmjElectionSecretKey() {
		return ccmjElectionSecretKey;
	}

	public ElGamalPublicKey getCcmjElectionPublicKey() {
		return ccmjElectionPublicKey;
	}

	public byte[] getCcmjElectionPublicKeySignature() {
		return ccmjElectionPublicKeySignature;
	}
}
