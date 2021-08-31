/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.confirmation.ConfirmationMessageValidation;

/**
 * Decorator for confirmation message validation.
 */
@Decorator
public class ConfirmationMessageSignatureValidationDecorator implements ConfirmationMessageValidation {

	@Inject
	@Delegate
	private ConfirmationMessageSignatureValidation confirmationMessageSignatureValidation;

	/**
	 * @see ConfirmationMessageValidation#execute(String, String, String, ConfirmationInformation, AuthenticationToken)
	 */
	@Override
	public ValidationError execute(String tenantId, String electionEventId, String votingCardId, ConfirmationInformation confirmationInformation,
			AuthenticationToken authenticationToken) {
		return confirmationMessageSignatureValidation.execute(tenantId, electionEventId, votingCardId, confirmationInformation, authenticationToken);
	}

}
