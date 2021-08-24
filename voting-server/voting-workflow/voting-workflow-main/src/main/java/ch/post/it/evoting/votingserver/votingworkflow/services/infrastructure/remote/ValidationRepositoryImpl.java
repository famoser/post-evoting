/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.votingworkflow.services.infrastructure.remote;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;
import ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.validation.ValidationRepository;
import ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.vote.ValidationVoteResult;

/**
 * Implementation of the repository.
 */
@Stateless
public class ValidationRepositoryImpl implements ValidationRepository {

	// PropertiesFile
	private static final PropertiesFileReader PROPERTIES = PropertiesFileReader.getInstance();

	// The path to the resource validations.
	private static final String VALIDATIONS_PATH = PROPERTIES.getPropertyValue("VALIDATIONS_PATH");
	private final VerificationClient verificationClient;
	private final ElectionInformationClient electionInformationClient;
	// Instance of the track Id which will be written in the logs
	@Inject
	private TrackIdInstance trackId;

	@Inject
	ValidationRepositoryImpl(final VerificationClient verificationClient, final ElectionInformationClient electionInformationClient) {
		this.verificationClient = verificationClient;
		this.electionInformationClient = electionInformationClient;
	}

	/**
	 * @throws ResourceNotFoundException when the remote call fails.
	 * @see ValidationRepository#validateVoteInVV(String,
	 * String, Vote)
	 */
	@Override
	public ValidationResult validateVoteInVV(String tenantId, String electionEventId, Vote vote) throws ResourceNotFoundException {
		return RetrofitConsumer
				.processResponse(verificationClient.validateVote(trackId.getTrackId(), VALIDATIONS_PATH, tenantId, electionEventId, vote));
	}

	/**
	 * @see ValidationRepository#validateVoteInEI(String,
	 * String, Vote)
	 */
	@Override
	public ValidationResult validateVoteInEI(String tenantId, String electionEventId, Vote vote) throws ResourceNotFoundException {
		return RetrofitConsumer
				.processResponse(electionInformationClient.validateVote(trackId.getTrackId(), VALIDATIONS_PATH, tenantId, electionEventId, vote));
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
	@Override
	public ValidationResult validateElectionDatesInEI(String tenantId, String electionEventId, String ballotBoxId) throws ResourceNotFoundException {
		return RetrofitConsumer.processResponse(
				electionInformationClient.validateElectionInDates(trackId.getTrackId(), VALIDATIONS_PATH, tenantId, electionEventId, ballotBoxId));
	}

	@Override
	public ValidationVoteResult validateVote(String tenantId, String electionEventId, Vote vote) throws ResourceNotFoundException {
		// result of vote validation
		ValidationVoteResult validationVoteResult = new ValidationVoteResult();
		ValidationResult validationResultEI = validateVoteInEI(tenantId, electionEventId, vote);
		validationVoteResult.setValidationError(validationResultEI.getValidationError());
		if (validationResultEI.isResult()) {
			ValidationResult validationResultVV = validateVoteInVV(tenantId, electionEventId, vote);
			validationVoteResult.setValidationError(validationResultVV.getValidationError());
			validationVoteResult.setValid(validationResultVV.isResult());
		}
		return validationVoteResult;
	}
}
