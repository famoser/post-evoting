/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.services.domain.service.confirmation;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.confirmation.ConfirmationInformation;
import ch.post.it.evoting.votingserver.commons.confirmation.ConfirmationInformationResult;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.model.confirmation.ConfirmationMessageValidation;

/**
 * Service which validates the confirmation message sent by the client.
 */
public class ConfirmationMessageValidationServiceImpl implements ConfirmationMessageValidationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationMessageSignatureValidation.class);
	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();
	private final Collection<ConfirmationMessageValidation> validations = new ArrayList<>();

	@Inject
	@Any
	void setValidations(Instance<ConfirmationMessageValidation> instance) {
		for (ConfirmationMessageValidation validation : instance) {
			validations.add(validation);
		}
	}

	@Override
	public ConfirmationInformationResult validateConfirmationMessage(String tenantId, String electionEventId, String votingCardId,
			ConfirmationInformation confirmationInformation, AuthenticationToken authenticationToken) {

		LOGGER.info(I18N.getMessage("ConfirmationMessageValidationService.validate.start"));

		// result of validation
		ConfirmationInformationResult confirmationInformationResult = new ConfirmationInformationResult();
		confirmationInformationResult.setValid(true);
		confirmationInformationResult.setElectionEventId(electionEventId);
		confirmationInformationResult.setVotingCardId(votingCardId);

		// execute validations
		for (ConfirmationMessageValidation validation : validations) {
			ValidationError validationErrorResult = validation
					.execute(tenantId, electionEventId, votingCardId, confirmationInformation, authenticationToken);
			if (validationErrorResult.getValidationErrorType().equals(ValidationErrorType.SUCCESS)) {
				LOGGER.info(I18N.getMessage("ConfirmationMessageValidationService.validate.validationSuccess"),
						validation.getClass().getSimpleName());
			} else {
				LOGGER.info(I18N.getMessage("ConfirmationMessageValidationService.validate.validationFail"), validation.getClass().getSimpleName());
				confirmationInformationResult.setValid(false);
				confirmationInformationResult.setValidationError(validationErrorResult);

				if (!(validation instanceof ConfirmationMessageMathematicalGroupValidation)) {
					break;
				}
			}
		}

		LOGGER.info(I18N.getMessage("ConfirmationMessageValidationService.validate.resultOfValidation"), confirmationInformationResult.isValid());

		return confirmationInformationResult;
	}
}
