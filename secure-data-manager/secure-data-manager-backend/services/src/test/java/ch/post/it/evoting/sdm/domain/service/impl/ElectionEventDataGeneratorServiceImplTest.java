/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * Junit for the class {@link ElectionEventDataGeneratorServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ElectionEventDataGeneratorServiceImplTest {

	@InjectMocks
	@Spy
	private final ElectionEventDataGeneratorServiceImpl electionEventDataGeneratorService = new ElectionEventDataGeneratorServiceImpl();

	@Mock
	private PathResolver pathResolverMock;

	@Mock
	private Path configPathMock;

	@Mock
	private ElectionEventRepository electionEventRepositoryMock;

	private String electionEventId;

	@Test
	void generateWithIdNull() throws IOException {
		assertFalse(electionEventDataGeneratorService.generate(null).isSuccessful());
	}

	@Test
	void generateWithIdEmpty() throws IOException {
		electionEventId = "";

		assertFalse(electionEventDataGeneratorService.generate(electionEventId).isSuccessful());
	}

	@Test
	void generateMakePathThrowIOException() throws IOException {
		electionEventId = "123";

		when(pathResolverMock.resolve(anyString())).thenReturn(configPathMock);
		doThrow(new IOException()).when(electionEventDataGeneratorService).makePath(configPathMock);

		assertFalse(electionEventDataGeneratorService.generate(electionEventId).isSuccessful());
	}

	@Test
	void generateElectionEventNotFound() throws IOException {
		electionEventId = "123";

		when(pathResolverMock.resolve(anyString())).thenReturn(configPathMock);
		doReturn(configPathMock).when(electionEventDataGeneratorService).makePath(configPathMock);
		when(electionEventRepositoryMock.find(electionEventId)).thenReturn(JsonConstants.EMPTY_OBJECT);

		assertFalse(electionEventDataGeneratorService.generate(electionEventId).isSuccessful());
	}
}
