/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.ballotbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.BallotBox;
import ch.post.it.evoting.domain.election.BallotBoxContextData;
import ch.post.it.evoting.domain.election.ElectionEvent;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;
import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.api.output.BallotBoxesServiceOutput;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionInputDataPack;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.generators.ElectionCredentialDataPackGenerator;
import ch.post.it.evoting.sdm.config.exceptions.CreateBallotBoxesException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateBallotBoxesException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;
import ch.post.it.evoting.sdm.utils.KeyStoreReader;

/**
 * Generates a given number of {@link BallotBox}, which are linked to the provided {@link Ballot}.
 */
@Service
public class BallotBoxGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxGenerator.class);

	private final ElectionCredentialDataPackGenerator electionCredentialDataPackGenerator;

	private final ConfigObjectMapper mapper;

	@Autowired
	LoggingFactory loggerFactory;

	private LoggingWriter logWriter;

	public BallotBoxGenerator(final ElectionCredentialDataPackGenerator electionCredentialDataPackGenerator) {
		this.electionCredentialDataPackGenerator = electionCredentialDataPackGenerator;
		this.mapper = new ConfigObjectMapper();
	}

	@PostConstruct
	private void initializeLogger() {
		logWriter = loggerFactory.getLogger(BallotBoxGenerator.class);
	}

	/**
	 * Generates as many {@link BallotBox} as ballot box IDs passed as parameter.
	 *
	 * @param ballotBoxHolder
	 * @return
	 */
	public BallotBoxesServiceOutput generate(final BallotBoxParametersHolder ballotBoxHolder) {

		final String ballotBoxID = ballotBoxHolder.getBallotBoxID();

		final String alias = ballotBoxHolder.getAlias();

		final String ballotID = ballotBoxHolder.getBallotID();

		final String electoralAutorityId = ballotBoxHolder.getElectoralAuthorityID();

		final String isTest = ballotBoxHolder.getTest();

		final String gracePeriod = ballotBoxHolder.getGracePeriod();

		final Path absoluteOutputPath = ballotBoxHolder.getOutputPath();

		if (!doesBallotIdMatchAnyExistingBallot(ballotID,
				Paths.get(absoluteOutputPath.toString(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
						Constants.CONFIG_DIR_NAME_BALLOTS, ballotID).toAbsolutePath())) {
			throw new GenerateBallotBoxesException("The specified Ballot ID does not match any existing ballot.");
		}

		final String eeID = ballotBoxHolder.getEeID();

		final Path absoluteOnlinePath = Paths
				.get(absoluteOutputPath.toString(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
						Constants.CONFIG_DIR_NAME_BALLOTS, ballotID, Constants.CONFIG_DIR_NAME_BALLOTBOXES).toAbsolutePath();

		final EncryptionParameters encParams = ballotBoxHolder.getEncryptionParameters();

		final String writeInAlphabet = ballotBoxHolder.getWriteInAlphabet();

		final CryptoAPIX509Certificate servicesCACert = ballotBoxHolder.getServicesCACert();

		final CryptoAPIX509Certificate electionCACert = ballotBoxHolder.getElectionCACert();

		final CredentialProperties ballotBoxCredentialProperties = ballotBoxHolder.getBallotBoxCredentialProperties();

		final ElectionInputDataPack electionInputDataPack = ballotBoxHolder.getInputDataPack();

		electionInputDataPack.setCredentialProperties(ballotBoxCredentialProperties);

		final PrivateKey servicesCAPrivKey = ballotBoxHolder.getSignerPrivateKey();

		electionInputDataPack.setParentKeyPair(new KeyPair(servicesCACert.getPublicKey(), servicesCAPrivKey));

		String keyForProtectingKeystorePassword = ballotBoxHolder.getKeyForProtectingKeystorePassword();

		LOGGER.info("Generating ballot box {}...", ballotBoxID);
		final Path onlinePath = Paths.get(absoluteOnlinePath.toString(), ballotBoxID);

		createDirectories(onlinePath);

		// create new replacements
		final ReplacementsHolder replacementsHolder = new ReplacementsHolder(eeID, ballotBoxID);

		final ElectionCredentialDataPack dataPack;

		try {
			dataPack = electionCredentialDataPackGenerator
					.generate(electionInputDataPack, replacementsHolder, Constants.CONFIGURATION_BALLOTBOX_JSON_TAG, keyForProtectingKeystorePassword,
							ballotBoxHolder.getCertificateProperties(), servicesCACert, electionCACert);
		} catch (final GeneralCryptoLibException e) {
			throw new CreateBallotBoxesException("An error occurred while generating the ballot box data pack: " + e.getMessage(), e);
		}

		try {
			storeOnlineBallotBox(ballotID, eeID, ballotBoxID, alias, electoralAutorityId, isTest, gracePeriod, encParams, writeInAlphabet, dataPack,
					onlinePath);
		} catch (final IOException e) {
			throw new CreateBallotBoxesException("An error occurred while saving info on online ballot box id folder: " + e.getMessage(), e);
		} finally {
			dataPack.clearPassword();
		}

		logWriter.log(Level.INFO, new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENBB_SUCCESS_CREATED_AND_STORED)
				.electionEvent(ballotBoxHolder.getEeID()).user("adminID").additionalInfo("bbid", ballotBoxID).createLogInfo());

		LOGGER.info("Ballot box {} was successfully created", ballotBoxID);

		final Path[] outputPaths = new Path[2];
		outputPaths[0] = absoluteOnlinePath;

		BallotBoxesServiceOutput ballotBoxesServiceOutput = new BallotBoxesServiceOutput();
		ballotBoxesServiceOutput.setOutputPath(outputPaths[0].toString());
		return ballotBoxesServiceOutput;
	}

	private boolean doesBallotIdMatchAnyExistingBallot(final String ballotID, final Path ballotsDirectoryPath) {

		final Ballot ballotFromFile;
		final Path enrichedBallotPath = Paths.get(ballotsDirectoryPath.toString(), Constants.CONFIG_FILE_NAME_BALLOT_JSON);

		try {
			ballotFromFile = mapper.fromJSONFileToJava(enrichedBallotPath.toFile(), Ballot.class);

			if (ballotFromFile.getId().equals(ballotID)) {
				return true;
			}
		} catch (final IOException e) {
			throw new CreateBallotBoxesException("An error reconstructing a ballot from a JSON file: " + e.getMessage(), e);
		}

		return false;
	}

	private void storeOnlineBallotBox(final String ballotID, final String eeID, final String bbid, final String alias,
			final String electoralAuthorityId, final String isTest, final String gracePeriod, final EncryptionParameters encParams,
			final String writeInAlphabet, final ElectionCredentialDataPack dataPack, final Path onlinePath) throws IOException {

		final BallotBox ballotBox = new BallotBox();

		ballotBox.setBid(ballotID);
		ballotBox.setEeid(eeID);
		ballotBox.setId(bbid);
		ballotBox.setAlias(alias);
		ballotBox.setBallotBoxCert(new String(dataPack.getCertificate().getPemEncoded(), StandardCharsets.UTF_8));
		ballotBox.setStartDate(dataPack.getStartDate().toString());
		ballotBox.setEndDate(dataPack.getEndDate().toString());

		ballotBox.setElectoralAuthorityId(electoralAuthorityId);
		ballotBox.setTest(Boolean.parseBoolean(isTest));
		ballotBox.setGracePeriod(gracePeriod);
		final EncryptionParameters encryptionParameters = new EncryptionParameters(encParams.getP(), encParams.getQ(), encParams.getG());

		ballotBox.setEncryptionParameters(encryptionParameters);
		ballotBox.setWriteInAlphabet(writeInAlphabet);

		final Path pathFile = Paths.get(onlinePath.toString(), Constants.CONFIG_DIR_NAME_BALLOTBOX_JSON);
		mapper.fromJavaToJSONFileWithoutNull(ballotBox, pathFile.toFile());

		BallotBoxContextData ballotBoxContextData = new BallotBoxContextData();
		ballotBoxContextData.setElectionEvent(new ElectionEvent(eeID));
		ballotBoxContextData.setId(bbid);
		ballotBoxContextData.setKeystore(KeyStoreReader.toString(dataPack.getKeyStore(), dataPack.getPassword()));
		ballotBoxContextData.setPasswordKeystore(dataPack.getEncryptedPassword());

		final Path ballotBoxContextDataPath = Paths.get(onlinePath.toString(), Constants.CONFIG_DIR_NAME_BALLOTBOX_CONTEXT_DATA_JSON);
		mapper.fromJavaToJSONFileWithoutNull(ballotBoxContextData, ballotBoxContextDataPath.toFile());

	}

	private void createDirectories(final Path onlinePath) {
		try {
			Files.createDirectories(onlinePath);
		} catch (final IOException e) {
			throw new IllegalArgumentException("An error occurred while creating the following path: " + onlinePath, e);
		}
	}

}
