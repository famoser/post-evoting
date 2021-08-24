/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.handler;

import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.cryptolib.api.secretsharing.ThresholdSecretSharingServiceAPI;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalDecrypter;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.sdm.config.multishare.ElGamalPrivateKeySharesService;
import ch.post.it.evoting.sdm.config.multishare.MultipleSharesContainer;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.exception.SmartcardException;
import ch.post.it.evoting.sdm.config.shares.keys.PrivateKeySerializer;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalPrivateKeyAdapter;
import ch.post.it.evoting.sdm.config.shares.keys.elgamal.ElGamalPublicKeyAdapter;
import ch.post.it.evoting.sdm.config.shares.service.SmartCardService;

/**
 * Stateless implementation of a read shares handler.
 */
public class StatelessReadSharesHandler {

	private final PrivateKeySerializer privateKeySerializer;

	private final SmartCardService smartcardService;

	private final AsymmetricServiceAPI asymmetricServiceAPI;

	private final ElGamalServiceAPI elGamalServiceAPI;

	private final ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI;

	public StatelessReadSharesHandler(final PrivateKeySerializer privateKeySerializer, final SmartCardService smartcardService,
			AsymmetricServiceAPI asymmetricService, ElGamalServiceAPI elGamalService,
			ThresholdSecretSharingServiceAPI thresholdSecretSharingServiceAPI) {
		this.privateKeySerializer = privateKeySerializer;
		this.smartcardService = smartcardService;
		this.asymmetricServiceAPI = asymmetricService;
		this.elGamalServiceAPI = elGamalService;
		this.thresholdSecretSharingServiceAPI = thresholdSecretSharingServiceAPI;
	}

	public Share readShare(final String pin, final PublicKey issuerPublicKey) throws SharesException {
		Share share;
		try {
			share = smartcardService.read(pin, issuerPublicKey);
		} catch (SmartcardException e) {
			throw new SharesException("An error occurred while reading the smartcard", e);
		}

		return share;
	}

	public Share readShareElGamal(final String pin, final PublicKey issuerPublicKey) throws SharesException {
		Share share;
		try {
			share = smartcardService.readElGamal(pin, issuerPublicKey);
		} catch (SmartcardException e) {
			throw new SharesException("An error occurred while reading the smartcard", e);
		}

		return share;
	}

	/**
	 * Returns a base64 encoded string, which contains the serialized share. The issuer public key is used to verify the signature of the smartcard.
	 */
	public String readShareAndStringify(final String pin, final PublicKey issuerPublicKey) throws SharesException {
		Share share = readShare(pin, issuerPublicKey);
		byte[] serializedShare = thresholdSecretSharingServiceAPI.serialize(share);

		return new String(Base64.getEncoder().encode(serializedShare), StandardCharsets.UTF_8);
	}

	public String readShareAndStringifyElGamal(final String pin, final PublicKey issuerPublicKey) throws SharesException {
		Share share = readShareElGamal(pin, issuerPublicKey);

		return Base64.getEncoder().encodeToString(thresholdSecretSharingServiceAPI.serialize(share));
	}

	public PrivateKey getPrivateKey(final Set<Share> shares, final PublicKey subjectPublicKey) throws SharesException {
		PrivateKey privateKey;
		try {

			byte[] recovered = thresholdSecretSharingServiceAPI.recover(shares);
			privateKey = privateKeySerializer.reconstruct(recovered, subjectPublicKey);

			validateKeyPair(subjectPublicKey, privateKey);
		} catch (KeyException | GeneralCryptoLibException e) {
			throw new SharesException("There was an error reconstructing the private key from the shares", e);
		}

		return privateKey;
	}

	/**
	 * Reconstructs key given a set of serialized shares in base64 encoded format, and the corresponding public key.
	 */
	public PrivateKey getPrivateKeyWithSerializedShares(final Set<String> serializedShares, final PublicKey subjectPublicKey) throws SharesException {

		final Set<Share> shares = new HashSet<>();

		for (String serializedShare : serializedShares) {
			byte[] shareBytes = Base64.getDecoder().decode(serializedShare);
			Share share;
			try {
				share = thresholdSecretSharingServiceAPI.deserialize(shareBytes);
			} catch (GeneralCryptoLibException e) {
				throw new SharesException("There was an error while deserializing the shares", e);
			}
			shares.add(share);
		}

		return getPrivateKey(shares, subjectPublicKey);
	}

	public ElGamalPrivateKey getPrivateKeyWithSerializedSharesElGamal(final Set<String> serializedShares, final ElGamalPublicKey elGamalPublicKey)
			throws SharesException {

		final List<MultipleSharesContainer> multipleSharesContainers = new ArrayList<>();

		for (String serializedShare : serializedShares) {
			byte[] shareBytes = Base64.getDecoder().decode(serializedShare.getBytes(StandardCharsets.UTF_8));

			MultipleSharesContainer multipleSharesContainer;
			multipleSharesContainer = new MultipleSharesContainer(shareBytes, MultipleSharesContainer.getModulusFromSerializedData(shareBytes));
			multipleSharesContainers.add(multipleSharesContainer);
		}

		ElGamalPrivateKeySharesService service = new ElGamalPrivateKeySharesService();
		return service.recover(multipleSharesContainers, elGamalPublicKey);
	}

	public String getSmartcardLabel() throws SharesException {
		try {
			return smartcardService.readSmartcardLabel();
		} catch (SmartcardException e) {
			throw new SharesException("Error while trying to read the smartcard label", e);
		}
	}

	private void validateKeyPair(final PublicKey subjectPublicKey, final PrivateKey privateKey) throws SharesException {

		if ("EL_GAMAL".equals(subjectPublicKey.getAlgorithm())) {

			// The only number that will be member of any given group
			String valueToEncrypt = "1";

			ElGamalPublicKeyAdapter elGamalPublicKeyAdapter = (ElGamalPublicKeyAdapter) subjectPublicKey;
			ElGamalPublicKey elGamalPublicKey = elGamalPublicKeyAdapter.getPublicKey();

			ElGamalPrivateKeyAdapter elGamalPrivateKeyAdapter = (ElGamalPrivateKeyAdapter) privateKey;
			ElGamalPrivateKey elGamalPrivateKey = elGamalPrivateKeyAdapter.getPrivateKey();

			CryptoAPIElGamalEncrypter encrypter;
			List<ZpGroupElement> decrypted;

			try {
				encrypter = elGamalServiceAPI.createEncrypter(elGamalPublicKey);
				List<String> toEncrypt = new ArrayList<>();
				toEncrypt.add(valueToEncrypt);
				ElGamalEncrypterValues elGamalEncrypterValues = encrypter.encryptStrings(toEncrypt);

				CryptoAPIElGamalDecrypter decrypter = elGamalServiceAPI.createDecrypter(elGamalPrivateKey);
				decrypted = decrypter.decrypt(elGamalEncrypterValues.getElGamalCiphertext(), true);

			} catch (GeneralCryptoLibException e) {
				throw new SharesException("There was an error while validating the reconstructed private key with the given public key", e);
			}
			String decryptedValue = decrypted.get(0).getValue().toString();

			if (!valueToEncrypt.equals(decryptedValue)) {
				throw new SharesException("There was an error validating the reconstructed private key with the given public key");
			}

		} else {

			final String testString = "foobar";

			byte[] encryptedTestString;
			byte[] decrypted;

			try {
				encryptedTestString = asymmetricServiceAPI.encrypt(subjectPublicKey, testString.getBytes(StandardCharsets.UTF_8));
				decrypted = asymmetricServiceAPI.decrypt(privateKey, encryptedTestString);
			} catch (GeneralCryptoLibException e) {
				throw new SharesException("There was an error while validating the reconstructed private key with the given public key", e);
			}
			String decryptedTestString = new String(decrypted, StandardCharsets.UTF_8);

			if (!testString.equals(decryptedTestString)) {
				throw new SharesException("There was an error validating the reconstructed private key with the given public key");
			}
		}

	}

}
