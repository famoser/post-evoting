/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.it.utils.KeyPairValidator;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;
import ch.post.it.evoting.sdm.utils.X509CertificateLoader;

class VotersHolderInitializerTest {

	private static final String ELECTION_EVENT_ID = "314bd34dcf6e4de4b771a92fa3849d3d";
	private static final String NUMBER_VOTING_CARDS = "10";
	private static final String BALLOT_ID = "1234";
	private static final String BALLOT_BOX_ID = "5678";
	private static final String VOTING_CARD_SET_ID = "111111";
	private static final String VERIFICATION_CARD_SET_ID = "111111";
	private static final String VOTING_CARD_SET_ALIAS = "TESTALIAS";
	private static final String ELECTORAL_AUTHORITY_ID = "222222";
	private static final String SECURE_DATA_MANAGER_PRIVATE_PRIVATE_KEY_JSON = "secureDataManagerPrivaateKeyJson";
	private static final String PLATFORM_ROOT_CA_PEM = "platformRootCAPem";
	private static final String BASE_PATH = "src/test/resources/votingCardSet/";
	private static final String OUTPUT_PATH = BASE_PATH + "output/20150615125123199000000/314bd34dcf6e4de4b771a92fa3849d3d";
	private static final String ENRICHED_BALLOT_PATH = BASE_PATH + "input/enrichedBallot.json";
	private static final String NUMBER_CREDENTIALS_PER_FILE = "1000";
	private static final String NUMBER_OF_PROCESSORS = "2";
	private static final String TEST_EE_PROPS = BASE_PATH + "input/input.properties";
	private static final List<String> CHOICE_CODES_KEY = Collections.singletonList("CHOICE_CODES_KEY");

	private static ConfigObjectMapper configObjectMapper;
	private static EncryptionParametersLoader encryptionParametersLoader;
	private static ConfigurationInputReader configurationInputReader;
	private static ExtendedKeyStoreService extendedKeyStoreService;
	private static X509CertificateLoader x509CertificateLoader;
	private static KeyPairValidator keyPairValidator;

	@BeforeAll
	public static void setUp() throws CertificateException, NoSuchProviderException {
		configObjectMapper = new ConfigObjectMapper();
		configurationInputReader = new ConfigurationInputReader();
		encryptionParametersLoader = new EncryptionParametersLoader();
		extendedKeyStoreService = new ExtendedKeyStoreService();
		x509CertificateLoader = new X509CertificateLoader();
		keyPairValidator = new KeyPairValidator();
	}

	@Test
	void adaptTheParametersCorrectly() throws GeneralCryptoLibException, IOException, CertificateException {

		final VotersHolderInitializer votersHolderInitializer = new VotersHolderInitializer(configurationInputReader, x509CertificateLoader,
				extendedKeyStoreService, encryptionParametersLoader);

		final Properties props = new Properties();
		try (final BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(TEST_EE_PROPS))) {
			props.load(bufferedReader);
		}

		final String start = (String) props.get("start");
		final int validityPeriod = Integer.parseInt((String) props.get("validityPeriod"));

		final ZonedDateTime startValidityPeriod = ZonedDateTime.now(ZoneOffset.UTC);
		final ZonedDateTime electionStartDate = ZonedDateTime.ofInstant(Instant.parse(start), ZoneOffset.UTC);
		final ZonedDateTime endValidityPeriod = electionStartDate.plusYears(validityPeriod);

		VotersParametersHolder holder = new VotersParametersHolder(Integer.parseInt(NUMBER_VOTING_CARDS), BALLOT_ID, getBallot(), BALLOT_BOX_ID,
				VOTING_CARD_SET_ID, VERIFICATION_CARD_SET_ID, ELECTORAL_AUTHORITY_ID, Paths.get(OUTPUT_PATH),
				Integer.parseInt(NUMBER_CREDENTIALS_PER_FILE), Integer.parseInt(NUMBER_OF_PROCESSORS), ELECTION_EVENT_ID, startValidityPeriod,
				endValidityPeriod, Constants.EMPTY, VOTING_CARD_SET_ALIAS, CHOICE_CODES_KEY, PLATFORM_ROOT_CA_PEM, null);

		holder = votersHolderInitializer.init(holder, getConfigurationFile());

		final CryptoAPIX509Certificate credentialCACert = holder.getCredentialCACert();
		final PrivateKey credentialCAPrivKey = holder.getCredentialCAPrivKey();

		keyPairValidator.validateKeyPair(credentialCACert.getPublicKey(), credentialCAPrivKey);

		assertNotNull(holder.getVotingCardCredentialInputDataPack());
		assertEquals("auth_sign", holder.getVotingCardCredentialInputDataPack().getCredentialAuthProperties().getAlias().get("privateKey"));
		assertEquals("sign", holder.getVotingCardCredentialInputDataPack().getCredentialSignProperties().getAlias().get("privateKey"));
		assertNotNull(holder.getVotingCardSetCredentialInputDataPack());
	}

	private Ballot getBallot() {
		final File ballotFile = Paths.get(ENRICHED_BALLOT_PATH).toAbsolutePath().toFile();

		return getBallotFromFile(ENRICHED_BALLOT_PATH, ballotFile);
	}

	private Ballot getBallotFromFile(final String ballotPath, final File ballotFile) {
		try {
			return configObjectMapper.fromJSONFileToJava(ballotFile, Ballot.class);
		} catch (final IOException e) {
			throw new IllegalArgumentException("An error occurred while mapping \"" + ballotPath + "\" to a Ballot: " + e.getMessage());
		}
	}

	private File getConfigurationFile() {
		return Paths.get("src/test/resources/", Constants.KEYS_CONFIG_FILENAME).toFile();
	}
}
