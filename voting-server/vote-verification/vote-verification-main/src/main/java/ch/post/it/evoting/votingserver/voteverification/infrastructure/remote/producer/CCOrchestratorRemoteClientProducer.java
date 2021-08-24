/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.producer;

import javax.enterprise.inject.Produces;

import ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.OrchestratorClient;

public class CCOrchestratorRemoteClientProducer extends RemoteClientProducer {

	public static final String URI_ORCHESTRATOR = System.getenv("ORCHESTRATOR_CONTEXT_URL");

	@Produces
	OrchestratorClient ccOrchestratorClient() {
		return createRestClient(URI_ORCHESTRATOR, OrchestratorClient.class);
	}
}
