/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.util.List;

import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingException;

public interface ChoiceCodesDecryptionContributionsService {

	List<ChoiceCodesVerificationDecryptResPayload> requestChoiceCodesDecryptionContributionsSync(String trackingId, String tenantId,
			String electionEventId, String verificationCardSetId, String verificationCardId, ReturnCodesInput partialCodes)
			throws ResourceNotFoundException;

	void startup() throws MessagingException;

	void shutdown() throws MessagingException;
}
