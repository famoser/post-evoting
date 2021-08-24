/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services;

import java.util.UUID;

import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.messaging.StreamSerializable;
import ch.post.it.evoting.domain.returncodes.KeyCreationDTO;
import ch.post.it.evoting.votingserver.commons.messaging.MessagingService;
import ch.post.it.evoting.votingserver.orchestrator.commons.config.QueuesConfig;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.Jdbc;
import ch.post.it.evoting.votingserver.orchestrator.commons.infrastructure.persistence.PartialResultsRepository;
import ch.post.it.evoting.votingserver.orchestrator.commons.messaging.AbstractPartialResultsListener;

@ChoiceCodesKeyGeneration
@Contributions
public class ChoiceCodesKeyGenerationConsumer extends AbstractPartialResultsListener {

	@Inject
	public ChoiceCodesKeyGenerationConsumer(
			@Jdbc
					PartialResultsRepository<byte[]> repository, MessagingService messagingService) {
		super(repository, messagingService);
	}

	@Override
	protected UUID getCorrelationId(Object message) {
		return ((KeyCreationDTO) message).getCorrelationId();
	}

	@Override
	protected StreamSerializable getPartialResult(Object message) {
		return ((KeyCreationDTO) message);
	}

	@Override
	protected int partialResultCount() {
		return QueuesConfig.CHOICE_CODES_KEY_GENERATION_RES_QUEUES.length;
	}
}
