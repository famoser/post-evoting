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
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
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

public class ChoiceCodesDecryptionContributionsServiceImpl implements ChoiceCodesDecryptionContributionsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceCodesDecryptionContributionsServiceImpl.class);
	private final StreamSerializableObjectReader<ChoiceCodesVerificationDecryptResPayload> reader = new StreamSerializableObjectReaderImpl<>();
	@Inject
	private MessagingService messagingService;
	@Inject
	@ChoiceCodesDecryption
	@Contributions
	private MessageListener contributionsConsumer;
	@Inject
	@ChoiceCodesDecryption
	@ResultsReady
	private MessageListener resultsReadyConsumer;
	@Inject
	@ChoiceCodesDecryption
	private PollingService<List<byte[]>> pollingService;

	@Override
	public List<ChoiceCodesVerificationDecryptResPayload> requestChoiceCodesDecryptionContributionsSync(String trackingId, String tenantId,
			String electionEventId, String verificationCardSetId, String verificationCardId, ReturnCodesInput partialCodes)
			throws ResourceNotFoundException {

		LOGGER.info("OR - Requesting the choice codes decryption contributions");

		ReturnCodeComputationDTO<ReturnCodesInput> requestDTO = publishGammasToReqQ(electionEventId, verificationCardSetId, verificationCardId,
				trackingId, partialCodes);

		return collectDecryptionContributionsResponses(requestDTO);
	}

	@Override
	public void startup() throws MessagingException {
		consumeResultsReady();
		LOGGER.info("OR - Consuming the results ready notification for choice codes decryption");
		consumeDecryptionContributions();
		LOGGER.info("OR - Consuming the choice codes decryption contributions");
	}

	@Override
	public void shutdown() throws MessagingException {
		stopConsumingResultsReady();
		LOGGER.info("OR - Consuming the results ready notification for choice codes decryption is stopped");
		stopConsumingDecryptionContributions();
		LOGGER.info("OR - Consuming the choice codes decryption contributions is stopped");
	}

	private void consumeDecryptionContributions() throws MessagingException {
		Queue[] queues = QueuesConfig.VERIFICATION_DECRYPTION_CONTRIBUTIONS_RES_QUEUES;

		if (queues == null || queues.length == 0) {
			throw new IllegalStateException("No choice codes decryption contributions response queues provided to listen to.");
		}

		for (Queue queue : queues) {
			messagingService.createReceiver(queue, contributionsConsumer);
		}
	}

	private void consumeResultsReady() throws MessagingException {
		messagingService.createReceiver(TopicsConfig.HA_TOPIC, resultsReadyConsumer);
	}

	private void stopConsumingDecryptionContributions() throws MessagingException {
		Queue[] queues = QueuesConfig.VERIFICATION_DECRYPTION_CONTRIBUTIONS_RES_QUEUES;

		if (queues == null || queues.length == 0) {
			throw new IllegalStateException("No choice codes decryption contributions response queues provided to listen to.");
		}

		for (Queue queue : queues) {
			messagingService.destroyReceiver(queue, contributionsConsumer);
		}
	}

	private void stopConsumingResultsReady() throws MessagingException {
		messagingService.destroyReceiver(TopicsConfig.HA_TOPIC, resultsReadyConsumer);
	}

	private ReturnCodeComputationDTO<ReturnCodesInput> publishGammasToReqQ(String electionEventId, String verificationCardSetId,
			String verificationCardId, String trackingId, ReturnCodesInput partialCodes) throws ResourceNotFoundException {

		Queue[] queues = QueuesConfig.VERIFICATION_DECRYPTION_CONTRIBUTIONS_REQ_QUEUES;

		if (queues == null || queues.length == 0) {
			throw new IllegalStateException("No choice codes decryption contributions request queues provided to publish to.");
		}

		ReturnCodeComputationDTO<ReturnCodesInput> returnCodeComputationDTO = new ReturnCodeComputationDTO<>(UUID.randomUUID(), trackingId,
				electionEventId, verificationCardSetId, verificationCardId, partialCodes);

		for (Queue queue : queues) {
			try {
				messagingService.send(queue, returnCodeComputationDTO);
			} catch (MessagingException e) {
				throw new ResourceNotFoundException("Error publishing the decryption contributions to the nodes queues. ", e);
			}
		}
		return returnCodeComputationDTO;
	}

	private List<ChoiceCodesVerificationDecryptResPayload> collectDecryptionContributionsResponses(ReturnCodeComputationDTO<?> requestDTO)
			throws ResourceNotFoundException {

		UUID correlationId = requestDTO.getCorrelationId();

		List<byte[]> collectedContributions;
		try {

			LOGGER.info("OR - Waiting for the decryption contributions from the nodes to be returned.");

			collectedContributions = pollingService.getResults(correlationId);
		} catch (TimeoutException e) {
			throw new ResourceNotFoundException("Error collecting the decryption contributions from the nodes. ", e);
		}

		return collectedContributions.stream().map(this::deserializeContribution).collect(Collectors.toList());
	}

	private ChoiceCodesVerificationDecryptResPayload deserializeContribution(byte[] bytes) {
		try {
			return reader.read(bytes);
		} catch (SafeStreamDeserializationException e) {
			throw new IllegalArgumentException("Failed to deserialize contribution.", e);
		}
	}
}
