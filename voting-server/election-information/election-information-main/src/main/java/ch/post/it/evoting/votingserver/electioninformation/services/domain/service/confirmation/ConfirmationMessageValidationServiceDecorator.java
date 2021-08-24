/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.commons.confirmation.ConfirmationInformationResult;

/**
 * Decorator of the confirmation message validation service.
 */
@Decorator
public class ConfirmationMessageValidationServiceDecorator implements ConfirmationMessageValidationService {

	@Inject
	@Delegate
	private ConfirmationMessageValidationService confirmationMessageValidationService;

	/**
	 * @see ConfirmationMessageValidationService#validateConfirmationMessage(String,
	 * String, String,
	 * ConfirmationInformation,
	 * AuthenticationToken)
	 */
	@Override
	public ConfirmationInformationResult validateConfirmationMessage(String tenantId, String electionEventId, String votingCardId,
			ConfirmationInformation confirmationInformation, AuthenticationToken authenticationToken) {
		return confirmationMessageValidationService
				.validateConfirmationMessage(tenantId, electionEventId, votingCardId, confirmationInformation, authenticationToken);
	}

}
