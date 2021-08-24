/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.votingcardset;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.orientechnologies.common.exception.OException;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.ballot.BallotRepository;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseFixture;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Tests of {@link VotingCardSetRepositoryImpl}.
 */
class VotingCardSetRepositoryImplTest {

	private DatabaseFixture fixture;
	private BallotBoxRepository ballotBoxRepository;
	private BallotRepository ballotRepository;
	private VotingCardSetRepositoryImpl repository;

	@BeforeEach
	void setUp() throws OException, IOException {
		fixture = new DatabaseFixture(getClass());
		fixture.setUp();
		DatabaseManager manager = fixture.databaseManager();
		ballotBoxRepository = mock(BallotBoxRepository.class);
		ballotRepository = mock(BallotRepository.class);
		repository = new VotingCardSetRepositoryImpl(manager);
		repository.ballotBoxRepository = ballotBoxRepository;
		repository.ballotRepository = ballotRepository;
		repository.initialize();
		URL resource = getClass().getResource(getClass().getSimpleName() + ".json");
		fixture.createDocuments(repository.entityName(), resource);
	}

	@AfterEach
	void tearDown() {
		fixture.tearDown();
	}

	@Test
	void testGetBallotBoxId() {
		assertEquals("268872255b9b44f39c5404f3ebd85c07", repository.getBallotBoxId("1d9bf23fecd24f899c30b11fe1a6cb5f"));
	}

	@Test
	void testGetBallotBoxIdNotFound() {
		assertTrue(repository.getBallotBoxId("unknownVotingCardSet").isEmpty());
	}

	@Test
	void testUpdateRelatedBallot() {
		JsonObject ballotBox = Json.createObjectBuilder().add(JsonConstants.ALIAS, "ballotBoxAlias")
				.add(JsonConstants.BALLOT, Json.createObjectBuilder().add(JsonConstants.ID, "ballotId")).build();
		when(ballotBoxRepository.find("268872255b9b44f39c5404f3ebd85c07")).thenReturn(ballotBox.toString());
		JsonObject ballot = Json.createObjectBuilder().add(JsonConstants.ALIAS, "ballotAlias").build();
		when(ballotRepository.find("ballotId")).thenReturn(ballot.toString());
		repository.updateRelatedBallot(singletonList("1d9bf23fecd24f899c30b11fe1a6cb5f"));
		String json = repository.find("1d9bf23fecd24f899c30b11fe1a6cb5f");
		JsonObject object = JsonUtils.getJsonObject(json);
		assertEquals("ballotBoxAlias", object.getString(JsonConstants.BALLOT_BOX_ALIAS));
		assertEquals("ballotAlias", object.getString(JsonConstants.BALLOT_ALIAS));
	}

	@Test
	void testUpdateRelatedVerificationCardSet() {
		repository.updateRelatedVerificationCardSet("1d9bf23fecd24f899c30b11fe1a6cb5f", "verificationCardSetId");
		String json = repository.find("1d9bf23fecd24f899c30b11fe1a6cb5f");
		assertEquals("verificationCardSetId", JsonUtils.getJsonObject(json).getString(JsonConstants.VERIFICATION_CARD_SET_ID));
	}

	@Test
	void testUpdateRelatedVerificationCardSetNotFound() {
		assertThrows(DatabaseException.class, () -> repository.updateRelatedVerificationCardSet("unknownVotingCardSet", "verificationCardSetId"));
	}

	@Test
	void testListByElectionEvent() {
		String json = repository.listByElectionEvent("101549c5a4a04c7b88a0cb9be8ab3df6");
		JsonArray array = JsonUtils.getJsonObject(json).getJsonArray(JsonConstants.RESULT);
		assertEquals(1, array.size());
		JsonObject object = array.getJsonObject(0);
		assertEquals("1d9bf23fecd24f899c30b11fe1a6cb5f", object.getString(JsonConstants.ID));

	}

	@Test
	void testListByElectionEventUnknown() {
		String json = repository.listByElectionEvent("unknownElectionEvent");
		JsonArray array = JsonUtils.getJsonObject(json).getJsonArray(JsonConstants.RESULT);
		assertTrue(array.isEmpty());
	}

	@Test
	void testGetVotingCardSetAlias() {
		final String alias = repository.getVotingCardSetAlias("1d9bf23fecd24f899c30b11fe1a6cb5f");
		assertEquals("vcs_133", alias);
	}

	@Test
	void testGetVotingCardSetAliasWithNullAndInvalidParameter() {
		assertThrows(NullPointerException.class, () -> repository.getVotingCardSetAlias(null));
		assertThrows(IllegalArgumentException.class, () -> repository.getVotingCardSetAlias("1d9"));
	}

}
