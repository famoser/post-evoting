/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.DuplicateEntryException;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote.SuccessfulVote;

@Stateless
@javax.ejb.ApplicationException(rollback = true)
public class SuccessfulVotesAccess {

	@EJB
	private SuccessfulVotesRepository successfulVotesRepository;

	public SuccessfulVote save(SuccessfulVote successfulVote) throws DuplicateEntryException {
		return successfulVotesRepository.save(successfulVote);
	}
}
