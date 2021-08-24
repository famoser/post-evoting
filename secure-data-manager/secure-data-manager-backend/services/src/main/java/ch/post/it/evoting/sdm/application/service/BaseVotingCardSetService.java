/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import javax.json.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.status.InvalidStatusTransitionException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * This is an application service that manages voting card sets.
 */
@Service
class BaseVotingCardSetService {

	@Autowired
	protected PathResolver pathResolver;

	@Autowired
	protected VotingCardSetRepository votingCardSetRepository;

	@Autowired
	protected ObjectMapper objectMapper;

	/**
	 * Check whether a voting card set can be transitioned from this status.
	 *
	 * @param electionEventId the election event the voting card set is for.
	 * @param votingCardSetId the voting card set to check.
	 * @param from            the expected current status
	 * @param to              the expected final status
	 * @throws InvalidStatusTransitionException
	 * @throws ResourceNotFoundException
	 */
	protected void checkVotingCardSetStatusTransition(String electionEventId, String votingCardSetId, Status from, Status to)
			throws ResourceNotFoundException, InvalidStatusTransitionException {
		JsonObject votingCardSetJson = votingCardSetRepository.getVotingCardSetJson(electionEventId, votingCardSetId);

		if (votingCardSetJson != null) {
			if (!(votingCardSetJson.containsKey(JsonConstants.STATUS) && votingCardSetJson.getString(JsonConstants.STATUS).equals(from.name()))) {
				throw new InvalidStatusTransitionException(Status.valueOf(votingCardSetJson.getString(JsonConstants.STATUS)), to);
			}
		} else {
			throw new InvalidStatusTransitionException(null, to);
		}
	}

}
