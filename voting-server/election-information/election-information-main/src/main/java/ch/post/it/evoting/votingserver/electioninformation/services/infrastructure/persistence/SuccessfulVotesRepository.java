/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import java.util.List;

import javax.ejb.Local;

import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote.SuccessfulVote;

@Local
public interface SuccessfulVotesRepository extends BaseRepository<SuccessfulVote, Integer> {

	/**
	 * Get a successful votes entry list
	 */
	List<SuccessfulVote> getSuccessfulVotes(String tenantId, String electionEventId, String ballotBoxId, int firstElement, int maxResult);
}
