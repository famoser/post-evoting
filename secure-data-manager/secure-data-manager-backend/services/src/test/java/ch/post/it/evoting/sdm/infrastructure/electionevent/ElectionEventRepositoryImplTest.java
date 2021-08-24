/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.electionevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.orientechnologies.common.exception.OException;

import ch.post.it.evoting.sdm.infrastructure.DatabaseFixture;

/**
 * Tests of {@link ElectionEventRepositoryImpl}.
 */
class ElectionEventRepositoryImplTest {

	private DatabaseFixture fixture;
	private ElectionEventRepositoryImpl repository;

	@BeforeEach
	void setUp() throws OException, IOException {
		fixture = new DatabaseFixture(getClass());
		fixture.setUp();
		repository = new ElectionEventRepositoryImpl(fixture.databaseManager());
		repository.initialize();
		URL resource = getClass().getResource(getClass().getSimpleName() + ".json");
		fixture.createDocuments(repository.entityName(), resource);
	}

	@AfterEach
	void tearDown() {
		fixture.tearDown();
	}

	@Test
	void testGetElectionEventAlias() {
		assertEquals("legislative2017T2", repository.getElectionEventAlias("101549c5a4a04c7b88a0cb9be8ab3df6"));
	}

	@Test
	void testGetElectionEventAliasNotFound() {
		assertTrue(repository.getElectionEventAlias("unknownElectionEvent").isEmpty());
	}

	@Test
	void testListIds() {
		List<String> ids = repository.listIds();
		assertEquals(2, ids.size());
		assertTrue(ids.contains("101549c5a4a04c7b88a0cb9be8ab3df6"));
		assertTrue(ids.contains("101549c5a4a04c7b88a0cb9be8ab3df7"));
	}
}
