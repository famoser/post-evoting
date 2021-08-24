/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.infrastructure.remote;

import javax.inject.Inject;

import org.slf4j.Logger;

import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;
import ch.post.it.evoting.votingserver.orchestrator.mixdec.infrastructure.remote.ElectionInformationClient;

/**
 * The validation repository implementation.
 */
public class ValidationRepositoryImpl {

	// The name of the property path.
	private static final String PROPERTY_PATH_VALIDATIONS = "path.validations";

	// The value of the property validations.
	private static final String PATH_VALIDATIONS = PropertiesFileReader.getInstance().getPropertyValue(PROPERTY_PATH_VALIDATIONS);

	@Inject
	private TrackIdInstance trackId;

	private ElectionInformationClient electionInformationClient;

	@Inject
	private Logger LOGGER;

	@Inject
	ValidationRepositoryImpl(final ElectionInformationClient electionInformationClient) {
		this.electionInformationClient = electionInformationClient;
	}

	/**
	 * Validates if an election is in dates.
	 *
	 * @param tenantId        - the tenant id
	 * @param electionEventId - the election event id.
	 * @param ballotBoxId     - the ballot box id.
	 * @return the result of the validation.
	 * @throws ResourceNotFoundException
	 */
	public ValidationResult validateElectionInDates(String tenantId, String electionEventId, String ballotBoxId) throws ResourceNotFoundException {
		try {
			LOGGER.info("Validating election dates in authentication");
			ValidationResult ballotBoxStatus = RetrofitConsumer.processResponse(getElectionInformationClient()
					.validateElectionInDates(trackId.getTrackId(), PATH_VALIDATIONS, tenantId, electionEventId, ballotBoxId));
			LOGGER.info("Election is: {}", ballotBoxStatus.getValidationError().getValidationErrorType().name());
			return ballotBoxStatus;
		} catch (RetrofitException e) {
			throw e;
		} catch (ResourceNotFoundException e) {
			throw e;
		}
	}

	/**
	 * Gets electionInformationClient.
	 *
	 * @return Value of electionInformationClient.
	 */
	public ElectionInformationClient getElectionInformationClient() {
		return electionInformationClient;
	}
}
