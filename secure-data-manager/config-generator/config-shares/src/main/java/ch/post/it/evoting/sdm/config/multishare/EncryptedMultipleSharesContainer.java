/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.multishare;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingServiceAPI;
import ch.post.it.evoting.cryptolib.commons.destroy.SecretKeyDestroyer;
import ch.post.it.evoting.cryptolib.secretsharing.service.ThresholdSecretSharingService;
import ch.post.it.evoting.sdm.config.shares.EncryptedShare;
import ch.post.it.evoting.sdm.config.shares.SharesCrypto;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;

/**
 * Note: this class is based on the class {@link EncryptedShare}. This class serves the same purpose for MultipleSharesContainer as EncryptedShare
 * does for Share.
 */
public class EncryptedMultipleSharesContainer {

	// public part
	private final transient byte[] encryptedShare;

	// public part
	private final transient byte[] encryptedShareSignature;

	// private part
	private transient byte[] secretKeyBytes;

	// cryptographic helper
	private SharesCrypto sharesCrypto;

	private ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI;

	/**
	 * Constructor in decryption mode: The information is retrieved from the smartcard, and this class will be used to validate and decrypt the
	 * share.
	 * <p>
	 * The constructor will verify the signature before finishing, and throw a {@link SharesException} if the signature is not correct. See {@link
	 * #decrypt(byte[])}.
	 *
	 * @param encryptedShare          : byte[] with the encrypted share.
	 * @param encryptedShareSignature byte[] with a signature over the encryptedShare
	 * @param boardPublic             : Public key to verify the encryptedShareSignature
	 * @throws SharesException if the signature is not correct
	 */
	public EncryptedMultipleSharesContainer(final byte[] encryptedShare, final byte[] encryptedShareSignature, final PublicKey boardPublic)
			throws SharesException {

		initServices();

		this.encryptedShare = clone(encryptedShare);
		this.encryptedShareSignature = clone(encryptedShareSignature);
		if (!sharesCrypto.verifyShare(this.encryptedShare, this.encryptedShareSignature, boardPublic)) {
			throw new SharesException("This share does not belong to this board");
		}
	}

	/**
	 * Constructor in encryption mode: The information to encrypt is provided in the {@link Share} object, and the {@link PrivateKey} is used to
	 * encrypt it. The constructor generates a {@link SecretKey} which is used to encrypt the {@link Share} content.
	 * <p>
	 * It is the responsibility of the caller to control the {@link PrivateKey} and {@link Share} life cycles, which contain sensitive information.
	 * This method will not make any copy of them.
	 *
	 * @param share        the {@link Share} to encrypt.
	 * @param boardPrivate the board private key.
	 */
	public EncryptedMultipleSharesContainer(final Share share, final PrivateKey boardPrivate) {
		initServices();

		SecretKeyDestroyer secretKeyDestroyer = new SecretKeyDestroyer();
		SecretKey shareSecretKey = sharesCrypto.generateSecretKey();
		secretKeyBytes = sharesCrypto.serializeSecretKey(shareSecretKey);
		encryptedShare = sharesCrypto.encryptShare(thresholdSecretSharingServiceAPI.serialize(share), shareSecretKey);
		secretKeyDestroyer.destroyInstances((SecretKeySpec) shareSecretKey);
		encryptedShareSignature = sharesCrypto.signShare(encryptedShare, boardPrivate);
	}

	/**
	 * Use the secret key serialized form to decrypt the content of the share.
	 * <p>
	 * The caller is responsible for the secret key life cycle. This method will not make any copies of it.
	 *
	 * @param secretKeyBytes The serialized secret key.
	 * @return The decrypted {@link Share} if the key is correct.
	 */
	public MultipleSharesContainer decrypt(final byte[] secretKeyBytes) throws SharesException {
		if (secretKeyBytes == null) {
			return null;
		}
		byte[] shareDecrypted = sharesCrypto.decryptShare(encryptedShare, secretKeyBytes);
		return new MultipleSharesContainer(shareDecrypted, MultipleSharesContainer.getModulusFromSerializedData(shareDecrypted));
	}

	/**
	 * Overwrite all the object attributes with 0x00 in memory. This method must always be called in order to guarantee no memory analysis can reveal
	 * the secret.
	 */
	public void destroy() {
		if (secretKeyBytes != null) {
			Arrays.fill(secretKeyBytes, (byte) 0x00);
		}
		if (encryptedShare != null) {
			Arrays.fill(encryptedShare, (byte) 0x00);
		}
		if (encryptedShareSignature != null) {
			Arrays.fill(encryptedShareSignature, (byte) 0x00);
		}
	}

	/**
	 * Return the encrypted share content as a byte[].
	 *
	 * @return Returns the encryptedShare.
	 */
	public byte[] getEncryptedShare() {
		return encryptedShare.clone();
	}

	private byte[] clone(final byte[] value) {
		if (value == null) {
			return null;
		} else {
			return value.clone();
		}
	}

	private void initServices() {
		this.thresholdSecretSharingServiceAPI = new ThresholdSecretSharingService();
		this.sharesCrypto = new SharesCrypto();
	}

}
