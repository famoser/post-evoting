/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.util.UriComponentsBuilder;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignatureException;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.application.service.GenerateVerificationData;
import ch.post.it.evoting.sdm.application.service.VotingCardSetComputationService;
import ch.post.it.evoting.sdm.application.service.VotingCardSetPreparationService;
import ch.post.it.evoting.sdm.application.service.VotingCardSetService;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetUpdateInputData;
import ch.post.it.evoting.sdm.infrastructure.cc.PayloadStorageException;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
class VotingCardSetControllerTest {

	private final String VALID_ELECTION_EVENT_ID = "17ccbe962cf341bc93208c26e911090c";
	private final String VALID_VOTING_CARD_SET_ID = "17ccbe962cf341bc93208c26e911090c";

	@Mock
	GenerateVerificationData generateVerificationData;

	@Mock
	VotingCardSetPreparationService votingCardSetPreparationService;

	@Mock
	VotingCardSetComputationService votingCardSetComputationService;

	@Mock
	VotingCardSetService votingCardSetService;

	@InjectMocks
	VotingCardSetController sut = new VotingCardSetController();

	@Test
	void testCorrectStatusTransition()
			throws ResourceNotFoundException, IOException, GeneralCryptoLibException, PayloadStorageException, PayloadSignatureException,
			PayloadVerificationException {

		assertEquals(HttpStatus.NO_CONTENT, getResultingStatusCode(Status.PRECOMPUTED));
		assertEquals(HttpStatus.NO_CONTENT, getResultingStatusCode(Status.COMPUTING));
		assertEquals(HttpStatus.NO_CONTENT, getResultingStatusCode(Status.VCS_DOWNLOADED));
	}

	@Test
	void testUnsupportedStatusTransition()
			throws ResourceNotFoundException, InvalidStatusTransitionException, IOException, GeneralCryptoLibException, PayloadStorageException,
			PayloadSignatureException, PayloadVerificationException {

		doThrow(InvalidStatusTransitionException.class).when(votingCardSetService).download(anyString(), anyString());
		assertEquals(HttpStatus.BAD_REQUEST, getResultingStatusCode(Status.VCS_DOWNLOADED));
	}

	@Test
	void testGenerate() throws IOException, ResourceNotFoundException, InvalidStatusTransitionException {
		DataGeneratorResponse dataGeneratorResponse = mock(DataGeneratorResponse.class);
		when(dataGeneratorResponse.isSuccessful()).thenReturn(true);
		when(votingCardSetService.generate(any(), any())).thenReturn(dataGeneratorResponse);

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("");

		ResponseEntity<?> response = sut.createVotingCardSet(VALID_ELECTION_EVENT_ID, VALID_VOTING_CARD_SET_ID, uriBuilder);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
	}

	private HttpStatus getResultingStatusCode(Status status)
			throws ResourceNotFoundException, IOException, GeneralCryptoLibException, PayloadStorageException, PayloadSignatureException,
			PayloadVerificationException {

		return sut.setVotingCardSetStatus(VALID_ELECTION_EVENT_ID, VALID_VOTING_CARD_SET_ID, new VotingCardSetUpdateInputData(status, null, null))
				.getStatusCode();
	}
}
