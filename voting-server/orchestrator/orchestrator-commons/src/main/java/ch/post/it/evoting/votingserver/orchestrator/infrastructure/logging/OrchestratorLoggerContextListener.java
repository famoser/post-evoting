/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.infrastructure.logging;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines any steps to be performed when the ORCHESTRATOR context is first initialized and destroyed.
 */
public class OrchestratorLoggerContextListener implements ServletContextListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorLoggerContextListener.class);

	private static final String CONTEXT = "OR";

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	@Override
	public void contextInitialized(final ServletContextEvent servletContextEvent) {
		LOGGER.info(CONTEXT + " - context initialized");
	}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
		// Nothing to do on context destruction.
	}
}
