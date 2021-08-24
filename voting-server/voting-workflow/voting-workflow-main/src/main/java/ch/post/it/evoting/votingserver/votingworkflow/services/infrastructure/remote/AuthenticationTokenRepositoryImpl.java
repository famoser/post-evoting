/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.votingworkflow.services.infrastructure.remote;

import java.io.IOException;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.challenge.ChallengeInformation;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.util.PropertiesFileReader;
import ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.authentication.AuthenticationTokenMessage;
import ch.post.it.evoting.votingserver.votingworkflow.services.domain.model.authentication.AuthenticationTokenRepository;

/**
 * Implementation of the repository using a Rest Client.
 */
@Stateless(name = "vw_authenticationTokenRepositoryImpl")
public class AuthenticationTokenRepositoryImpl implements AuthenticationTokenRepository {

	// The properties file reader.
	private static final PropertiesFileReader PROPERTIES = PropertiesFileReader.getInstance();

	// The path to the resource authentication information.
	private static final String AUTHENTICATION_TOKEN_PATH = PROPERTIES.getPropertyValue("AUTHENTICATION_TOKEN_PATH");

	// The path to the resource for validate the authentication token.
	private static final String VALIDATION_AUTHENTICATION_TOKEN_PATH = PROPERTIES.getPropertyValue("VALIDATION_AUTHENTICATION_TOKEN_PATH");
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenRepositoryImpl.class);
	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();
	private final AuthenticationClient authenticationClient;
	// Instance of the track Id which will be written in the logs
	@Inject
	private TrackIdInstance trackId;

	@Inject
	AuthenticationTokenRepositoryImpl(final AuthenticationClient authenticationClient) {
		this.authenticationClient = authenticationClient;
	}

	/**
	 * Gets the associated AuthenticationToken using a Rest client.
	 *
	 * @param tenantId             - the identifier of the tenant.
	 * @param electionEventId      - the identifier of the election event.
	 * @param credentialId         - the identifier of the credential.
	 * @param challengeInformation - the challenge information including client challenge, server
	 *                             challenge, and server timestamp.
	 * @return An AuthenticationToken.
	 * @throws ResourceNotFoundException if authentication token can not be successfully build.
	 */
	@Override
	public AuthenticationTokenMessage getAuthenticationToken(String tenantId, String electionEventId, String credentialId,
			ChallengeInformation challengeInformation) throws ResourceNotFoundException {
		return RetrofitConsumer.processResponse(authenticationClient
				.getAuthenticationToken(AUTHENTICATION_TOKEN_PATH, tenantId, electionEventId, credentialId, trackId.getTrackId(),
						challengeInformation));
	}

	/**
	 * Validates through a Rest if a given authentication token is valid.
	 *
	 * @param tenantId            - the tenant identifier.
	 * @param electionEventId     - the electionEventIdentifier.
	 * @param votingCardId        - the voting card identifier.
	 * @param authenticationToken - the token to be validated.
	 * @return an AuthenticationTokenValidationResult object.
	 * @throws IOException               if there is a problem during conversion of authentication token to json
	 *                                   format.
	 * @throws ResourceNotFoundException if there are problems validating token.
	 */
	@Override
	public ValidationResult validateAuthenticationToken(String tenantId, String electionEventId, String votingCardId, String authenticationToken)
			throws IOException, ResourceNotFoundException {
		try {
			return RetrofitConsumer.processResponse(authenticationClient
					.validateAuthenticationToken(VALIDATION_AUTHENTICATION_TOKEN_PATH, tenantId, electionEventId, votingCardId, trackId.getTrackId(),
							authenticationToken));
		} catch (RetrofitException e) {
			LOGGER.info(I18N.getMessage("AuthenticationTokenRepoImpl.validatingAuthenticationToken.error"), tenantId, electionEventId, votingCardId,
					authenticationToken, e);
			return ObjectMappers.fromJson(e.getErrorBody().byteStream(), ValidationResult.class);
		}
	}
}
