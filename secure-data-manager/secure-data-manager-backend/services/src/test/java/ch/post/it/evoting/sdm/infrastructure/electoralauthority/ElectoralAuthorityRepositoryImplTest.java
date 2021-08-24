/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.electoralauthority;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseFixture;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Tests of {@link ElectoralAuthorityRepositoryImpl}.
 */
class ElectoralAuthorityRepositoryImplTest {

	private DatabaseFixture fixture;
	private BallotBoxRepository ballotBoxRepository;
	private ElectoralAuthorityRepositoryImpl repository;

	@BeforeEach
	void setUp() throws OException, IOException {
		fixture = new DatabaseFixture(getClass());
		fixture.setUp();
		ballotBoxRepository = mock(BallotBoxRepository.class);
		repository = new ElectoralAuthorityRepositoryImpl(fixture.databaseManager());
		repository.ballotBoxRepository = ballotBoxRepository;
		repository.initialize();
		URL resource = getClass().getResource(getClass().getSimpleName() + ".json");
		fixture.createDocuments(repository.entityName(), resource);
	}

	@AfterEach
	void tearDown() {
		fixture.tearDown();
	}

	@Test
	void testUpdateRelatedBallotBox() {
		JsonObject ballotBoxes = Json.createObjectBuilder().add(JsonConstants.RESULT,
				Json.createArrayBuilder().add(Json.createObjectBuilder().add(JsonConstants.ALIAS, "4"))
						.add(Json.createObjectBuilder().add(JsonConstants.ALIAS, "5"))).build();
		when(ballotBoxRepository.findByElectoralAuthority("331279febbb0423298d44ee58702d581")).thenReturn(ballotBoxes.toString());
		ballotBoxes = Json.createObjectBuilder().add(JsonConstants.RESULT,
				Json.createArrayBuilder().add(Json.createObjectBuilder().add(JsonConstants.ALIAS, "6"))
						.add(Json.createObjectBuilder().add(JsonConstants.ALIAS, "7"))).build();
		when(ballotBoxRepository.findByElectoralAuthority("331279febbb0423298d44ee58702d582")).thenReturn(ballotBoxes.toString());
		repository.updateRelatedBallotBox(asList("331279febbb0423298d44ee58702d581", "331279febbb0423298d44ee58702d582"));
		try (ODatabaseDocument database = fixture.databaseManager().openDatabase()) {
			ORecordIteratorClass<ODocument> iterator = database.browseClass(repository.entityName());
			while (iterator.hasNext()) {
				ODocument document = iterator.next();
				String id = document.field(JsonConstants.ID, String.class);
				List<String> aliases = document.field(JsonConstants.BALLOT_BOX_ALIAS, List.class);
				switch (id) {
				case "331279febbb0423298d44ee58702d581":
					assertEquals(2, aliases.size());
					assertEquals("4", aliases.get(0));
					assertEquals("5", aliases.get(1));
					break;
				case "331279febbb0423298d44ee58702d582":
					assertEquals(2, aliases.size());
					assertEquals("6", aliases.get(0));
					assertEquals("7", aliases.get(1));
					break;
				default:
					assertEquals(1, aliases.size());
					assertEquals("3", aliases.get(0));
					break;
				}
			}
		}
	}

	@Test
	void testUpdateRelatedBallotBoxNotFound() {
		final List<String> unknownElectoralAuthority = singletonList("unknownElectoralAuthority");
		assertThrows(DatabaseException.class, () -> repository.updateRelatedBallotBox(unknownElectoralAuthority));
	}

	@Test
	void testListByElectionEvent() {
		String json = repository.listByElectionEvent("101549c5a4a04c7b88a0cb9be8ab3df6");
		JsonArray array = JsonUtils.getJsonObject(json).getJsonArray(JsonConstants.RESULT);
		assertEquals(2, array.size());
		Set<String> ids = new HashSet<>();
		for (JsonValue value : array) {
			ids.add(((JsonObject) value).getString(JsonConstants.ID));
		}
		assertTrue(ids.contains("331279febbb0423298d44ee58702d581"));
		assertTrue(ids.contains("331279febbb0423298d44ee58702d582"));
	}

	@Test
	void testListByElectionEventNotFound() {
		String json = repository.listByElectionEvent("unknownElectionEvent");
		JsonArray array = JsonUtils.getJsonObject(json).getJsonArray(JsonConstants.RESULT);
		assertTrue(array.isEmpty());
	}
}
