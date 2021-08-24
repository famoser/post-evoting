/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * The implementation of the operations on the verification set repository. The implementation uses
 * JPA as data layer to access database.
 */
@Stateless(name = "vv-verificationSetRepositoryImpl")
public class VerificationSetRepositoryImpl extends BaseRepositoryImpl<VerificationSetEntity, Integer> implements VerificationSetRepository {

	/* The name of parameter tenant id */
	private static final String PARAMETER_TENANT_ID = "tenantId";

	/* The name of parameter election event id */
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	/* The name of parameter verification card id */
	private static final String PARAMETER_VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	/**
	 * Returns a verification data for a given tenant, election event and verification card. In this
	 * implementation, we use a database query to obtain verification data taking into account the
	 * input parameters.
	 *
	 * @param tenantId              - the tenant identifier.
	 * @param electionEventId       - the election event identifier.
	 * @param verificationCardSetId - the verification card identifier.
	 * @return The verification data.
	 * @throws ResourceNotFoundException if the verification data is not found.
	 */
	@Override
	public VerificationSetEntity findByTenantIdElectionEventIdVerificationCardSetId(String tenantId, String electionEventId,
			String verificationCardSetId) throws ResourceNotFoundException {
		TypedQuery<VerificationSetEntity> query = entityManager.createQuery(
				"SELECT v FROM VerificationSetEntity v WHERE v.tenantId = :tenantId AND v.electionEventId = :electionEventId AND v.verificationCardSetId = :verificationCardSetId",
				VerificationSetEntity.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_VERIFICATION_CARD_SET_ID, verificationCardSetId);

		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException("", e);
		}
	}
}
