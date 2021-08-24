/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static ch.post.it.evoting.sdm.utils.JsonUtils.getJsonArray;
import static ch.post.it.evoting.sdm.utils.JsonUtils.getJsonObject;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(ControlComponentKeysAccessorServiceTestSpringConfig.class)
class ControlComponentKeysAccessorServiceTest {

	private static final String ELECTION_EVENT_ID = "0b149cfdaad04b04b990c3b1d4ca7639";
	private static final String ELECTORAL_AUTHORITY_ID = "16e020d934594544a6e17d1e410da513";
	private static final String ELECTORAL_AUTHORITY_REPOSITORY_PATH = "/electoralAuthorityRepository.json";
	private static final String VERIFICATION_CARD_SET_ID = "ffbf3c4fd4314309b5988b3df2668a2c";
	private static final String VOTING_CARD_SET_ID = "4652f56eb88a4d7dac24a4239fbe16c3";
	private static final String VOTING_CARD_SET_REPOSITORY_PATH = "/votingCardSetRepository.json";

	private static JsonObject electoralAuthorityRepositoryJson;
	private static JsonArray mixingKeysJsonArray;
	private static JsonObject votingCardSetRepositoryJson;
	private static JsonArray choiceCodeKeysJsonArray;

	@Autowired
	private ControlComponentKeysAccessorService controlComponentKeysAccessorService;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private VotingCardSetRepository votingCardSetRepositoryMock;

	@Autowired
	private ElectoralAuthorityRepository electoralAuthorityRepositoryMock;

	@BeforeAll
	static void init() throws IOException, URISyntaxException {

		URL repoUrl = ControlComponentKeysAccessorServiceTest.class.getResource(ELECTORAL_AUTHORITY_REPOSITORY_PATH);
		Path repoPath = Paths.get(repoUrl.toURI());
		electoralAuthorityRepositoryJson = getJsonObject(new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8));
		mixingKeysJsonArray = getJsonArray(electoralAuthorityRepositoryJson.getString(Constants.MIX_DEC_KEY_LABEL));

		repoUrl = ControlComponentKeysAccessorServiceTest.class.getResource(VOTING_CARD_SET_REPOSITORY_PATH);
		repoPath = Paths.get(repoUrl.toURI());
		votingCardSetRepositoryJson = getJsonObject(new String(Files.readAllBytes(repoPath), StandardCharsets.UTF_8));
		choiceCodeKeysJsonArray = getJsonArray(votingCardSetRepositoryJson.getString(JsonConstants.CHOICE_CODES_ENCRYPTION_KEY));
	}

	@Test
	void downloadMixingKeys() throws ResourceNotFoundException {
		when(electoralAuthorityRepositoryMock.find(anyString())).thenReturn(electoralAuthorityRepositoryJson.toString());

		final JsonArray mixingKeysJsonArray = controlComponentKeysAccessorService.downloadMixingKeys(ELECTORAL_AUTHORITY_ID);

		assertEquals(mixingKeysJsonArray.toString(), ControlComponentKeysAccessorServiceTest.mixingKeysJsonArray.toString());
	}

	@Test
	void writeMixingKeys() throws IOException {

		assertDoesNotThrow(() -> controlComponentKeysAccessorService.writeMixingKeys(ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID, mixingKeysJsonArray));

		final Path mixingKeysJsonArrayPath = pathResolver
				.resolve(Constants.CONFIG_FILES_BASE_DIR, ELECTION_EVENT_ID, Constants.CONFIG_DIR_NAME_ONLINE,
						Constants.CONFIG_DIR_NAME_ELECTORAL_AUTHORITY, ELECTORAL_AUTHORITY_ID, Constants.MIX_DEC_KEYS_JSON);

		final String mixingKeysJsonArrayStr = new String(Files.readAllBytes(mixingKeysJsonArrayPath), StandardCharsets.UTF_8);

		assertEquals(mixingKeysJsonArrayStr, mixingKeysJsonArray.toString());
	}

	@Test
	void downloadChoiceCodeKeys() {
		when(votingCardSetRepositoryMock.find(anyString())).thenReturn(votingCardSetRepositoryJson.toString());

		final JsonArray choiceCodeKeysJsonArray = assertDoesNotThrow(
				() -> controlComponentKeysAccessorService.downloadChoiceCodeKeys(VOTING_CARD_SET_ID));

		assertEquals(choiceCodeKeysJsonArray.toString(), ControlComponentKeysAccessorServiceTest.choiceCodeKeysJsonArray.toString());
	}

	@Test
	void writeChoiceCodeKeys() throws IOException {

		assertDoesNotThrow(
				() -> controlComponentKeysAccessorService.writeChoiceCodeKeys(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, choiceCodeKeysJsonArray));

		final Path choiceCodeKeysJsonArrayPath = pathResolver
				.resolve(Constants.CONFIG_FILES_BASE_DIR, ELECTION_EVENT_ID, Constants.CONFIG_DIR_NAME_ONLINE,
						Constants.CONFIG_DIR_NAME_VOTERVERIFICATION, VERIFICATION_CARD_SET_ID, Constants.CHOICE_CODES_ENCRYPTION_KEYS_JSON);

		final String choiceCodeKeysJsonArrayStr = new String(Files.readAllBytes(choiceCodeKeysJsonArrayPath), StandardCharsets.UTF_8);

		assertEquals(choiceCodeKeysJsonArrayStr, choiceCodeKeysJsonArray.toString());
	}
}
