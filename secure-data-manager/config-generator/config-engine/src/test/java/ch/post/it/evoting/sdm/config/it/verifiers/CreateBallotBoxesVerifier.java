/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.it.verifiers;

import static java.util.Arrays.fill;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore.PasswordProtection;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.security.auth.DestroyFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.factory.CryptoX509Certificate;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.domain.election.BallotBox;
import ch.post.it.evoting.domain.election.BallotBoxContextData;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.it.utils.CertificateValidator;
import ch.post.it.evoting.sdm.config.it.utils.KeyLoader;
import ch.post.it.evoting.sdm.config.it.utils.KeyPairValidator;
import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.PasswordEncrypter;

/**
 * A class to execute all validations related with Create Ballot Boxes command
 */
public class CreateBallotBoxesVerifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateBallotBoxesVerifier.class);

	private final ConfigObjectMapper configObjectMapper = new ConfigObjectMapper();

	private final KeyPairValidator keyPairValidator = new KeyPairValidator();

	private final ExtendedKeyStoreService extendedKeyStoreService;

	private final Map<String, CryptoX509Certificate> loadedCertificates;

	private final ConfigurationInputReader configurationInputReader = new ConfigurationInputReader();

	public CreateBallotBoxesVerifier(final Map<String, CryptoX509Certificate> loadedCertificates) {

		extendedKeyStoreService = new ExtendedKeyStoreService();
		this.loadedCertificates = loadedCertificates;
	}

	public void createBallotBoxesValidations(final File eeidFolder, final String bid, final String bbid,
			final PrivateKey privateKeyToDecryptKeystorePassword) throws IOException, GeneralCryptoLibException, CertificateException {

		List<String> bbIdsList = new ArrayList<>();
		bbIdsList.add(bbid);

		onlineFolderValidations(eeidFolder, bid, bbIdsList, privateKeyToDecryptKeystorePassword);
	}

	private void onlineFolderValidations(final File eeidFolder, final String bid, final List<String> bbIdsList,
			final PrivateKey privateKeyToDecryptKeystorePassword) throws IOException, CertificateException, GeneralCryptoLibException {

		Path ballotsBoxesFolder = Paths
				.get(eeidFolder.getAbsolutePath(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
						Constants.CONFIG_DIR_NAME_BALLOTS, bid, Constants.CONFIG_DIR_NAME_BALLOTBOXES);
		assertTrue(Files.exists(ballotsBoxesFolder));

		for (String bbid : bbIdsList) {

			ReplacementsHolder replacementsHolder = new ReplacementsHolder(eeidFolder.getName(), bbid);
			CertificateValidator certificateValidator = new CertificateValidator(replacementsHolder);

			Path ballotBoxIdFolder = Paths.get(ballotsBoxesFolder.toString(), bbid);
			assertTrue(Files.exists(ballotBoxIdFolder));

			Path ballotBoxJSONPath = Paths.get(ballotBoxIdFolder.toString(), Constants.CONFIG_DIR_NAME_BALLOTBOX_JSON);
			assertTrue(Files.exists(ballotBoxJSONPath));

			BallotBox ballotBox = configObjectMapper.fromJSONFileToJava(ballotBoxJSONPath.toFile(), BallotBox.class);
			assertEquals(bid, ballotBox.getBid());
			assertEquals(bbid, ballotBox.getId());

			// cert chain validation
			// 3) electionEventCA -> servicesCA -> ballotBox
			CryptoX509Certificate ballotBoxCert = KeyLoader.convertPEMStringtoCryptoX509Certificate(ballotBox.getBallotBoxCert());

			certificateValidator.validateCert(ballotBoxCert, getBallotBoxCredentialProperties());

			assertEquals(0,
					certificateValidator.checkChain(loadedCertificates.get("electioneventca"), loadedCertificates.get("servicesca"), ballotBoxCert)
							.size());

			Path ballotBoxContentDataJSONPath = Paths.get(ballotBoxIdFolder.toString(), Constants.CONFIG_DIR_NAME_BALLOTBOX_CONTEXT_DATA_JSON);
			assertTrue(Files.exists(ballotBoxContentDataJSONPath));

			BallotBoxContextData ballotBoxContextData = configObjectMapper
					.fromJSONFileToJava(ballotBoxContentDataJSONPath.toFile(), BallotBoxContextData.class);
			assertEquals(bbid, ballotBoxContextData.getId());

			byte[] decodedKeystore = Base64.getDecoder().decode(ballotBoxContextData.getKeystore().getBytes(StandardCharsets.UTF_8));
			InputStream in = new ByteArrayInputStream(decodedKeystore);

			char[] keystorePassword = getKeystorePassword(ballotBoxContextData.getPasswordKeystore(), privateKeyToDecryptKeystorePassword);
			PasswordProtection passwordProtection = new PasswordProtection(keystorePassword);

			CryptoAPIExtendedKeyStore keystore;
			try {
				keystore = extendedKeyStoreService.loadKeyStoreFromJSON(in, passwordProtection);
			} finally {
				fill(keystorePassword, ' ');
				try {
					passwordProtection.destroy();
				} catch (DestroyFailedException e) {
					LOGGER.warn("Failed to destroy password", e);
				}
			}

			assertEquals(1, keystore.getPrivateKeyAliases().size());
			String alias = keystore.getPrivateKeyAliases().get(0);
			Certificate[] certChain = keystore.getCertificateChain(alias);

			CryptoX509Certificate[] cryptoX509Certificates = KeyLoader.fromCertificateArrayToCryptoCertificateArray(certChain);

			// the order of certs to be passed to checkChain method is: root,
			// intermediate, leaf
			assertEquals(0, certificateValidator.checkChain(cryptoX509Certificates[2], cryptoX509Certificates[1], cryptoX509Certificates[0]).size());

			PrivateKey privateKey = keystore.getPrivateKeyEntry(alias, ballotBoxContextData.getPasswordKeystore().toCharArray());
			PublicKey publicKey = certChain[0].getPublicKey();
			keyPairValidator.validateKeyPair(publicKey, privateKey);
		}
	}

	private char[] getKeystorePassword(final String passwordKeystore, final PrivateKey privateKey) throws GeneralCryptoLibException {

		AsymmetricService asymmetricService = new AsymmetricService();

		PasswordEncrypter passwordEncrypter = new PasswordEncrypter(asymmetricService);

		return passwordEncrypter.decryptPassword(passwordKeystore, privateKey);
	}

	private CredentialProperties getBallotBoxCredentialProperties() throws IOException {
		Path keysConfigJSON = Paths.get("./src/test/resources/keys_config.json");
		ConfigurationInput configurationInput = configurationInputReader.fromFileToJava(keysConfigJSON.toFile());
		return configurationInput.getBallotBox();
	}
}
