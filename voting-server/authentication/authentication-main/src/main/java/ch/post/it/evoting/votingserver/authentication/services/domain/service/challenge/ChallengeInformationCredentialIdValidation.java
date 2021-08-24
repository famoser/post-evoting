/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.challenge;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;

import ch.post.it.evoting.votingserver.authentication.services.domain.model.validation.ChallengeInformationValidation;
import ch.post.it.evoting.votingserver.commons.beans.challenge.ChallengeInformation;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;

/**
 * Implementation of {@link ChallengeInformationValidation} which checks that credential identifier
 * passed in URI matches one passed in {@link ChallengeInformation}.
 */
public class ChallengeInformationCredentialIdValidation implements ChallengeInformationValidation {
	public static final int RULE_ORDER = 0;
	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();
	private final int order;
	@Inject
	private Logger logger;
	@Inject
	private TrackIdInstance trackId;

	/**
	 * Constructor.
	 */
	public ChallengeInformationCredentialIdValidation() {
		this.order = RULE_ORDER;
	}

	/**
	 * Constructor. For tests only.
	 *
	 * @param logger
	 * @param trackId
	 */
	ChallengeInformationCredentialIdValidation(final Logger logger) {
		this.logger = logger;
		this.order = RULE_ORDER;
	}

	@Override
	public boolean execute(final String tenantId, final String electionEventId, final String credentialId,
			final ChallengeInformation challengeInformation) {
		boolean valid = Objects.equals(credentialId, challengeInformation.getCredentialId());
		if (valid) {
			logger.info(I18N.getMessage("ChallengeInformationCredentialIdValidation.execute.credentialIdOK"), tenantId, electionEventId,
					credentialId);
		} else {
			logger.info(I18N.getMessage("ChallengeInformationCredentialIdValidation.execute.credentialIdInvalid"), tenantId, electionEventId,
					credentialId, challengeInformation.getCredentialId());
		}
		return valid;
	}

	@Override
	public int getOrder() {
		return order;
	}
}
