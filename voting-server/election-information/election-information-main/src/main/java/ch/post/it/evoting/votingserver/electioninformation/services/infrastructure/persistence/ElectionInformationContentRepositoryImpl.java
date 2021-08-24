/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.persistence;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContent;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content.ElectionInformationContentRepository;

/**
 * Implementation of the election information content repository with jpa.
 */
@Stateless
public class ElectionInformationContentRepositoryImpl extends BaseRepositoryImpl<ElectionInformationContent, Integer>
		implements ElectionInformationContentRepository {

	// The name of the parameter which identifies the tenantId
	private static final String PARAMETER_TENANT_ID = "tenantId";

	// The name of the parameter which identifies the electionEventId
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	/**
	 * Searches for a election information content with the given tenant and election event. This
	 * implementation uses database access by executing a SQL-query to select the data to be
	 * retrieved.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the election event.
	 * @return a entity representing the election information content.
	 */
	@Override
	public ElectionInformationContent findByTenantIdElectionEventId(String tenantId, String electionEventId) throws ResourceNotFoundException {
		TypedQuery<ElectionInformationContent> query = entityManager
				.createQuery("SELECT e FROM ElectionInformationContent e WHERE e.tenantId = :tenantId AND e.electionEventId = :electionEventId",
						ElectionInformationContent.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException("", e);
		}
	}
}
