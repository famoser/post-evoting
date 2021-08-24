/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.electoralauthority;

import java.util.List;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.EntityRepository;

/**
 * Interface for operations on the repository of electoral authority.
 */
public interface ElectoralAuthorityRepository extends EntityRepository {

	/**
	 * Updates the related ballot box(es).
	 *
	 * @param list The list of identifiers of the electoral authorities where to update the
	 *             identifiers of the related ballot boxes.
	 */
	void updateRelatedBallotBox(List<String> list);

	/**
	 * Lists authorities matching which belong to the specified election event.
	 *
	 * @param electionEventId the election event identifier
	 * @return the authorities in JSON format
	 * @throws DatabaseException failed to list the authorities
	 */
	String listByElectionEvent(String electionEventId);
}
