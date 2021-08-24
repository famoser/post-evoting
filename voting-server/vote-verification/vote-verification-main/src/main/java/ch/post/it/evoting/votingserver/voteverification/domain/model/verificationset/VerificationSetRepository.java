/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset;

import javax.ejb.Local;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;

/**
 * Provides operations on the verification set repository.
 */
@Local
public interface VerificationSetRepository extends BaseRepository<VerificationSetEntity, Integer> {

	/**
	 * Returns a verification data for a given tenant, election event and voting card.
	 *
	 * @param tenantId              - the tenant identifier.
	 * @param electionEventId       - the election event identifier.
	 * @param verificationCardSetId - the voting card set identifier.
	 * @return The verification data.
	 * @throws ResourceNotFoundException if the
	 *                                   verification data is not found.
	 */
	VerificationSetEntity findByTenantIdElectionEventIdVerificationCardSetId(String tenantId, String electionEventId, String verificationCardSetId)
			throws ResourceNotFoundException;
}
