/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.PartialResultsRepository;

public class ChoiceCodesDecryptionResultsHandlerTest {

	private static final UUID CORRELATION_ID = new UUID(0, 0);
	@ClassRule
	public static EnvironmentVariables environmentVariables = new EnvironmentVariables();
	private PartialResultsRepository<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>> choiceCodesPartialResultsRepository;

	private ChoiceCodesDecryptionResultsHandler sut;

	private int count;

	/**
	 * 4 queues are defined, thus 4 results need to be returned from the choiceCodesPartialResultsRepository for the results to be considered ready
	 */
	@Before
	@SuppressWarnings("unchecked")
	public void setup() {
		choiceCodesPartialResultsRepository = mock(PartialResultsRepository.class);
		sut = new ChoiceCodesDecryptionResultsHandler(choiceCodesPartialResultsRepository);
		environmentVariables.set("CC_QUEUE_NAMES",
				"{\"g4\": {\"cg-comp\": {\"res\": \"cg-comp-g4-res\", \"req\": \"cg-comp-g4-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g4-res\", \"req\": \"cg-keygen-g4-req\"}}, \"g3\": {\"cg-comp\": {\"res\": \"cg-comp-g3-res\", \"req\": \"cg-comp-g3-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g3-res\", \"req\": \"cg-keygen-g3-req\"}}, \"g2\": {\"cg-comp\": {\"res\": \"cg-comp-g2-res\", \"req\": \"cg-comp-g2-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g2-res\", \"req\": \"cg-keygen-g2-req\"}}, \"g1\": {\"cg-comp\": {\"res\": \"cg-comp-g1-res\", \"req\": \"cg-comp-g1-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g1-res\", \"req\": \"cg-keygen-g1-req\"}}, \"v1\": {\"cv-comp\": {\"res\": \"cv-comp-v1-res\", \"req\": \"cv-comp-v1-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v1-res\", \"req\": \"cv-dec-v1-req\"}}, \"v2\": {\"cv-comp\": {\"res\": \"cv-comp-v2-res\", \"req\": \"cv-comp-v2-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v2-res\", \"req\": \"cv-dec-v2-req\"}}, \"v3\": {\"cv-comp\": {\"res\": \"cv-comp-v3-res\", \"req\": \"cv-comp-v3-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v3-res\", \"req\": \"cv-dec-v3-req\"}}, \"m1\": {\"md-keygen\": {\"res\": \"md-keygen-m1-res\", \"req\": \"md-keygen-m1-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m1-res\", \"req\": \"md-mixdec-m1-req\"}}, \"m3\": {\"md-keygen\": {\"res\": \"md-keygen-m3-res\", \"req\": \"md-keygen-m3-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m3-res\", \"req\": \"md-mixdec-m3-req\"}}, \"m2\": {\"md-keygen\": {\"res\": \"md-keygen-m2-res\", \"req\": \"md-keygen-m2-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m2-res\", \"req\": \"md-mixdec-m2-req\"}}, \"v4\": {\"cv-comp\": {\"res\": \"cv-comp-v4-res\", \"req\": \"cv-comp-v4-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v4-res\", \"req\": \"cv-dec-v4-req\"}}}");
		count = sut.getPartialResultsCount();
	}

	@Test
	public void areResultsReadyPositiveTest() {
		when(choiceCodesPartialResultsRepository.deleteListIfHasAll(CORRELATION_ID, count)).thenReturn(Optional.of(emptyList()));
		assertTrue(sut.handleResultsIfReady(CORRELATION_ID).isPresent());
	}

	@Test
	public void areResultsReadyNegativeTest() {
		when(choiceCodesPartialResultsRepository.deleteListIfHasAll(CORRELATION_ID, count)).thenReturn(Optional.empty());
		assertFalse(sut.handleResultsIfReady(CORRELATION_ID).isPresent());
	}

	@Test
	public void handleResultsIfReadyAndChecked() {
		List<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>> expected = Arrays
				.asList(createChoiceCodeVerDTO(), createChoiceCodeVerDTO(), createChoiceCodeVerDTO(), createChoiceCodeVerDTO());
		when(choiceCodesPartialResultsRepository.deleteListIfHasAll(CORRELATION_ID, count)).thenReturn(Optional.of(expected));
		Optional<List<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>>> result = sut.handleResultsIfReady(CORRELATION_ID);
		assertTrue(result.isPresent());
		assertEquals(expected, result.get());
	}

	@Test
	public void handleResultsIfReadyNotChecked() {
		List<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>> expected = Arrays
				.asList(createChoiceCodeVerDTO(), createChoiceCodeVerDTO(), createChoiceCodeVerDTO(), createChoiceCodeVerDTO());
		when(choiceCodesPartialResultsRepository.deleteListIfHasAll(CORRELATION_ID, count)).thenReturn(Optional.of(expected));
		List<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>> result = sut.handleResultsIfReady(CORRELATION_ID).get();
		assertEquals(expected, result);
	}

	public void handleResultsWhenNotReady() {
		List<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>> expected = asList(createChoiceCodeVerDTO(),
				createChoiceCodeVerDTO());
		when(choiceCodesPartialResultsRepository.deleteListIfHasAll(CORRELATION_ID, count)).thenReturn(Optional.of(expected));
		Optional<List<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>>> result = sut.handleResultsIfReady(CORRELATION_ID);
		assertFalse(result.isPresent());
	}

	private ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload> createChoiceCodeVerDTO() {
		ChoiceCodesVerificationDecryptResPayload payload = new ChoiceCodesVerificationDecryptResPayload();
		return new ReturnCodeComputationDTO<>(CORRELATION_ID, "reqId", "eeId", "verCardSetId", "verCardId", payload);
	}
}
