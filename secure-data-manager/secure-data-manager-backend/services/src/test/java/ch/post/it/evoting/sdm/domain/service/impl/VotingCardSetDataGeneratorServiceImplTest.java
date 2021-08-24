/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.sdm.application.service.ControlComponentKeysAccessorService;
import ch.post.it.evoting.sdm.application.service.ElectionEventService;
import ch.post.it.evoting.sdm.application.service.PlatformRootCAService;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.PrefixPathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.domain.service.utils.SystemTenantPublicKeyLoader;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * JUnit tests for class {@link VotingCardSetDataGeneratorServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class VotingCardSetDataGeneratorServiceImplTest {

	private static final String resourcesDir = "src/test/resources/";
	private static final String targetDir = "target/votingCardSetDataGeneratorServiceTest/";
	private static final String electionEventId = "0b149cfdaad04b04b990c3b1d4ca7639";
	private static final String votingCardSetId = "4652f56eb88a4d7dac24a4239fbe16c3";
	private static final String verificationCardSetId = "ffbf3c4fd4314309b5988b3df2668a2c";
	private static final String votingCardSetRepositoryPath = "/votingCardSetRepository.json";
	private static final String ballotBoxRepositoryPath = "/ballotBoxRepository.json";
	private static final String electionEventRepositoryPath = "/electionEventRepository.json";
	private static final String platformRootCAPath = "/mixing/platformRootCA.pem";
	private static final String secureDataManagerPrivateKeyPath = "/secureDataManagerPrivateKey.json";
	private static final String certificatePropertiesDirname = "certificateProperties";
	private static final String credentialSignCertificatePropertiesFilename = "credentialSignX509Certificate.properties";
	private static final String credentialAuthCertificatePropertiessFilename = "credentialAuthX509Certificate.properties";
	private static final String verificationCardSetCertificateFilename = "verificationCardSetX509Certificate.properties";

	private static PathResolver resourcesPathResolver;
	private static PathResolver targetPathResolver;
	private static JsonObject votingCardSetRepositoryJson;
	private static JsonObject ballotBoxRepositoryJson;
	private static JsonObject electionEventRepositoryJson;
	private static X509Certificate platformRootCA;
	private static ElGamalPrivateKey secureDataManagerPrivateKey;
	private static JsonArray choiceCodeKeysJsonArray;
	@Spy
	@InjectMocks
	private final VotingCardSetDataGeneratorServiceImpl votingCardSetDataGeneratorService = new VotingCardSetDataGeneratorServiceImpl();
	@InjectMocks
	private final ControlComponentKeysAccessorService controlComponentKeysAccessorService = new ControlComponentKeysAccessorService();
	@Mock
	private VotingCardSetRepository votingCardSetRepositoryMock;
	@Mock
	private BallotBoxRepository ballotBoxRepositoryMock;
	@Mock
	private ElectionEventRepository electionEventRepositoryMock;
	@Mock
	private ElectionEventService electionEventServiceMock;
	@Mock
	private SystemTenantPublicKeyLoader systemTenantPublicKeyLoaderMock;
	@Mock
	private PlatformRootCAService platformRootCAServiceMock;
	@Mock
	private CCPublicKeySignatureValidator keySignatureValidatorMock;

	@BeforeAll
	static void init() throws URISyntaxException, IOException, GeneralCryptoLibException {
		setDefaultMockedPathResolvers();
		setDefaultMockedReturnValues();
	}

	private static void setDefaultMockedPathResolvers() {
		String resourcesPath = Paths.get(resourcesDir).toAbsolutePath().toString();
		resourcesPathResolver = new PrefixPathResolver(resourcesPath);

		String targetPath = Paths.get(targetDir).toAbsolutePath().toString();
		targetPathResolver = new PrefixPathResolver(targetPath);
	}

	private static void setDefaultMockedReturnValues() throws URISyntaxException, IOException, GeneralCryptoLibException {
		URL repoUrl = VotingCardSetDataGeneratorServiceImplTest.class.getResource(votingCardSetRepositoryPath);
		Path repoPath = Paths.get(repoUrl.toURI());
		votingCardSetRepositoryJson = JsonUtils.getJsonObject(new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8));

		repoUrl = VotingCardSetDataGeneratorServiceImplTest.class.getResource(ballotBoxRepositoryPath);
		repoPath = Paths.get(repoUrl.toURI());
		ballotBoxRepositoryJson = JsonUtils.getJsonObject(new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8));

		repoUrl = VotingCardSetDataGeneratorServiceImplTest.class.getResource(electionEventRepositoryPath);
		repoPath = Paths.get(repoUrl.toURI());
		electionEventRepositoryJson = JsonUtils.getJsonObject(new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8));

		repoUrl = VotingCardSetDataGeneratorServiceImplTest.class.getResource(platformRootCAPath);
		repoPath = Paths.get(repoUrl.toURI());
		platformRootCA = (X509Certificate) PemUtils.certificateFromPem(new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8));

		repoUrl = VotingCardSetDataGeneratorServiceImplTest.class.getResource(secureDataManagerPrivateKeyPath);
		repoPath = Paths.get(repoUrl.toURI());
		String secureDataManagerPrivateKeyJsonStr = new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8);
		secureDataManagerPrivateKey = ElGamalPrivateKey.fromJson(secureDataManagerPrivateKeyJsonStr);

		choiceCodeKeysJsonArray = JsonUtils.getJsonArray(votingCardSetRepositoryJson.getString(JsonConstants.CHOICE_CODES_ENCRYPTION_KEY));
	}

	@BeforeEach
	void setUp() {
		setDefaultMockedMethods();
		setDefaultMockedValues();
	}

	@Test
	void generate() throws Exception {
		when(votingCardSetRepositoryMock.find(anyString())).thenReturn(votingCardSetRepositoryJson.toString());
		when(ballotBoxRepositoryMock.find(anyString())).thenReturn(ballotBoxRepositoryJson.toString());
		when(electionEventRepositoryMock.find(anyString())).thenReturn(electionEventRepositoryJson.toString());
		when(platformRootCAServiceMock.load()).thenReturn(platformRootCA);
		doNothing().when(keySignatureValidatorMock).checkChoiceCodesEncryptionKeySignature(any(), any(), any(), anyString(), anyString());

		votingCardSetDataGeneratorService.generate(votingCardSetId, electionEventId);

		Path choiceKeysJsonArrayPath = targetPathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_VOTERVERIFICATION, verificationCardSetId, Constants.CHOICE_CODES_ENCRYPTION_KEYS_JSON);

		String choiceCodeKeysJsonArrayStr = new String(Files.readAllBytes(choiceKeysJsonArrayPath), StandardCharsets.UTF_8);

		assertEquals(choiceCodeKeysJsonArray.toString(), choiceCodeKeysJsonArrayStr);
	}

	@Test
	void generateWithNullVotingCardSetId() {
		DataGeneratorResponse result = votingCardSetDataGeneratorService.generate(null, electionEventId);
		assertFalse(result.isSuccessful());
	}

	@Test
	void generateWithEmptyVotingCardSetId() {
		DataGeneratorResponse result = votingCardSetDataGeneratorService.generate("", electionEventId);
		assertFalse(result.isSuccessful());
	}

	@Test
	void generateWithEmptyVotingCardSet() {
		when(votingCardSetRepositoryMock.find(anyString())).thenReturn(JsonConstants.EMPTY_OBJECT);

		DataGeneratorResponse result = votingCardSetDataGeneratorService.generate(votingCardSetId, electionEventId);
		assertFalse(result.isSuccessful());
	}

	private void setDefaultMockedMethods() {
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "pathResolver", resourcesPathResolver);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "controlComponentKeysAccessorService", controlComponentKeysAccessorService);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "votingCardSetRepository", votingCardSetRepositoryMock);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "ballotBoxRepository", ballotBoxRepositoryMock);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "electionEventRepository", electionEventRepositoryMock);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "electionEventService", electionEventServiceMock);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "systemTenantPublicKeyLoader", systemTenantPublicKeyLoaderMock);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "platformRootCAService", platformRootCAServiceMock);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "keySignatureValidator", keySignatureValidatorMock);

		ReflectionTestUtils.setField(controlComponentKeysAccessorService, "pathResolver", targetPathResolver);
	}

	private void setDefaultMockedValues() {
		Path certificatePropertiesPath = resourcesPathResolver.resolve(Constants.SDM_CONFIG_DIR_NAME, certificatePropertiesDirname);
		String credentialSignCertificatePropertiesMock = resourcesPathResolver
				.resolve(certificatePropertiesPath.toString(), credentialSignCertificatePropertiesFilename).toString();
		String credentialAuthCertificatePropertiesMock = resourcesPathResolver
				.resolve(certificatePropertiesPath.toString(), credentialAuthCertificatePropertiessFilename).toString();
		String verificationCardSetCertificatePropertiesMock = resourcesPathResolver
				.resolve(certificatePropertiesPath.toString(), verificationCardSetCertificateFilename).toString();

		ReflectionTestUtils
				.setField(votingCardSetDataGeneratorService, "credentialSignCertificateProperties", credentialSignCertificatePropertiesMock);
		ReflectionTestUtils
				.setField(votingCardSetDataGeneratorService, "credentialAuthCertificateProperties", credentialAuthCertificatePropertiesMock);
		ReflectionTestUtils.setField(votingCardSetDataGeneratorService, "verificationCardSetCertificateProperties",
				verificationCardSetCertificatePropertiesMock);
	}
}
