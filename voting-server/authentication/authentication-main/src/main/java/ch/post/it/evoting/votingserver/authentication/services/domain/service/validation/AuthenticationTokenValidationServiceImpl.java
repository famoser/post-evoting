/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.validation;

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.model.authentication.AuthenticationContent;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.authentication.AuthenticationToken;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.validation.AuthenticationTokenValidation;
import ch.post.it.evoting.votingserver.authentication.services.domain.service.authenticationcontent.AuthenticationContentService;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.AuthTokenValidationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;

/**
 * This service provides the functionality to validate a authentication token based on the
 * predefined rules.
 */
@Stateless
public class AuthenticationTokenValidationServiceImpl implements AuthenticationTokenValidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenValidationServiceImpl.class);
	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();
	private final Collection<AuthenticationTokenValidation> validations = new ArrayList<>();
	@Inject
	private AuthenticationContentService authenticationContentService;

	@Inject
	@Any
	void setValidations(Instance<AuthenticationTokenValidation> instance) {
		for (AuthenticationTokenValidation validation : instance) {
			validations.add(validation);
		}
	}

	/**
	 * @see AuthenticationTokenValidationService#validate(String,
	 * String, String,
	 * AuthenticationToken)
	 */
	@Override
	public ValidationResult validate(String tenantId, String electionEventId, String votingCardId, AuthenticationToken authenticationToken)
			throws ResourceNotFoundException, CertificateException {
		// result of validation
		ValidationResult validationResult = new ValidationResult(true);

		try {
			// execute validations
			for (AuthenticationTokenValidation validation : validations) {
				validation.execute(tenantId, electionEventId, votingCardId, authenticationToken);
			}
		} catch (AuthTokenValidationException atE) {
			LOGGER.info(I18N.getMessage("AuthenticationTokenValidationService.validate.validationError"), atE);
			validationResult.setResult(false);
			validationResult.setValidationError(new ValidationError(atE.getErrorType()));
		}

		return validationResult;
	}

	/**
	 * @see AuthenticationTokenValidationService#getAuthenticationContent(String,
	 * String)
	 */
	@Override
	public AuthenticationContent getAuthenticationContent(String tenantId, String electionEventId) throws ResourceNotFoundException {
		return authenticationContentService.getAuthenticationContent(tenantId, electionEventId);
	}
}
