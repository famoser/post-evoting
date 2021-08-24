/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import java.io.IOException;

import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.domain.election.model.confirmation.TraceableConfirmationMessage;
import ch.post.it.evoting.domain.returncodes.CastCodeAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;

/**
 * Interface for generating the short vote cast return code based on the confirmation message - in
 * interaction with the control components.
 */
public interface CastCodeService {

	/**
	 * Calculates in interaction with the control components the short vote cast return code based on
	 * the confirmation message received by the voting client.
	 *
	 * @param tenantId            - tenant identifier.
	 * @param eeid                - election event identifier.
	 * @param verificationCardId  - verification card id
	 * @param confirmationMessage - The confirmation message received by the voting client
	 * @return An object cast code message that contains the short vote cast return code
	 * @throws ResourceNotFoundException
	 * @throws CryptographicOperationException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	CastCodeAndComputeResults retrieveCastCode(String tenantId, String eeid, String verificationCardId,
			TraceableConfirmationMessage confirmationMessage)
			throws ResourceNotFoundException, CryptographicOperationException, ClassNotFoundException, IOException;

}
