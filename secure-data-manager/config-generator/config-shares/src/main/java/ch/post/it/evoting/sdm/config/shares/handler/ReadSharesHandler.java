/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.handler;

import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingServiceAPI;
import ch.post.it.evoting.sdm.config.shares.domain.ReadSharesOperationContext;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.exception.SmartcardException;
import ch.post.it.evoting.sdm.config.shares.keys.PrivateKeySerializer;
import ch.post.it.evoting.sdm.config.shares.service.SmartCardService;

public final class ReadSharesHandler {

	private final ReadSharesOperationContext context;

	private final Set<Share> shares;

	private final PrivateKeySerializer privateKeySerializer;

	private final SmartCardService smartcardService;

	private final AsymmetricServiceAPI asymmetricServiceAPI;

	private final ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI;

	public ReadSharesHandler(final ReadSharesOperationContext context, final PrivateKeySerializer privateKeySerializer,
			final SmartCardService smartcardService, AsymmetricServiceAPI asymmetricServiceAPI,
			ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI) {
		this.context = context;
		this.privateKeySerializer = privateKeySerializer;
		this.shares = new HashSet<>();
		this.smartcardService = smartcardService;
		this.asymmetricServiceAPI = asymmetricServiceAPI;
		this.thresholdSecretSharingServiceAPI = thresholdSecretSharingServiceAPI;
	}

	public void readShare(final String pin) throws SharesException {

		Share share;
		try {
			share = smartcardService.read(pin, context.getAuthoritiesPublicKey());
			shares.add(share);
		} catch (SmartcardException e) {
			throw new SharesException("An error occurred while reading the smartcard", e);
		}
	}

	public PrivateKey getPrivateKey() throws SharesException {
		try {

			byte[] recovered = thresholdSecretSharingServiceAPI.recover(shares);

			PublicKey boardPublicKey = context.getBoardPublicKey();
			PrivateKey privateKey = privateKeySerializer.reconstruct(recovered, boardPublicKey);

			validateKeyPair(boardPublicKey, privateKey);

			return privateKey;

		} catch (KeyException e) {
			throw new SharesException("There was an error reconstructing the private key from the shares", e);
		} catch (GeneralCryptoLibException e) {
			throw new SharesException("There was an error reconstructing the secret from the shares", e);
		}
	}

	public String readSmartcardLabel() throws SharesException {

		try {
			return smartcardService.readSmartcardLabel();
		} catch (SmartcardException e) {
			throw new SharesException("Error while trying to read the smartcard label", e);
		}
	}

	public boolean isSmartcardOk() {
		return smartcardService.isSmartcardOk();
	}

	public int getTotalNumberOfCards() {

		if (shares.isEmpty()) {
			return 0;
		}

		return shares.iterator().next().getNumberOfParts();
	}

	public int getThreshold() {

		if (shares.isEmpty()) {
			return 0;
		}

		return shares.iterator().next().getThreshold();
	}

	private void validateKeyPair(final PublicKey subjectPublicKey, final PrivateKey privateKey) throws SharesException {

		final String testString = "foobar";
		byte[] encryptedTestString;
		byte[] decryptedTestString;

		try {
			encryptedTestString = asymmetricServiceAPI.encrypt(subjectPublicKey, testString.getBytes(StandardCharsets.UTF_8));
			decryptedTestString = asymmetricServiceAPI.decrypt(privateKey, encryptedTestString);
		} catch (GeneralCryptoLibException e) {
			throw new SharesException("There was an error while validating the reconstructed private key with the given public key", e);
		}
		final String decrypted = new String(decryptedTestString, StandardCharsets.UTF_8);

		if (!testString.equals(decrypted)) {
			throw new SharesException("There was an error validating the reconstructed private key with the given public key");
		}
	}

}
