/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.exceptions.LambdaException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.commons.PrefixPathResolver;
import ch.post.it.evoting.sdm.config.shares.exception.SharesException;
import ch.post.it.evoting.sdm.config.shares.handler.CreateSharesHandler;
import ch.post.it.evoting.sdm.config.shares.handler.StatelessReadSharesHandler;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.utils.JsonUtils;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(ElectoralAuthorityServiceTestSpringConfig.class)
class ElectoralAuthorityServiceTest {

	private static final ControlComponentKeysAccessorService controlComponentKeysAccessorService = new ControlComponentKeysAccessorService();

	private static final String ELECTION_EVENT_ID = "0b149cfdaad04b04b990c3b1d4ca7639";
	private static final String ELECTORAL_AUTHORITY_ID = "16e020d934594544a6e17d1e410da513";
	private static final String PIN = "222222";
	private static final String PUK = PIN;
	private static final String TEST_MEMBER_0 = "John";
	private static final String TEST_MEMBER_1 = "Peter";
	private static final String RESOURCES_DIR = "src/test/resources/";
	private static final String TARGET_DIR = "target/electoralAuthorityServiceTest/";
	private static final String ELECTORAL_AUTHORITY_REPOSITORY_PATH = "/electoralAuthorityRepository.json";
	private static final String ELECTORAL_AUTHORITY_JSON_WITHOUT_STATUS = "{" + //
			"    \"id\": \"16e020d934594544a6e17d1e410da513\"," + //
			"    \"defaultTitle\": \"Electoral authority\"," + //
			"    \"defaultDescription\": \"A  sample EA\"," + //
			"    \"alias\": \"EA1\"," + //
			"    \"electionEvent\": {" + //
			"        \"id\": \"0b149cfdaad04b04b990c3b1d4ca7639\"" + //
			"    }," + //
			"    \"minimumThreshold\": \"2\"," + //
			"    \"electoralBoard\": [" + //
			"        \"" + TEST_MEMBER_0 + "\"," + //
			"        \"" + TEST_MEMBER_1 + "\"" + //
			"    ]" + //
			"}"; //

	private static JsonObject electoralAuthorityRepositoryJson;
	private static JsonArray mixingKeysJsonArray;

	@Autowired
	private ElectoralAuthorityService electoralAuthorityService;

	@Autowired
	private ElectoralAuthorityRepository electoralAuthorityRepositoryMock;

	@Autowired
	private CreateSharesHandler createSharesHandlerMock;

	@Autowired
	private StatelessReadSharesHandler statelessReadSharesHandlerMock;

	@BeforeAll
	static void setUp() throws IOException, URISyntaxException {
		final Path repoPath = Paths.get(ElectoralAuthorityServiceTest.class.getResource(ELECTORAL_AUTHORITY_REPOSITORY_PATH).toURI());

		electoralAuthorityRepositoryJson = JsonUtils.getJsonObject(new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8));
		mixingKeysJsonArray = JsonUtils.getJsonArray(electoralAuthorityRepositoryJson.getString(Constants.MIX_DEC_KEY_LABEL));
	}

	@BeforeEach
	void init() {
		electoralAuthorityService.init();
	}

	@Test
	void notSignAnElectoralAuthorityWithoutStatus() throws IOException, GeneralCryptoLibException, ResourceNotFoundException {

		when(electoralAuthorityRepositoryMock.find(anyString())).thenReturn(ELECTORAL_AUTHORITY_JSON_WITHOUT_STATUS);

		assertFalse(electoralAuthorityService.sign(ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID, SigningTestData.PRIVATE_KEY_PEM));
	}

	@Test
	void downloadAndWriteMixingKeys() throws IOException, GeneralCryptoLibException, SharesException, ResourceNotFoundException {

		final PathResolver resourcesPathResolver = new PrefixPathResolver(Paths.get(RESOURCES_DIR).toAbsolutePath().toString());
		final PathResolver targetPathResolver = new PrefixPathResolver(Paths.get(TARGET_DIR).toAbsolutePath().toString());

		doAnswer(invocation -> {
			try {
				JsonArray mixingKeysJsonArray = controlComponentKeysAccessorService.downloadMixingKeys(ELECTORAL_AUTHORITY_ID);

				controlComponentKeysAccessorService.writeMixingKeys(ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID, mixingKeysJsonArray);
			} catch (IOException e) {
				throw new LambdaException(e);
			}

			return null;
		}).when(createSharesHandlerMock).writeShare(anyInt(), anyString(), anyString(), anyString(), any(), any());

		when(electoralAuthorityRepositoryMock.find(anyString())).thenReturn(electoralAuthorityRepositoryJson.toString());
		when(statelessReadSharesHandlerMock.getSmartcardLabel()).thenReturn(getLabelForMember(TEST_MEMBER_0));

		final Map<String, CreateSharesHandler> createSharesHandlerElGamalMap = new HashMap<>();
		createSharesHandlerElGamalMap.put(ELECTORAL_AUTHORITY_ID, createSharesHandlerMock);

		ReflectionTestUtils.setField(electoralAuthorityService, "puk", PUK);
		ReflectionTestUtils.setField(electoralAuthorityService, "createSharesHandlerElGamalMap", createSharesHandlerElGamalMap);
		ReflectionTestUtils.setField(electoralAuthorityService, "hashService", new HashService());
		ReflectionTestUtils.setField(electoralAuthorityService, "pathResolver", resourcesPathResolver);
		ReflectionTestUtils.setField(electoralAuthorityService, "statelessReadSharesHandler", statelessReadSharesHandlerMock);

		ReflectionTestUtils.setField(controlComponentKeysAccessorService, "electoralAuthorityRepository", electoralAuthorityRepositoryMock);
		ReflectionTestUtils.setField(controlComponentKeysAccessorService, "pathResolver", targetPathResolver);

		electoralAuthorityService.writeShare(ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID, 0, PIN);

		final Path mixingKeysJsonArrayPath = targetPathResolver
				.resolve(Constants.CONFIG_FILES_BASE_DIR, ELECTION_EVENT_ID, Constants.CONFIG_DIR_NAME_ONLINE,
						Constants.CONFIG_DIR_NAME_ELECTORAL_AUTHORITY, ELECTORAL_AUTHORITY_ID, Constants.MIX_DEC_KEYS_JSON);

		final String mixingKeysJsonArrayStr = new String(Files.readAllBytes(mixingKeysJsonArrayPath), StandardCharsets.UTF_8);

		assertEquals(mixingKeysJsonArrayStr, mixingKeysJsonArray.toString());
	}

	private String getLabelForMember(final String member) {
		final String hashValue = getHashValueForMember(member);

		return hashValue.substring(0, Math.min(hashValue.length(), Constants.SMART_CARD_LABEL_MAX_LENGTH));
	}

	private String getHashValueForMember(final String member) {

		try {
			final MessageDigest mdEnc = MessageDigest.getInstance(Constants.MESSAGE_DIGEST_ALGORITHM);
			final byte[] memberByteArray = member.getBytes(StandardCharsets.UTF_8);

			mdEnc.update(memberByteArray, 0, memberByteArray.length);

			return Base64.getEncoder().encodeToString(mdEnc.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No such algorithm exception when getting hash for member: " + member, e);
		}

	}
}
