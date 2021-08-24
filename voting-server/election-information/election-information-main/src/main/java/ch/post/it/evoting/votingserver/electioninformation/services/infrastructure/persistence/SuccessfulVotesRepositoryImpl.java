/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;

import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.vote.SuccessfulVote;

@Stateless
public class SuccessfulVotesRepositoryImpl extends BaseRepositoryImpl<SuccessfulVote, Integer> implements SuccessfulVotesRepository {

	private static final String PARAMETER_TENANT_ID = "tenantId";

	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	private static final String PARAMETER_BALLOT_BOX_ID = "ballotBoxId";

	@Override
	public List<SuccessfulVote> getSuccessfulVotes(String tenantId, String electionEventId, String ballotBoxId, int firstElement, int maxResult) {
		TypedQuery<SuccessfulVote> getSuccessfulVotesQuery = entityManager.createQuery(
				"SELECT s FROM SuccessfulVote s WHERE s.tenantId = :tenantId AND s.electionEventId=:electionEventId AND s.ballotBoxId=:ballotBoxId ORDER BY s.id DESC",
				SuccessfulVote.class);
		getSuccessfulVotesQuery.setFirstResult(firstElement);
		getSuccessfulVotesQuery.setMaxResults(maxResult);
		getSuccessfulVotesQuery.setParameter(PARAMETER_TENANT_ID, tenantId);
		getSuccessfulVotesQuery.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		getSuccessfulVotesQuery.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);
		return getSuccessfulVotesQuery.getResultList();
	}
}
