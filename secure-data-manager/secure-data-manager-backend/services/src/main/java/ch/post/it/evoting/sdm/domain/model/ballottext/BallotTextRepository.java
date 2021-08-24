/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.ballottext;

import java.util.List;
import java.util.Map;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.domain.model.EntityRepository;

/**
 * Interface providing operations with ballot text.
 */
public interface BallotTextRepository extends EntityRepository {

	void updateSignedBallotText(String ballotTextId, String signedBallotText);

	/**
	 * Lists the signatures of the entities matching the criteria.
	 *
	 * @param criteria the criteria
	 * @return the signatures
	 * @throws DatabaseException failed to list signatures.
	 */
	List<String> listSignatures(Map<String, Object> criteria);
}
