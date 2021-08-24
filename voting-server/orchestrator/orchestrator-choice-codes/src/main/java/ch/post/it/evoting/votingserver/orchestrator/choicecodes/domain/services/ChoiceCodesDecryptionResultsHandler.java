/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import javax.inject.Inject;

import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.QueuesConfig;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.InMemory;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.PartialResultsRepository;
import ch.post.it.evoting.votingserver.orchestrator.commons.polling.AbstractPartialResultsHandler;

public final class ChoiceCodesDecryptionResultsHandler
		extends AbstractPartialResultsHandler<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>> {

	@Inject
	public ChoiceCodesDecryptionResultsHandler(
			@InMemory
					PartialResultsRepository<ReturnCodeComputationDTO<ChoiceCodesVerificationDecryptResPayload>> repository) {
		super(repository);
	}

	@Override
	protected int getPartialResultsCount() {
		return QueuesConfig.VERIFICATION_DECRYPTION_CONTRIBUTIONS_RES_QUEUES.length;
	}
}
