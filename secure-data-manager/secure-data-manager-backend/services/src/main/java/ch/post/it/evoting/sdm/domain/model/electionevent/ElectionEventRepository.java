/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.electionevent;

import java.util.List;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.EntityRepository;

/**
 * Interface providing operations with election event.
 */
public interface ElectionEventRepository extends EntityRepository {

	/**
	 * Returns the election event alias from the election event identified by the given id.
	 *
	 * @param electionEventId identifies the election event where to search.
	 * @return the election event alias.
	 */
	String getElectionEventAlias(String electionEventId);

	/**
	 * Lists all the identifiers.
	 *
	 * @return the identifiers
	 * @throws DatabaseException failed to list the identifier
	 */
	List<String> listIds();
}
