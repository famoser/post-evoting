/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import java.io.IOException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.domain.returncodes.ChoiceCodeAndComputeResults;
import ch.post.it.evoting.domain.returncodes.VoteAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;

/**
 * Interface for generating the short choice return codes based on the encrypted partial choice
 * return codes - in interaction with the control components.
 */
public interface ChoiceCodesService {

	/**
	 * Generates the short choice return codes in interaction with the control components.
	 *
	 * @param tenantId              the tenant id.
	 * @param electionEventId       the election event id.
	 * @param verificationCardId    the verification card id.
	 * @param voteAndComputeResults the vote submitted by the voting client containing the encrypted
	 *                              partial choice return codes
	 * @return the corresponding choice codes.
	 * @throws ResourceNotFoundException
	 * @throws CryptographicOperationException
	 */
	ChoiceCodeAndComputeResults generateChoiceCodes(String tenantId, String electionEventId, String verificationCardId,
			VoteAndComputeResults voteAndComputeResults)
			throws ResourceNotFoundException, GeneralCryptoLibException, IOException, CryptographicOperationException;

}
