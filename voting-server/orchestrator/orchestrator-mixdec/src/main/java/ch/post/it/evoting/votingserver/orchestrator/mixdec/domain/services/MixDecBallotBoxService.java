/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.services;

import java.util.List;
import java.util.concurrent.TimeoutException;

import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;
import ch.post.it.evoting.domain.mixnet.MixnetPayload;
import ch.post.it.evoting.domain.mixnet.MixnetState;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingException;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.model.BallotBoxStatus;

public interface MixDecBallotBoxService {

	/**
	 * Starts the service up.
	 *
	 * @throws MessagingException if the MessageService fails to start.
	 */
	void startup() throws MessagingException;

	/**
	 * Shuts the service down.
	 *
	 * @throws MessagingException if the MessageService fails to shutdown.
	 */
	void shutdown() throws MessagingException;

	/**
	 * Starts the mixing process for all ballot boxes in ballotBoxIds. If the mixing has already started it returns an error code.
	 *
	 * @param electionEventId the election event id corresponding to the ballot boxes to mix.
	 * @param ballotBoxIds    the ids of the ballot boxes to mix.
	 * @return list of processing or error statuses for each of the passes ballot box ids
	 */
	List<BallotBoxStatus> processBallotBoxes(final String electionEventId, final List<String> ballotBoxIds, final String trackingId);

	/**
	 * Sets the mixing status of a given ballot box to be processed.
	 *
	 * @param electionEventId the election event id corresponding to the ballot box to update.
	 * @param ballotBoxId     the id of the ballot box to update the status.
	 */
	void updateProcessedBallotBoxStatus(final String electionEventId, final String ballotBoxId);

	/**
	 * Sets the error status of a given ballot box
	 *
	 * @param electionEventId the election event id corresponding to the ballot box to update.
	 * @param ballotBoxId     the id of the ballot box to update the status.
	 * @param errorMessage    the error message to set.
	 */
	void updateErrorBallotBoxStatus(final String electionEventId, final String ballotBoxId, final String errorMessage);

	/**
	 * Retrieves the ballot box mixing status based on the mixing statuses of the ballot box chunks
	 *
	 * @param electionEventId the election event id corresponding to the ballot boxes.
	 * @param ballotBoxIds    the ids of the ballot boxes to get the status.
	 * @return list of ballot box statuses
	 */
	List<BallotBoxStatus> getMixDecBallotBoxStatus(final String electionEventId, final String[] ballotBoxIds);

	/**
	 * Start a mixing-and-decryption process from a DTO.
	 *
	 * @param mixnetState the data to process.
	 */
	void sendMessage(final MixnetState mixnetState) throws TimeoutException, ApplicationException;

	/**
	 * Provides the list of payloads for a ballot box.
	 *
	 * @param ballotBoxId the ballot box
	 */
	List<MixnetPayload> getBallotBoxPayloadList(final BallotBoxId ballotBoxId);

}
