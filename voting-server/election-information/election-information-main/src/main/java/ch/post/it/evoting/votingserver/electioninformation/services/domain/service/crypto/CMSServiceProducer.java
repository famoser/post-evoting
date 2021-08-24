/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.crypto;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import ch.post.it.evoting.votingserver.commons.cms.CMSService;
import ch.post.it.evoting.votingserver.commons.cms.CMSServiceImpl;

/**
 * The producer for the CMS service implementation.
 */
public class CMSServiceProducer {

	@Produces
	@ApplicationScoped
	public CMSService getInstance() {
		return CMSServiceImpl.newInstance();
	}
}
