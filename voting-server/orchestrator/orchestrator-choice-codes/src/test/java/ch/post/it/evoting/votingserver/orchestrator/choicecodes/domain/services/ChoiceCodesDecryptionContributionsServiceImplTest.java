/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectWriterImpl;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingException;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingService;
import ch.post.it.evoting.votingserver.commons.messaging.Queue;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.PollingService;

@RunWith(MockitoJUnitRunner.class)
public class ChoiceCodesDecryptionContributionsServiceImplTest {

	@ClassRule
	public static EnvironmentVariables environmentVariables = new EnvironmentVariables();
	@InjectMocks
	private final ChoiceCodesDecryptionContributionsServiceImpl sut = new ChoiceCodesDecryptionContributionsServiceImpl();
	private final String trackingId = "trackingId";
	private final String tenantId = "100";
	private final String eeId = "eeId";
	private final String verCardSetId = "verCardSetId";
	private final String verCardId = "verCardId";
	private final Queue[] queues = { new Queue("cv-dec-v1-res"), new Queue("cv-dec-v2-res"), new Queue("cv-dec-v3-res"), new Queue("cv-dec-v4-res") };
	@Mock
	private MessagingService messagingService;
	@Mock
	private ChoiceCodesDecryptionContributionsConsumer choiceCodesDecryptionContributionsConsumer;
	@Mock
	@ChoiceCodesDecryption
	private PollingService<List<byte[]>> pollingService;
	@Mock
	private Logger LOGGER;
	private String ccQueueNames;

	private String ccTopicNames;

	@Before
	public void setUp() {
		ccQueueNames = System.getenv("CC_QUEUE_NAMES");
		environmentVariables.set("CC_QUEUE_NAMES",
				"{\"g4\": {\"cg-comp\": {\"res\": \"cg-comp-g4-res\", \"req\": \"cg-comp-g4-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g4-res\", \"req\": \"cg-keygen-g4-req\"}}, \"g3\": {\"cg-comp\": {\"res\": \"cg-comp-g3-res\", \"req\": \"cg-comp-g3-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g3-res\", \"req\": \"cg-keygen-g3-req\"}}, \"g2\": {\"cg-comp\": {\"res\": \"cg-comp-g2-res\", \"req\": \"cg-comp-g2-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g2-res\", \"req\": \"cg-keygen-g2-req\"}}, \"g1\": {\"cg-comp\": {\"res\": \"cg-comp-g1-res\", \"req\": \"cg-comp-g1-req\"}, \"cg-keygen\": {\"res\": \"cg-keygen-g1-res\", \"req\": \"cg-keygen-g1-req\"}}, \"v1\": {\"cv-comp\": {\"res\": \"cv-comp-v1-res\", \"req\": \"cv-comp-v1-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v1-res\", \"req\": \"cv-dec-v1-req\"}}, \"v2\": {\"cv-comp\": {\"res\": \"cv-comp-v2-res\", \"req\": \"cv-comp-v2-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v2-res\", \"req\": \"cv-dec-v2-req\"}}, \"v3\": {\"cv-comp\": {\"res\": \"cv-comp-v3-res\", \"req\": \"cv-comp-v3-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v3-res\", \"req\": \"cv-dec-v3-req\"}}, \"m1\": {\"md-keygen\": {\"res\": \"md-keygen-m1-res\", \"req\": \"md-keygen-m1-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m1-res\", \"req\": \"md-mixdec-m1-req\"}}, \"m3\": {\"md-keygen\": {\"res\": \"md-keygen-m3-res\", \"req\": \"md-keygen-m3-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m3-res\", \"req\": \"md-mixdec-m3-req\"}}, \"m2\": {\"md-keygen\": {\"res\": \"md-keygen-m2-res\", \"req\": \"md-keygen-m2-req\"}, \"md-mixdec\": {\"res\": \"md-mixdec-m2-res\", \"req\": \"md-mixdec-m2-req\"}}, \"v4\": {\"cv-comp\": {\"res\": \"cv-comp-v4-res\", \"req\": \"cv-comp-v4-req\"}, \"cv-dec\": {\"res\": \"cv-dec-v4-res\", \"req\": \"cv-dec-v4-req\"}}}");
		ccTopicNames = System.getenv("CC_TOPIC_NAMES");
		environmentVariables.set("CC_TOPIC_NAMES", "{\"or-ha\": \"or-ha\"}");
	}

	@After
	public void tearDown() {
		if (ccQueueNames != null) {
			environmentVariables.set("CC_QUEUE_NAMES", ccQueueNames);
		} else {
			System.clearProperty("CC_QUEUE_NAMES");
		}
		if (ccTopicNames != null) {
			environmentVariables.set("CC_TOPIC_NAMES", ccTopicNames);
		} else {
			System.clearProperty("CC_TOPIC_NAMES");
		}
	}

	@Test
	public void requestContributionsTest() throws ResourceNotFoundException, TimeoutException, IOException {
		String expected1 = "computed value1";
		String expected2 = "computed value2";

		ChoiceCodesVerificationDecryptResPayload choiceCodesVerificationDecryptResPayload = new ChoiceCodesVerificationDecryptResPayload();
		choiceCodesVerificationDecryptResPayload.setDecryptContributionResult(Arrays.asList(expected1, expected2));
		ReturnCodesInput gammaAndVote = new ReturnCodesInput();
		gammaAndVote.setReturnCodesInputElements(singletonList(new BigInteger("1")));

		List<byte[]> nodeContribution = singletonList(new StreamSerializableObjectWriterImpl().write(choiceCodesVerificationDecryptResPayload));

		when(pollingService.getResults(any(UUID.class))).thenReturn(nodeContribution);

		List<ChoiceCodesVerificationDecryptResPayload> result = sut
				.requestChoiceCodesDecryptionContributionsSync(trackingId, tenantId, eeId, verCardSetId, verCardId, gammaAndVote);

		assertEquals(expected1, result.get(0).getDecryptContributionResult().get(0));
		assertEquals(expected2, result.get(0).getDecryptContributionResult().get(1));
	}

	@Test
	public void consumeContributionsTest() throws MessagingException {
		sut.startup();

		verify(messagingService, times(1)).createReceiver(queues[0], choiceCodesDecryptionContributionsConsumer);
		verify(messagingService, times(1)).createReceiver(queues[1], choiceCodesDecryptionContributionsConsumer);
		verify(messagingService, times(1)).createReceiver(queues[2], choiceCodesDecryptionContributionsConsumer);
		verify(messagingService, times(1)).createReceiver(queues[3], choiceCodesDecryptionContributionsConsumer);
	}
}
