/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.challenge;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.model.authentication.AuthenticationContent;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.validation.ChallengeInformationValidation;
import ch.post.it.evoting.votingserver.authentication.services.domain.service.authenticationcontent.AuthenticationContentService;
import ch.post.it.evoting.votingserver.commons.beans.challenge.ChallengeInformation;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;

/**
 * This class implements the expiration time validation of the challenge information.
 */
public class ChallengeInformationExpirationTimeValidation implements ChallengeInformationValidation {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChallengeInformationExpirationTimeValidation.class);

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();

	private static final Long CONSTANT_ZERO = 0l;

	private static final Long MILLIS = 1000l;
	private static final int RULE_ORDER = 1;
	private final int order;
	@Inject
	private AuthenticationContentService authenticationContentService;

	public ChallengeInformationExpirationTimeValidation() {
		this.order = RULE_ORDER;
	}

	/**
	 * This method implements the validation of challenge expiration time. That is, the difference
	 * between the current time and the token timestamp is less or equal than the (previously
	 * configured) challenge expiration time.
	 *
	 * @param tenantId             - the tenant identifier.
	 * @param electionEventId      - the election event identifier.
	 * @param credentialId         - the credential identifier.
	 * @param challengeInformation - the challenge information to be validated.
	 * @return true, if successfully validated. Otherwise, false.
	 * @throws ResourceNotFoundException
	 */
	@Override
	public boolean execute(final String tenantId, final String electionEventId, final String credentialId,
			final ChallengeInformation challengeInformation) throws ResourceNotFoundException {
		// get current time
		long currentTimestamp = System.currentTimeMillis();

		// convert server timestamp to long
		long serverTimestamp = Long.parseLong(challengeInformation.getServerChallengeMessage().getTimestamp());

		LOGGER.info(I18N.getMessage("ChallengeInformationExpirationTimeValidation.currentTimestamp"), currentTimestamp);
		LOGGER.info(I18N.getMessage("ChallengeInformationExpirationTimeValidation.serverTimestamp"), serverTimestamp);

		// positive server timestamp
		if (serverTimestamp < CONSTANT_ZERO) {
			LOGGER.info(I18N.getMessage("ChallengeInformationExpirationTimeValidation.negativeServerTimestamp"));
			return false;
		}

		// positive difference between timestamps
		long timestampDifference = currentTimestamp - serverTimestamp;
		if (timestampDifference < CONSTANT_ZERO) {
			LOGGER.info(I18N.getMessage("ChallengeInformationExpirationTimeValidation.negativeDifferenceTimestamp"));
			return false;
		}

		// validation result
		AuthenticationContent authenticationContent = authenticationContentService.getAuthenticationContent(tenantId, electionEventId);
		long expirationTimeInMilliseconds = authenticationContent.getChallengeExpirationTime() * MILLIS;

		// valid = not expired = true
		boolean valid = timestampDifference <= expirationTimeInMilliseconds;
		LOGGER.info(I18N.getMessage("ChallengeInformationExpirationTimeValidation.expirationTimestampValidationOK"), valid);

		return valid;
	}

	@Override
	public int getOrder() {
		return order;
	}

}
