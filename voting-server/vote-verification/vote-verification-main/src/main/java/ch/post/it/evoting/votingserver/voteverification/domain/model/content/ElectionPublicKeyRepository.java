/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.content;

import javax.ejb.Local;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;

/**
 * Repository for handling ElectionPublicKey entities
 */
@Local
public interface ElectionPublicKeyRepository extends BaseRepository<ElectionPublicKey, Integer> {

	/**
	 * Searches election public key associated to this tenant, election event and electoral authority
	 * ids.
	 *
	 * @param tenantId             - the identifier of the tenant.
	 * @param electionEventId      - the identifier of the electionEvent.
	 * @param electoralAuthorityId - the identifier of the electoralAuthorityId.
	 * @return a entity representing the election public key.
	 */
	ElectionPublicKey findByTenantIdElectionEventIdElectoralAuthorityId(String tenantId, String electionEventId, String electoralAuthorityId)
			throws ResourceNotFoundException;

}
