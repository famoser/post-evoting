/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.persistence.BaseRepositoryImpl;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationDerivedKeys;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationDerivedKeysRepository;

@Stateless
public class VerificationDerivedKeysRepositoryImpl extends BaseRepositoryImpl<VerificationDerivedKeys, Integer>
		implements VerificationDerivedKeysRepository {

	// The name of the parameter which identifies the tenantId
	private static final String PARAMETER_TENANT_ID = "tenantId";

	// The name of the parameter which identifies the electionEventId
	private static final String PARAMETER_ELECTION_EVENT_ID = "electionEventId";

	// The name of the parameter which identifies the verificationCardId
	private static final String PARAMETER_VERIFICATION_CARD_ID = "verificationCardId";

	@Override
	public VerificationDerivedKeys findByTenantIdElectionEventIdVerificationCardId(String tenantId, String electionEventId, String verificationCardId)
			throws ResourceNotFoundException {
		TypedQuery<VerificationDerivedKeys> query = entityManager.createQuery(
				"SELECT a FROM VerificationDerivedKeys a WHERE a.tenantId = :tenantId AND a.electionEventId = :electionEventId AND a.verificationCardId = :verificationCardId",
				VerificationDerivedKeys.class);
		query.setParameter(PARAMETER_TENANT_ID, tenantId);
		query.setParameter(PARAMETER_ELECTION_EVENT_ID, electionEventId);
		query.setParameter(PARAMETER_VERIFICATION_CARD_ID, verificationCardId);

		try {
			return query.getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException("Derived keys not found for verification card " + verificationCardId, e);
		}
	}

}
