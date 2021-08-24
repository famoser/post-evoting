/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.ballotbox;

import java.util.List;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.EntityRepository;

/**
 * Interface providing operations with ballot box.
 */
public interface BallotBoxRepository extends EntityRepository {

	/**
	 * Returns the ballot id from the ballot box identified by the given id.
	 *
	 * @param ballotBoxId identifies the ballot box where to search.
	 * @return the ballot identifier.
	 */
	String getBallotId(String ballotBoxId);

	/**
	 * Lists the aliases of the ballot boxes which belongs to the specified ballot.
	 *
	 * @param ballotId the ballot identifier
	 * @return the aliases
	 * @throws DatabaseException failed to list aliases.
	 */
	List<String> listAliases(String ballotId);

	/**
	 * Updates the content of a ballotBox with the alias of its related ballot
	 *
	 * @param id - the id of the ballot box to update
	 */
	void updateRelatedBallotAlias(List<String> id);

	/**
	 * Returns entities based on an electoral authority
	 *
	 * @param id - identifier of
	 * @return
	 */
	String findByElectoralAuthority(String id);

	/**
	 * Lists authorities matching which belong to the specified election event.
	 *
	 * @param electionEventId the election event identifier
	 * @return the authorities in JSON format
	 * @throws DatabaseException failed to list the authorities
	 */
	String listByElectionEvent(String electionEventId);
}
