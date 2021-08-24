/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.verificationset;

import java.io.IOException;

import javax.ejb.Local;

import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.beans.verificationset.VerificationSet;

/**
 * Repository for handling VerificationSet data.
 */
@Local
public interface VerificationSetRepository {

	/**
	 * Searches for the verification set data related to given parameters.
	 *
	 * @param tenantId              - the identifier of the tenant.
	 * @param electionEventId       - the identifier of the election event.
	 * @param verificationCardSetId - the identifier of the verification card set.
	 * @return The associated verification data, if found.
	 * @throws ResourceNotFoundException if the verification is not found.
	 * @throws IOException               when converting to json fails.
	 */
	VerificationSet findByTenantElectionEventVerificationCardSetId(String tenantId, String electionEventId, String verificationCardSetId)
			throws ResourceNotFoundException, IOException;
}
