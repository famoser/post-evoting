/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReader;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableObjectReaderImpl;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.messaging.MessageListener;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingException;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingService;
import ch.post.it.evoting.votingserver.commons.messaging.Queue;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.QueuesConfig;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.TopicsConfig;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.PollingService;

public class ChoiceCodesVerificationContributionsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceCodesVerificationContributionsService.class);
	private final StreamSerializableObjectReader<ReturnCodesExponentiationResponsePayload> reader = new StreamSerializableObjectReaderImpl<>();
	@Inject
	private MessagingService messagingService;
	@Inject
	@ChoiceCodesVerification
	@Contributions
	private MessageListener contributionsConsumer;
	@Inject
	@ChoiceCodesVerification
	@ResultsReady
	private MessageListener resultsReadyConsumer;
	@Inject
	@ChoiceCodesVerification
	private PollingService<List<byte[]>> pollingService;

	public List<ReturnCodesExponentiationResponsePayload> request(String trackingId, String electionEventId, String verificationCardSetId,
			String verificationCardId, ReturnCodesInput partialCodes) throws ResourceNotFoundException {

		LOGGER.info("OR - Requesting the choice codes compute contributions for the voting phase");

		ReturnCodeComputationDTO<ReturnCodesInput> requestDTO = publishPartialCodesToReqQ(electionEventId, verificationCardSetId, verificationCardId,
				trackingId, partialCodes);

		return collectPartialCodesContributionsResponses(requestDTO);
	}

	public void startup() throws MessagingException {
		consumeResultsReady();
		LOGGER.info("OR - Consuming the results ready notifications for choice codes verification");
		consumeComputeContributions();
		LOGGER.info("OR - Consuming the choice codes verification contributions");
	}

	public void shutdown() throws MessagingException {
		stopConsumingResultsReady();
		LOGGER.info("OR - Consuming the results ready notifications for choice codes verification is stopped");
		stopConsumingComputeContributions();
		LOGGER.info("OR - Consuming the choice codes verification contributions is stopped");
	}

	private void consumeComputeContributions() throws MessagingException {

		Queue[] queues = QueuesConfig.VERIFICATION_COMPUTE_CONTRIBUTIONS_RES_QUEUES;

		if (queues.length == 0) {
			throw new IllegalStateException("No choice codes compute contributions response queues provided to listen to.");
		}

		for (Queue queue : queues) {
			messagingService.createReceiver(queue, contributionsConsumer);
		}
	}

	private void consumeResultsReady() throws MessagingException {
		messagingService.createReceiver(TopicsConfig.HA_TOPIC, resultsReadyConsumer);
	}

	private void stopConsumingComputeContributions() throws MessagingException {

		Queue[] queues = QueuesConfig.VERIFICATION_COMPUTE_CONTRIBUTIONS_RES_QUEUES;

		if (queues.length == 0) {
			throw new IllegalStateException("No choice codes compute contributions response queues provided to listen to.");
		}

		for (Queue queue : queues) {
			messagingService.destroyReceiver(queue, contributionsConsumer);
		}
	}

	private void stopConsumingResultsReady() throws MessagingException {
		messagingService.destroyReceiver(TopicsConfig.HA_TOPIC, resultsReadyConsumer);
	}

	private ReturnCodeComputationDTO<ReturnCodesInput> publishPartialCodesToReqQ(String electionEventId, String verificationCardSetId,
			String verificationCardId, String trackingId, ReturnCodesInput partialCodes) throws ResourceNotFoundException {

		Queue[] queues = QueuesConfig.VERIFICATION_COMPUTE_CONTRIBUTIONS_REQ_QUEUES;

		if (queues.length == 0) {
			throw new IllegalStateException("No choice codes compute contributions request queues provided to publish to.");
		}

		UUID correlationId = UUID.randomUUID();

		ReturnCodeComputationDTO<ReturnCodesInput> returnCodeComputationDTO = new ReturnCodeComputationDTO<>(correlationId, trackingId,
				electionEventId, verificationCardSetId, verificationCardId, partialCodes);

		for (Queue queue : queues) {
			try {
				messagingService.send(queue, returnCodeComputationDTO);
			} catch (MessagingException e) {
				throw new ResourceNotFoundException("Error publishing partial codes to the nodes queues ", e);
			}
		}
		return returnCodeComputationDTO;
	}

	private List<ReturnCodesExponentiationResponsePayload> collectPartialCodesContributionsResponses(ReturnCodeComputationDTO<?> requestDTO)
			throws ResourceNotFoundException {

		UUID correlationId = requestDTO.getCorrelationId();

		List<byte[]> collectedContributions;
		try {

			LOGGER.info("OR - Waiting for the compute contributions from the nodes to be returned.");

			collectedContributions = pollingService.getResults(correlationId);
		} catch (TimeoutException e) {
			throw new ResourceNotFoundException("Error collecting partial codes from the nodes queues ", e);
		}

		return collectedContributions.stream().map(this::deserializeContribution).collect(Collectors.toList());
	}

	private ReturnCodesExponentiationResponsePayload deserializeContribution(byte[] bytes) {
		try {
			return reader.read(bytes);
		} catch (SafeStreamDeserializationException e) {
			throw new IllegalArgumentException("Failed to deserialize contribution.", e);
		}
	}
}
