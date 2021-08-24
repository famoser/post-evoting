/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.sdm.domain.model.EntityRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

/**
 * JUnit for the class {@link ConfigurationEntityStatusServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationEntityStatusServiceImplTest {

	private final String newStatus = "";
	private final String id = "";
	@InjectMocks
	private final ConfigurationEntityStatusService configurationEntityStatusService = new ConfigurationEntityStatusServiceImpl();
	private String updateResult = JsonConstants.EMPTY_OBJECT;
	@Mock
	private EntityRepository baseRepository;

	@Test
	void updateEmptyObjectReturned() {
		when(baseRepository.update(anyString())).thenReturn(updateResult);

		assertEquals(JsonConstants.EMPTY_OBJECT, configurationEntityStatusService.update(newStatus, id, baseRepository));
	}

	@Test
	void update() {
		updateResult = JsonConstants.RESULT_EMPTY;
		when(baseRepository.update(anyString())).thenReturn(updateResult);

		assertEquals(JsonConstants.RESULT_EMPTY, configurationEntityStatusService.update(newStatus, id, baseRepository));
	}
}
