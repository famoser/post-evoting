/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.sdm.application.service.GenerateVerificationData.GenVerDatOutput;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitivesService;
import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomService;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.election.payload.sign.PayloadSigner;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.domain.returncodes.ReturnCodeGenerationRequestPayload;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.CryptolibPayloadSignatureService;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadFileSystemRepository;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadRepository;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;

@ExtendWith(MockitoExtension.class)
class GenerateVerificationDataTest extends VotingCardSetServiceTestBase {

	private static final String ELECTION_EVENT_ID = "a3d790fd1ac543f9b0a05ca79a20c9e2";
	private static final String BALLOT_JSON = "{\"id\":\"1c18f7d21dce4ac1aa1f025599bf2cfd\",\"defaultTitle\":\"ballot_Election-1\",\"defaultDescription\":\"ballot_Election-1\",\"alias\":\"ballot_1\",\"electionEvent\":{\"id\":\"a3d790fd1ac543f9b0a05ca79a20c9e2\"},\"contests\":[{\"id\":\"31d17941cacb4ea7b0dd45cd5bbc1074\",\"defaultTitle\":\"Regierungsratswahl\",\"alias\":\"Election-1\",\"defaultDescription\":\"Regierungsratswahl\",\"electionEvent\":{\"id\":\"a3d790fd1ac543f9b0a05ca79a20c9e2\"},\"template\":\"listsAndCandidates\",\"fullBlank\":\"true\",\"options\":[{\"id\":\"f4206470b5194e0086e548b1cbf532b5\",\"representation\":\"2\",\"attribute\":\"47383e930b494529ae2cb96bc4e27307\"},{\"id\":\"5d97df0d0c554c6fb9cd0a06c6296939\",\"representation\":\"5\",\"attribute\":\"47383e930b494529ae2cb96bc4e27307\"},{\"id\":\"d67ebbcff9ce40b989a7592c5f1aea9d\",\"representation\":\"13\",\"attribute\":\"47383e930b494529ae2cb96bc4e27307\"}],\"attributes\":[{\"id\":\"1a1c3be008be42ab920e740b5308cc24\",\"alias\":\"BLANK_0f2fd4984d2f4d88ae11ba6633efb57f\",\"correctness\":\"false\",\"related\":[\"ba7ca9c0d9594b409c259ece6ed0a16b\"]},{\"id\":\"ba7ca9c0d9594b409c259ece6ed0a16b\",\"alias\":\"lists\",\"correctness\":\"true\",\"related\":[]},{\"id\":\"47383e930b494529ae2cb96bc4e27307\",\"alias\":\"BLANK_b471aa814ed34ad5b93d4bde9ba98813\",\"correctness\":\"false\",\"related\":[\"7256d97c13cf4240ba7b647d78b150ee\",\"ba7ca9c0d9594b409c259ece6ed0a16b\"]},{\"id\":\"7746bfbce4594cd0a68607d08a9cd0a3\",\"alias\":\"WRITE_IN_0f2fd4984d2f4d88ae11ba6633efb57f\",\"correctness\":\"false\",\"related\":[\"ba7ca9c0d9594b409c259ece6ed0a16b\"]},{\"id\":\"d75a1f67671e4806be90797a6ea51258\",\"alias\":\"WRITE_IN_b471aa814ed34ad5b93d4bde9ba98813\",\"correctness\":\"false\",\"related\":[\"7256d97c13cf4240ba7b647d78b150ee\"]},{\"id\":\"7256d97c13cf4240ba7b647d78b150ee\",\"alias\":\"candidates\",\"correctness\":\"true\",\"related\":[]},{\"id\":\"862f4043e9cc492b88f7455987191406\",\"alias\":\"1-EVP\",\"correctness\":\"false\",\"related\":[\"ba7ca9c0d9594b409c259ece6ed0a16b\"]},{\"id\":\"d2c4025787cd497ea35d0e4d2605a959\",\"alias\":\"11-CSP\",\"correctness\":\"false\",\"related\":[\"ba7ca9c0d9594b409c259ece6ed0a16b\"]},{\"id\":\"87816135634e4e31ba42dcfcc50ee3df\",\"alias\":\"681ba77a-62ff-3449-9e3c-22777629faee\",\"correctness\":\"false\",\"related\":[\"862f4043e9cc492b88f7455987191406\",\"7256d97c13cf4240ba7b647d78b150ee\"]},{\"id\":\"4618b676d7a04f5dad83b39331c7ef0a\",\"alias\":\"15179821-f0ab-3cc6-b8d6-1f51e69f6819\",\"correctness\":\"false\",\"related\":[\"d2c4025787cd497ea35d0e4d2605a959\",\"7256d97c13cf4240ba7b647d78b150ee\"]}],\"questions\":[{\"id\":\"b471aa814ed34ad5b93d4bde9ba98813\",\"max\":\"3\",\"min\":\"1\",\"accumulation\":\"1\",\"writeIn\":\"true\",\"blankAttribute\":\"47383e930b494529ae2cb96bc4e27307\",\"writeInAttribute\":\"d75a1f67671e4806be90797a6ea51258\",\"attribute\":\"7256d97c13cf4240ba7b647d78b150ee\",\"fusions\":[]},{\"id\":\"0f2fd4984d2f4d88ae11ba6633efb57f\",\"max\":\"1\",\"min\":\"1\",\"accumulation\":\"1\",\"writeIn\":\"true\",\"blankAttribute\":\"1a1c3be008be42ab920e740b5308cc24\",\"writeInAttribute\":\"7746bfbce4594cd0a68607d08a9cd0a3\",\"attribute\":\"ba7ca9c0d9594b409c259ece6ed0a16b\",\"fusions\":[]}]}],\"status\":\"SIGNED\",\"details\":\"Ready to Mix\",\"synchronized\":\"false\",\"ballotBoxes\":\"FR-CH-1|FR-MU-9999\",\"signedObject\":\"fakesignature\"}";
	private static final String VOTING_CARD_SET_ID = "74a4e530b24f4086b099d153321cf1b3";
	private static final String BALLOT_BOX_ID = "4b88e5ec5fc14e14934c2ef491fe2de7";
	private static final String BALLOT_ID = "dd5bd34dcf6e4de4b771a92fa38abc11";
	private static final String VERIFICATION_CARD_SET_ID = "9a0";
	private static final String PRECOMPUTED_VALUES_PATH = "computeTest";
	private static final String ADMIN_BOARD_ID = "dc742be1d49b42ee83cfe1652a8170ac";
	private static final int SIGNING_KEY_SIZE = 1024;
	private static final BigInteger P = new BigInteger(
			"16370518994319586760319791526293535327576438646782139419846004180837103527129035954742043590609421369665944746587885814920851694546456891767644945459124422553763416586515339978014154452159687109161090635367600349264934924141746082060353483306855352192358732451955232000593777554431798981574529854314651092086488426390776811367125009551346089319315111509277347117467107914073639456805159094562593954195960531136052208019343392906816001017488051366518122404819967204601427304267380238263913892658950281593755894747339126531018026798982785331079065126375455293409065540731646939808640273393855256230820509217411510058759");
	private static final BigInteger Q = new BigInteger(
			"8185259497159793380159895763146767663788219323391069709923002090418551763564517977371021795304710684832972373293942907460425847273228445883822472729562211276881708293257669989007077226079843554580545317683800174632467462070873041030176741653427676096179366225977616000296888777215899490787264927157325546043244213195388405683562504775673044659657555754638673558733553957036819728402579547281296977097980265568026104009671696453408000508744025683259061202409983602300713652133690119131956946329475140796877947373669563265509013399491392665539532563187727646704532770365823469904320136696927628115410254608705755029379");
	private static final BigInteger G = new BigInteger("2");
	private static final int CHUNK_SIZE = 3;
	private static String administrationBoardPrivateKeyPEM = "";

	private static GqGroup gqGroup;
	@Spy
	private final ObjectMapper objectMapper = ObjectMapperMixnetConfig.getNewInstance();
	@InjectMocks
	private GenerateVerificationData generateVerificationData;
	@Mock
	private BallotService ballotService;
	@Mock
	private IdleStatusService idleStatusServiceMock;
	@Mock
	private PathResolver pathResolverMock;
	@Mock
	private VotingCardSetRepository votingCardSetRepositoryMock;
	@Mock
	private BallotBoxRepository ballotBoxRepositoryMock;
	@Mock
	private ConfigurationEntityStatusService configurationEntityStatusServiceMock;
	@Mock
	private VoterCodesService voterCodesServiceMock;
	@Mock
	private CryptoAPIElGamalEncrypter cryptoAPIElGamalEncrypterMock;
	@Mock
	private ElGamalServiceAPI elGamalServiceAPIMock;
	@Mock
	private ElectionEventService electionEventServiceMock;
	@Mock
	private ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepositoryMock;
	@Mock
	private AdminBoardService adminBoardServiceMock;
	@Mock
	private PayloadSigner payloadSignerMock;
	@Mock
	private PrimitivesServiceAPI primitivesServiceAPIMock;
	@Mock
	private EncryptionParametersLoader encryptionParametersLoaderMock;

	@Spy
	private CryptoPrimitivesService cryptoPrimitivesService;
	@Mock
	private CryptolibPayloadSignatureService payloadSignatureService;

	@Mock
	private HashService hashService;

	@BeforeAll
	static void setUp() throws NoSuchAlgorithmException, NoSuchProviderException, GeneralCryptoLibException {
		Security.addProvider(new BouncyCastleProvider());

		gqGroup = new GqGroup(P, Q, G);

		// Generate the signing key pair.
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
		generator.initialize(SIGNING_KEY_SIZE);

		final KeyPair signingKeyPair = generator.generateKeyPair();

		// Store the private key PEM.
		administrationBoardPrivateKeyPEM = PemUtils.privateKeyToPem(signingKeyPair.getPrivate());
	}

	@AfterAll
	static void tearDown() {
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
	}

	@BeforeEach
	void beforeEachTest() {
		ReflectionTestUtils.setField(generateVerificationData, "chunkSize", CHUNK_SIZE);
		ReflectionTestUtils.setField(generateVerificationData, "tenantId", "100");
		ReflectionTestUtils.setField(generateVerificationData, "electionEventId", ELECTION_EVENT_ID);
	}

	@Test
	void precompute()
			throws ResourceNotFoundException, IOException, URISyntaxException, GeneralCryptoLibException, CertificateException, CMSException,
			PayloadSignatureException {

		setUpService();

		when(idleStatusServiceMock.getIdLock(anyString())).thenReturn(true);
		when(hashService.recursiveHash(any())).thenReturn((new BigInteger("4").toByteArray()));
		when(payloadSignatureService.sign(any(), any(), any())).thenReturn(new CryptolibPayloadSignature(new byte[] {}, new X509Certificate[] {}));

		final EncryptionParameters encryptionParameters = new EncryptionParameters(P.toString(10), Q.toString(10), G.toString(10));
		when(encryptionParametersLoaderMock.load(any())).thenReturn(encryptionParameters);

		final Path setupKeyPath = getPathOfFileInResources(
				Paths.get(PRECOMPUTED_VALUES_PATH, ELECTION_EVENT_ID, Constants.CONFIG_DIR_NAME_OFFLINE, Constants.SETUP_SECRET_KEY_FILE_NAME));
		when(pathResolverMock
				.resolve(Constants.CONFIG_FILES_BASE_DIR, ELECTION_EVENT_ID, Constants.CONFIG_DIR_NAME_OFFLINE, Constants.SETUP_SECRET_KEY_FILE_NAME))
				.thenReturn(setupKeyPath);

		assertDoesNotThrow(
				() -> generateVerificationData.precompute(VOTING_CARD_SET_ID, ELECTION_EVENT_ID, administrationBoardPrivateKeyPEM, ADMIN_BOARD_ID));

		final Path path = ReturnCodeGenerationRequestPayloadFileSystemRepository
				.getStoragePath(pathResolverMock, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, 0);

		final ReturnCodeGenerationRequestPayload payload = objectMapper.readValue(path.toFile(), ReturnCodeGenerationRequestPayload.class);

		assertAll(() -> assertEquals(ELECTION_EVENT_ID, payload.getElectionEventId()),
				() -> assertEquals(VERIFICATION_CARD_SET_ID, payload.getVerificationCardSetId()),
				() -> verify(returnCodeGenerationRequestPayloadRepositoryMock, times(4)).store(any()));
	}

	@Test
	void precomputeInvalidParams() {
		assertThrows(IllegalArgumentException.class, () -> generateVerificationData.precompute("", "", "", ""));
	}

	@Test
	void precomputeInvalidStatus() throws ResourceNotFoundException {
		setStatusForVotingCardSetFromRepository(Status.SIGNED.name(), votingCardSetRepositoryMock);

		when(idleStatusServiceMock.getIdLock(anyString())).thenReturn(true);

		assertThrows(InvalidStatusTransitionException.class,
				() -> generateVerificationData.precompute(VOTING_CARD_SET_ID, ELECTION_EVENT_ID, administrationBoardPrivateKeyPEM, ADMIN_BOARD_ID));
	}

	@Test
	void precomputeInvalidSigningParameters()
			throws ResourceNotFoundException, URISyntaxException, GeneralCryptoLibException, IOException, CertificateException, CMSException {
		setUpService();

		when(idleStatusServiceMock.getIdLock(anyString())).thenReturn(true);
		when(hashService.recursiveHash(any())).thenReturn((new BigInteger("4").toByteArray()));

		final EncryptionParameters encryptionParameters = new EncryptionParameters(P.toString(10), Q.toString(10), G.toString(10));
		when(encryptionParametersLoaderMock.load(any())).thenReturn(encryptionParameters);

		final Path setupKeyPath = getPathOfFileInResources(
				Paths.get(PRECOMPUTED_VALUES_PATH, ELECTION_EVENT_ID, Constants.CONFIG_DIR_NAME_OFFLINE, Constants.SETUP_SECRET_KEY_FILE_NAME));
		when(pathResolverMock
				.resolve(Constants.CONFIG_FILES_BASE_DIR, ELECTION_EVENT_ID, Constants.CONFIG_DIR_NAME_OFFLINE, Constants.SETUP_SECRET_KEY_FILE_NAME))
				.thenReturn(setupKeyPath);

		assertThrows(PayloadSignatureException.class, () -> generateVerificationData.precompute(VOTING_CARD_SET_ID, ELECTION_EVENT_ID, "", ""));
	}

	@Test
	void persistBallotCastingKeysTest(
			@TempDir
			final Path tempDir) throws IOException {

		when(pathResolverMock.resolve(any())).thenReturn(tempDir);

		final Map<String, String> ballotCastingKeyPairs = new HashMap<String, String>() {{
			put("verificationCardId1", "ballotCastingKey1");
			put("verificationCardId2", "ballotCastingKey2");
			put("verificationCardId3", "ballotCastingKey3");
		}};

		final List<String> verificationCardIds = asList("verificationCardId1", "verificationCardId2", "verificationCardId3");
		final List<String> ballotCastingKeys = asList("ballotCastingKey1", "ballotCastingKey2", "ballotCastingKey3");
		final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, 3, new RandomService());
		final List<ElGamalMultiRecipientKeyPair> keyPairs = Collections.nCopies(3, keyPair);
		final ElGamalMultiRecipientCiphertext ciphertext = ElGamalMultiRecipientCiphertext.neutralElement(3, gqGroup);

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> returnCodes = GroupVector.of(ciphertext, ciphertext, ciphertext);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> confirmationKey = GroupVector.of(ciphertext, ciphertext, ciphertext);

		final GenVerDatOutput genVerDatOutput = new GenVerDatOutput(verificationCardIds, keyPairs, ballotCastingKeys, returnCodes, confirmationKey);

		assertDoesNotThrow(() -> generateVerificationData.persistBallotCastingKeys("verificationCardSetId", genVerDatOutput));

		final Stream<Path> streamedFiles = Files.list(tempDir.resolve(ELECTION_EVENT_ID).resolve(Constants.CONFIG_DIR_NAME_OFFLINE)
				.resolve(Constants.CONFIG_BALLOT_CASTING_KEYS_DIRECTORY).resolve("verificationCardSetId"));
		final List<Path> filesList = streamedFiles.collect(Collectors.toList());
		streamedFiles.close();

		for (final Path file : filesList) {
			final String BCK = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
			assertEquals(ballotCastingKeyPairs.get(file.getFileName().toString().split("\\.")[0]), BCK);
		}

	}

	private void setUpService() throws URISyntaxException, ResourceNotFoundException, JsonProcessingException {
		setStatusForVotingCardSetFromRepository(Status.LOCKED.name(), votingCardSetRepositoryMock);

		final Map<String, Object> ballotBoxQueryMap = new HashMap<>();
		ballotBoxQueryMap.put(JsonConstants.ELECTION_EVENT_DOT_ID, ELECTION_EVENT_ID);
		ballotBoxQueryMap.put(JsonConstants.ID, BALLOT_BOX_ID);

		when(pathResolverMock.resolve(any())).thenReturn(getPathOfFileInResources(Paths.get(PRECOMPUTED_VALUES_PATH)));
		when(ballotBoxRepositoryMock.find(ballotBoxQueryMap)).thenReturn(getBallotBoxWithStatus(Status.READY));
		when(votingCardSetRepositoryMock.getVerificationCardSetId(VOTING_CARD_SET_ID)).thenReturn(VERIFICATION_CARD_SET_ID);
		when(voterCodesServiceMock.generateBallotCastingKey()).thenReturn("4");
		Ballot ballot = objectMapper.readValue(BALLOT_JSON, Ballot.class);
		when(ballotService.getBallot(ELECTION_EVENT_ID, BALLOT_ID)).thenReturn(ballot);
	}

}
