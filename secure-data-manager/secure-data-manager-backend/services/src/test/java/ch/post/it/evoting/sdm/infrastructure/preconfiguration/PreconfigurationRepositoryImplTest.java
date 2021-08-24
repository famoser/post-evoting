/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.preconfiguration;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;

import ch.post.it.evoting.sdm.domain.config.Config;
import ch.post.it.evoting.sdm.domain.model.preconfiguration.PreconfigurationRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("test")
class PreconfigurationRepositoryImplTest {
	private static final Set<String> SYSTEM_CLASSES = new HashSet<>(
			asList("OFunction", "OIdentity", "ORestricted", "ORIDs", "ORole", "OSchedule", "OTriggered", "OUser", "OSecurityPolicy"));

	@Autowired
	private PreconfigurationRepository repository;

	@Autowired
	private DatabaseManager manager;

	@Value("${elections.config.filename}")
	private String filename;

	@AfterEach
	void tearDown() throws IOException {
		try (ODatabaseDocument database = manager.openDatabase()) {
			OSchema schema = database.getMetadata().getSchema();
			for (OClass oClass : schema.getClasses()) {
				if (!SYSTEM_CLASSES.contains(oClass.getName())) {
					oClass.truncate();
				}
			}
		}
	}

	@Test
	void readFromFileAndSave() throws IOException, URISyntaxException {
		Path path = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
		String result = repository.readFromFileAndSave(path.toAbsolutePath().toString());

		assertNotNull(result);
		JsonObject jsonObject = JsonUtils.getJsonObject(result);
		assertNotNull(jsonObject);
		assertFalse(jsonObject.isEmpty());
		assertFalse(jsonObject.getJsonArray(JsonConstants.ADMINISTRATION_AUTHORITIES).isEmpty());
		assertFalse(jsonObject.getJsonArray(JsonConstants.ELECTION_EVENTS).isEmpty());
		assertFalse(jsonObject.getJsonArray(JsonConstants.VOTING_CARD_SETS).isEmpty());
		assertFalse(jsonObject.getJsonArray(JsonConstants.BALLOT_BOXES).isEmpty());
	}

	@Test
	void readFromFileAndSaveExceptionDuplicated() throws IOException, URISyntaxException {
		Path path = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
		repository.readFromFileAndSave(path.toAbsolutePath().toString());
		String result = repository.readFromFileAndSave(path.toAbsolutePath().toString());

		assertNotNull(result);
		JsonObject jsonObject = JsonUtils.getJsonObject(result);
		assertNotNull(jsonObject);
		assertFalse(jsonObject.isEmpty());
		assertTrue(jsonObject.getJsonArray(JsonConstants.ADMINISTRATION_AUTHORITIES).isEmpty());
		assertTrue(jsonObject.getJsonArray(JsonConstants.ELECTION_EVENTS).isEmpty());
		assertTrue(jsonObject.getJsonArray(JsonConstants.VOTING_CARD_SETS).isEmpty());
		assertTrue(jsonObject.getJsonArray(JsonConstants.BALLOT_BOXES).isEmpty());
	}
}
