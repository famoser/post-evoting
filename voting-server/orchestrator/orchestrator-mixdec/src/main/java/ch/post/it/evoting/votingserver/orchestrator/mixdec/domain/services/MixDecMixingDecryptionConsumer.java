/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.services;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.mixnet.BallotBoxDetails;
import ch.post.it.evoting.domain.mixnet.MixnetState;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.commons.messaging.MessageListener;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence.MixDecNodeOutputRepository;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.persistence.MixDecNodeOutputRepositoryException;

public class MixDecMixingDecryptionConsumer implements MessageListener {

	@Inject
	private Logger logger;

	@Inject
	private MixDecNodeOutputRepository mixDecNodeOutputRepository;

	@Inject
	private MixDecBallotBoxService mixDecBallotBoxService;

	@Inject
	private ObjectMapper mapper;

	/**
	 * Receives the result of mixing and decrypting in one node. If the DTO has been through all the nodes already, the process is finished.
	 * Otherwise, the resulting DTO must be sent to one of the nodes that have not yet visited.
	 *
	 * @param message the output mixing DTO that has been processed by a node.
	 */
	@Override
	public void onMessage(final Object message) {
		final byte[] messageBytes = (byte[]) message;

		final MixnetState mixnetState;
		try {
			mixnetState = mapper.readValue(messageBytes, MixnetState.class);
		} catch (IOException e) {
			logger.error("Failed to deserialize received message into a MixnetState.", e);
			return;
		}

		final BallotBoxDetails ballotBoxDetails = mixnetState.getBallotBoxDetails();
		final int lastVisitedNode = mixnetState.getNodeToVisit();

		logger.info("Received MixnetState for ballot box {} from node {}", ballotBoxDetails, lastVisitedNode);

		// Check whether the DTO is reporting an error.
		if (mixnetState.getMixnetError() == null) {
			logger.debug("Processing ballot box {} ...", ballotBoxDetails);

			try {
				// Persist the received MixnetState.
				persistPartialResults(mixnetState);

				// Check the index of the last visited node.
				if (lastVisitedNode == 3) {
					// All nodes have been visited. Store the final results.
					logger.info("All nodes have been visited for ballot box {}, persisting final result.", ballotBoxDetails);
					persistFinalResults(mixnetState);
				} else {
					// If the MixnetState has not yet been through all nodes, send it to the next one.
					mixnetState.incrementNodeToVisit();
					mixDecBallotBoxService.sendMessage(mixnetState);
				}
			} catch (TimeoutException e) {
				logger.error("Mixing payload of ballot box {} delivery timed out.", ballotBoxDetails, e);
			} catch (ApplicationException e) {
				logger.error("Error mixing payload of ballot box {}.", ballotBoxDetails, e);
			} catch (MixDecNodeOutputRepositoryException e) {
				logger.error("Mixing payload of ballot box {} could not be stored properly.", ballotBoxDetails, e);
			}
		} else {
			logger.warn("MixnetState payload of ballot box {} processing failed: {}", ballotBoxDetails, mixnetState.getMixnetError());

			mixDecBallotBoxService.updateErrorBallotBoxStatus(ballotBoxDetails.getElectionEventId(), ballotBoxDetails.getBallotBoxId(),
					mixnetState.getMixnetError());
			try {
				if (mixnetState.getRetryCount() > 0) {
					mixnetState.decrementRetryCount();
					mixDecBallotBoxService.sendMessage(mixnetState);
				} else {
					// No retries left.
					logger.error("Processing payload of ballot box {} will not be further attempted: {}", ballotBoxDetails,
							mixnetState.getMixnetError());
				}
			} catch (TimeoutException e) {
				logger.error("Mixing payload of ballot box {} delivery timed out.", ballotBoxDetails, e);
			} catch (ApplicationException e) {
				logger.error("Error mixing payload of ballot box {}.", ballotBoxDetails, e);
			}
		}

	}

	/**
	 * Stores the results of having mixed and decrypted a ballot box in one node.
	 *
	 * @param mixnetState the data as coming out from an online mixing node.
	 */
	private void persistPartialResults(final MixnetState mixnetState) throws MixDecNodeOutputRepositoryException {
		final BallotBoxDetails ballotBoxDetails = mixnetState.getBallotBoxDetails();
		final int nodeToVisit = mixnetState.getNodeToVisit();

		logger.debug("Storing mixing and decryption results for ballot box {} from node {}...", ballotBoxDetails, nodeToVisit);
		try {
			mixDecNodeOutputRepository.save(mixnetState);
			logger.info("Mixing and decryption results for ballot box {} from node {} have been stored", ballotBoxDetails, nodeToVisit);
		} catch (DuplicateEntryException e) {
			logger.warn("Node {} output for ballot box {} is already stored", nodeToVisit, ballotBoxDetails);
		}
	}

	/**
	 * Stores the results of having fully mixed and decrypted a vote set.
	 *
	 * @param mixnetState the data as coming out from the last online mixing node.
	 */
	private void persistFinalResults(final MixnetState mixnetState) {
		final BallotBoxDetails ballotBoxDetails = mixnetState.getBallotBoxDetails();

		logger.debug("Storing final mixing and decryption results for ballot box {}...", ballotBoxDetails);

		mixDecBallotBoxService.updateProcessedBallotBoxStatus(ballotBoxDetails.getElectionEventId(), ballotBoxDetails.getBallotBoxId());
		logger.info("Ballot box {} has been fully processed and persisted.", ballotBoxDetails);
	}

}
