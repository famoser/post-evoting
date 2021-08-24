/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox.BallotBoxContentRepository;

/**
 * Implementation of the ballot box info repository with jpa
 */
@Stateless
public class BallotBoxContentRepositoryImpl extends BaseRepositoryImpl<BallotBoxContent, Integer> implements BallotBoxContentRepository {

	// The name of the parameter which identifies the externalId
	private static final String PARAMETER_BALLOT_BOX_ID = "ballotBoxId";

	// The name of the parameter which identifies the tenantId
	private static final String PARAMETER_TENANT_ID = "tenantId";

	// The name of the parameter which identifies the electionEventId
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	/**
	 * Searches for a ballot box content with the given tenant, election event and ballot box id. This
	 * implementation uses database access by executing a SQL-query to select the data to be
	 * retrieved.
	 *
	 * @param ballotBoxId     - the external identifier of the ballot box.
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the election event.
	 * @return a entity representing the ballot box content.
	 */
	@Override
	public BallotBoxContent findByTenantIdElectionEventIdBallotBoxId(String tenantId, String electionEventId, String ballotBoxId)
			throws ResourceNotFoundException {
		TypedQuery<BallotBoxContent> query = entityManager.createQuery(
				"SELECT b FROM BallotBoxContent b WHERE b.tenantId = :tenantId AND b.electionEventId = :electionEventId AND b.ballotBoxId = :ballotBoxId",
				BallotBoxContent.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_BALLOT_BOX_ID, ballotBoxId);
		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException("", e);
		}
	}

	@Override
	public String findFirstBallotBoxForElection(String tenantId, String electionEventId) throws ResourceNotFoundException {
		TypedQuery<String> query = entityManager.createQuery(
				"SELECT b.ballotBoxId FROM BallotBoxContent b WHERE b.tenantId = :tenantId AND b.electionEventId = :electionEventId"
						+ " AND ROWNUM = 1 order by b.ballotBoxId ASC", String.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException("", e);
		}
	}

}
