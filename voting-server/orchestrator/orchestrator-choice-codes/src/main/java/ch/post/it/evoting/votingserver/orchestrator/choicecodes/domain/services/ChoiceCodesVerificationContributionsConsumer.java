/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.util.UUID;

import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;
import ch.post.it.evoting.domain.returncodes.ReturnCodeComputationDTO;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingService;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.QueuesConfig;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.Jdbc;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.PartialResultsRepository;
import ch.post.it.evoting.votingserver.orchestrator.commons.messaging.AbstractPartialResultsListener;

@ChoiceCodesVerification
@Contributions
public class ChoiceCodesVerificationContributionsConsumer extends AbstractPartialResultsListener {

	@Inject
	public ChoiceCodesVerificationContributionsConsumer(
			@Jdbc
					PartialResultsRepository<byte[]> repository, MessagingService messagingService) {
		super(repository, messagingService);
	}

	@Override
	protected UUID getCorrelationId(Object message) {
		return ((ReturnCodeComputationDTO<?>) message).getCorrelationId();
	}

	@Override
	protected StreamSerializable getPartialResult(Object message) {
		@SuppressWarnings("unchecked")
		ReturnCodeComputationDTO<ReturnCodesExponentiationResponsePayload> dto = (ReturnCodeComputationDTO<ReturnCodesExponentiationResponsePayload>) message;
		return dto.getPayload();
	}

	@Override
	protected int partialResultCount() {
		return QueuesConfig.VERIFICATION_COMPUTE_CONTRIBUTIONS_RES_QUEUES.length;
	}
}
