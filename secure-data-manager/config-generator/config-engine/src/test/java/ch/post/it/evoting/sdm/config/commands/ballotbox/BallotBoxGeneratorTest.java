/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.ballotbox;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionInputDataPack;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(BallotBoxGeneratorTestSpringConfig.class)
class BallotBoxGeneratorTest {

	private final static String BALLOT_ID = "a5c0305db01142e786533cb48df1c794";
	private final static String BALLOT_BOX_ID = "ballotBoxId";
	private final static String BALLOT_FILENAME = "ballot.json";
	private final static String OUTPUT_PATH_TEMP_DIR_NAME = "outputPath";

	private static EncryptionParameters encryptionParameters;
	private static Path outputPath;

	@Autowired
	private BallotBoxGenerator ballotBoxGenerator;

	@Spy
	@InjectMocks
	private BallotBoxParametersHolder ballotBoxParametersHolder;

	@Mock
	private CryptoAPIX509Certificate servicesCACert;

	@Mock
	private CryptoAPIX509Certificate electionCACert;

	@Mock
	private CredentialProperties ballotBoxCredentialProperties;

	@Spy
	private ElectionInputDataPack electionInputDataPack;

	@Mock
	private PrivateKey privateKey;

	@Mock
	private Properties certificateProperties;

	@BeforeAll
	static void setUp() throws IOException, URISyntaxException {

		encryptionParameters = new EncryptionParameters();
		encryptionParameters.setP("23");
		encryptionParameters.setQ("11");
		encryptionParameters.setG("2");

		outputPath = Files.createTempDirectory(OUTPUT_PATH_TEMP_DIR_NAME);

		Path absoluteOnlinePath = Paths.get(outputPath.toString(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
				Constants.CONFIG_DIR_NAME_BALLOTS, BALLOT_ID, Constants.CONFIG_DIR_NAME_BALLOTBOXES).toAbsolutePath();

		Path absoluteOfflinePath = Paths.get(outputPath.toString(), Constants.CONFIG_DIR_NAME_OFFLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
				Constants.CONFIG_DIR_NAME_BALLOTS, BALLOT_ID, Constants.CONFIG_DIR_NAME_BALLOTBOXES).toAbsolutePath();

		Files.createDirectories(Paths.get(absoluteOnlinePath.toString(), BALLOT_BOX_ID));
		Files.createDirectories(Paths.get(absoluteOfflinePath.toString(), BALLOT_BOX_ID));

		Path ballotFilePath = Paths.get(outputPath.toString(), Constants.CONFIG_DIR_NAME_ONLINE, Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION,
				Constants.CONFIG_DIR_NAME_BALLOTS, BALLOT_ID, BALLOT_FILENAME);

		// Copy file from test resources to output path
		Files.copy(Paths.get(BallotBoxGeneratorTest.class.getResource("/" + BALLOT_FILENAME).toURI()), ballotFilePath,
				StandardCopyOption.REPLACE_EXISTING);
	}

	@AfterAll
	static void tearDown() throws IOException {
		FileUtils.deleteDirectory(outputPath.toFile());
	}

	@Test
	void generateValidBallotBox() {

		electionInputDataPack.setCredentialProperties(ballotBoxCredentialProperties);

		when(ballotBoxParametersHolder.getBallotBoxID()).thenReturn(BALLOT_BOX_ID);
		when(ballotBoxParametersHolder.getBallotID()).thenReturn(BALLOT_ID);
		when(ballotBoxParametersHolder.getOutputPath()).thenReturn(outputPath);
		when(ballotBoxParametersHolder.getEncryptionParameters()).thenReturn(encryptionParameters);
		when(ballotBoxParametersHolder.getServicesCACert()).thenReturn(servicesCACert);
		when(ballotBoxParametersHolder.getElectionCACert()).thenReturn(electionCACert);
		when(ballotBoxParametersHolder.getBallotBoxCredentialProperties()).thenReturn(ballotBoxCredentialProperties);
		when(ballotBoxParametersHolder.getInputDataPack()).thenReturn(electionInputDataPack);
		when(ballotBoxParametersHolder.getSignerPrivateKey()).thenReturn(privateKey);
		when(ballotBoxParametersHolder.getCertificateProperties()).thenReturn(certificateProperties);

		assertDoesNotThrow(() -> ballotBoxGenerator.generate(ballotBoxParametersHolder));
	}
}
