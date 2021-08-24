/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.verification;

import javax.ejb.Local;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;

@Local
public interface VerificationDerivedKeysRepository extends BaseRepository<VerificationDerivedKeys, Integer> {

	/**
	 * Find verification derived key by tenant, eeId, and verificationCardId
	 *
	 * @param tenantId
	 * @param electionEventId
	 * @param verificationCardId
	 * @return verification derived key
	 * @throws ResourceNotFoundException
	 */
	VerificationDerivedKeys findByTenantIdElectionEventIdVerificationCardId(String tenantId, String electionEventId, String verificationCardId)
			throws ResourceNotFoundException;

}


