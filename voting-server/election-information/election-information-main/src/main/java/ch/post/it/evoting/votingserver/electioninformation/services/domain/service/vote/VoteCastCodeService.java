/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.vote;

import java.io.IOException;

import ch.post.it.evoting.domain.returncodes.CastCodeAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;

public interface VoteCastCodeService {

	/**
	 * Saves the cast code that has been derived from the vote confirmation code
	 *
	 * @param tenantId
	 * @param electionEventId
	 * @param votingCardId
	 * @param voteCastCode
	 * @throws ApplicationException
	 * @throws IOException
	 */
	void save(String tenantId, String electionEventId, String votingCardId, CastCodeAndComputeResults voteCastCode)
			throws ApplicationException, IOException;

}
