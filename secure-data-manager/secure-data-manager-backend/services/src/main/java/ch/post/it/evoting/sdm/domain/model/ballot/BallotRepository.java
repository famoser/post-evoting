/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.ballot;

import java.util.List;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.EntityRepository;

/**
 * Interface providing operations with ballot.
 */
public interface BallotRepository extends EntityRepository {

	/**
	 * Lists aliases of the specified ballot.
	 *
	 * @param id the ballot identifier
	 * @return the aliases
	 * @throws DatabaseException failed to list aliases.
	 */
	List<String> listAliases(String id);

	/**
	 * Update the related ballot box to a given list of ballots
	 *
	 * @param ids - the list of ballots ids to be updated
	 */
	void updateRelatedBallotBox(List<String> ids);

	/**
	 * Updates a ballot adding its signature
	 *
	 * @param ballotId     - identifier of the ballot to be updated
	 * @param signedBallot - signature of the ballot
	 */
	void updateSignedBallot(final String ballotId, String signedBallot);

	/**
	 * Lists ballots which belong to a given election event in JSON format.
	 *
	 * @param electionEventId the election event identifier
	 * @return the ballots.
	 */
	String listByElectionEvent(String electionEventId);
}
