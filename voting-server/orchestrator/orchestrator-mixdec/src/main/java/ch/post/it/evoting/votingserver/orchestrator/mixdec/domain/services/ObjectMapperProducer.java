/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.orchestrator.mixdec.domain.services;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;

@Dependent
public class ObjectMapperProducer {

	@Produces
	public ObjectMapper getInstance() {
		return ObjectMapperMixnetConfig.getNewInstance();
	}

}
