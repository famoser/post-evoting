/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.secretsharing.Share;
import ch.post.it.evoting.sdm.config.multishare.EncryptedMultipleSharesContainer;
import ch.post.it.evoting.sdm.config.multishare.MultipleSharesContainer;
import ch.post.it.evoting.sdm.config.shares.EncryptedShare;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.exception.SmartcardException;

/**
 * Implementation of the Smart Cards Services using the file system to write and read the Shares. The reason for the existence of this class is only
 * for providing a way to do and automatic e2e
 */
class FileSystemSmartCardService implements SmartCardService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemSmartCardService.class);

	private static final Path SMART_CARD_FOLDER = Paths.get(System.getProperty("user.home") + "/sdm/smart-cards");

	private static final Path SMART_CARD_FILE_PATH = Paths.get(SMART_CARD_FOLDER.toString(), "/smart-card.b64");

	@Override
	public void write(final Share share, final String name, final String oldPinPuk, final String newPinPuk, final PrivateKey signingPrivateKey)
			throws SmartcardException {

		try {
			EncryptedShare encryptedShare = new EncryptedShare(share, signingPrivateKey);
			Files.createDirectories(SMART_CARD_FOLDER);
			Path path = SMART_CARD_FILE_PATH;
			LOGGER.info("Saving smartcard to: {}", path.toAbsolutePath());
			Files.write(path, Arrays.asList(Base64.getEncoder().encodeToString(encryptedShare.getEncryptedShare()),
					Base64.getEncoder().encodeToString(encryptedShare.getEncryptedShareSignature()),
					Base64.getEncoder().encodeToString(encryptedShare.getSecretKeyBytes()), name));
		} catch (IOException e) {
			throw new SmartcardException(e.getMessage(), e);
		}
	}

	@Override
	public Share read(final String pin, final PublicKey signatureVerificationPublicKey) throws SmartcardException {

		try {
			List<String> lines = readFile();

			EncryptedShare es = new EncryptedShare(Base64.getDecoder().decode(lines.get(0)), Base64.getDecoder().decode(lines.get(1)),
					signatureVerificationPublicKey);
			return es.decrypt(Base64.getDecoder().decode(lines.get(2)));
		} catch (SharesException | IOException e) {
			throw new SmartcardException(e.getMessage(), e);
		}
	}

	/**
	 * Checks the status of the inserted smartcard.
	 *
	 * @return true if the smartcard status is satisfactory, false otherwise
	 */
	@Override
	public boolean isSmartcardOk() {

		return Files.exists(SMART_CARD_FILE_PATH);
	}

	/**
	 * Read the smartcard label
	 *
	 * @return the label written to the smartcard
	 * @throws SmartcardException if an {@link IOException} occurs.
	 */
	@Override
	public String readSmartcardLabel() throws SmartcardException {
		try {
			List<String> lines = readFile();
			return lines.get(3);
		} catch (IOException e) {
			throw new SmartcardException(e.getMessage(), e);
		}
	}

	private List<String> readFile() throws IOException {
		Files.createDirectories(SMART_CARD_FOLDER);
		return Files.readAllLines(SMART_CARD_FILE_PATH);
	}

	@Override
	public MultipleSharesContainer readElGamal(String pin, PublicKey signatureVerificationPublicKey) throws SmartcardException {
		try {
			List<String> lines = readFile();
			EncryptedMultipleSharesContainer es = new EncryptedMultipleSharesContainer(Base64.getDecoder().decode(lines.get(0)),
					Base64.getDecoder().decode(lines.get(1)), signatureVerificationPublicKey);
			return es.decrypt(Base64.getDecoder().decode(lines.get(2)));
		} catch (SharesException | IOException e) {
			throw new SmartcardException(e.getMessage(), e);
		}
	}

}
