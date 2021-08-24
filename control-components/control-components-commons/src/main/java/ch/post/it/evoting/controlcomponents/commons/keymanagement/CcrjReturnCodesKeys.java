/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Container for the CCR_j Return Codes keys.
 */
public final class CcrjReturnCodesKeys {

	private final ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey;
	private final ElGamalPublicKey ccrjReturnCodesGenerationPublicKey;
	private final byte[] ccrjReturnCodesGenerationPublicKeySignature;

	private final ElGamalPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey;
	private final ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey;
	private final byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature;

	private final ElGamalEncryptionParameters encryptionParameters;

	private CcrjReturnCodesKeys(final ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey, final ElGamalPublicKey ccrjReturnCodesGenerationPublicKey,
			final byte[] ccrjReturnCodesGenerationPublicKeySignature, final ElGamalPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey,
			final ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey, final byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature,
			final ElGamalEncryptionParameters encryptionParameters) {

		this.ccrjReturnCodesGenerationSecretKey = ccrjReturnCodesGenerationSecretKey;
		this.ccrjReturnCodesGenerationPublicKey = ccrjReturnCodesGenerationPublicKey;
		this.ccrjReturnCodesGenerationPublicKeySignature = ccrjReturnCodesGenerationPublicKeySignature;
		this.ccrjChoiceReturnCodesEncryptionSecretKey = ccrjChoiceReturnCodesEncryptionSecretKey;
		this.ccrjChoiceReturnCodesEncryptionPublicKey = ccrjChoiceReturnCodesEncryptionPublicKey;
		this.ccrjChoiceReturnCodesEncryptionPublicKeySignature = ccrjChoiceReturnCodesEncryptionPublicKeySignature;
		this.encryptionParameters = encryptionParameters;
	}

	public ElGamalPrivateKey getCcrjChoiceReturnCodesEncryptionSecretKey() {
		return ccrjChoiceReturnCodesEncryptionSecretKey;
	}

	public ElGamalPublicKey getCcrjChoiceReturnCodesEncryptionPublicKey() {
		return ccrjChoiceReturnCodesEncryptionPublicKey;
	}

	public byte[] getCcrjChoiceReturnCodesEncryptionPublicKeySignature() {
		return ccrjChoiceReturnCodesEncryptionPublicKeySignature;
	}

	public ElGamalEncryptionParameters getEncryptionParameters() {
		return encryptionParameters;
	}

	public ElGamalPrivateKey getCcrjReturnCodesGenerationSecretKey() {
		return ccrjReturnCodesGenerationSecretKey;
	}

	public ElGamalPublicKey getCcrjReturnCodesGenerationPublicKey() {
		return ccrjReturnCodesGenerationPublicKey;
	}

	public byte[] getCcrjReturnCodesGenerationPublicKeySignature() {
		return ccrjReturnCodesGenerationPublicKeySignature;
	}

	/**
	 * Builder for creating {@link CcrjReturnCodesKeys} instances.
	 */
	static class Builder {

		private ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey;
		private ElGamalPublicKey ccrjReturnCodesGenerationPublicKey;
		private byte[] ccrjReturnCodesGenerationPublicKeySignature;

		private ElGamalPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey;
		private ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey;
		private byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature;

		public CcrjReturnCodesKeys build() {
			final ZpSubgroup group = ccrjReturnCodesGenerationSecretKey.getGroup();

			final ElGamalEncryptionParameters encryptionParameters;
			try {
				encryptionParameters = new ElGamalEncryptionParameters(group.getP(), group.getQ(), group.getG());
			} catch (GeneralCryptoLibException e) {
				throw new IllegalStateException("Failed to get encryption parameters from the CCR_j Return Codes generation private key.", e);
			}

			return new CcrjReturnCodesKeys(ccrjReturnCodesGenerationSecretKey, ccrjReturnCodesGenerationPublicKey,
					ccrjReturnCodesGenerationPublicKeySignature, ccrjChoiceReturnCodesEncryptionSecretKey, ccrjChoiceReturnCodesEncryptionPublicKey,
					ccrjChoiceReturnCodesEncryptionPublicKeySignature, encryptionParameters);
		}

		public Builder setCcrjChoiceReturnCodesEncryptionKeys(final ElGamalPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey,
				final ElGamalPublicKey ccrjChoiceReturnCodesEncryptionPublicKey, byte[] ccrjChoiceReturnCodesEncryptionPublicKeySignature) {
			this.ccrjChoiceReturnCodesEncryptionSecretKey = ccrjChoiceReturnCodesEncryptionSecretKey;
			this.ccrjChoiceReturnCodesEncryptionPublicKey = ccrjChoiceReturnCodesEncryptionPublicKey;
			this.ccrjChoiceReturnCodesEncryptionPublicKeySignature = ccrjChoiceReturnCodesEncryptionPublicKeySignature;
			return this;
		}

		public Builder setCcrjReturnCodesGenerationKeys(final ElGamalPrivateKey ccrjReturnCodesGenerationSecretKey,
				final ElGamalPublicKey ccrjReturnCodesGenerationPublicKey, final byte[] ccrjReturnCodesGenerationPublicKeySignature) {
			this.ccrjReturnCodesGenerationSecretKey = ccrjReturnCodesGenerationSecretKey;
			this.ccrjReturnCodesGenerationPublicKey = ccrjReturnCodesGenerationPublicKey;
			this.ccrjReturnCodesGenerationPublicKeySignature = ccrjReturnCodesGenerationPublicKeySignature;
			return this;
		}
	}
}
