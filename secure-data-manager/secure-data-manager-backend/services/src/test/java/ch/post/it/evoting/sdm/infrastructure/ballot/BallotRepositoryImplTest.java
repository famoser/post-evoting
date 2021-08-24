/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.ballot;

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
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Tests of {@link BallotRepositoryImpl}.
 */
class BallotRepositoryImplTest {

	private DatabaseFixture fixture;
	private DatabaseManager manager;
	private BallotBoxRepository ballotBoxRepository;
	private BallotRepositoryImpl repository;

	@BeforeEach
	void setUp() throws OException, IOException {
		fixture = new DatabaseFixture(getClass());
		fixture.setUp();
		manager = fixture.databaseManager();
		ballotBoxRepository = mock(BallotBoxRepository.class);
		repository = new BallotRepositoryImpl(manager);
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
	void testListAliases() {
		List<String> aliases = repository.listAliases("3d3da2b8ad05485c9a5daab870e8c1c5");
		assertEquals(1, aliases.size());
		assertEquals("343", aliases.get(0));
	}

	@Test
	void testListAliasesNotFound() {
		assertTrue(repository.listAliases("unknownBallot").isEmpty());
	}

	@Test
	void testUpdateRelatedBallotBox() {
		when(ballotBoxRepository.listAliases("3d3da2b8ad05485c9a5daab870e8c1c5")).thenReturn(asList("ballotBox1", "ballotBox2"));
		when(ballotBoxRepository.listAliases("6770803ba4fa48d0afd1aeb9106af2dc")).thenReturn(asList("ballotBox3", "ballotBox4"));
		repository.updateRelatedBallotBox(asList("3d3da2b8ad05485c9a5daab870e8c1c5", "6770803ba4fa48d0afd1aeb9106af2dc"));
		try (ODatabaseDocument database = manager.openDatabase()) {
			ORecordIteratorClass<ODocument> iterator = database.browseClass(repository.entityName());
			while (iterator.hasNext()) {
				ODocument document = iterator.next();
				String id = document.field(JsonConstants.ID, String.class);
				String ballotBoxes = document.field("ballotBoxes", String.class);
				switch (id) {
				case "3d3da2b8ad05485c9a5daab870e8c1c5":
					assertEquals("ballotBox1,ballotBox2", ballotBoxes);
					break;
				case "6770803ba4fa48d0afd1aeb9106af2dc":
					assertEquals("ballotBox3,ballotBox4", ballotBoxes);
					break;
				default:
					assertEquals("56,70,72,85,119,152,176,177,198,203,222", ballotBoxes);
				}
			}
		}
	}

	@Test
	void testUpdateRelatedBallotBoxNoyFound() {
		final List<String> unknownBallot = singletonList("unknownBallot");
		assertThrows(DatabaseException.class, () -> repository.updateRelatedBallotBox(unknownBallot));
	}

	@Test
	void testUpdateSignedBallot() {
		repository.updateSignedBallot("3d3da2b8ad05485c9a5daab870e8c1c5", "signedBallot");
		String json = repository.find("3d3da2b8ad05485c9a5daab870e8c1c5");
		JsonObject object = JsonUtils.getJsonObject(json);
		assertEquals("signedBallot", object.getString(JsonConstants.SIGNED_OBJECT));
	}

	@Test
	void testUpdateSignedBallotNotFound() {
		assertThrows(DatabaseException.class, () -> repository.updateSignedBallot("unknownObject", "signedBallot"));
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

		assertTrue(ids.containsAll(asList("3d3da2b8ad05485c9a5daab870e8c1c5", "6770803ba4fa48d0afd1aeb9106af2dc")));
	}

	@Test
	void testListByElectionEventNotFound() {
		String json = repository.listByElectionEvent("unknownElectionEvent");
		JsonArray array = JsonUtils.getJsonObject(json).getJsonArray(JsonConstants.RESULT);
		assertTrue(array.isEmpty());
	}
}
