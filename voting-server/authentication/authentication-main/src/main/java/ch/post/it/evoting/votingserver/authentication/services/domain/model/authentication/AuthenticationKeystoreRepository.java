/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication;

import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.authentication.AuthenticationContent;
import ch.post.it.evoting.votingserver.authentication.services.domain.service.authenticationcontent.AuthenticationContentService;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.crypto.KeystoreRepository;

public class AuthenticationKeystoreRepository implements KeystoreRepository {

	@Inject
	private AuthenticationContentService authenticationContentService;

	/**
	 * @see KeystoreRepository#getJsonByTenantEEID(String,
	 * String)
	 */
	@Override
	public String getJsonByTenantEEID(String tenantId, String electionEventId) throws ResourceNotFoundException {

		AuthenticationContent authenticationContent = authenticationContentService.getAuthenticationContent(tenantId, electionEventId);

		return authenticationContent.getKeystore().toString();
	}

}
