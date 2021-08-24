/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static java.nio.file.Files.exists;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.google.gson.Gson;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.election.VerificationCardSetData;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.requestBeans.SignRequest;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.common.SignedObject;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.domain.service.BallotBoxDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.BallotDataGeneratorService;
import ch.post.it.evoting.sdm.domain.service.VotingCardSetDataGeneratorService;
import ch.post.it.evoting.sdm.infrastructure.cc.PayloadStorageException;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.ConfigObjectMapper;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(VotingCardSetServiceTestSpringConfig.class)
class VotingCardSetServiceTest extends VotingCardSetServiceTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardSetServiceTest.class);

	private static final String ELECTION_EVENT_ID = "a3d790fd1ac543f9b0a05ca79a20c9e2";
	private static final String VERIFICATION_CARD_SET_ID = "9a0";
	private static final String PRECOMPUTED_VALUES_PATH = "computeTest";
	private static final String ONLINE_PATH = "ONLINE";
	private static final String VOTE_VERIFICATION_FOLDER = "voteVerification";

	@TempDir
	Path tempDir;

	@Autowired
	private VotingCardSetService votingCardSetService;

	@Autowired
	private VotingCardSetRepository votingCardSetRepositoryMock;

	@Autowired
	private IdleStatusService idleStatusService;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private BallotBoxRepository ballotBoxRepository;

	@Autowired
	private BallotDataGeneratorService ballotDataGeneratorServiceMock;

	@Autowired
	private BallotBoxDataGeneratorService ballotBoxDataGeneratorServiceMock;

	@Autowired
	private ConfigurationEntityStatusService configurationEntityStatusServiceMock;

	@Autowired
	private VotingCardSetDataGeneratorService votingCardSetDataGeneratorServiceMock;

	@Autowired
	private VotingCardSetChoiceCodesService votingCardSetChoiceCodesServiceMock;

	@Autowired
	private ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepository;

	@BeforeAll
	static void setup() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@AfterAll
	static void tearDown() {
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
	}

	@Test
	void sign() throws IOException, GeneralCryptoLibException, ResourceNotFoundException {
		setStatusForVotingCardSetFromRepository("GENERATED", votingCardSetRepositoryMock);

		when(pathResolver.resolve(any())).thenReturn(Paths.get("src/test/resources/votingcardsetservice/"));

		assertTrue(votingCardSetService.sign(ELECTION_EVENT_ID, VOTING_CARD_SET_ID, SigningTestData.PRIVATE_KEY_PEM));

		final PublicKey publicKey = PemUtils.publicKeyFromPem(SigningTestData.PUBLIC_KEY_PEM);
		final Path outputPath = Paths.get("src/test/resources/votingcardsetservice/" + ELECTION_EVENT_ID + "/ONLINE/");

		verifyVoterMaterialCSVs(publicKey, outputPath);
		verifyVoteVerificationCSVs(publicKey, outputPath);
		verifyVoteVerificationJSONs(publicKey, outputPath);
		verifyExtendedAuth(publicKey, outputPath);
		verifyPrintingDataCSVs(publicKey, outputPath);
	}

	@Test
	void signReturnsFalseWhenTheRequestedSetIsAlreadySigned() throws IOException, GeneralCryptoLibException, ResourceNotFoundException {
		assertFalse(votingCardSetService.sign(ELECTION_EVENT_ID, VOTING_CARD_SET_ID, SigningTestData.PRIVATE_KEY_PEM));
	}

	private void verifyVoteVerificationJSONs(final PublicKey publicKey, final Path outputPath) throws IOException {

		final Path signedVerificationCardSetData = Paths
				.get(outputPath.toString(), "voteVerification/9a0/", Constants.CONFIG_FILE_NAME_SIGNED_VERIFICATIONSET_DATA);
		final Path signedVerificationContextData = Paths
				.get(outputPath.toString(), "voteVerification/9a0/", Constants.CONFIG_FILE_NAME_SIGNED_VERIFICATION_CONTEXT_DATA);

		assertAll(() -> assertTrue(exists(signedVerificationCardSetData)), () -> assertTrue(exists(signedVerificationContextData)));

		final ConfigObjectMapper mapper = new ConfigObjectMapper();

		final SignedObject signedVerificationCardSetDataObj = mapper.fromJSONFileToJava(signedVerificationCardSetData.toFile(), SignedObject.class);
		final String signatureVerCardSetData = signedVerificationCardSetDataObj.getSignature();

		final SignedObject signedVerificationContextDataObj = mapper.fromJSONFileToJava(signedVerificationContextData.toFile(), SignedObject.class);
		final String signatureVerCardContextData = signedVerificationContextDataObj.getSignature();

		JsonSignatureService.verify(publicKey, signatureVerCardSetData, VerificationCardSetData.class);
		JsonSignatureService.verify(publicKey, signatureVerCardContextData, VoteVerificationContextData.class);

		Files.deleteIfExists(signedVerificationCardSetData);
		Files.deleteIfExists(signedVerificationContextData);
	}

	private void verifyExtendedAuth(final PublicKey publicKey, final Path outputPath) throws IOException, GeneralCryptoLibException {
		verifyDataCSVs(publicKey, outputPath, "extendedAuthentication", VOTING_CARD_SET_ID);
	}

	private void verifyVoteVerificationCSVs(final PublicKey publicKey, final Path outputPath) throws IOException, GeneralCryptoLibException {
		verifyDataCSVs(publicKey, outputPath, "voteVerification", "9a0");
	}

	private void verifyVoterMaterialCSVs(final PublicKey publicKey, final Path outputPath) throws IOException, GeneralCryptoLibException {
		verifyDataCSVs(publicKey, outputPath, "voterMaterial", VOTING_CARD_SET_ID);
	}

	private void verifyPrintingDataCSVs(final PublicKey publicKey, final Path outputPath) throws IOException, GeneralCryptoLibException {
		final Path printingPath = Paths.get(outputPath.toString(), "printing", VOTING_CARD_SET_ID, "/");

		final List<Path> printingFiles = Files.walk(printingPath, 1).filter(filePath -> filePath.getFileName().toString().endsWith(Constants.CSV))
				.collect(Collectors.toList());

		for (final Path csvFilePath : printingFiles) {
			if (csvFilePath.getFileName().toString().startsWith(Constants.PRINTING_DATA) && csvFilePath.getFileName().toString()
					.endsWith(Constants.CSV)) {
				final String csvAndSignature = concatenateCSVAndSignature(csvFilePath);
				final Path tempFile = Files.createTempFile("tmp", ".tmp");

				Files.write(tempFile, csvAndSignature.getBytes(StandardCharsets.UTF_8));

				assertTrue(verify(publicKey, tempFile));

				Files.deleteIfExists(Paths.get(csvFilePath + Constants.SIGN));
			}
		}
	}

	private void verifyDataCSVs(final PublicKey publicKey, final Path outputPath, final String subFolderName, final String subSubFolderName)
			throws IOException, GeneralCryptoLibException {
		final Path path = Paths.get(outputPath.toString(), subFolderName, subSubFolderName, "/");

		final List<Path> filesPath = Files.walk(path, 1).filter(filePath -> filePath.getFileName().toString().endsWith(Constants.CSV))
				.collect(Collectors.toList());

		for (final Path csvFilePath : filesPath) {
			if (csvFilePath.getFileName().toString().endsWith(Constants.CSV)) {
				final String csvAndSignature = concatenateCSVAndSignature(csvFilePath);
				final Path tempFile = Files.createTempFile("tmp", ".tmp");

				Files.write(tempFile, csvAndSignature.getBytes(StandardCharsets.UTF_8));

				assertTrue(verify(publicKey, tempFile));

				Files.deleteIfExists(Paths.get(csvFilePath + Constants.SIGN));
			}
		}
	}

	/**
	 * Verifies the signature of a given CSV file, and removes the signature from it.
	 */
	private boolean verify(PublicKey publicKey, Path csvSignedFile) throws IOException, GeneralCryptoLibException {

		// Validate file path
		if (csvSignedFile == null) {
			throw new IOException("Error to validate CSV file path. The given file path cannot be null.");
		} else if (!csvSignedFile.toFile().exists()) {
			throw new IOException("Error to validate CSV file path. The given file path " + csvSignedFile + ", should exist.");
		}

		// Get signature from file
		File csvFile = csvSignedFile.toFile();
		String signatureB64;
		try (ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(csvFile, 4096, StandardCharsets.UTF_8)) {
			signatureB64 = reversedLinesFileReader.readLine();
		}

		// Remove signature from file
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(csvFile, "rw")) {

			long length = randomAccessFile.length();
			int sizeLastLine = signatureB64.getBytes(StandardCharsets.UTF_8).length + 1;
			randomAccessFile.setLength(length - sizeLastLine);
		}

		// Validate signature
		try (FileInputStream csvFileIn = new FileInputStream(csvFile)) {

			byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
			AsymmetricService asymmetricService = new AsymmetricService();
			return asymmetricService.verifySignature(signatureBytes, publicKey, csvFileIn);
		}
	}

	private String concatenateCSVAndSignature(final Path filePath) throws IOException {
		final Path signedFile = Paths.get(filePath.toString() + Constants.SIGN);
		final String csvText = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		final String signatureB64 = new String(Files.readAllBytes(signedFile), StandardCharsets.UTF_8);

		return csvText + "\n" + signatureB64;
	}

	@Test
	void generateElectionEventIdNull() throws ResourceNotFoundException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> votingCardSetService.generate(VOTING_CARD_SET_ID, null));
	}

	@Test
	void generateElectionEventIdEmpty() throws ResourceNotFoundException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> votingCardSetService.generate(VOTING_CARD_SET_ID, ""));
	}

	@Test
	void generateVotingCardSetIdNull() throws ResourceNotFoundException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(idleStatusService.getIdLock(null)).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> votingCardSetService.generate(null, ELECTION_EVENT_ID));
	}

	@Test
	void generateVotingCardSetIdEmpty() throws ResourceNotFoundException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> votingCardSetService.generate("", ELECTION_EVENT_ID));
	}

	@Test
	void generateEncryptParamsGenerationFails() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertFalse(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void generateReadBallotBoxId4VotingCardSetIdEmpty() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(votingCardSetRepositoryMock.getBallotBoxId(VOTING_CARD_SET_ID)).thenReturn(BALLOT_BOX_ID);
		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertFalse(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void generateReadBallotId4BallotBoxIdEmpty() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {

		when(votingCardSetRepositoryMock.getBallotBoxId(VOTING_CARD_SET_ID)).thenReturn("");
		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		assertFalse(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void generateGenerateBallotDataFails() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(votingCardSetRepositoryMock.getBallotBoxId(VOTING_CARD_SET_ID)).thenReturn(BALLOT_BOX_ID);
		when(ballotBoxRepository.getBallotId(BALLOT_BOX_ID)).thenReturn(BALLOT_ID);

		final DataGeneratorResponse ballotDataResult = new DataGeneratorResponse();
		ballotDataResult.setSuccessful(false);

		when(ballotDataGeneratorServiceMock.generate(BALLOT_ID, ELECTION_EVENT_ID)).thenReturn(ballotDataResult);
		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertFalse(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void generateGenerateBallotBoxDataDoneFails() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(votingCardSetRepositoryMock.getBallotBoxId(VOTING_CARD_SET_ID)).thenReturn(BALLOT_BOX_ID);
		when(ballotBoxRepository.getBallotId(BALLOT_BOX_ID)).thenReturn(BALLOT_ID);

		final DataGeneratorResponse ballotDataResult = new DataGeneratorResponse();
		ballotDataResult.setSuccessful(true);

		when(ballotDataGeneratorServiceMock.generate(BALLOT_ID, ELECTION_EVENT_ID)).thenReturn(ballotDataResult);
		when(ballotBoxRepository.find(BALLOT_BOX_ID)).thenReturn(getBallotBoxWithStatus(Status.LOCKED));

		final DataGeneratorResponse ballotBoxDataResult = new DataGeneratorResponse();
		ballotBoxDataResult.setSuccessful(false);

		when(ballotBoxDataGeneratorServiceMock.generate(BALLOT_BOX_ID, ELECTION_EVENT_ID)).thenReturn(ballotBoxDataResult);
		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertFalse(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void generateFailsGenerateBallotBoxDataDone() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {

		when(votingCardSetRepositoryMock.getBallotBoxId(VOTING_CARD_SET_ID)).thenReturn(BALLOT_BOX_ID);
		when(ballotBoxRepository.getBallotId(BALLOT_BOX_ID)).thenReturn(BALLOT_ID);

		final DataGeneratorResponse ballotDataResult = new DataGeneratorResponse();
		ballotDataResult.setSuccessful(true);

		when(ballotDataGeneratorServiceMock.generate(BALLOT_ID, ELECTION_EVENT_ID)).thenReturn(ballotDataResult);
		when(ballotBoxRepository.find(BALLOT_BOX_ID)).thenReturn(getBallotBoxWithStatus(Status.LOCKED));

		final DataGeneratorResponse ballotBoxDataResult = new DataGeneratorResponse();
		ballotBoxDataResult.setSuccessful(true);

		when(ballotBoxDataGeneratorServiceMock.generate(BALLOT_BOX_ID, ELECTION_EVENT_ID)).thenReturn(ballotBoxDataResult);
		when(configurationEntityStatusServiceMock.update(anyString(), anyString(), any(BallotBoxRepository.class))).thenReturn("");

		final DataGeneratorResponse votingCardSetDataResultMock = new DataGeneratorResponse();
		votingCardSetDataResultMock.setSuccessful(false);

		when(votingCardSetDataGeneratorServiceMock.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID)).thenReturn(votingCardSetDataResultMock);
		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		assertFalse(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void generateFailsGenerateBallotBoxDataNotDone() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {
		setStatusForVotingCardSetFromRepository("VCS_DOWNLOADED", votingCardSetRepositoryMock);

		when(votingCardSetRepositoryMock.getBallotBoxId(VOTING_CARD_SET_ID)).thenReturn(BALLOT_BOX_ID);
		when(ballotBoxRepository.getBallotId(BALLOT_BOX_ID)).thenReturn(BALLOT_ID);

		final DataGeneratorResponse ballotDataResult = new DataGeneratorResponse();
		ballotDataResult.setSuccessful(true);

		when(ballotDataGeneratorServiceMock.generate(BALLOT_ID, ELECTION_EVENT_ID)).thenReturn(ballotDataResult);
		when(ballotBoxRepository.find(BALLOT_BOX_ID)).thenReturn(getBallotBoxWithStatus(Status.READY));

		final DataGeneratorResponse votingCardSetDataResult = new DataGeneratorResponse();
		votingCardSetDataResult.setSuccessful(false);

		when(votingCardSetDataGeneratorServiceMock.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID)).thenReturn(votingCardSetDataResult);
		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertFalse(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void generateGenerateBallotBoxDataNotDone() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {
		when(idleStatusService.getIdLock(anyString())).thenReturn(false);

		assertTrue(votingCardSetService.generate(VOTING_CARD_SET_ID, ELECTION_EVENT_ID).isSuccessful());
	}

	@Test
	void download() throws ResourceNotFoundException, URISyntaxException, PayloadStorageException, IOException {
		setStatusForVotingCardSetFromRepository(Status.COMPUTED.name(), votingCardSetRepositoryMock);

		final Path basePath = getPathOfFileInResources(Paths.get(PRECOMPUTED_VALUES_PATH));
		final Path folder = basePath.resolve(ELECTION_EVENT_ID).resolve(ONLINE_PATH).resolve(VOTE_VERIFICATION_FOLDER)
				.resolve(VERIFICATION_CARD_SET_ID);

		when(pathResolver.resolve(any())).thenReturn(basePath);
		when(configurationEntityStatusServiceMock.update(anyString(), anyString(), any())).thenReturn("");
		when(returnCodeGenerationRequestPayloadRepository.getCount(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)).thenReturn(3);
		when(votingCardSetChoiceCodesServiceMock.download(anyString(), anyString(), anyInt()))
				.thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), new ByteArrayInputStream(new byte[] { 4, 5, 6 }),
						new ByteArrayInputStream(new byte[] { 7, 8, 9 }));
		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertAll(() -> assertDoesNotThrow(() -> votingCardSetService.download(VOTING_CARD_SET_ID, ELECTION_EVENT_ID)),
				() -> assertTrue(exists(folder.resolve(Constants.CONFIG_FILE_NAME_NODE_CONTRIBUTIONS + ".0" + Constants.JSON))),
				() -> assertTrue(exists(folder.resolve(Constants.CONFIG_FILE_NAME_NODE_CONTRIBUTIONS + ".1" + Constants.JSON))),
				() -> assertTrue(exists(folder.resolve(Constants.CONFIG_FILE_NAME_NODE_CONTRIBUTIONS + ".2" + Constants.JSON))));

	}

	@Test
	void downloadInvalidStatus() throws ResourceNotFoundException {
		setStatusForVotingCardSetFromRepository("SIGNED", votingCardSetRepositoryMock);

		when(idleStatusService.getIdLock(anyString())).thenReturn(true);

		assertThrows(InvalidStatusTransitionException.class, () -> votingCardSetService.download(VOTING_CARD_SET_ID, ELECTION_EVENT_ID));
	}

	private void copyTestFiles() throws IOException, URISyntaxException {
		FileUtils.copyDirectory(getPathOfFileInResources(Paths.get("VotingCardSetServiceTest")).toFile(), tempDir.toFile());
	}

	@Test
	void testBeanToJson() throws IOException, URISyntaxException {

		copyTestFiles();

		final SignRequest signRequest = getSignRequest();
		final Gson gson = new Gson();
		final SignRequest fromJson = gson.fromJson(gson.toJson(signRequest), SignRequest.class);

		assertAll(() -> assertEquals(signRequest.getElectionEventId(), fromJson.getElectionEventId()),
				() -> assertEquals(signRequest.getCertificatesChain().size(), fromJson.getCertificatesChain().size()));

	}

	private SignRequest getSignRequest() throws IOException {
		final SignRequest signRequest = new SignRequest();
		signRequest.setVotingCardSetId(VOTING_CARD_SET_ID);
		signRequest.setElectionEventId(ELECTION_EVENT_ID);
		signRequest.setPrivateKeyPEM(SigningTestData.PRIVATE_KEY_PEM);
		signRequest.setCertificatesChain(getCertificatesChain());

		return signRequest;
	}

	private List<String> getCertificatesChain() throws IOException {

		final String adapterSignTenantCertName = "tenant_100.pem";
		final String adapterSignRootCertName = "rootCA.pem";

		final String certificatesPathStr = tempDir.toAbsolutePath().toString();
		final Path tenantCertPath = Paths.get(certificatesPathStr, adapterSignTenantCertName);
		final Path rootCertPath = Paths.get(certificatesPathStr, adapterSignRootCertName);

		final List<String> certificates = new ArrayList<>();
		certificates.add(new String(Files.readAllBytes(tenantCertPath), StandardCharsets.UTF_8));
		certificates.add(new String(Files.readAllBytes(rootCertPath), StandardCharsets.UTF_8));

		LOGGER.info("Used Tenant certificate: {}", adapterSignTenantCertName);
		LOGGER.info("Used Root certificate: {}", adapterSignRootCertName);

		return certificates;
	}

}
