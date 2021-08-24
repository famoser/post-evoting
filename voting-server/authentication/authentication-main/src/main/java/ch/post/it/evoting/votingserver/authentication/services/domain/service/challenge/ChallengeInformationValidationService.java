/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.authentication.services.domain.service.challenge;

import java.util.Map;
import java.util.TreeMap;

import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.domain.election.validation.ValidationResult;
import ch.post.it.evoting.votingserver.authentication.services.domain.model.validation.ChallengeInformationValidation;
import ch.post.it.evoting.votingserver.commons.beans.challenge.ChallengeInformation;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.logging.service.I18nLoggerMessages;

/**
 * This service provides the functionality to validate a challenge information based on the
 * predefined rules.
 */
@Stateless
public class ChallengeInformationValidationService {

	private static final I18nLoggerMessages I18N = I18nLoggerMessages.getInstance();
	private static final Logger LOGGER = LoggerFactory.getLogger(ChallengeInformationValidationService.class);
	private final Map<Integer, ChallengeInformationValidation> validations = new TreeMap<>();

	@Inject
	@Any
	void setValidations(Instance<ChallengeInformationValidation> instance) {
		for (ChallengeInformationValidation validation : instance) {
			validations.put(validation.getOrder(), validation);
		}
	}

	/**
	 * This method validates an challenge information by applying the set of validation rules.
	 *
	 * @param tenantId             - the tenant identifier.
	 * @param electionEventId      - the election event identifier.
	 * @param votingCardId         - the voting card identifier.
	 * @param challengeInformation - the challenge information to be validated.
	 * @return an AuthenticationTokenValidationResult containing the result of the validation.
	 * @throws ResourceNotFoundException
	 * @throws CryptographicOperationException
	 */
	public ValidationResult validate(String tenantId, String electionEventId, String votingCardId, ChallengeInformation challengeInformation)
			throws ResourceNotFoundException, CryptographicOperationException {
		LOGGER.info(I18N.getMessage("ChallengeInformationValidationService.validate.start"));

		// result of validation
		ValidationResult validationResult = new ValidationResult();
		validationResult.setResult(true);

		// execute validations
		for (ChallengeInformationValidation validation : validations.values()) {
			if (validation.execute(tenantId, electionEventId, votingCardId, challengeInformation)) {
				LOGGER.info(I18N.getMessage("ChallengeInformationValidationService.validate.validationSuccess"),
						validation.getClass().getSimpleName());
			} else {
				LOGGER.info(I18N.getMessage("ChallengeInformationValidationService.validate.validationFail"), validation.getClass().getSimpleName());
				validationResult.setResult(false);
				break;
			}
		}

		LOGGER.info(I18N.getMessage("ChallengeInformationValidationService.validate.resultOfValidation"), validationResult.isResult());

		return validationResult;
	}
}
