/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKeyRepository;

/**
 * Implementation of the repository with JPA
 */
@Stateless(name = "vv-ElectionPublicKeyRepositoryImpl")
public class ElectionPublicKeyRepositoryImpl extends BaseRepositoryImpl<ElectionPublicKey, Integer> implements ElectionPublicKeyRepository {

	// The name of the parameter which identifies the tenantId
	private static final String PARAMETER_TENANT_ID = "tenantId";

	// The name of the parameter which identifies the electionEventId
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	// The name of the parameter which identifies the electoralAuthorityId
	private static final String PARAMETER_ELECTORAL_AUTHORITY_ID = "electoralAuthorityId";

	/**
	 * Searches for an election public key associated with the given tenant, election event and
	 * electoral authority ids. This implementation uses database access by executing a SQL-query to
	 * select the data to be retrieved.
	 *
	 * @param tenantId             - the identifier of the tenant.
	 * @param electionEventId      - the identifier of the electionEvent.
	 * @param electoralAuthorityId - the identifier of the electoralAuthority.
	 * @return a entity representing the election public key.
	 */
	@Override
	public ElectionPublicKey findByTenantIdElectionEventIdElectoralAuthorityId(String tenantId, String electionEventId, String electoralAuthorityId)
			throws ResourceNotFoundException {
		TypedQuery<ElectionPublicKey> query = entityManager.createQuery(
				"SELECT a FROM ElectionPublicKey a WHERE a.tenantId = :tenantId AND a.electionEventId = :electionEventId AND a.electoralAuthorityId = :electoralAuthorityId",
				ElectionPublicKey.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_ELECTORAL_AUTHORITY_ID, electoralAuthorityId);

		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException("", e);
		}
	}
}
