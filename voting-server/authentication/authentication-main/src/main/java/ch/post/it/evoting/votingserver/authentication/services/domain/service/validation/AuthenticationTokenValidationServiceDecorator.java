/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.validation;

import java.security.cert.CertificateException;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.model.authentication.AuthenticationContent;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationToken;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;

/**
 * Decorator for authentication token validation service.
 */
@Decorator
public class AuthenticationTokenValidationServiceDecorator implements AuthenticationTokenValidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenValidationServiceDecorator.class);
	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	@Inject
	@Delegate
	private AuthenticationTokenValidationService authenticationTokenValidationService;

	/**
	 * @see AuthenticationTokenValidationService#validate(String,
	 * String, String,
	 * AuthenticationToken)
	 */
	@Override
	public ValidationResult validate(String tenantId, String electionEventId, String votingCardId, AuthenticationToken authenticationToken)
			throws ResourceNotFoundException, CertificateException {
		LOGGER.info(I18N.getMessage("AuthenticationTokenValidationService.validate.start"));
		try {
			ValidationResult validationResult = authenticationTokenValidationService
					.validate(tenantId, electionEventId, votingCardId, authenticationToken);
			LOGGER.info(I18N.getMessage("AuthenticationTokenValidationService.validate.resultOfValidation"), validationResult.isResult());
			return validationResult;
		} catch (ResourceNotFoundException | CertificateException e) {
			LOGGER.info(I18N.getMessage("AuthenticationTokenValidationService.validate.resultOfValidation"), false);
			throw e;
		}
	}

	/**
	 * @see AuthenticationTokenValidationService#getAuthenticationContent(String,
	 * String)
	 */
	@Override
	public AuthenticationContent getAuthenticationContent(String tenantId, String electionEventId) throws ResourceNotFoundException {
		return authenticationTokenValidationService.getAuthenticationContent(tenantId, electionEventId);
	}
}
