/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.infrastructure.persistence;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationCerts;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationCertsRepository;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;

/**
 * Implementation of the repository with JPA
 */
@Stateless
public class AuthenticationCertsRepositoryImpl extends BaseRepositoryImpl<AuthenticationCerts, Integer> implements AuthenticationCertsRepository {

	// The name of the parameter which identifies the tenantId
	private static final String PARAMETER_TENANT_ID = "tenantId";

	// The name of the parameter which identifies the electionEventId
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	/**
	 * Searches for the authentication certs with the given tenant, election event. This
	 * implementation uses database access by executing a SQL-query to select the data to be
	 * retrieved.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the electionEvent.
	 * @return a entity representing the authentication certs.
	 */
	@Override
	public AuthenticationCerts findByTenantIdElectionEventId(String tenantId, String electionEventId) throws ResourceNotFoundException {
		TypedQuery<AuthenticationCerts> query = entityManager
				.createQuery("SELECT a FROM AuthenticationCerts a WHERE a.tenantId = :tenantId AND a.electionEventId = :electionEventId",
						AuthenticationCerts.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		try {
			return query.getSingleResult();
		} catch (NoResultException e) {

			throw new ResourceNotFoundException("", e);
		}
	}
}
