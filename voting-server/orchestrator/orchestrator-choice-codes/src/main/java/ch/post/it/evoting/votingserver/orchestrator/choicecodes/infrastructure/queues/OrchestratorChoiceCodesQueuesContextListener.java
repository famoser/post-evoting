/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.choicecodes.infrastructure.queues;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.votingserver.commons.messaging.MessagingException;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesDecryptionContributionsService;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesGenerationContributionsService;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesKeyGenerationService;
import ch.post.it.evoting.votingserver.orchestrator.choicecodes.domain.services.ChoiceCodesVerificationContributionsService;

/**
 * Defines any steps to be performed when the ORCHESTRATOR context is first initialized and destroyed.
 */
public class OrchestratorChoiceCodesQueuesContextListener implements ServletContextListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorChoiceCodesQueuesContextListener.class);

	private static final String CONTEXT = "OR";

	@Inject
	ChoiceCodesDecryptionContributionsService choiceCodesDecryptionContributionsService;

	@Inject
	ChoiceCodesVerificationContributionsService choiceCodesVerificationContributionsService;

	@Inject
	ChoiceCodesGenerationContributionsService choiceCodesGenerationContributionsService;

	@Inject
	ChoiceCodesKeyGenerationService choiceCodesKeyGenerationService;

	@Override
	public void contextInitialized(final ServletContextEvent servletContextEvent) {

		LOGGER.info(CONTEXT + " - triggering the consumption of the Control Components response queues");

		try {
			choiceCodesDecryptionContributionsService.startup();
			choiceCodesVerificationContributionsService.startup();
			choiceCodesGenerationContributionsService.startup();
			choiceCodesKeyGenerationService.startup();
		} catch (MessagingException e) {
			LOGGER.error("Error consuming a Control Component response queue: ", e);
			throw new IllegalStateException("Error consuming a Control Component response queue: ", e);
		}

	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
		LOGGER.info(CONTEXT + " - triggering the disconnetion of the Control Components response queues");

		try {
			choiceCodesDecryptionContributionsService.shutdown();
			choiceCodesVerificationContributionsService.shutdown();
			choiceCodesGenerationContributionsService.shutdown();
			choiceCodesKeyGenerationService.shutdown();
		} catch (MessagingException e) {
			LOGGER.error("Error disconnecting from a Control Component response queue: ", e);
			throw new IllegalStateException("Error disconnectiong a Control Component response queue: ", e);
		}
	}
}
