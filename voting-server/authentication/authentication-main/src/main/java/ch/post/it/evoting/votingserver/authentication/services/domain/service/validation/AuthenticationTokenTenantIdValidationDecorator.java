/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.validation;

import java.security.cert.CertificateException;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationToken;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.validation.AuthenticationTokenValidation;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;

/**
 * Decorator for authentication token validation.
 */
@Decorator
public abstract class AuthenticationTokenTenantIdValidationDecorator implements AuthenticationTokenValidation {

	@Inject
	@Delegate
	private AuthenticationTokenTenantIdValidation authenticationTokenTenantIdValidation;

	/**
	 * @see AuthenticationTokenValidation#execute(String,
	 * String, String,
	 * AuthenticationToken)
	 */
	@Override
	public ValidationResult execute(String tenantId, String electionEventId, String votingCardId, AuthenticationToken authenticationToken)
			throws ResourceNotFoundException, CertificateException {
		return authenticationTokenTenantIdValidation.execute(tenantId, electionEventId, votingCardId, authenticationToken);
	}

}
