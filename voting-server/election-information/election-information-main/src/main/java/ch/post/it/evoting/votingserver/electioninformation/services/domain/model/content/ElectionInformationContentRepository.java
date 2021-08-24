/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.content;

import javax.ejb.Local;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;

/**
 * Repository for handling election information content entities.
 */
@Local
public interface ElectionInformationContentRepository extends BaseRepository<ElectionInformationContent, Integer> {

	/**
	 * Searches for an election information content with the given tenant and election event.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the electionEvent.
	 * @return a entity representing the authentication content.
	 */
	ElectionInformationContent findByTenantIdElectionEventId(String tenantId, String electionEventId) throws ResourceNotFoundException;

}
