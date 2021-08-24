/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.votingworkflow.services.infrastructure.remote;

import java.io.IOException;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.challenge.ChallengeInformation;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ApplicationException;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.authentication.AuthenticationTokenMessage;
import ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.authentication.AuthenticationTokenRepository;

/**
 * Decorator of the authentication token repository.
 */
@Decorator
public class AuthenticationTokenRepositoryDecorator implements AuthenticationTokenRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenRepositoryDecorator.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	@Inject
	@Delegate
	private AuthenticationTokenRepository authenticationTokenRepository;

	/**
	 * @see AuthenticationTokenRepository#getAuthenticationToken(String, String, String, ChallengeInformation)
	 */
	@Override
	public AuthenticationTokenMessage getAuthenticationToken(String tenantId, String electionEventId, String credentialId,
			ChallengeInformation challengeInformation) throws ResourceNotFoundException, ApplicationException {
		LOGGER.info(I18N.getMessage("AuthenticationInformationRepoImpl.getAuthToken"), tenantId, electionEventId, credentialId);

		try {

			AuthenticationTokenMessage authenticationToken = authenticationTokenRepository
					.getAuthenticationToken(tenantId, electionEventId, credentialId, challengeInformation);
			LOGGER.info(I18N.getMessage("AuthenticationInformationRepoImpl.getAuthToken.found"), authenticationToken.getAuthenticationToken() != null,
					tenantId, electionEventId, credentialId);

			return authenticationToken;

		} catch (RetrofitException e) {
			logTokenNotFoundError(tenantId, electionEventId, credentialId);
			if (e.getHttpCode() == 404) {
				throw new ResourceNotFoundException(e.getMessage());
			} else {
				throw new ApplicationException(e.getMessage());
			}
		} catch (ResourceNotFoundException e) {
			logTokenNotFoundError(tenantId, electionEventId, credentialId);
			throw e;
		}
	}

	/**
	 * @see AuthenticationTokenRepository#validateAuthenticationToken(String, String, String, String)
	 */
	@Override
	public ValidationResult validateAuthenticationToken(String tenantId, String electionEventId, String votingCardId, String authenticationToken)
			throws IOException, ResourceNotFoundException, ApplicationException {
		LOGGER.info(I18N.getMessage("AuthenticationInformationRepoImpl.validateAuthToken"), tenantId, electionEventId, votingCardId);

		try {

			return authenticationTokenRepository.validateAuthenticationToken(tenantId, electionEventId, votingCardId, authenticationToken);

		} catch (RetrofitException e) {
			logTokenNotFoundError(tenantId, electionEventId, votingCardId);
			if (e.getHttpCode() == 404) {
				throw new ResourceNotFoundException(e.getMessage());
			} else {
				throw new ApplicationException(e.getMessage());
			}
		} catch (ResourceNotFoundException e) {
			logTokenNotFoundError(tenantId, electionEventId, votingCardId);
			throw e;
		}
	}

	private void logTokenNotFoundError(String tenant, String electionEvent, String credential) {
		LOGGER.error(I18N.getMessage("AuthenticationInformationRepoImpl.getAuthToken.notFound"), tenant, electionEvent, credential);
	}
}
