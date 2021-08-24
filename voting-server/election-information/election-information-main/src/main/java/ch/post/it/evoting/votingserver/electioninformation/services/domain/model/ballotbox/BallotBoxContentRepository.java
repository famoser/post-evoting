/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.model.ballotbox;

import javax.ejb.Local;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.BaseRepository;

/**
 * Repository for handling ballot box content entities.
 */
@Local
public interface BallotBoxContentRepository extends BaseRepository<BallotBoxContent, Integer> {

	/**
	 * Searches for an ballot box content with the given tenant, election event, ballot box id.
	 *
	 * @param tenantId        - the identifier of the tenant.
	 * @param electionEventId - the identifier of the electionEvent.
	 * @param ballotBoxId     - the identifier of the ballot box.
	 * @return a entity representing the authentication content.
	 */
	BallotBoxContent findByTenantIdElectionEventIdBallotBoxId(String tenantId, String electionEventId, String ballotBoxId)
			throws ResourceNotFoundException;

	String findFirstBallotBoxForElection(String tenantId, String electionEventId) throws ResourceNotFoundException;

}
